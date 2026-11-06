package io.gentics.mesh.mcp.server;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Single chokepoint that protects Gentics Mesh from being overwhelmed by
 * an unbounded number of MCP tool calls.
 *
 * <p>Three independent guards apply in this order:
 * <ol>
 *   <li><strong>Rate limit</strong> – token bucket per Bearer token <em>and</em> a
 *       global bucket. Each tool consumes a weighted number of permits
 *       (e.g. {@code mesh_run_graphql} costs 5, {@code mesh_list_projects} costs 1).</li>
 *   <li><strong>Bulkhead</strong> – a fixed number of permits caps the number of
 *       in-flight Mesh requests, regardless of which tool issues them. Long-running
 *       GraphQL queries cannot starve the rest of the system.</li>
 *   <li><strong>Circuit breaker</strong> – when the failure rate over the
 *       sliding window exceeds the configured percentage, the breaker opens
 *       and fails fast for a cooldown period. This prevents the MCP server from
 *       piling more load on an already-struggling Mesh.</li>
 * </ol>
 *
 * <p>All three steps fail fast: there is no internal queueing, no retry, no
 * waiting. A denied call surfaces as an {@link AppError} with one of
 * {@link ErrorCode#RATE_LIMITED}, {@link ErrorCode#BUSY}, or
 * {@link ErrorCode#CIRCUIT_OPEN} – the HTTP transport translates these to
 * 429 / 503 with a {@code Retry-After} header.
 *
 * @since 0.3.0
 */
public final class MeshCallGuard {

    private static final Logger log = LoggerFactory.getLogger(MeshCallGuard.class);

    /**
     * Per-tool weight (= rate-limit permits) reflecting approximate Mesh load.
     * Increasing a number throttles that tool more aggressively per second.
     */
    public static final Map<String, Integer> TOOL_WEIGHTS = Map.of(
            "mesh_list_projects", 1,
            "mesh_list_schemas",  1,
            "mesh_get_node",      2,
            "mesh_search_nodes",  3,
            "mesh_get_webroot",   2,
            "mesh_run_graphql",   5
    );

    private final MeshConfig          config;
    private final Metrics             metrics;
    private final RateLimiter         globalLimiter;
    private final Map<String, RateLimiter> perTokenLimiters = new ConcurrentHashMap<>();
    private final Bulkhead            bulkhead;
    private final CircuitBreaker      circuitBreaker;
    private final boolean             breakerEnabled;

    public MeshCallGuard(MeshConfig config, Metrics metrics) {
        this.config         = config;
        this.metrics        = metrics;
        this.breakerEnabled = config.mcpCircuitBreakerEnabled();

        this.globalLimiter = RateLimiter.of("mesh-global",
                RateLimiterConfig.custom()
                        .limitForPeriod(config.mcpRateLimitPerSecond())
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ZERO) // fail-fast
                        .build());

        this.bulkhead = Bulkhead.of("mesh-client",
                BulkheadConfig.custom()
                        .maxConcurrentCalls(config.mcpMeshMaxConcurrent())
                        .maxWaitDuration(Duration.ZERO) // fail-fast
                        .build());

        this.circuitBreaker = CircuitBreaker.of("mesh-client",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(config.mcpCircuitBreakerWindowSize())
                        .minimumNumberOfCalls(Math.min(5, config.mcpCircuitBreakerWindowSize()))
                        .failureRateThreshold(config.mcpCircuitBreakerFailureRatePct())
                        .waitDurationInOpenState(Duration.ofMillis(config.mcpCircuitBreakerOpenDurationMs()))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .recordExceptions(Throwable.class)
                        // Don't trip the breaker for client-side validation/forbidden errors –
                        // those aren't Mesh's fault. Only Mesh-side failures count.
                        .ignoreExceptions(ClientFault.class)
                        .build());

        // Bridge breaker state into a Prometheus gauge.
        circuitBreaker.getEventPublisher().onStateTransition(event -> {
            metrics.setCircuitBreakerState(event.getStateTransition().getToState().name());
            log.info("CircuitBreaker '{}' transitioned: {} → {}",
                    circuitBreaker.getName(),
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState());
        });
        metrics.setCircuitBreakerState(circuitBreaker.getState().name());
    }

    /**
     * Executes the given Mesh call under all three guards.
     *
     * @param toolName the MCP tool being invoked – determines the rate-limit weight
     * @param tokenKey a stable key per client (e.g. SHA-256 of the Bearer token,
     *                 {@code "stdio"} for the stdio transport, or {@code "no-auth"}
     *                 when auth is disabled). Used to scope per-token rate-limiters.
     * @param call     the work that actually talks to Mesh (use-case execution)
     * @return whatever the call returns
     * @throws AppError(RATE_LIMITED) when the bucket is empty
     * @throws AppError(BUSY)         when the bulkhead is full
     * @throws AppError(CIRCUIT_OPEN) when the breaker has opened
     * @throws AppError               propagated from the call itself
     * @throws RuntimeException       any other failure from the call (also feeds the breaker)
     */
    public <T> T execute(String toolName, String tokenKey, Supplier<T> call) {
        int weight = TOOL_WEIGHTS.getOrDefault(toolName, 1);

        // 1) Per-token rate-limit (omit for stdio: there's only one client by design)
        if (tokenKey != null && !tokenKey.equals("stdio")) {
            RateLimiter perToken = perTokenLimiters.computeIfAbsent(tokenKey, k -> RateLimiter.of(
                    "mesh-token-" + k.substring(0, Math.min(8, k.length())),
                    RateLimiterConfig.custom()
                            .limitForPeriod(config.mcpRateLimitPerTokenPerSecond())
                            .limitRefreshPeriod(Duration.ofSeconds(1))
                            .timeoutDuration(Duration.ZERO)
                            .build()));
            // acquirePermission() *returns* false when the bucket is empty – it does not
            // throw. Ignoring the result would disable rate-limiting entirely.
            if (!perToken.acquirePermission(weight)) {
                metrics.recordRateLimitRejected("token", toolName);
                throw new AppError(ErrorCode.RATE_LIMITED,
                        "Token rate-limit überschritten (Tool '" + toolName + "', Weight " + weight + ").");
            }
        }

        // 2) Global rate-limit
        if (!globalLimiter.acquirePermission(weight)) {
            metrics.recordRateLimitRejected("global", toolName);
            throw new AppError(ErrorCode.RATE_LIMITED,
                    "Globales Rate-Limit überschritten (Tool '" + toolName + "', Weight " + weight + ").");
        }

        // 3) Bulkhead
        if (!bulkhead.tryAcquirePermission()) {
            metrics.recordConcurrencyRejected(toolName);
            throw new AppError(ErrorCode.BUSY,
                    "Mesh ist ausgelastet (max " + config.mcpMeshMaxConcurrent() + " parallele Calls).");
        }

        try {
            // 4) Circuit breaker
            if (breakerEnabled) {
                return circuitBreaker.executeSupplier(() -> {
                    try {
                        return call.get();
                    } catch (AppError ae) {
                        // Mark client-side errors as "not Mesh's fault" so they don't trip the breaker.
                        switch (ae.getCode()) {
                            case VALIDATION_ERROR:
                            case FORBIDDEN:
                            case NOT_FOUND:
                                throw new ClientFault(ae);
                            default:
                                throw ae;
                        }
                    }
                });
            } else {
                return call.get();
            }
        } catch (CallNotPermittedException e) {
            metrics.recordCircuitBreakerRejected();
            throw new AppError(ErrorCode.CIRCUIT_OPEN,
                    "Mesh-Anfragen sind aktuell ausgesetzt (Circuit Breaker offen, " +
                    "Cooldown " + config.mcpCircuitBreakerOpenDurationMs() + " ms).");
        } catch (BulkheadFullException e) {
            metrics.recordConcurrencyRejected(toolName);
            throw new AppError(ErrorCode.BUSY,
                    "Mesh ist ausgelastet (max " + config.mcpMeshMaxConcurrent() + " parallele Calls).");
        } catch (ClientFault cf) {
            // Unwrap and re-throw the original AppError so callers see the real cause.
            throw cf.appError;
        } finally {
            bulkhead.releasePermission();
        }
    }

    /**
     * Marker exception used to tell the circuit breaker that the failure was a
     * client-side problem (validation, allowlist, 404) and should not count
     * against Mesh's health metrics.
     */
    private static final class ClientFault extends RuntimeException {
        final AppError appError;
        ClientFault(AppError ae) {
            super(ae);
            this.appError = ae;
        }
    }

    // ── Accessors for tests ───────────────────────────────────────────────

    CircuitBreaker circuitBreakerForTest() { return circuitBreaker; }
    Bulkhead bulkheadForTest() { return bulkhead; }
}
