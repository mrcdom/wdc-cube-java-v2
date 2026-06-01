package br.com.wdc.shopping.view.remote.host;

import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.framework.cube.remote.RemoteHostModule;
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
     */
    public static void configure(JavalinConfig config) {
        var security = RemoteAppSecurity.createDefault();
        var registry = new RemoteApplicationRegistry<>(ShoppingApplicationImpl::createApp);

        ShoppingApplicationImpl.initialize(security, registry);

        module = new RemoteHostModule<>(security, registry, "shopping");
        module.configure(config);
    }
}
