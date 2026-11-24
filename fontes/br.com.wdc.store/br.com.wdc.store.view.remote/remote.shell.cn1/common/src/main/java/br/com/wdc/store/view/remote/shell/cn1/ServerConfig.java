package br.com.wdc.store.view.remote.shell.cn1;

import com.codename1.ui.Display;

/**
 * Resolves the backend base URL according to the build target.
 *
 * <p>
 * <b>Default (dev):</b> the URL the emulator/simulator uses to reach the backend running on the host —
 * iOS/simulator/desktop via {@code localhost}, Android via the {@code 10.0.2.2} alias. <b>Real device:</b>
 * neither {@code localhost} nor {@code 10.0.2.2} works; provide the target URL via {@link #DEVICE_URL}
 * (or the build hint {@code -Dcodename1.arg.serverUrl=...}), which takes precedence.
 * </p>
 */
public final class ServerConfig {

    private ServerConfig() {
        // NOOP
    }

    /** Final device artifact: set the production URL here (or via {@code -Dcodename1.arg.serverUrl}). */
    private static final String DEVICE_URL = "";

    /** Backend base URL: the override (device artifact) takes precedence; otherwise the per-platform default. */
    public static String baseUrl() {
        Display d = Display.getInstance();
        // override: serverUrl build hint (if exposed by the build) or the DEVICE_URL constant.
        String override = d.getProperty("serverUrl", DEVICE_URL);
        if (override != null && !override.isEmpty()) {
            return override;
        }
        // DEV default: the URL the emulator/simulator uses to reach the backend on the host.
        if ("and".equals(d.getPlatformName())) {
            return "http://10.0.2.2:8080"; // Android: host alias in the emulator
        }
        return "http://localhost:8080";    // iOS/simulator/desktop

    }
}
