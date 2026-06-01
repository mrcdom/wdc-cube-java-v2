package br.com.wdc.shopping.view.remote.host;

import br.com.wdc.shopping.view.remote.host.app.ShoppingApplicationRegistry;
import br.com.wdc.shopping.view.remote.host.javalin.DispatcherController;
import br.com.wdc.shopping.view.remote.host.javalin.IndexHtmlController;
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

    private RemoteHostBootstrap() {
    }

    /**
     * Starts the application registry (flush loop, expiry checker).
     */
    public static void start() {
        ShoppingApplicationRegistry.init();
    }

    /**
     * Stops the application registry and releases resources.
     */
    public static void stop() {
        ShoppingApplicationRegistry.shutdown();
    }

    /**
     * Registers all remote.host routes on the given Javalin configuration.
     */
    public static void configure(JavalinConfig config) {
        DispatcherController.configure(config);
        IndexHtmlController.configure(config);
    }
}
