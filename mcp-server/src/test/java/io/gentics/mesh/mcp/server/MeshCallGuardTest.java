package io.gentics.mesh.mcp.server;

import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MeshCallGuard}.
 *
 * <p>The tests run against tight, deterministic configs so the limits trip
 * in a single test thread without sleeps.
 */
class MeshCallGuardTest {

    /** Builds a config with explicit guard-related values; other fields are test defaults. */
    private static MeshConfig cfg(int globalRate, int tokenRate, int maxConcurrent,
                                  boolean breakerEnabled, int failurePct, int windowSize) {
        return new MeshConfig(
                "http://localhost:8080", null, "u", "p",
                List.of("demo"), List.of(), List.of("de", "en"),
                true, 6, 5_000L, 10_000L, "info",
                "stdio", "0.0.0.0", 3001, null, true, List.of(),
                globalRate, tokenRate, maxConcurrent,
                breakerEnabled, failurePct, windowSize, 1_000L, 4_000);
    }

    @Test
    void rateLimit_perToken_tripsAfterBudgetExhausted() {
        Metrics m = new Metrics();
        // Per-token rate = 2 per second; list_projects weight = 1 → 2 calls max in 1s.
        MeshCallGuard guard = new MeshCallGuard(cfg(100, 2, 100, false, 50, 20), m);

        String ok1 = guard.execute("mesh_list_projects", "alice", () -> "ok1");
        String ok2 = guard.execute("mesh_list_projects", "alice", () -> "ok2");
        AppError ex = assertThrows(AppError.class,
                () -> guard.execute("mesh_list_projects", "alice", () -> "should-not-run"));
        assertEquals(ErrorCode.RATE_LIMITED, ex.getCode());
        assertEquals("ok1", ok1);
        assertEquals("ok2", ok2);
    }

    @Test
    void rateLimit_global_protectsAgainstManyTokens() {
        Metrics m = new Metrics();
        // Global rate = 2; per-token rate is generous so won't trip first.
        MeshCallGuard guard = new MeshCallGuard(cfg(2, 1000, 100, false, 50, 20), m);

        guard.execute("mesh_list_projects", "tokenA", () -> "ok");
        guard.execute("mesh_list_projects", "tokenB", () -> "ok");
        AppError ex = assertThrows(AppError.class,
                () -> guard.execute("mesh_list_projects", "tokenC", () -> "ok"));
        assertEquals(ErrorCode.RATE_LIMITED, ex.getCode());
    }

    @Test
    void rateLimit_perToken_skippedForStdio() {
        Metrics m = new Metrics();
        // Per-token rate = 1 but tokenKey = "stdio" → only global limiter applies.
        MeshCallGuard guard = new MeshCallGuard(cfg(100, 1, 100, false, 50, 20), m);

        // Three calls in a row would have tripped a per-token limit of 1, but stdio is exempt.
        guard.execute("mesh_list_projects", "stdio", () -> "ok");
        guard.execute("mesh_list_projects", "stdio", () -> "ok");
        guard.execute("mesh_list_projects", "stdio", () -> "ok");
    }

    @Test
    void rateLimit_toolWeight_chargesGraphqlMore() {
        Metrics m = new Metrics();
        // Per-token = 4 per second. graphql weight = 5 → first call blocks immediately.
        MeshCallGuard guard = new MeshCallGuard(cfg(100, 4, 100, false, 50, 20), m);
        AppError ex = assertThrows(AppError.class,
                () -> guard.execute("mesh_run_graphql", "alice", () -> "ok"));
        assertEquals(ErrorCode.RATE_LIMITED, ex.getCode());
    }

    @Test
    void bulkhead_tripsWhenAllPermitsHeld() throws Exception {
        Metrics m = new Metrics();
        // Max-concurrent = 1, rate limits relaxed.
        MeshCallGuard guard = new MeshCallGuard(cfg(10_000, 10_000, 1, false, 50, 20), m);

        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread t = new Thread(() -> guard.execute("mesh_list_projects", "stdio", () -> {
            inside.countDown();
            try { release.await(); } catch (InterruptedException ignored) {}
            return "ok";
        }));
        t.start();
        assertTrue(inside.await(2, java.util.concurrent.TimeUnit.SECONDS),
                "Worker thread must enter the guarded section");

        try {
            AppError ex = assertThrows(AppError.class,
                    () -> guard.execute("mesh_list_projects", "stdio", () -> "should-not-run"));
            assertEquals(ErrorCode.BUSY, ex.getCode());
        } finally {
            release.countDown();
            t.join(2_000);
        }
    }

    @Test
    void circuitBreaker_opensAfterFailureBurstAndFailsFast() {
        Metrics m = new Metrics();
        // Window of 5 calls, threshold 50%, minimum-calls = 5.
        MeshCallGuard guard = new MeshCallGuard(cfg(10_000, 10_000, 100, true, 50, 5), m);

        AtomicInteger calls = new AtomicInteger();
        // Burn 5 failures.
        for (int i = 0; i < 5; i++) {
            assertThrows(RuntimeException.class,
                    () -> guard.execute("mesh_list_projects", "alice", () -> {
                        calls.incrementAndGet();
                        throw new RuntimeException("mesh down");
                    }));
        }
        assertEquals(5, calls.get());

        // Breaker should now be OPEN — next call rejected fast without invoking the supplier.
        AppError ex = assertThrows(AppError.class,
                () -> guard.execute("mesh_list_projects", "alice", () -> {
                    calls.incrementAndGet();
                    return "should-not-run";
                }));
        assertEquals(ErrorCode.CIRCUIT_OPEN, ex.getCode());
        assertEquals(5, calls.get(), "Supplier must not run when breaker is open");
    }

    @Test
    void circuitBreaker_clientFaultsDoNotTrip() {
        Metrics m = new Metrics();
        MeshCallGuard guard = new MeshCallGuard(cfg(10_000, 10_000, 100, true, 50, 5), m);

        // 10 forbidden errors – these should NOT trip the breaker since they're client-side.
        for (int i = 0; i < 10; i++) {
            AppError ex = assertThrows(AppError.class,
                    () -> guard.execute("mesh_list_projects", "alice", () -> {
                        throw AppError.forbidden("not allowed");
                    }));
            assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        }

        // Breaker must still allow calls.
        String ok = guard.execute("mesh_list_projects", "alice", () -> "ok");
        assertEquals("ok", ok);
    }
}
