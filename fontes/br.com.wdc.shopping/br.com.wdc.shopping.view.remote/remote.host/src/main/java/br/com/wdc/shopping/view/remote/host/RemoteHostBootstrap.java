package br.com.wdc.shopping.view.remote.host;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.framework.cube.remote.RemoteHostModule;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.config.AppConfig;
import io.javalin.config.JavalinConfig;

import java.time.Duration;

/**
 * Bootstrap for the remote.host infrastructure.
 * <p>
 * Registers all Javalin routes and manages the application registry lifecycle.
 * <p>
 * Usage in the backend:
 * <pre>
 * // During server configuration:
 * RemoteHostBootstrap.configure(javalinConfig);
 *
 * // During business context startup:
 * RemoteHostBootstrap.start();
 *
 * // During shutdown:
 * RemoteHostBootstrap.stop();
 * </pre>
 */
public final class RemoteHostBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteHostBootstrap.class);

    // Development-only defaults — MUST be replaced in production via application.local.toml [remote] section
    private static final String DEV_CIPHER_PUBLIC_KEY  = "1ekh:3n88eu224huxfvj7lndkkf4n2vye4lus611fecnoc57qod2m7d";
    private static final String DEV_CIPHER_PRIVATE_KEY = "2n9arhz94hevkz4ge8vxwje5c37k7aqol1st01wvzln81u5m69";
    private static final String DEV_SIGN_PUBLIC_KEY    = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAIkDxriJZ2BLyg26A7hR-qzJPRSj33156sXy_r6JLa0NWz2uY1z9FwnQRtrU3CztutyAIhwyHaOxfMGWyvgFsokCAwEAAQ==";
    private static final String DEV_SIGN_PRIVATE_KEY   = "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAiQPGuIlnYEvKDboDuFH6rMk9FKPffXnqxfL-voktrQ1bPa5jXP0XCdBG2tTcLO263IAiHDIdo7F8wZbK-AWyiQIDAQABAkABpau1PygHILu4tTC0ZEsblbnhltdHxfPW2m_KGUVqXjg71xASB-0rctP7pu9qgOPaj_ltTki3xHXQX07QKnJZAiEAvxFzS6c6FqJ8LbrVta72W5i-pb3AkLAM-wyoPmAOOxsCIQC3k8lagaTvRvdlkLrfJZ3K4q4JcsUHG6M2h43P34SfKwIgYtC9ljTIYAhsvKHSAQKZusmGX-WA_9NtAzGKmafH9F0CIGVwnpUKio9F0bMn1Hs2GAliVPUXnFQfK4MYSH6Tbn9dAiEAimwgt_xSziP2RejiFY3_Ek6ROpRG6uL9s89NuaoGFvY=";

    private static RemoteHostModule<ShoppingApplicationImpl> module;

    private RemoteHostBootstrap() {
    }

    /**
     * Starts the application registry (flush loop, expiry checker).
     */
    public static void start() {
        module.start();
    }

    /**
     * Stops the application registry and releases resources.
     */
    public static void stop() {
        module.stop();
    }

    /**
     * Registers all remote.host routes on the given Javalin configuration.
     * Context paths are discovered from the frontend directory (work/frontend/).
     */
    public static void configure(JavalinConfig config) {
        var appConfig = AppConfig.load();

        // Ensure [remote] keys exist in application.local.toml; if absent, write dev defaults and warn
        ensureRemoteSecurityKeys(appConfig);

        // Bridge AppConfig [remote] → System properties consumed by RemoteAppSecurity
        propagateRemoteKey(appConfig, "remote.cipherPublicKey",  "wdc.web.public_key");
        propagateRemoteKey(appConfig, "remote.cipherPrivateKey", "wdc.web.private_key");
        propagateRemoteKey(appConfig, "remote.signPublicKey",    "wdc.sign.wdc.web.public_key");
        propagateRemoteKey(appConfig, "remote.signPrivateKey",   "wdc.sign.wdc.web.private_key");

        int maxSessions = appConfig.getInt("server.maxSessions", -1);

        var security = RemoteAppSecurity.createDefault();
        var registry = new RemoteApplicationRegistry<>(ShoppingApplicationImpl::createApp);

        if (maxSessions > 0) {
            registry.setMaxInstances(maxSessions);
            LOG.info("Session limit configured: maxSessions={}", maxSessions);
        }

        int sessionTtlSeconds = appConfig.getInt("server.sessionTtlSeconds", -1);
        if (sessionTtlSeconds == 0) {
            registry.setSessionTimeSpan(Duration.ZERO);
            LOG.info("Session TTL disabled: sessions released immediately on disconnect");
        } else if (sessionTtlSeconds > 0) {
            registry.setSessionTimeSpan(Duration.ofSeconds(sessionTtlSeconds));
            LOG.info("Session TTL configured: {}s", sessionTtlSeconds);
        }

        ShoppingApplicationImpl.initialize(security, registry);

        String[] contextPaths = discoverFrontendContextPaths();
        module = new RemoteHostModule<>(security, registry, contextPaths);
        module.configure(config);
    }

    private static void propagateRemoteKey(AppConfig config, String configKey, String sysProp) {
        var value = config.get(configKey);
        if (value != null && !value.isBlank()) {
            System.setProperty(sysProp, value);
        }
    }

    /**
     * Writes the development-default RSA keys into {@code application.local.toml} when absent.
     * This ensures the app starts correctly on a fresh checkout without manual configuration.
     * The log warning reminds operators to replace these keys before production deployment.
     */
    private static void ensureRemoteSecurityKeys(AppConfig appConfig) {
        if (appConfig.get("remote.cipherPublicKey") != null) {
            return; // already configured
        }
        Path localToml = appConfig.getConfigFilePath().resolveSibling("application.local.toml");
        String block = "\n[remote]\n"
                + "# RSA key pair for client handshake (cipher: exponent:modulus in base36)\n"
                + "# CHANGE THESE KEYS IN PRODUCTION — these are development-only defaults\n"
                + "cipherPublicKey = \"" + DEV_CIPHER_PUBLIC_KEY + "\"\n"
                + "cipherPrivateKey = \"" + DEV_CIPHER_PRIVATE_KEY + "\"\n"
                + "# RSA key pair for URL/intent signing (URL-safe Base64 DER)\n"
                + "# CHANGE THESE KEYS IN PRODUCTION — these are development-only defaults\n"
                + "signPublicKey = \"" + DEV_SIGN_PUBLIC_KEY + "\"\n"
                + "signPrivateKey = \"" + DEV_SIGN_PRIVATE_KEY + "\"\n";
        try {
            Files.writeString(localToml, block, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOG.warn("Remote security keys were absent — dev defaults written to {}", localToml);
            LOG.warn("IMPORTANT: replace [remote] keys in application.local.toml before production deployment.");
        } catch (IOException e) {
            LOG.error("Failed to bootstrap remote security keys into {}", localToml, e);
        }
    }

    private static String[] discoverFrontendContextPaths() {
        Path frontendBase = ShoppingConfig.getBaseDir().resolve("frontend");
        if (!Files.isDirectory(frontendBase)) {
            LOG.warn("Frontend directory not found: {} — no context paths registered", frontendBase);
            return new String[0];
        }

        try (Stream<Path> subdirs = Files.list(frontendBase)) {
            String[] paths = subdirs
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith("remote."))
                    .toArray(String[]::new);
            LOG.info("Discovered frontend context paths: {}", (Object) paths);
            return paths;
        } catch (IOException e) {
            LOG.warn("Failed to scan frontend directory: {}", frontendBase, e);
            return new String[0];
        }
    }
}
