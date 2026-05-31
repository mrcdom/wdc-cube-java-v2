package br.com.wdc.shopping.view.remote.shell.teavm.bridge;

import org.teavm.jso.JSBody;

import br.com.wdc.shopping.view.teavm.commons.interop.JsStringConsumer;

/**
 * Provides AES-GCM encryption for sensitive form fields (e.g., passwords).
 * Delegates to the browser's Web Crypto API via the global __wdc_cipher function
 * defined in the security boot script (index.html).
 */
public final class DataSecurity {

    private DataSecurity() {}

    /**
     * Encrypts text using AES-GCM (key derived during boot from the same password
     * used in the app_signature). The result is base64-encoded ciphertext.
     * Asynchronous: calls the callback with the encrypted string when done.
     */
    public static void cipher(String text, JsStringConsumer callback) {
        doCipher(text, callback);
    }

    @JSBody(params = {"text", "callback"}, script = ""
            + "if (window.__wdc_cipher) {"
            + "  window.__wdc_cipher(text, callback);"
            + "} else {"
            + "  console.error('[DataSecurity] __wdc_cipher not available');"
            + "  callback('');"
            + "}")
    private static native void doCipher(String text, JsStringConsumer callback);
}
