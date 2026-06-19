package br.com.wdc.cube.backend.controller;

import java.lang.management.ManagementFactory;
import java.util.Map;

import io.javalin.config.JavalinConfig;

/**
 * Dev-only endpoint that reports the server's current JVM heap usage.
 * <p>
 * Used by {@code MemoryPerSessionScenario} to measure server-side memory
 * consumption per session without requiring a JMX connection.
 *
 * <pre>
 *   GET /__dev/heap
 *   → 200 {"usedBytes": 134217728, "maxBytes": 10737418240, "committedBytes": 268435456}
 * </pre>
 */
public final class DevHeapController {

    private DevHeapController() {
    }

    public static void configure(JavalinConfig config) {
        config.routes.get("/__dev/heap", ctx -> {
            var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            ctx.status(200).json(Map.of(
                    "usedBytes", heap.getUsed(),
                    "maxBytes", heap.getMax(),
                    "committedBytes", heap.getCommitted()));
        });
    }
}
