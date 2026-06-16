package br.com.wdc.shopping.view.remote.host;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.commons.security.RSA;
import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.framework.cube.remote.RemoteHostModule;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.framework.domain.config.AppConfig;
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
        var appConfig = ShoppingConfig.loadConfig();

        // Ensure [remote] keys exist in application.local.toml; if absent, write dev defaults and warn
        ensureRemoteSecurityKeys(appConfig);

        // Bridge AppConfig [remote] → System properties consumed by RemoteAppSecurity
        propagateRemoteKey(appConfig, "remote.rsaPublicKey",  "wdc.web.public_key");
        propagateRemoteKey(appConfig, "remote.rsaPrivateKey", "wdc.web.private_key");
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
     * Generates fresh RSA key pairs and writes them into {@code application.local.toml} when absent.
     * Keys are randomly generated on each fresh checkout — no hardcoded values exist in the source.
     * The log warning reminds operators to replace these keys before production deployment.
     */
    private static void ensureRemoteSecurityKeys(AppConfig appConfig) {
        if (appConfig.get("remote.rsaPublicKey") != null) {
            return; // already configured
        }

        var rnd = new SecureRandom();

        // RSA key pair for client handshake (custom RSA class, base36 format)
        var handshakeRsa = new RSA(512, rnd);
        String rsaPublicKey = handshakeRsa.getPublicExponent().toString(36) + ":" + handshakeRsa.getPublicKey().toString(36);
        String rsaPrivateKey = handshakeRsa.getPrivateKey().toString(36);

        // RSA key pair for URL/intent signing (standard JCA, DER URL-safe Base64 format)
        String signPublicKey;
        String signPrivateKey;
        try {
            var keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512, rnd);
            var keyPair = keyGen.generateKeyPair();
            var b64 = Base64.getUrlEncoder().withoutPadding();
            signPublicKey = b64.encodeToString(keyPair.getPublic().getEncoded());
            signPrivateKey = b64.encodeToString(keyPair.getPrivate().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("RSA not available", e);
        }

        // Propagate to System properties immediately — AppConfig was already loaded before
        // this method ran, so appConfig.get() won't see the values we're about to write.
        System.setProperty("wdc.web.public_key",          rsaPublicKey);
        System.setProperty("wdc.web.private_key",         rsaPrivateKey);
        System.setProperty("wdc.sign.wdc.web.public_key", signPublicKey);
        System.setProperty("wdc.sign.wdc.web.private_key",signPrivateKey);

        Path localToml = appConfig.getConfigFilePath().resolveSibling("application.local.toml");
        String block = "\n[remote]\n"
                + "# RSA key pair for client handshake (format: exponent:modulus in base36)\n"
                + "# Generated automatically — REPLACE THESE KEYS IN PRODUCTION\n"
                + "rsaPublicKey = \"" + rsaPublicKey + "\"\n"
                + "rsaPrivateKey = \"" + rsaPrivateKey + "\"\n"
                + "# RSA key pair for URL/intent signing (URL-safe Base64 DER)\n"
                + "# Generated automatically — REPLACE THESE KEYS IN PRODUCTION\n"
                + "signPublicKey = \"" + signPublicKey + "\"\n"
                + "signPrivateKey = \"" + signPrivateKey + "\"\n";
        try {
            Files.writeString(localToml, block, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOG.warn("Remote security keys were absent — fresh keys generated and written to {}", localToml);
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
