package br.com.wdc.shopping.backend.controller;

import java.lang.management.ManagementFactory;

import io.javalin.config.JavalinConfig;

/**
 * Dev-only endpoint that requests a JVM garbage collection cycle and waits
 * for it to reduce the heap before returning the updated heap snapshot.
 * <p>
 * Used by {@code MemoryPerSessionScenario} to establish a stable, post-GC
 * baseline before each measurement round, preventing cross-round heap drift
 * from skewing the per-session delta.
 *
 * <pre>
 *   POST /__dev/gc
 *   → 200 {"usedBytes": 134217728, "maxBytes": 10737418240, "gcRequested": true}
 * </pre>
 *
 * <p>
 * The endpoint calls {@code System.gc()} (a suggestion — the JVM may ignore it
 * or defer it) and then sleeps 1 s to give ZGC / G1 time to complete at least
 * one concurrent cycle before measuring and returning.  It is intentionally
 * simple: this is a load-test utility, not production code.
 */
public final class DevGcController {

    /**
     * Milliseconds to wait after requesting GC before reading the heap.
     * <p>
     * ZGC runs concurrently: a full cycle on a 10 GB heap can take 3–5 s.
     * Sleeping 4 s after {@code System.gc()} gives ZGC time to complete at
     * least one full concurrent cycle so the snapshot reflects a post-GC state.
     * G1GC/Shenandoah complete faster (~1 s), so 4 s is safe for all collectors.
     */
    private static final int GC_SETTLE_MS = 4_000;

    private DevGcController() {
    }

    public static void configure(JavalinConfig config) {
        config.routes.post("/__dev/gc", ctx -> {
            // Two hints improve coverage: the first triggers the cycle, the
            // second collects objects finalized during the first cycle.
            System.gc(); // NOSONAR – intentional, dev/test endpoint only
            Thread.sleep(GC_SETTLE_MS);
            System.gc(); // NOSONAR – second pass for objects finalized in first cycle
            Thread.sleep(GC_SETTLE_MS / 4L); // short settle for the second pass
            var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            ctx.status(200).json(java.util.Map.of(
                    "gcRequested", true,
                    "usedBytes",   heap.getUsed(),
                    "maxBytes",    heap.getMax()));
        });
    }
}
