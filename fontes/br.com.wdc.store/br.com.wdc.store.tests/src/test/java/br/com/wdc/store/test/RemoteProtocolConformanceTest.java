package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import br.com.wdc.framework.commons.security.ChannelCrypto;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.framework.cube.remote.RemoteHostModule;
import br.com.wdc.store.test.util.TestEnvironment;
import br.com.wdc.store.view.remote.host.StoreApplicationImpl;
import io.javalin.Javalin;

/**
 * The host fixtures of the remote protocol ({@code conformance/host/*.json} of {@code framework.cube.remote}), run
 * against the remote host with the WDC Store application, over a real WebSocket.
 */
@RunWith(Parameterized.class)
public class RemoteProtocolConformanceTest {

    private static final Path FIXTURES = Path.of(
            "../../br.com.wdc.framework/br.com.wdc.framework.cube.remote/conformance/host");
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static final Duration QUIET = Duration.ofMillis(400);
    private static final Gson GSON = new Gson();

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

    private static final Defer CLEAN_UP = new Defer();
    private static Javalin javalin;

    @Parameters(name = "{0}")
    public static Collection<Object[]> fixtures() throws Exception {
        try (Stream<Path> files = Files.list(FIXTURES)) {
            return files.filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .map(p -> new Object[] { p.getFileName().toString(), p })
                    .toList();
        }
    }

    private final Path fixture;

    public RemoteProtocolConformanceTest(String name, Path fixture) {
        this.fixture = fixture;
    }

    @BeforeClass
    public static void startHost() {
        var security = RemoteAppSecurity.withKey(new byte[32]);
        var registry = new RemoteApplicationRegistry<>(StoreApplicationImpl::createApp);
        StoreApplicationImpl.initialize(security, registry);
        var module = new RemoteHostModule<>(security, registry, new String[0]);
        module.start(CLEAN_UP);
        javalin = Javalin.create(module::configure).start(0);
    }

    @AfterClass
    public static void stopHost() {
        if (javalin != null) {
            javalin.stop();
        }
        CLEAN_UP.run();
    }

    @Test
    public void run() throws Exception {
        var spec = JsonParser.parseString(Files.readString(fixture)).getAsJsonObject();
        var base = "http://localhost:" + javalin.port();
        var http = HttpClient.newHttpClient();

        var init = http.send(HttpRequest.newBuilder(URI.create(base + "/api/session/init")).build(),
                HttpResponse.BodyHandlers.ofString());
        var appId = JsonParser.parseString(init.body()).getAsJsonObject().get("appId").getAsString();

        var listener = new Listener();
        var ws = http.newWebSocketBuilder()
                .buildAsync(URI.create(base.replaceFirst("^http", "ws") + "/dispatcher/" + appId), listener)
                .join();
        try {
            for (var element : spec.getAsJsonArray("steps")) {
                runStep(element.getAsJsonObject(), ws, listener);
            }
        } finally {
            ws.abort();
        }
    }

    private void runStep(JsonObject step, WebSocket ws, Listener listener) throws Exception {
        if (step.has("send")) {
            ws.sendText(GSON.toJson(placeholders(step.get("send"))), true).join();
            if (step.has("expect")) {
                var text = listener.messages.poll(WAIT.toMillis(), TimeUnit.MILLISECONDS);
                assertNotNull("no answer to " + step.get("send"), text);
                match(step.get("expect"), JsonParser.parseString(text), "$");
            } else if (step.has("expectNothing")) {
                assertNull("unexpected message", listener.messages.poll(QUIET.toMillis(), TimeUnit.MILLISECONDS));
            }
        } else if (step.has("expectClosed")) {
            var code = listener.closed.get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
            assertEquals(step.get("expectClosed").getAsInt(), code.intValue());
        } else {
            fail("unknown step " + step);
        }
    }

