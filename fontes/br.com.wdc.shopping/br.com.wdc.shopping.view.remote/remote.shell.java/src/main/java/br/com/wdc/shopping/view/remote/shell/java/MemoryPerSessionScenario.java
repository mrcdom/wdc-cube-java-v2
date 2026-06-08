package br.com.wdc.shopping.view.remote.shell.java;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.shopping.view.remote.shell.java.shopping.presentation.BrowserPresenterClient;
import br.com.wdc.shopping.view.remote.shell.java.shopping.presentation.CartPresenterClient;
import br.com.wdc.shopping.view.remote.shell.java.shopping.presentation.LoginPresenterClient;
import br.com.wdc.shopping.view.remote.shell.java.shopping.presentation.ShoppingRoutes;

/**
 * Mede o consumo de memória heap por sessão ativa no servidor em duas fases:
 *
 * <ol>
 *   <li><b>Idle footprint</b> — sessões abertas e autenticadas, sem navegação. Heap medido
 *       sem GC logo após o login. Captura os objetos de longa vida criados pela sessão
 *       (presenter tree, contexto de segurança, etc.).</li>
 *   <li><b>Working set</b> — sessões que navegaram (login → Cart → Produtos) antes da medição.
 *       Heap medido sem GC. Revela se a navegação deixa objetos adicionais vivos na sessão.</li>
 * </ol>
 *
 * <p>Em ambas as fases o GC é solicitado apenas antes do baseline de cada rodada,
 * não após as sessões serem abertas — isso evita que o ZGC colete objetos do baseline
 * e cause deltas negativos.</p>
 *
 * <h3>Pré-requisitos</h3>
 * <ul>
 *   <li>Host rodando em {@code http://localhost:8080} com {@code server.devMode=true}</li>
 *   <li>{@code server.sessionTtlSeconds=0} em {@code application.toml} — libera sessões
 *       imediatamente no disconnect, evitando acúmulo de sessões "zumbi" entre rodadas</li>
 *   <li>Usuário {@code admin/admin} existente (banco no estado pós-DBReset)</li>
 *   <li>H2 em modo TCP (externo) para isolar o heap do banco do heap do servidor</li>
 * </ul>
 *
 * <h3>Configuração de JVM recomendada para o servidor</h3>
 * <p>O GC influencia diretamente o resultado. Use ZGC Generacional com coleta periódica
 * para resultados consistentes e representativos de produção:</p>
 * <pre>
 *   -Xms4g -Xmx10g
 *   -XX:+UseZGC -XX:+ZGenerational
 *   -XX:SoftMaxHeapSize=9g       # ZGC mantém abaixo de 9 GB; 1 GB de buffer para picos
 *   -XX:ZCollectionInterval=5    # GC preditivo: coleta a cada 5 s mesmo sem pressão
 *                                # sem isso o ZGC pode não coletar entre rodadas,
 *                                # inflando o baseline da próxima rodada
 *   -XX:MaxMetaspaceSize=512m
 *   -XX:ReservedCodeCacheSize=256m
 * </pre>
 * <p><b>Atenção:</b> sem {@code ZCollectionInterval} o ZGC pode não coletar as sessões
 * liberadas entre rodadas antes do próximo {@code POST /__dev/gc}, fazendo o baseline
 * subir progressivamente e os deltas ficarem menores que o real.</p>
 *
 * <h3>Execução</h3>
 * <pre>
 *   mvn exec:java -pl ...remote.shell.java-client \
 *     -Dexec.mainClass=br.com.wdc.shopping.view.remote.javaclient.scenario.MemoryPerSessionScenario \
 *     [-Dexec.args="http://localhost:8080 1000"]
 * </pre>
 * <p>Use lotes grandes (≥ 1000) para diluir o overhead de warmup do JVM e obter
 * deltas proporcionais ao footprint real por sessão.</p>
 */
public class MemoryPerSessionScenario {

    static final PrintStream out = System.out; // NOSONAR
    static final PrintStream err = System.err; // NOSONAR

    /** Número de sessões abertas por rodada de medição. */
    private static final int DEFAULT_BATCH = 30;

    /** Número de rodadas de medição (a mediana é usada como resultado). */
    private static final int ROUNDS = 3;

    /** Memórias-alvo para as quais calcular o maxSessions recomendado (em GB). */
    private static final int[] TARGET_HEAP_GB = { 1, 2, 4, 8, 10, 16, 32 };

    /**
     * Fração do heap que reservamos para overhead de JVM, código, metaspace
     * e picos de alocação durante requests. 35% = 65% disponível para sessões.
     */
    private static final double OVERHEAD_FRACTION = 0.35;

    public static void main(String[] args) throws Exception {
        String serverUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        int batch = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_BATCH;

        out.println("╔══════════════════════════════════════════════════════╗");
        out.println("║      MemoryPerSessionScenario — Host/Shell           ║");
        out.println("╠══════════════════════════════════════════════════════╣");
        out.printf ("║  Server : %-42s ║%n", serverUrl);
        out.printf ("║  Batch  : %-42d ║%n", batch);
        out.printf ("║  Rounds : %-42d ║%n", ROUNDS);
        out.println("╚══════════════════════════════════════════════════════╝");
        out.println();

        new MemoryPerSessionScenario().run(serverUrl, batch);
    }

