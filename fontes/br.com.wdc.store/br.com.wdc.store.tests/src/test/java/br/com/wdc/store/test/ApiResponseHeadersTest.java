package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.ClassRule;
import org.junit.Test;

import br.com.wdc.store.test.util.TestEnvironment;

/** The security, cache and timing headers on every API response, errors included. */
public class ApiResponseHeadersTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST);

    private HttpResponse<String> post(String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(env.baseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void assertHeaders(HttpResponse<String> response) {
        var headers = response.headers();
        assertEquals("nosniff", headers.firstValue("X-Content-Type-Options").orElse(null));
        assertEquals("DENY", headers.firstValue("X-Frame-Options").orElse(null));
        assertEquals("no-referrer", headers.firstValue("Referrer-Policy").orElse(null));
        assertEquals("no-store", headers.firstValue("Cache-Control").orElse(null));
        assertTrue(headers.firstValue("Server-Timing").orElse("").matches("total;dur=\\d+\\.\\d"));
    }

    @Test
    public void aSuccessfulAnswer_carriesTheHeaders() throws Exception {
        var response = post("/api/repo/product/count", "{}");

        assertEquals(200, response.statusCode());
        assertHeaders(response);
    }

    @Test
    public void aRefusal_carriesTheHeadersToo() throws Exception {
        var response = post("/api/repo/product/fetch", "{\"orderBy\":\"NO_SUCH_ORDER\"}");

        assertEquals(400, response.statusCode());
        assertHeaders(response);
    }
}
