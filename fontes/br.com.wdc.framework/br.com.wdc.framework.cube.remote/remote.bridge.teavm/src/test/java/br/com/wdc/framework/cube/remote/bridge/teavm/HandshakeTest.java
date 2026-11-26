package br.com.wdc.framework.cube.remote.bridge.teavm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class HandshakeTest {

    @Test
    public void helloCarriesTheVersionAndTheKeyAndNothingElse() {
        var hello = Handshake.hello("MFkw");

        assertEquals(Map.of("protocol", 1, "clientKey", "MFkw"), hello);
        assertEquals("{\"protocol\":1,\"clientKey\":\"MFkw\"}", JsonParser.stringify(hello));
    }

    @Test
    public void anAnswerWithTheVersionAndAKeyIsAccepted() {
        var answer = JsonParser.parseObject("{\"protocol\":1,\"serverKey\":\"MFkwEw\"}");

        assertEquals(Handshake.Answer.ACCEPTED, Handshake.read(answer));
        assertEquals("MFkwEw", Handshake.serverKey(answer));
    }

    @Test
    public void unsupportedProtocolIsARefusal() {
        var answer = JsonParser.parseObject("{\"error\":\"unsupported-protocol\",\"protocols\":[1]}");

        assertEquals(Handshake.Answer.REFUSED, Handshake.read(answer));
    }

    @Test
    public void anythingElseIsUnexpected() {
        for (var json : List.of(
                "{\"protocol\":2,\"serverKey\":\"MFkwEw\"}",
                "{\"protocol\":1}",
                "{\"protocol\":1,\"serverKey\":\"\"}",
                "{\"protocol\":1.5,\"serverKey\":\"MFkwEw\"}",
                "{\"scopes\":[]}",
                "{\"error\":\"capacity_exceeded\"}")) {
            var message = JsonParser.parseObject(json);
            assertEquals(json, Handshake.Answer.UNEXPECTED, Handshake.read(message));
        }
        assertNull(Handshake.serverKey(JsonParser.parseObject("{\"serverKey\":7}")));
    }
}
