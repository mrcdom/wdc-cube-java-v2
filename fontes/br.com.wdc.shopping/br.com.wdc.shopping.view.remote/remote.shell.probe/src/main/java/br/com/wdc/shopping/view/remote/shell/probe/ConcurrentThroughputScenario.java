package br.com.wdc.shopping.view.remote.shell.probe;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.shopping.view.remote.shell.probe.shopping.presentation.BrowserPresenterClient;
import br.com.wdc.shopping.view.remote.shell.probe.shopping.presentation.LoginPresenterClient;
import br.com.wdc.shopping.view.remote.shell.probe.shopping.presentation.ProductsPanelPresenterClient;
import br.com.wdc.shopping.view.remote.shell.probe.shopping.presentation.ShoppingRoutes;

/**
 * Mede a capacidade de atendimento concorrente do servidor Host/Shell.
 *
 * <h3>Fases</h3>
 * <ol>
 *   <li><b>Warmup</b> — abre N sessões, faz login em todas, navega uma vez cada uma.
 *       Aquece o JIT e os caches antes de medir.</li>
 *   <li><b>Sustained load</b> — todas as N sessões executam o worker loop
 *       simultaneamente por {@code holdSeconds} segundos. Os workers são
 *       coordenados por um {@link CountDownLatch} para saírem juntos.</li>
 *   <li><b>Report</b> — agrega histograma de latência, throughput, erros e heap
 *       do servidor. O heap é amostrado a cada 5 s em thread separada.</li>
 * </ol>
 *
 * <h3>Modo de envio</h3>
 * <ul>
 *   <li><b>fire-and-wait (padrão)</b> — cada worker espera a resposta antes de
 *       enviar a próxima requisição. O throughput total reflete a capacidade real
 *       do servidor para o RTT observado.</li>
 *   <li><b>fixed-rate</b> — cada worker envia no máximo {@code ratePerSession} req/s,
 *       independentemente do RTT. Permite descobrir o ponto de saturação.
 *       Ativado via argumento {@code --mode=rate:<req/s-por-sessão>} (ex: {@code --mode=rate:2}).</li>
 * </ul>
 *
 * <h3>Worker loop</h3>
 * Cada sessão executa em loop:
 * <ol>
 *   <li>Navigate → Products (lista produtos)</li>
 *   <li>Navigate → Product detail (abre primeiro produto disponível)</li>
 *   <li>Navigate → Cart (lista itens do carrinho)</li>
 *   <li>Navigate → Home (volta ao início)</li>
 * </ol>
 *
 * <h3>Pré-requisitos</h3>
 * <ul>
 *   <li>Host rodando em {@code http://localhost:8080} com {@code server.devMode=true}</li>
 *   <li>{@code server.sessionTtlSeconds} ≥ {@code holdSeconds} + 60 s para evitar expiração durante o teste</li>
 *   <li>Usuário {@code admin/admin} existente (banco no estado pós-DBReset)</li>
 *   <li>H2 em modo TCP (externo) para isolar o heap do banco</li>
 * </ul>
 *
 * <h3>Configuração de JVM recomendada para o servidor</h3>
 * <pre>
 *   -Xms4g -Xmx10g
 *   -XX:+UseZGC -XX:+ZGenerational
 *   -XX:SoftMaxHeapSize=9g
 *   -XX:ZCollectionInterval=5
 *   -XX:MaxMetaspaceSize=512m
 *   -XX:ReservedCodeCacheSize=256m
 * </pre>
 *
 * <h3>Execução</h3>
 * <pre>
 *   mvn exec:java -pl ...remote.shell.probe-client \
 *     -Dexec.mainClass=br.com.wdc.shopping.view.remote.javaclient.scenario.ConcurrentThroughputScenario \
 *     [-Dexec.args="http://localhost:8080 500 30 [--mode=rate:2]"]
 *
 *   Argumentos: &lt;serverUrl&gt; &lt;sessions&gt; &lt;holdSeconds&gt; [--mode=rate:&lt;req/s&gt;]
 *   Defaults   : http://localhost:8080  100  30
 * </pre>
 */
