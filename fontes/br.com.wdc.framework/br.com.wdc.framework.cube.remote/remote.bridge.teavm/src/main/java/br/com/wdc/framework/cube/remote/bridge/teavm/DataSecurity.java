package br.com.wdc.framework.cube.remote.bridge.teavm;

import br.com.wdc.framework.cube.remote.bridge.teavm.interop.JsStringConsumer;

/**
 * Provides AES-GCM encryption for sensitive form fields (e.g., passwords).
 * Delegates to {@link SecurityBoot} which manages the crypto key lifecycle.
 */
public final class DataSecurity {

    private DataSecurity() {}

    public static void cipher(String text, JsStringConsumer callback) {
        SecurityBoot.cipher(text, callback);
    }
}
