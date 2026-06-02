package br.com.wdc.framework.cube.remote.bridge.teavm;

import org.teavm.jso.JSBody;

import br.com.wdc.framework.cube.remote.bridge.teavm.interop.JsStringConsumer;

/**
 * Provides AES-GCM encryption for sensitive form fields (e.g., passwords).
 * Delegates to the browser's Web Crypto API via the global __wdc_cipher function
 * defined in the security boot script (index.html).
 */
public final class DataSecurity {

    private DataSecurity() {}

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
