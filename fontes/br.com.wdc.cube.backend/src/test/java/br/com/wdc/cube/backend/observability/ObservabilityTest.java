package br.com.wdc.cube.backend.observability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Test;

import br.com.wdc.framework.commons.util.Defer;
import io.javalin.Javalin;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

/** {@code /metrics} behind its token, the request timing by route, and the live and ready health checks. */
public class ObservabilityTest {

    private final Defer cleanUp = new Defer();
    private DataSource dataSource = h2();

    @After
    public void stop() {
        cleanUp.run();
    }

    @Test
    public void metricsAskForTheToken_andCountRequestsByTheirRoute() throws Exception {
        var port = start("s3cret");
        get(port, "/items/42", null);
        get(port, "/items/7", null);
        Counter.builder("store.test.counted").register(Metrics.globalRegistry).increment();

        assertEquals(401, get(port, "/metrics", null).statusCode());
        assertEquals(401, get(port, "/metrics", "Bearer wrong").statusCode());

        var scrape = get(port, "/metrics", "bearer s3cret");
        assertEquals(200, scrape.statusCode());
        var body = scrape.body();
        assertTrue("the route as declared, both requests in one series",
                body.contains("http_server_requests_seconds_count{method=\"GET\",route=\"/items/{id}\",status=\"200\"} 2"));
        assertFalse("not the addresses", body.contains("/items/42"));
        assertTrue("the JVM", body.contains("jvm_memory_used_bytes"));
        assertTrue("what the lower layers count in the global registry", body.contains("store_test_counted_total 1"));
        assertTrue(body.contains("store_remote_sessions"));
        assertTrue(body.contains("store_remote_transactions_total{event=\"begun\"}"));
    }

    @Test
    public void withoutAToken_thereIsNoMetricsRoute() throws Exception {
        var port = start(null);
        assertEquals(404, get(port, "/metrics", "Bearer anything").statusCode());
    }

    @Test
    public void readyAsksTheDatabase_liveDoesNot() throws Exception {
        var port = start(null);
        assertEquals(200, get(port, "/health", null).statusCode());
        assertEquals(200, get(port, "/health/live", null).statusCode());
        var ready = get(port, "/health/ready", null);
        assertEquals(200, ready.statusCode());
        assertTrue(ready.body(), ready.body().contains("\"database\":\"UP\""));

        // a database server that is not there
        var gone = new JdbcDataSource();
        gone.setURL("jdbc:h2:tcp://localhost:1/gone");
        dataSource = gone;
        var down = get(port, "/health/ready", null);
        assertEquals(503, down.statusCode());
        assertTrue(down.body(), down.body().contains("\"database\":\"DOWN\""));
        assertEquals("the process still answers", 200, get(port, "/health/live", null).statusCode());
    }

    // :: Helpers

    private int start(String token) {
        var observability = new Observability(token, () -> dataSource, () -> null, () -> null, () -> null);
        var app = Javalin.create(config -> {
            observability.configure(config);
            config.routes.get("/items/{id}", ctx -> ctx.result("item " + ctx.pathParam("id")));
        });
        observability.attach(cleanUp);
        observability.start(cleanUp);
        app.start(0);
        cleanUp.push(app::stop);
        return app.port();
    }

    private static DataSource h2() {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:observability;DB_CLOSE_DELAY=-1");
        return ds;
    }

    private static HttpResponse<String> get(int port, String path, String authorization) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        try (var http = HttpClient.newHttpClient()) {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
