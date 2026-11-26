package br.com.wdc.store.view.remote.host;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.framework.cube.remote.RemoteHostModule;
import br.com.wdc.store.domain.StoreConfig;
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

    private static RemoteHostModule<StoreApplicationImpl> module;

    private RemoteHostBootstrap() {
    }

    /**
     * Starts the application registry (flush loop, expiry checker).
     */
    public static void start(Defer cleanUp) {
        module.start(cleanUp);
    }

    /**
     * Registers all remote.host routes on the given Javalin configuration.
     * Context paths are discovered from the frontend directory (work/frontend/).
     */
    public static void configure(JavalinConfig config) {
        var appConfig = StoreConfig.loadConfig();

        // The key that signs session ids and addresses: from [remote] signingKey, generated on the first start
        System.setProperty(RemoteAppSecurity.SIGNING_KEY_PROPERTY, ensureSigningKey(appConfig));

        int maxSessions = appConfig.getInt("server.maxSessions", -1);

        var security = RemoteAppSecurity.fromSystemProperty();
        var registry = new RemoteApplicationRegistry<>(StoreApplicationImpl::createApp);

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

        StoreApplicationImpl.initialize(security, registry);

        String[] contextPaths = discoverFrontendContextPaths();
        module = new RemoteHostModule<>(security, registry, contextPaths);
        module.configure(config);
    }

    /**
     * The {@code [remote] signingKey} of the configuration. When there is none, a new random key is written into
     * {@code application.local.toml} (inside its {@code [remote]} table, or in a new one), so that a restart keeps
     * accepting the session ids and addresses signed before.
     */
    private static String ensureSigningKey(AppConfig appConfig) {
        var configured = appConfig.get("remote.signingKey");
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }

        var key = RemoteAppSecurity.newEncodedKey();
        var line = "# HMAC-SHA256 key that signs the remote sessions and addresses — generated on the first start; "
                + "instances that accept each other's sessions share it\nsigningKey = \"" + key + "\"\n";
        Path localToml = appConfig.getConfigFilePath().resolveSibling("application.local.toml");
        try {
            var text = Files.exists(localToml) ? Files.readString(localToml) : "";
            var table = Pattern.compile("(?m)^\\[remote\\][ \\t]*$").matcher(text);
            if (table.find()) {
                var rest = table.end() < text.length() ? text.substring(table.end() + 1) : "";
                text = text.substring(0, table.end()) + "\n" + line + rest;
            } else {
                text = text + (text.isEmpty() || text.endsWith("\n") ? "" : "\n") + "\n[remote]\n" + line;
            }
            Files.writeString(localToml, text);
            LOG.warn("The remote signing key was absent; a new one was written to {}", localToml);
        } catch (IOException e) {
            LOG.error("Failed to write the remote signing key into {}; it lasts until the server stops", localToml, e);
        }
        return key;
    }

    private static String[] discoverFrontendContextPaths() {
        Path frontendBase = StoreConfig.getBaseDir().resolve("frontend");
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