public class ConcurrentThroughputScenario {

    static final PrintStream out = System.out; // NOSONAR
    static final PrintStream err = System.err; // NOSONAR

    private static final int DEFAULT_SESSIONS    = 100;
    private static final int DEFAULT_HOLD_SECS   = 30;
    private static final int HEAP_SAMPLE_SECS    = 5;

    /**
     * Latency histogram buckets (upper bound in ms, inclusive).
     * The last bucket captures everything above the previous bound.
     */
    private static final int[] LATENCY_BUCKETS_MS = { 5, 10, 20, 50, 100, 200, 500, 1000, 5000 };

    // :: Entry point

    public static void main(String[] args) throws Exception {
        String serverUrl  = args.length > 0 ? args[0] : "http://localhost:8080";
        int    sessions   = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_SESSIONS;
        int    holdSecs   = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_HOLD_SECS;

        // --mode=rate:<req/s-per-session>  or "fire-and-wait" (default)
        double ratePerSession = 0; // 0 = fire-and-wait
        for (String arg : args) {
            if (arg.startsWith("--mode=rate:")) {
                ratePerSession = Double.parseDouble(arg.substring("--mode=rate:".length()));
            }
        }

        String modeLabel = ratePerSession > 0
                ? String.format("fixed-rate (%.1f req/s per session)", ratePerSession)
                : "fire-and-wait";

        out.println("╔════════════════════════════════════════════════════════════════╗");
        out.println("║           ConcurrentThroughputScenario — Host/Shell            ║");
        out.println("╠════════════════════════════════════════════════════════════════╣");
        out.printf ("║  Server   : %-50s ║%n", serverUrl);
        out.printf ("║  Sessions : %-50d ║%n", sessions);
        out.printf ("║  Hold     : %-50s ║%n", holdSecs + " s");
        out.printf ("║  Mode     : %-50s ║%n", modeLabel);
        out.println("╚════════════════════════════════════════════════════════════════╝");
        out.println();

        new ConcurrentThroughputScenario().run(serverUrl, sessions, holdSecs, ratePerSession);
    }

    // :: Core

    public void run(String serverUrl, int sessions, int holdSecs, double ratePerSession)
            throws Exception {

        // ── Phase 1: Warmup ───────────────────────────────────────────────────
        out.printf("[warmup] Opening %d sessions and warming up (login + navigate)...%n", sessions);
        List<HostClient> clients = new ArrayList<>(sessions);
        try {
            for (int i = 0; i < sessions; i++) {
                clients.add(openWarmedSession(serverUrl));
                if ((i + 1) % 50 == 0) {
                    out.printf("  ... %d/%d ready%n", i + 1, sessions);
                }
            }
        } catch (Exception e) {
            out.printf("[error] Warmup failed after %d sessions: %s%n", clients.size(), e.getMessage());
            closeAll(clients);
            return;
        }
        out.printf("[warmup] %d sessions ready.%n%n", sessions);

        // ── Phase 2: Sustained load ───────────────────────────────────────────
        var metrics = new Metrics();
        var startGun = new CountDownLatch(1);
        var stop     = new AtomicBoolean(false);

        // Heap sampler (runs during sustained phase)
        var heapSampler = Thread.ofVirtual().start(() ->
                sampleHeapLoop(serverUrl, metrics, stop));

        // Worker threads (one virtual thread per session)
        var workersDone = new CountDownLatch(sessions);
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var client : clients) {
                final double rate = ratePerSession;
                exec.submit(() -> {
                    try {
                        startGun.await();
                        if (rate > 0) {
                            runFixedRate(client, metrics, stop, rate);
                        } else {
                            runFireAndWait(client, metrics, stop);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        workersDone.countDown();
                    }
                    return null;
                });
            }

            out.printf("[load] Starting sustained load (%d s)...%n", holdSecs);
            long startMs = System.currentTimeMillis();
            startGun.countDown(); // fire!

            // Live progress loop
            long nextReport = startMs + HEAP_SAMPLE_SECS * 1000L;
            long elapsed = 0;
            while ((elapsed = System.currentTimeMillis() - startMs) < holdSecs * 1000L) {
                if (System.currentTimeMillis() >= nextReport) {
                    printLive(metrics, elapsed, sessions);
                    nextReport += HEAP_SAMPLE_SECS * 1000L;
                }
                Thread.sleep(500);
            }

            stop.set(true);
            workersDone.await();
            heapSampler.join(10_000);
        } finally {
            closeAll(clients);
        }

