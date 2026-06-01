package br.com.wdc.framework.cube.remote;

import br.com.wdc.framework.cube.remote.javalin.DispatcherController;
import br.com.wdc.framework.cube.remote.javalin.IndexHtmlController;
import io.javalin.config.JavalinConfig;

/**
 * Bootstrap module for a single remote application.
 * <p>
 * Each application creates its own module with its own security, registry,
 * and context paths. The backend calls {@code configure/start/stop} on each module.
 * <p>
 * Example:
 * <pre>
 * var security = RemoteAppSecurity.createDefault();
 * var registry = new RemoteApplicationRegistry&lt;&gt;(myFactory);
 * var module = new RemoteHostModule(security, registry, "shopping", "admin");
 *
 * // During Javalin configuration:
 * module.configure(javalinConfig);
 *
 * // During startup:
 * module.start();
 *
 * // During shutdown:
 * module.stop();
 * </pre>
 */
public final class RemoteHostModule<T extends RemoteApplication> {

    private final RemoteAppSecurity security;
    private final RemoteApplicationRegistry<T> registry;
    private final String[] contextPaths;

    private final DispatcherController dispatcherController;
    private final IndexHtmlController indexHtmlController;

    public RemoteHostModule(RemoteAppSecurity security,
                            RemoteApplicationRegistry<T> registry,
                            String... contextPaths) {
        this.security = security;
        this.registry = registry;
        this.contextPaths = contextPaths;
        this.dispatcherController = new DispatcherController(registry, security);
        this.indexHtmlController = new IndexHtmlController(security);
    }

    /**
     * Registers all routes (WebSocket dispatcher + session cookies) for this module.
     */
    public void configure(JavalinConfig config) {
        dispatcherController.configure(config, contextPaths);
        indexHtmlController.configure(config, contextPaths);
    }

    /**
     * Starts the application registry (flush loop, expiry checker).
     */
    public void start() {
        registry.init();
    }

    /**
     * Stops the application registry and releases resources.
     */
    public void stop() {
        registry.shutdown();
    }

    public RemoteAppSecurity getSecurity() {
        return security;
    }

    public RemoteApplicationRegistry<T> getRegistry() {
        return registry;
    }
}