    public void run(String serverUrl, int batch) throws Exception {
        // Warm-up baseline (server max heap discovery only — not used for delta)
        out.println("[init] Requesting server GC to establish a clean baseline...");
        requestServerGc(serverUrl);
        HeapInfo init = queryServerHeap(serverUrl);
        out.printf("[init] server heap used: %s  |  max: %s%n",
                formatBytes(init.used()), formatBytes(init.max()));
        out.println();

        long serverMaxHeap = init.max();

        // ── Phase 1: Idle footprint ───────────────────────────────────────────
        // Sessions open and authenticated; heap measured WITHOUT GC.
        // Captures long-lived objects created during login (presenter tree, security context).
        out.println("┌─ Phase 1: Idle footprint (authenticated sessions, no navigation) ─────");
        long[] idlePerSession = measureRounds(serverUrl, batch, false);
        long idleMedian = median(idlePerSession);
        out.printf("└─ Idle median: %s%n%n", formatBytes(idleMedian));

        // ── Phase 2: Working set ──────────────────────────────────────────────
        // Sessions navigate (login → Cart → Products) before measurement.
        // Heap measured WITHOUT GC. Reveals whether navigation leaves additional
        // long-lived objects in the session (lazy-loaded state, cached query results, etc.).
        out.println("┌─ Phase 2: Working set (sessions browsing: login → Cart → Products) ───");
        long[] workingSetPerSession = measureRounds(serverUrl, batch, true);
        long workingSetMedian = median(workingSetPerSession);
        out.printf("└─ Working set median: %s%n%n", formatBytes(workingSetMedian));

        // ── Results ───────────────────────────────────────────────────────────
        out.println("══════════════════════════════════════════════════════════════════════");
        out.println("  RESULT");
        out.println("══════════════════════════════════════════════════════════════════════");
        out.printf ("  Server max heap (-Xmx)                 : %s%n", formatBytes(serverMaxHeap));
        out.printf ("  Idle footprint / session (no nav)      : %s%n", formatBytes(idleMedian));
        out.printf ("  Working set / session (Cart + Products): %s%n", formatBytes(workingSetMedian));
        out.println();
        out.println("  Recommended maxSessions by available heap");
        out.println("  (overhead: " + (int)(OVERHEAD_FRACTION * 100) + "% reserved for JVM + GC headroom)");
        out.println("  Recommendation uses working set — more conservative and realistic.");
        out.println();
        out.printf ("  %-12s  %12s  %12s  %12s%n", "Heap", "Idle max", "Working max", "Recommended");
        out.printf ("  %-12s  %12s  %12s  %12s%n", "────────────", "────────────", "────────────", "────────────");

        for (int gb : TARGET_HEAP_GB) {
            long heapBytes = (long) gb * 1024 * 1024 * 1024;
            long usableBytes = (long) (heapBytes * (1.0 - OVERHEAD_FRACTION));
            long idleMax     = idleMedian       > 0 ? heapBytes  / idleMedian       : 0;
            long workingMax  = workingSetMedian > 0 ? heapBytes  / workingSetMedian : 0;
            long recommended = workingSetMedian > 0 ? usableBytes / workingSetMedian : 0;
            out.printf("  %-12s  %,12d  %,12d  %,12d%n", gb + " GB", idleMax, workingMax, recommended);
        }

        out.println();
        out.println("  To configure the server, add to application.toml:");
        out.printf ("    maxSessions = %d   # based on server -Xmx (%s), working set%n",
                recommendedForServer(serverMaxHeap, workingSetMedian), formatBytes(serverMaxHeap));
        out.println("══════════════════════════════════════════════════════════════════════");
    }

