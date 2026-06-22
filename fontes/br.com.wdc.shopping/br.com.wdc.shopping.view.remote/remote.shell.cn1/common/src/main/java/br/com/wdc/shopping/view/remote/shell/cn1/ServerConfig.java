package br.com.wdc.shopping.view.remote.shell.cn1;

import com.codename1.ui.Display;

/**
 * Resolve a URL base do backend conforme o destino do build.
 *
 * <p>
 * <b>Padrão (dev):</b> a URL que o emulador/simulador usa para alcançar o backend rodando no host —
 * iOS/simulador/desktop por {@code localhost}, Android pelo alias {@code 10.0.2.2}. <b>Device real:</b>
 * nem {@code localhost} nem {@code 10.0.2.2} valem; informe a URL de destino via {@link #DEVICE_URL}
 * (ou o build hint {@code -Dcodename1.arg.serverUrl=...}), que tem prioridade.
 * </p>
 */
public final class ServerConfig {

    private ServerConfig() {
        // NOOP
    }

    /** Artefato final de device: defina a URL de produção aqui (ou via {@code -Dcodename1.arg.serverUrl}). */
    private static final String DEVICE_URL = "";

    /** URL base do backend: override (artefato de device) tem prioridade; senão o padrão por plataforma. */
    public static String baseUrl() {
        Display d = Display.getInstance();
        // override: build hint serverUrl (se exposto pelo build) ou a constante DEVICE_URL.
        String override = d.getProperty("serverUrl", DEVICE_URL);
        if (override != null && !override.isEmpty()) {
            return override;
        }
        // padrão de DEV: a URL que o emulador/simulador usa p/ alcançar o backend no host.
        if ("and".equals(d.getPlatformName())) {
            return "http://10.0.2.2:8080"; // Android: alias do host no emulador
        }
        return "http://localhost:8080";    // iOS/simulador/desktop
    }
}