    /** {@code "$clientKey"}: the public key of a new P-256 key pair. */
    private static JsonElement placeholders(JsonElement element) {
        if (element.isJsonPrimitive() && "$clientKey".equals(element.getAsString())) {
            return new JsonPrimitive(ChannelCrypto.encodePublicKey(ChannelCrypto.newKeyPair().getPublic()));
        }
        if (element.isJsonObject()) {
            var copy = new JsonObject();
            element.getAsJsonObject().entrySet().forEach(e -> copy.add(e.getKey(), placeholders(e.getValue())));
            return copy;
        }
        if (element.isJsonArray()) {
            var copy = new JsonArray();
            element.getAsJsonArray().forEach(e -> copy.add(placeholders(e)));
            return copy;
        }
        return element;
    }

    /** The matching rules of {@code conformance/README.md}. */
    static void match(JsonElement expected, JsonElement actual, String path) {
        if (expected.isJsonPrimitive() && expected.getAsJsonPrimitive().isString()) {
            switch (expected.getAsString()) {
            case "$any" -> {
                assertNotNull(path + " should be present", actual);
                return;
            }
            case "$string" -> {
                if (actual == null || !actual.isJsonPrimitive() || !actual.getAsJsonPrimitive().isString()) {
                    fail(path + " should be a string, was " + actual);
                }
                return;
            }
            case "$number" -> {
                if (actual == null || !actual.isJsonPrimitive() || !actual.getAsJsonPrimitive().isNumber()) {
                    fail(path + " should be a number, was " + actual);
                }
                return;
            }
            default -> {
                // a literal string
            }
            }
        }
        if (expected.isJsonObject() && expected.getAsJsonObject().has("$contains")) {
            if (actual == null || !actual.isJsonArray()) {
                fail(path + " should be an array, was " + actual);
            }
            var wanted = expected.getAsJsonObject().get("$contains");
            for (var item : actual.getAsJsonArray()) {
                try {
                    match(wanted, item, path + "[]");
                    return;
                } catch (AssertionError notThisOne) {
                    // try the next
                }
            }
            fail(path + " has no element matching " + wanted + ": " + actual);
        }
        if (expected.isJsonObject()) {
            if (actual == null || !actual.isJsonObject()) {
                fail(path + " should be an object, was " + actual);
            }
            for (Map.Entry<String, JsonElement> e : expected.getAsJsonObject().entrySet()) {
                var value = actual.getAsJsonObject().get(e.getKey());
                if (e.getValue().isJsonPrimitive() && "$absent".equals(e.getValue().getAsString())) {
                    assertNull(path + "." + e.getKey() + " should be absent", value);
                } else {
                    assertNotNull(path + "." + e.getKey() + " should be present in " + actual, value);
                    match(e.getValue(), value, path + "." + e.getKey());
                }
            }
            return;
        }
        if (expected.isJsonArray()) {
            if (actual == null || !actual.isJsonArray() || actual.getAsJsonArray().size() != expected.getAsJsonArray().size()) {
                fail(path + " should be " + expected + ", was " + actual);
            }
            for (int i = 0; i < expected.getAsJsonArray().size(); i++) {
                match(expected.getAsJsonArray().get(i), actual.getAsJsonArray().get(i), path + "[" + i + "]");
            }
            return;
        }
        if (expected.isJsonPrimitive() && expected.getAsJsonPrimitive().isNumber() && actual != null
                && actual.isJsonPrimitive() && actual.getAsJsonPrimitive().isNumber()) {
            assertEquals(path, expected.getAsDouble(), actual.getAsDouble(), 0.0);
            return;
        }
        assertEquals(path, expected, actual);
    }

    private static final class Listener implements WebSocket.Listener {

        final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();
        final CompletableFuture<Integer> closed = new CompletableFuture<>();
        private final List<CharSequence> parts = new ArrayList<>();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            parts.add(data);
            if (last) {
                messages.add(String.join("", parts));
                parts.clear();
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            closed.complete(statusCode);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            closed.completeExceptionally(error);
        }
    }
}
