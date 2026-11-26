package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.JSObject;

import br.com.wdc.framework.cube.remote.bridge.teavm.interop.Console;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.JsStringConsumer;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.WebCrypto;

/**
 * The key of the current connection, and the ciphering of sensitive values with it (storage values, and event
 * parameters such as {@code p.password}); the counterpart of the host's {@code RemoteDataSecurity}.
 * <p>
 * Each connection agrees on its own key in the handshake ({@link FlushRequestContext}); there is no key between a
 * connection's opening and the host's answer, nor after a drop. A value to cipher in that time waits for the next
 * key, so a sensitive value never goes out in plaintext.
 */
public final class DataSecurity {

    private static JSObject key;
    private static final List<Runnable> waitingForKey = new ArrayList<>();

    private DataSecurity() {
    }

    /** Whether the current connection has agreed on its key. */
    public static boolean hasKey() {
        return key != null;
    }

    /**
     * Ciphers a text with the connection key: {@code Base64(IV ‖ ciphertext ‖ tag)}. Without a key yet, waits for
     * the next one. Calls back with an empty string if ciphering fails.
     */
    public static void cipher(String text, JsStringConsumer callback) {
        if (key == null) {
            waitingForKey.add(() -> cipher(text, callback));
            return;
        }
        WebCrypto.cipher(key, text, callback);
    }

    /**
     * Deciphers a value the host ciphered with the connection key. Calls back with an empty string if there is no
     * key, or the value does not authenticate.
     */
    public static void decipher(String value, JsStringConsumer callback) {
        if (key == null) {
            Console.warn("[DataSecurity] no connection key to decipher with");
            callback.accept("");
            return;
        }
        WebCrypto.decipher(key, value, callback);
    }

    /** Uses the key the connection agreed on, and ciphers what waited for it. */
    static void useKey(JSObject connectionKey) {
        key = connectionKey;
        var waiting = new ArrayList<>(waitingForKey);
        waitingForKey.clear();
        for (var task : waiting) {
            task.run();
        }
    }

    /** Drops the key of a connection that ended. */
    static void forgetKey() {
        key = null;
    }
}
