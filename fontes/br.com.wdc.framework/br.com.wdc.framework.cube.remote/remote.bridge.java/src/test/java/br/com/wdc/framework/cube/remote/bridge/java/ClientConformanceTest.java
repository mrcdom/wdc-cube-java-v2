package br.com.wdc.framework.cube.remote.bridge.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
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
import br.com.wdc.framework.commons.storage.InMemoryClientStorage;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

/**
 * The client fixtures of the remote protocol ({@code conformance/client/*.json}), run against {@link HostClient} with
 * a host scripted by each fixture: a WebSocket server that sends what the fixture says the client receives and keeps
 * what the client sends.
 */
@RunWith(Parameterized.class)
public class ClientConformanceTest {

    private static final Path FIXTURES = Path.of("../conformance/client");
    private static final String APP_ID = "conformance.session";
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static final Duration QUIET = Duration.ofMillis(400);
    private static final Gson GSON = new Gson();

    @Parameters(name = "{0}")
    public static Collection<Object[]> fixtures() throws IOException {
        try (Stream<Path> files = Files.list(FIXTURES)) {
            return files.filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .map(p -> new Object[] { p.getFileName().toString(), p })
                    .toList();
        }
    }

    private final Path fixture;

    private Javalin host;
    private final LinkedBlockingQueue<String> sent = new LinkedBlockingQueue<>();
    private final CompletableFuture<WsContext> connection = new CompletableFuture<>();
    private final InMemoryClientStorage storage = new InMemoryClientStorage();
    private CompletableFuture<HostClient> client;
    private String clientKey;
    private byte[] key;

    public ClientConformanceTest(String name, Path fixture) {
        this.fixture = fixture;
    }

    @Before
    public void startHost() {
        host = Javalin.create(config -> {
            config.routes.get("/api/session/init", ctx -> ctx.contentType("application/json").result("{\"appId\":\"" + APP_ID + "\"}"));
            config.routes.ws("/dispatcher/{id}", ws -> {
                ws.onConnect(connection::complete);
                ws.onMessage(ctx -> {
                    if (!ctx.message().contains("\"ping\"")) {
                        sent.add(ctx.message());
                    }
                });
            });
        }).start(0);
    }

    @After
    public void stopHost() throws Exception {
        if (client != null && client.isDone() && !client.isCompletedExceptionally()) {
            client.get().close();
        }
        host.stop();
    }

    @Test
    public void run() throws Exception {
        var spec = JsonParser.parseString(Files.readString(fixture)).getAsJsonObject();
        for (var element : spec.getAsJsonArray("steps")) {
            var step = element.getAsJsonObject();
            if (step.has("start")) {
                // connect() returns once the handshake is answered: it runs while the fixture plays the host
                client = CompletableFuture.supplyAsync(() -> {
                    try {
                        return HostClient.connect("http://localhost:" + host.port(), storage);
                    } catch (IOException | InterruptedException failed) {
                        throw new IllegalStateException(failed);
                    }
                });
            } else if (step.has("expectSent")) {
                var text = sent.poll(WAIT.toMillis(), TimeUnit.MILLISECONDS);
                if (text == null && client.isCompletedExceptionally()) {
                    client.join(); // shows why the client stopped
                }
                assertNotNull("the client sent nothing; expected " + step.get("expectSent"), text);
                var message = JsonParser.parseString(text).getAsJsonObject();
                if (message.has("clientKey")) {
                    clientKey = message.get("clientKey").getAsString();
                }
                match(step.get("expectSent"), message, "$");
            } else if (step.has("expectNothingSent")) {
                assertNull("the client should send nothing", sent.poll(QUIET.toMillis(), TimeUnit.MILLISECONDS));
            } else if (step.has("receive")) {
                var ws = connection.get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
                ws.send(GSON.toJson(placeholders(step.get("receive"))));
                if (step.getAsJsonObject("receive").has("storage")) {
                    client.get(WAIT.toMillis(), TimeUnit.MILLISECONDS)
                            .applyStorageDelta(client.get().awaitResponse(WAIT));
                }
            } else if (step.has("expectOutdated")) {
                try {
                    client.get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
                    fail("the client should know the host refused its version");
                } catch (ExecutionException refused) {
                    assertTrue(String.valueOf(refused.getCause()),
                            refused.getCause() instanceof IllegalStateException
                                    && refused.getCause().getCause() instanceof IOException);
                }
            } else if (step.has("expectStorage")) {
                var expected = step.getAsJsonObject("expectStorage");
                var scope = expected.get("scope").getAsString();
                var store = "persistent-secure".equals(scope) ? storage.secure() : storage;
                assertEquals(expected.get("value").getAsString(), store.get(expected.get("key").getAsString()));
            } else {
                fail("unknown step " + step);
            }
        }
    }

    /** {@code "$serverKey"}: a new key pair, whose key the connection uses; {@code "$cipher:<text>"}: ciphered with it. */
    private JsonElement placeholders(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            var text = element.getAsString();
            if ("$serverKey".equals(text)) {
                KeyPair pair = ChannelCrypto.newKeyPair();
                key = ChannelCrypto.deriveKey(pair.getPrivate(), ChannelCrypto.decodePublicKey(clientKey), APP_ID);
                return new JsonPrimitive(ChannelCrypto.encodePublicKey(pair.getPublic()));
            }
            if (text.startsWith("$cipher:")) {
                return new JsonPrimitive(ChannelCrypto.cipher(key, text.substring("$cipher:".length())));
            }
            return element;
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
    private static void match(JsonElement expected, JsonElement actual, String path) {
        if (expected.isJsonPrimitive() && expected.getAsJsonPrimitive().isString()) {
            var rule = expected.getAsString();
            if ("$any".equals(rule)) {
                assertNotNull(path + " should be present", actual);
                return;
            }
            if ("$string".equals(rule)) {
                assertTrue(path + " should be a string, was " + actual,
                        actual != null && actual.isJsonPrimitive() && actual.getAsJsonPrimitive().isString());
                return;
            }
            if ("$number".equals(rule)) {
                assertTrue(path + " should be a number, was " + actual,
                        actual != null && actual.isJsonPrimitive() && actual.getAsJsonPrimitive().isNumber());
                return;
            }
        }
        if (expected.isJsonObject()) {
            assertTrue(path + " should be an object, was " + actual, actual != null && actual.isJsonObject());
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
        if (expected.isJsonPrimitive() && expected.getAsJsonPrimitive().isNumber() && actual != null
                && actual.isJsonPrimitive() && actual.getAsJsonPrimitive().isNumber()) {
            assertEquals(path, expected.getAsDouble(), actual.getAsDouble(), 0.0);
            return;
        }
        assertEquals(path, expected, actual);
    }
}