        // ── Phase 3: Report ───────────────────────────────────────────────────
        long totalMs = metrics.totalDurationMs();
        printReport(metrics, sessions, holdSecs, ratePerSession, totalMs);
    }

    // :: Worker loops

    /**
     * Fire-and-wait: each step waits for the server response before proceeding.
     * Represents a user interaction pace driven by server RTT.
     */
    private static void runFireAndWait(HostClient client, Metrics metrics, AtomicBoolean stop) {
        while (!stop.get()) {
            try {
                step_navigateProducts(client, metrics);
                if (stop.get()) break;
                step_openFirstProduct(client, metrics);
                if (stop.get()) break;
                step_navigateCart(client, metrics);
                if (stop.get()) break;
                step_navigateHome(client, metrics);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                metrics.recordError();
            }
        }
    }

    /**
     * Fixed-rate: each session targets {@code ratePerSession} req/s.
     * If the server is slower than the target rate, requests queue up and latency rises.
     */
    private static void runFixedRate(HostClient client, Metrics metrics,
                                     AtomicBoolean stop, double ratePerSession) {
        long intervalNs = (long) (1_000_000_000.0 / ratePerSession);
        long nextDeadline = System.nanoTime();
        while (!stop.get()) {
            long now = System.nanoTime();
            if (now < nextDeadline) {
                long sleepNs = nextDeadline - now;
                try {
                    Thread.sleep(sleepNs / 1_000_000, (int)(sleepNs % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            nextDeadline += intervalNs;
            try {
                step_navigateProducts(client, metrics);
                if (stop.get()) break;
                step_openFirstProduct(client, metrics);
                if (stop.get()) break;
                step_navigateCart(client, metrics);
                if (stop.get()) break;
                step_navigateHome(client, metrics);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                metrics.recordError();
            }
        }
    }

    // :: Worker steps

    private static void step_navigateProducts(HostClient client, Metrics metrics)
            throws Exception {
        long t0 = System.currentTimeMillis();
        client.navigate(ShoppingRoutes.restrict_home());
        metrics.record(System.currentTimeMillis() - t0);
    }

    private static void step_openFirstProduct(HostClient client, Metrics metrics)
            throws Exception {
        // Read the first product id from the current products panel state.
        // If the state is unavailable, skip gracefully (still counts as a step).
        var panel = ProductsPanelPresenterClient.getFirst(client);
        if (panel.isEmpty()) return;
        var products = panel.get().products();
        if (products.isEmpty()) return;
        Object rawId = products.get(0).get("productId");
        if (rawId == null) return;
        long productId = ((Number) rawId).longValue();

        long t0 = System.currentTimeMillis();
        client.navigate(ShoppingRoutes.restrict_home_product(productId));
        metrics.record(System.currentTimeMillis() - t0);
    }

    private static void step_navigateCart(HostClient client, Metrics metrics)
            throws Exception {
        long t0 = System.currentTimeMillis();
        client.navigate(ShoppingRoutes.restrict_home_cart());
        metrics.record(System.currentTimeMillis() - t0);
    }

    private static void step_navigateHome(HostClient client, Metrics metrics)
            throws Exception {
        long t0 = System.currentTimeMillis();
        client.navigate(ShoppingRoutes.restrict_home());
        metrics.record(System.currentTimeMillis() - t0);
    }

    // :: Warmup

    private static HostClient openWarmedSession(String serverUrl) throws Exception {
        var client = HostClient.connect(serverUrl);
        try {
            client.awaitResponse();
            var root = BrowserPresenterClient.get(client).root();
            client.navigate(ShoppingRoutes.open_login());
            root.pageAs(LoginPresenterClient.class).onEnter("admin", "admin");
            // one navigation cycle to warm up the presenter tree
            client.navigate(ShoppingRoutes.restrict_home());
            return client;
        } catch (Exception e) {
            try { client.close(); } catch (Exception ignored) {}
            throw e;
        }
    }

    // :: Heap sampling

    private static void sampleHeapLoop(String serverUrl, Metrics metrics, AtomicBoolean stop) {
        var http = java.net.http.HttpClient.newHttpClient();
        while (!stop.get()) {
            try {
                Thread.sleep(HEAP_SAMPLE_SECS * 1000L);
                if (stop.get()) break;
                var req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(serverUrl + "/__dev/heap"))
                        .GET().build();
                var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    long used = extractLong(resp.body(), "usedBytes");
                    if (used > 0) metrics.recordHeap(used);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
                // heap sampling is best-effort
            }
        }
    }

    // :: Output

    private static void printLive(Metrics metrics, long elapsedMs, int sessions) {
        long reqs  = metrics.totalRequests();
        long errs  = metrics.totalErrors();
        double thr = reqs / (elapsedMs / 1000.0);
        long p50   = metrics.percentile(50);
        long p99   = metrics.percentile(99);
        long heap  = metrics.lastHeap();
        String heapStr = heap > 0 ? formatBytes(heap) : "n/a";
        out.printf("[live] %3ds | req: %,7d | err: %,4d | thr: %,6.0f req/s | p50: %4d ms | p99: %5d ms | heap: %s%n",
                elapsedMs / 1000, reqs, errs, thr, p50, p99, heapStr);
    }

    private static void printReport(Metrics metrics, int sessions, int holdSecs,
                                    double ratePerSession, long totalMs) {
        long   total   = metrics.totalRequests();
        long   errors  = metrics.totalErrors();
        double thr     = total / (holdSecs);
        long   p50     = metrics.percentile(50);
        long   p75     = metrics.percentile(75);
        long   p95     = metrics.percentile(95);
        long   p99     = metrics.percentile(99);
        long   pMax    = metrics.maxLatency();
        long   heapPeak = metrics.peakHeap();
        String modeStr  = ratePerSession > 0
                ? String.format("fixed-rate %.1f req/s/session", ratePerSession)
                : "fire-and-wait";

        out.println();
        out.println("══════════════════════════════════════════════════════════════════════");
        out.println("  RESULT");
        out.println("══════════════════════════════════════════════════════════════════════");
        out.printf ("  Sessions       : %d%n", sessions);
        out.printf ("  Hold           : %d s%n", holdSecs);
        out.printf ("  Mode           : %s%n", modeStr);
        out.printf ("  Total requests : %,d%n", total);
        out.printf ("  Errors         : %,d (%.2f%%)%n", errors, total > 0 ? errors * 100.0 / total : 0.0);
        out.println();
        out.printf ("  Throughput     : %,.0f req/s%n", thr);
        out.println();
        out.println("  Latency");
        out.printf ("    p50          : %,d ms%n", p50);
        out.printf ("    p75          : %,d ms%n", p75);
        out.printf ("    p95          : %,d ms%n", p95);
        out.printf ("    p99          : %,d ms%n", p99);
        out.printf ("    max          : %,d ms%n", pMax);
        out.println();
        if (heapPeak > 0) {
            out.printf ("  Heap peak      : %s%n", formatBytes(heapPeak));
        }
        out.println();
        out.println("  Latency distribution");
        out.printf ("  %-14s  %10s  %10s%n", "Bucket (ms)", "Count", "% of total");
        out.printf ("  %-14s  %10s  %10s%n", "──────────────", "──────────", "──────────");
        long[] hist = metrics.histogram();
        for (int i = 0; i < LATENCY_BUCKETS_MS.length; i++) {
            long count = hist[i];
            String label = (i == 0)
                    ? "<= " + LATENCY_BUCKETS_MS[i]
                    : LATENCY_BUCKETS_MS[i - 1] + "–" + LATENCY_BUCKETS_MS[i];
            out.printf("  %-14s  %,10d  %9.1f%%%n",
                    label, count, total > 0 ? count * 100.0 / total : 0.0);
        }
        long overflow = hist[LATENCY_BUCKETS_MS.length];
        out.printf("  %-14s  %,10d  %9.1f%%%n",
                "> " + LATENCY_BUCKETS_MS[LATENCY_BUCKETS_MS.length - 1],
                overflow, total > 0 ? overflow * 100.0 / total : 0.0);
        out.println("══════════════════════════════════════════════════════════════════════");
    }

    // :: Metrics

    /**
     * Thread-safe metrics accumulator using {@link LongAdder} for counters and
     * a lock-free histogram (one {@link LongAdder} per bucket).
     */
    static final class Metrics {

        private final LongAdder  requestCount = new LongAdder();
        private final LongAdder  errorCount   = new LongAdder();
        private final AtomicLong maxLatencyMs = new AtomicLong(0);
        private final AtomicLong lastHeapUsed = new AtomicLong(0);
        private final AtomicLong peakHeapUsed = new AtomicLong(0);
        private final long       startMs      = System.currentTimeMillis();

        // Histogram: one bucket per LATENCY_BUCKETS_MS entry + overflow bucket
        private final LongAdder[] hist = new LongAdder[LATENCY_BUCKETS_MS.length + 1];

        Metrics() {
            for (int i = 0; i < hist.length; i++) hist[i] = new LongAdder();
        }

        void record(long latencyMs) {
            requestCount.increment();
            // update max lock-free
            long cur;
            do { cur = maxLatencyMs.get(); }
            while (latencyMs > cur && !maxLatencyMs.compareAndSet(cur, latencyMs));
            // histogram
            int bucket = hist.length - 1; // overflow
            for (int i = 0; i < LATENCY_BUCKETS_MS.length; i++) {
                if (latencyMs <= LATENCY_BUCKETS_MS[i]) { bucket = i; break; }
            }
            hist[bucket].increment();
        }

        void recordError() { errorCount.increment(); }

        void recordHeap(long bytes) {
            lastHeapUsed.set(bytes);
            long cur;
            do { cur = peakHeapUsed.get(); }
            while (bytes > cur && !peakHeapUsed.compareAndSet(cur, bytes));
        }

        long totalRequests()  { return requestCount.sum(); }
        long totalErrors()    { return errorCount.sum(); }
        long maxLatency()     { return maxLatencyMs.get(); }
        long lastHeap()       { return lastHeapUsed.get(); }
        long peakHeap()       { return peakHeapUsed.get(); }
        long totalDurationMs(){ return System.currentTimeMillis() - startMs; }

        long[] histogram() {
            return Arrays.stream(hist).mapToLong(LongAdder::sum).toArray();
        }

        /** Approximate percentile from the histogram buckets. */
        long percentile(int pct) {
            long total = totalRequests();
            if (total == 0) return 0;
            long target = (long) Math.ceil(total * pct / 100.0);
            long cum = 0;
            for (int i = 0; i < LATENCY_BUCKETS_MS.length; i++) {
                cum += hist[i].sum();
                if (cum >= target) return LATENCY_BUCKETS_MS[i];
            }
            return maxLatencyMs.get();
        }
    }

    // :: Utilities

    private static void closeAll(List<HostClient> clients) {
        for (var c : clients) {
            try { c.close(); } catch (Exception ignored) {}
        }
        clients.clear();
    }

    private static long extractLong(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return -1;
        String after = json.substring(idx + key.length()).replaceAll("[^0-9]", " ").trim();
        String[] parts = after.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return -1;
        try { return Long.parseLong(parts[0]); } catch (NumberFormatException e) { return -1; }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
