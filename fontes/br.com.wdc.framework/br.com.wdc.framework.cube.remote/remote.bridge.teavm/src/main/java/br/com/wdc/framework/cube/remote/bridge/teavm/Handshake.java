package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The handshake of remote protocol version {@value #PROTOCOL} as messages (docs/bridge-skeleton-protocol.md §6.1):
 * what the client sends first on every connection, and how it reads the host's answer.
 */
final class Handshake {

    /** The version of the remote protocol this bridge speaks. */
    static final int PROTOCOL = 1;

    /** The error the host answers a version it does not speak with. */
    static final String UNSUPPORTED_PROTOCOL = "unsupported-protocol";

    /** What the host's answer to the handshake means. */
    enum Answer {
        /** The host speaks this version and sent its key ({@link #serverKey}). */
        ACCEPTED,
        /** The host does not speak this version: the page is out of date. */
        REFUSED,
        /** Neither: not an answer to this handshake. */
        UNEXPECTED
    }

    private Handshake() {
    }

    /** The first message of a connection: {@code {"protocol": 1, "clientKey": "<Base64 SPKI>"}}, nothing else. */
    static Map<String, Object> hello(String clientKey) {
        var message = new LinkedHashMap<String, Object>();
        message.put("protocol", PROTOCOL);
        message.put("clientKey", clientKey);
        return message;
    }

    /** Reads the host's answer to {@link #hello}. */
    static Answer read(Map<String, Object> message) {
        if (UNSUPPORTED_PROTOCOL.equals(message.get("error"))) {
            return Answer.REFUSED;
        }
        if (message.get("protocol") instanceof Number version && version.doubleValue() == PROTOCOL
                && serverKey(message) != null) {
            return Answer.ACCEPTED;
        }
        return Answer.UNEXPECTED;
    }

    /** The host's public key (Base64 SPKI) in an accepted answer, or {@code null}. */
    static String serverKey(Map<String, Object> message) {
        return message.get("serverKey") instanceof String key && !key.isEmpty() ? key : null;
    }
}
