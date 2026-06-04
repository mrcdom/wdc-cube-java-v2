package br.com.wdc.shopping.view.remote.host;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.framework.cube.remote.RemoteHostModule;
import br.com.wdc.shopping.domain.ShoppingConfig;
import io.javalin.config.JavalinConfig;

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
        var security = RemoteAppSecurity.createDefault();
        var registry = new RemoteApplicationRegistry<>(ShoppingApplicationImpl::createApp);

        ShoppingApplicationImpl.initialize(security, registry);

        String[] contextPaths = discoverFrontendContextPaths();
        module = new RemoteHostModule<>(security, registry, contextPaths);
        module.configure(config);
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
