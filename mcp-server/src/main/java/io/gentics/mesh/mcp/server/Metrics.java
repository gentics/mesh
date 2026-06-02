package io.gentics.mesh.mcp.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lightweight in-memory metrics registry rendered as Prometheus text format.
 *
 * <p>Deliberately minimal: dependencies on Micrometer or the official
 * Prometheus client would increase the fat-JAR size noticeably for a single
 * scrape endpoint. The counters below cover the questions operators
 * typically ask first ("is the server alive?", "how many tool calls?",
 * "how many errors?", "what's the latency?", "are protection layers tripping?").
 *
 * @since 0.2.0
 */
public final class Metrics {

    private final AtomicLong startupTimestamp = new AtomicLong(System.currentTimeMillis());
    private final Map<String, AtomicLong> toolCalls         = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> toolErrors        = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> toolDurationMs    = new ConcurrentHashMap<>();
    private final AtomicLong httpRequests   = new AtomicLong(0);
    private final AtomicLong httpAuthFailed = new AtomicLong(0);

    // ── Protection-layer counters ─────────────────────────────────────────
    /** Key: scope + "|" + toolName, e.g. "global|mesh_get_node". */
    private final Map<String, AtomicLong> rateLimitRejected   = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> concurrencyRejected = new ConcurrentHashMap<>();
    private final AtomicLong circuitBreakerRejected = new AtomicLong(0);
    private final AtomicReference<String> circuitBreakerState = new AtomicReference<>("CLOSED");

    /** Records a successful or failed tool invocation. */
    public void recordToolCall(String toolName, long durationMs, boolean error) {
        toolCalls.computeIfAbsent(toolName, k -> new AtomicLong()).incrementAndGet();
        toolDurationMs.computeIfAbsent(toolName, k -> new AtomicLong()).addAndGet(durationMs);
        if (error) {
            toolErrors.computeIfAbsent(toolName, k -> new AtomicLong()).incrementAndGet();
        }
    }

    public void incrementHttpRequests()   { httpRequests.incrementAndGet(); }
    public void incrementHttpAuthFailed() { httpAuthFailed.incrementAndGet(); }

    public void recordRateLimitRejected(String scope, String toolName) {
        rateLimitRejected
                .computeIfAbsent(scope + "|" + toolName, k -> new AtomicLong())
                .incrementAndGet();
    }

    public void recordConcurrencyRejected(String toolName) {
        concurrencyRejected
                .computeIfAbsent(toolName, k -> new AtomicLong())
                .incrementAndGet();
    }

    public void recordCircuitBreakerRejected() {
        circuitBreakerRejected.incrementAndGet();
    }

    public void setCircuitBreakerState(String state) {
        circuitBreakerState.set(state);
    }

    /**
     * Renders all counters in Prometheus text exposition format (version 0.0.4).
     */
    public String renderPrometheus() {
        StringBuilder sb = new StringBuilder(2048);

        sb.append("# HELP mcp_uptime_seconds Time since server startup\n")
          .append("# TYPE mcp_uptime_seconds gauge\n")
          .append("mcp_uptime_seconds ")
          .append((System.currentTimeMillis() - startupTimestamp.get()) / 1000.0)
          .append('\n');

        sb.append("# HELP mcp_http_requests_total Total HTTP requests received\n")
          .append("# TYPE mcp_http_requests_total counter\n")
          .append("mcp_http_requests_total ").append(httpRequests.get()).append('\n');

        sb.append("# HELP mcp_http_auth_failed_total HTTP requests rejected by auth\n")
          .append("# TYPE mcp_http_auth_failed_total counter\n")
          .append("mcp_http_auth_failed_total ").append(httpAuthFailed.get()).append('\n');

        sb.append("# HELP mcp_tool_calls_total Total tool calls per tool\n")
          .append("# TYPE mcp_tool_calls_total counter\n");
        toolCalls.forEach((name, count) ->
            sb.append("mcp_tool_calls_total{tool=\"").append(name).append("\"} ")
              .append(count.get()).append('\n'));

        sb.append("# HELP mcp_tool_errors_total Total tool errors per tool\n")
          .append("# TYPE mcp_tool_errors_total counter\n");
        toolErrors.forEach((name, count) ->
            sb.append("mcp_tool_errors_total{tool=\"").append(name).append("\"} ")
              .append(count.get()).append('\n'));

        sb.append("# HELP mcp_tool_duration_ms_total Total milliseconds spent per tool\n")
          .append("# TYPE mcp_tool_duration_ms_total counter\n");
        toolDurationMs.forEach((name, ms) ->
            sb.append("mcp_tool_duration_ms_total{tool=\"").append(name).append("\"} ")
              .append(ms.get()).append('\n'));

        // ── Protection layers ─────────────────────────────────────────────
        sb.append("# HELP mcp_rate_limit_rejected_total Calls rejected by the token-bucket rate limiter\n")
          .append("# TYPE mcp_rate_limit_rejected_total counter\n");
        rateLimitRejected.forEach((key, count) -> {
            int sep = key.indexOf('|');
            String scope = sep > 0 ? key.substring(0, sep) : "unknown";
            String tool  = sep > 0 ? key.substring(sep + 1) : key;
            sb.append("mcp_rate_limit_rejected_total{scope=\"").append(scope)
              .append("\",tool=\"").append(tool).append("\"} ")
              .append(count.get()).append('\n');
        });

        sb.append("# HELP mcp_concurrency_rejected_total Calls rejected because the bulkhead was full\n")
          .append("# TYPE mcp_concurrency_rejected_total counter\n");
        concurrencyRejected.forEach((tool, count) ->
            sb.append("mcp_concurrency_rejected_total{tool=\"").append(tool).append("\"} ")
              .append(count.get()).append('\n'));

        sb.append("# HELP mcp_circuit_breaker_rejected_total Calls rejected because the breaker was open\n")
          .append("# TYPE mcp_circuit_breaker_rejected_total counter\n")
          .append("mcp_circuit_breaker_rejected_total ").append(circuitBreakerRejected.get()).append('\n');

        // Gauge: 1 for the current state, 0 for the others. Pattern used by Resilience4j's
        // own Prometheus binding so dashboards can build state-aware alerts.
        sb.append("# HELP mcp_circuit_breaker_state Current circuit breaker state (1 = active)\n")
          .append("# TYPE mcp_circuit_breaker_state gauge\n");
        String current = circuitBreakerState.get();
        for (String s : new String[]{"CLOSED", "OPEN", "HALF_OPEN", "FORCED_OPEN", "DISABLED"}) {
            sb.append("mcp_circuit_breaker_state{state=\"").append(s).append("\"} ")
              .append(s.equals(current) ? 1 : 0).append('\n');
        }

        return sb.toString();
    }
}
