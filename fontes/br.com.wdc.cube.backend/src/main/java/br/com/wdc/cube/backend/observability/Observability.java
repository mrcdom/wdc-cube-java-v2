package br.com.wdc.cube.backend.observability;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import javax.sql.DataSource;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionCoordinator;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionStats;
import br.com.wdc.store.persistence.impl.security.AuthenticationServiceImpl;
import io.agroal.api.AgroalDataSource;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * The backend's metrics and health checks.
 * <p>
 * <b>Metrics</b>, in Prometheus format at {@code GET /metrics}, only when {@code management.token} is set, and only
 * with {@code Authorization: Bearer <token>}: the JVM (memory, GC, threads, CPU, uptime), every HTTP request by method,
 * route and status ({@code http.server.requests}), and the WDC Store's own — sign-ins by outcome, sessions (live,
 * renewed), the remote views (live sessions, each message's handling) and the REST API's remote transactions. What the
 * lower layers count goes to Micrometer's global registry, to which this one is added; the framework's numbers (remote
 * transactions, remote views) are read from what it exposes.
 * <p>
 * <b>Health</b>, open, for load balancers: {@code GET /health/live} (the process answers; also {@code /health}) and
 * {@code GET /health/ready} (the database answers; 503 when it does not).
 */
public final class Observability {

    private static final Log LOG = Log.getLogger(Observability.class);

    private static final String STARTED = "observability.started";
    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    /** Seconds the database has to answer the readiness check. */
    private static final int DATABASE_TIMEOUT_SECONDS = 2;

    private final String token;
    private final Supplier<DataSource> dataSource;
    private final Supplier<RemoteApplicationRegistry<?>> remoteApps;
    private final Supplier<RemoteTransactionCoordinator> transactions;
    private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    /**
     * @param token        the token {@code /metrics} asks for; {@code null} or blank keeps the route out
     * @param dataSource   what the readiness check asks
     * @param remoteApps   the remote view sessions, once the host is configured
     * @param transactions the REST API's remote transactions, when there is a coordinator
     */
    public Observability(String token, Supplier<DataSource> dataSource, Supplier<RemoteApplicationRegistry<?>> remoteApps,
            Supplier<RemoteTransactionCoordinator> transactions) {
        this.token = token == null || token.isBlank() ? null : token.trim();
        this.dataSource = dataSource;
        this.remoteApps = remoteApps;
        this.transactions = transactions;
    }

    /** The registry the meters are in. */
    public MeterRegistry registry() {
        return registry;
    }

    /**
     * Adds the registry to the global one and binds the meters; {@code cleanUp} takes them out again. Called once the
     * remote host is configured, so that its sessions can be followed.
     */
    public void start(Defer cleanUp) {
        Metrics.globalRegistry.add(registry);
        cleanUp.push(() -> {
            Metrics.globalRegistry.remove(registry);
            registry.close();
        });
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        var gc = new JvmGcMetrics();
        gc.bindTo(registry);
        cleanUp.push(gc::close);
        new JvmThreadMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        bindSessions();
        bindRemoteViews(cleanUp);
        bindRemoteTransactions();
        if (token == null) {
            LOG.info("Metrics off: management.token is not set");
        }
    }

    // :: Meters

    private void bindSessions() {
        Gauge.builder("store.sessions.live", () -> AuthenticationService.BEAN.get() instanceof AuthenticationServiceImpl impl
                ? impl.liveSessions()
                : 0)
                .description("User sessions this instance holds in memory")
                .register(registry);
    }

    private void bindRemoteViews(Defer cleanUp) {
        Gauge.builder("store.remote.sessions", () -> {
            var apps = remoteApps.get();
            return apps == null ? 0 : apps.size();
        })
                .description("Remote view sessions alive on this instance")
                .register(registry);
        var ok = messages("ok");
        var failed = messages("failed");
        var apps = remoteApps.get();
        if (apps != null) {
            var listening = apps.onMessage((nanos, isFailure) -> (isFailure ? failed : ok).record(nanos,
                    TimeUnit.NANOSECONDS));
            cleanUp.push(listening::remove);
        }
    }

    private Timer messages(String outcome) {
        return Timer.builder("store.remote.messages")
                .description("Remote view messages handled")
                .tag("outcome", outcome)
                .register(registry);
    }

    private void bindRemoteTransactions() {
        Gauge.builder("store.remote.transactions.open", () -> {
            var stats = stats();
            return stats == null ? 0 : stats.openNow();
        })
                .description("REST remote transactions open now")
                .register(registry);
        var events = new LinkedHashMap<String, ToLongFunction<RemoteTransactionStats>>();
        events.put("begun", RemoteTransactionStats::begun);
        events.put("committed", RemoteTransactionStats::committed);
        events.put("rolledback", RemoteTransactionStats::rolledBack);
        events.put("reaped", RemoteTransactionStats::reaped);
        events.put("refused", RemoteTransactionStats::rejectedByLimit);
        events.forEach((event, read) -> FunctionCounter.builder("store.remote.transactions", this, self -> {
            var stats = self.stats();
            return stats == null ? 0 : read.applyAsLong(stats);
        })
                .description("REST remote transactions, by what happened to them")
                .tag("event", event)
                .register(registry));
    }

    private RemoteTransactionStats stats() {
        var coordinator = transactions.get();
        return coordinator == null ? null : coordinator.stats();
    }

    // :: Routes

    /** Times every request, and adds {@code /metrics} and the health checks. */
    public void configure(JavalinConfig config) {
        config.routes.before(ctx -> ctx.attribute(STARTED, System.nanoTime()));
        config.routes.after(this::record);

        if (token != null) {
            config.routes.get("/metrics", this::metrics);
        }
        config.routes.get("/health", this::live);
        config.routes.get("/health/live", this::live);
        config.routes.get("/health/ready", this::ready);
    }

    private void record(Context ctx) {
        Long started = ctx.attribute(STARTED);
        if (started == null) {
            return;
        }
        Timer.builder("http.server.requests")
                .tag("method", ctx.method().name())
                .tag("route", routeOf(ctx))
                .tag("status", Integer.toString(ctx.statusCode()))
                .register(registry)
                .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
    }

    /** The route as declared ({@code /api/repo/product/{id}/image}), not the address, so that the series stay few. */
    private static String routeOf(Context ctx) {
        try {
            var endpoint = ctx.endpoints().matchedHttpEndpoint();
            return endpoint == null ? "unmatched" : endpoint.path;
        } catch (RuntimeException noEndpoint) {
            return "unmatched";
        }
    }

    private void metrics(Context ctx) {
        // `Bearer <token>`, the scheme in any case
        var header = ctx.header("Authorization");
        header = header == null ? "" : header.trim();
        var given = header.length() > 7 && header.substring(0, 7).equalsIgnoreCase("Bearer ")
                ? header.substring(7).trim()
                : null;
        if (given == null || !MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            ctx.header("WWW-Authenticate", "Bearer");
            ctx.status(401).json(Map.of("error", "A valid token is required"));
            return;
        }
        ctx.contentType(CONTENT_TYPE);
        ctx.result(registry.scrape());
    }

    private void live(Context ctx) {
        ctx.json(Map.of("status", "UP"));
    }

    private void ready(Context ctx) {
        var database = databaseAnswers();
        ctx.status(database ? 200 : 503);
        ctx.json(Map.of("status", database ? "UP" : "DOWN", "checks", Map.of("database", database ? "UP" : "DOWN")));
    }

    /**
     * Whether the database answers a connection check. A connection the pool kept from before the database went away
     * fails the check without an error, and would stay in the pool until its idle validation: when the check fails, the
     * pool drops its invalid connections and the check is made once more, on a new one.
     */
    private boolean databaseAnswers() {
        var ds = dataSource.get();
        if (ds == null) {
            return false;
        }
        if (validConnection(ds)) {
            return true;
        }
        discardInvalidConnections(ds);
        return validConnection(ds);
    }

    private static boolean validConnection(DataSource ds) {
        try (var connection = ds.getConnection()) {
            return connection.isValid(DATABASE_TIMEOUT_SECONDS);
        } catch (SQLException | RuntimeException e) {
            LOG.warn("Readiness: the database does not answer: {}", e.getMessage());
            return false;
        }
    }

    private static void discardInvalidConnections(DataSource ds) {
        try {
            if (ds.isWrapperFor(AgroalDataSource.class)) {
                ds.unwrap(AgroalDataSource.class).flush(AgroalDataSource.FlushMode.INVALID);
            }
        } catch (SQLException | RuntimeException e) {
            LOG.debug("Readiness: could not drop the invalid connections: {}", e.getMessage());
        }
    }
}