    /**
     * Runs {@link #ROUNDS} measurement rounds and returns bytes-per-session for each round.
     *
     * @param exerciseSessions {@code true} → navigate each session (Cart → Products) before
     *                         measuring and do NOT request GC (working set mode);
     *                         {@code false} → request GC before measuring (post-GC footprint mode)
     */
    private long[] measureRounds(String serverUrl, int batch, boolean exerciseSessions)
            throws Exception {
        long[] bytesPerSession = new long[ROUNDS];

        for (int round = 0; round < ROUNDS; round++) {
            out.printf("[round %d/%d] Requesting server GC for fresh baseline...%n", round + 1, ROUNDS);
            requestServerGc(serverUrl);
            HeapInfo before = queryServerHeap(serverUrl);
            out.printf("[round %d/%d] baseline: %s  |  Opening %d sessions...%n",
                    round + 1, ROUNDS, formatBytes(before.used()), batch);
            List<HostClient> clients = new ArrayList<>(batch);

            try {
                for (int i = 0; i < batch; i++) {
                    var client = openAuthenticatedSession(serverUrl);
                    clients.add(client);
                    if ((i + 1) % 10 == 0) {
                        out.printf("  ... %d/%d sessions open%n", i + 1, batch);
                    }
                }

                if (exerciseSessions) {
                    out.printf("  Exercising %d sessions (Cart → Products)...%n", clients.size());
                    for (var c : clients) {
                        exerciseSession(c);
                    }
                }

                // Give the server a moment to settle (flush loop, etc.)
                Thread.sleep(500);
                System.gc(); // client-side GC to reduce noise from our own allocations

                HeapInfo after = queryServerHeap(serverUrl); // no server GC — capture live heap

                long delta = after.used() - before.used();
                bytesPerSession[round] = delta / batch;

                out.printf("  heap after: %s  |  delta: %s  |  per session: %s%n",
                        formatBytes(after.used()),
                        formatBytes(delta),
                        formatBytes(bytesPerSession[round]));

            } finally {
                out.printf("  Closing %d sessions...%n", clients.size());
                for (var c : clients) {
                    try { c.close(); } catch (Exception ignored) {}
                }
                clients.clear();
                Thread.sleep(300); // let the server release sessions before next round
            }
            out.println();
        }

        return bytesPerSession;
    }

    /**
     * Exercises a session by navigating to the Cart and back to Products.
     * This triggers server-side DB queries and allocates presenter state that
     * would be live during normal user interaction, loading the working set.
     * <p>
     * Navigation: Home → Cart (query cart items) → Products (query product list).
     * Errors are silently ignored — the session still contributes to the delta,
     * just with less exercised state.
     */
    private static void exerciseSession(HostClient client) {
        try {
            client.navigate(ShoppingRoutes.restrict_home_cart());
            var cart = CartPresenterClient.getFirst(client);
            if (cart.isPresent()) {
                cart.get().onOpenProducts(); // back to products panel
            } else {
                client.navigate(ShoppingRoutes.restrict_home());
            }
        } catch (Exception ignored) {
            // best-effort: partial navigation still contributes to working set
        }
    }

    // :: Helpers

    record HeapInfo(long used, long max) {}

    /**
     * Opens a session and performs login so the server allocates the full
     * authenticated presenter tree (RootPresenter, HomePresenter, etc.).
     */
    private static HostClient openAuthenticatedSession(String serverUrl)
            throws Exception {
        var client = HostClient.connect(serverUrl);
        try {
            client.awaitResponse();
            var root = BrowserPresenterClient.get(client).root();
            client.navigate(ShoppingRoutes.open_login());
            root.pageAs(LoginPresenterClient.class).onEnter("admin", "admin");
            return client;
        } catch (Exception e) {
            try { client.close(); } catch (Exception ignored) {}
            throw e;
        }
    }

    /**
     * Requests a server-side GC cycle via {@code POST /__dev/gc}.
     * The endpoint blocks for ~1 s to let ZGC complete at least one
     * concurrent cycle before returning, so the next heap snapshot
     * reflects a post-GC state.
     * <p>
     * If the endpoint is unavailable (non-dev server, network error), the
     * call is silently ignored — measurements will be less precise but still valid.
     */
    private static void requestServerGc(String serverUrl) {
        try {
            var http = java.net.http.HttpClient.newHttpClient();
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(serverUrl + "/__dev/gc"))
                    .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();
            http.send(req, java.net.http.HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Best-effort: if unavailable, proceed without forced GC
        }
    }

    /**
     * Queries {@code GET /__dev/heap} and returns the server's heap info.
     * <p>
     * The endpoint is provided by {@code DevHeapController} (devMode only).
     * Falls back to reading local heap as a last resort (when running
     * server + scenario in the same JVM during unit tests).
     */
    private static HeapInfo queryServerHeap(String serverUrl) throws Exception {
        var http = java.net.http.HttpClient.newHttpClient();
        var req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(serverUrl + "/__dev/heap"))
                .GET()
                .build();
        var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            // Expected: {"usedBytes": N, "maxBytes": N, "committedBytes": N}
            String body = resp.body();
            long used = extractLong(body, "usedBytes");
            long max  = extractLong(body, "maxBytes");
            if (used >= 0) {
                return new HeapInfo(used, max);
            }
        }
        // Fallback (server and scenario in same JVM)
        var bean = ManagementFactory.getMemoryMXBean();
        var usage = bean.getHeapMemoryUsage();
        return new HeapInfo(usage.getUsed(), usage.getMax());
    }

    private static long extractLong(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return -1;
        String after = json.substring(idx + key.length()).replaceAll("[^0-9]", " ").trim();
        String[] parts = after.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return -1;
        try { return Long.parseLong(parts[0]); } catch (NumberFormatException e) { return -1; }
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static long recommendedForServer(long serverMaxHeap, long bytesPerSession) {
        if (bytesPerSession <= 0 || serverMaxHeap <= 0) return 0;
        long usable = (long) (serverMaxHeap * (1.0 - OVERHEAD_FRACTION));
        return usable / bytesPerSession;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
