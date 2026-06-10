package br.com.wdc.shopping.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.log.Slf4jLogFactory;

import br.com.wdc.shopping.persistence.rest.RepositoryApiRoutes;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.config.AppConfig;
import br.com.wdc.shopping.backend.controller.DevDbResetController;
import br.com.wdc.shopping.backend.controller.DevGcController;
import br.com.wdc.shopping.backend.controller.DevHeapController;
import br.com.wdc.shopping.backend.controller.DevReloadController;
import br.com.wdc.shopping.backend.controller.ImageController;
import br.com.wdc.shopping.backend.controller.LandingPageController;
import br.com.wdc.shopping.backend.controller.StatusController;
import br.com.wdc.shopping.backend.controller.WebCacheController;
import br.com.wdc.shopping.view.remote.host.RemoteHostBootstrap;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.staticfiles.Location;
import org.eclipse.jetty.ee10.servlet.FilterHolder;

/**
 * Standalone Javalin-based HTTP server for WeDoCode Shopping React frontend.
 * 
 * This server: - Serves React static assets (HTML, CSS, JS) from classpath - Manages ApplicationReactImpl instances for
 * session state - Cleans up expired sessions periodically
 */
@SuppressWarnings({ "java:S2139", "java:S1192" })
public class BackendServer {

    public static void main(String[] args) {
        BackendServer.doMain(args);
    }

    private static final Log LOG;

    private static final String STATIC_FILES_DIR = "/META-INF/resources";
    private static final String STATIC_IMAGES_FILES_DIR = STATIC_FILES_DIR + "/images";
    private static final String STATIC_HOSTED_IMAGE_PATH = "/images";
    
    private static final int DEFAULT_PORT = 8080;

    static {
        LogBootstrap.initialize();
        Log.setFactory(new Slf4jLogFactory());
        LOG = Log.getLogger(BackendServer.class);
    }

    private final Javalin app;
    private final int port;
    private final boolean devMode;
    private final BusinessContext businessContext = new BusinessContext();

    public BackendServer(int port, boolean devMode) {
        this.port = port;
        this.devMode = devMode;
        this.businessContext.configure();
        this.app = createJavalinApp();
        this.businessContext.start();
    }

    public BackendServer() {
        this(DEFAULT_PORT, false);
    }

    /**
     * Creates and configures the Javalin application instance.
     */
    private Javalin createJavalinApp() {
        return Javalin.create(config -> {
            // Javalin 7+ built-in virtual thread support for request handling.
            config.concurrency.useVirtualThreads = true;

            // Configure WebSocket idle timeout at the WebSocket container level.
            // Two minutes keeps a reasonable fail-fast window while leaving ample margin
            // over the 15-second keepalive used by browser and server.
            config.jetty.modifyWebSocketServletFactory(wsFactory -> wsFactory.setIdleTimeout(Duration.ofMinutes(2)));

            // Register MIME types and strip charset for binary/module resources.
            // BinaryContentTypeFilter ensures .wasm → application/wasm and .mjs → application/javascript
            // without the charset suffix that Jetty appends and browsers reject.
            config.jetty.modifyServletContextHandler(handler -> {
                handler.getMimeTypes().addMimeMapping("wasm", "application/wasm");
                handler.getMimeTypes().addMimeMapping("mjs", "application/javascript");
                var filterHolder = new FilterHolder(new BinaryContentTypeFilter());
                handler.addFilter(filterHolder, "/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));
            });

            // Enable CORS for Tauri desktop app, Android WebView, and local dev
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                rule.allowHost("tauri://localhost", "https://tauri.localhost",
                        "http://tauri.localhost",
                        "http://localhost:8080", "http://shopping-wdc.localhost:8080");
                rule.allowCredentials = true;
            }));

            // Serve frontend assets from work/frontend/<subdir> (each subdir at its own context)
            configureFrontendStaticFiles(config);

            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.directory = STATIC_IMAGES_FILES_DIR;
                staticFileConfig.location = Location.CLASSPATH;
                staticFileConfig.hostedPath = STATIC_HOSTED_IMAGE_PATH;
                staticFileConfig.precompressMaxSize = 0;
            });

            // Configure default content type
            config.http.defaultContentType = "application/json";

            configureRoutes(config);
        });
    }

    /**
     * Configures all HTTP routes and WebSocket endpoints.
     */
    private void configureRoutes(JavalinConfig config) {
        config.routes.exception(Exception.class, (e, ctx) -> {
            LOG.error("Unhandled exception in request processing", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        });

        config.routes.before(ctx -> LOG.debug("HTTP {} {}", ctx.method(), ctx.path()));

        // Health check endpoint
        StatusController.configure(config);
        ImageController.configure(config);
        WebCacheController.configure(config);

        // Repository REST API for Android (and other REST clients)
		RepositoryApiRoutes.configure(config, "");

        // Landing page: lists available frontend contexts
        LandingPageController.configure(config);

        // Remote host: WebSocket dispatcher + session cookies
        RemoteHostBootstrap.configure(config);

        // Dev-mode live reload: WebSocket + notify endpoint
        if (devMode) {
            DevReloadController.configure(config);
            DevDbResetController.configure(config);
            DevHeapController.configure(config);
            DevGcController.configure(config);
        }

        // SPA fallback: redirect unmatched paths within a frontend context to its index.html.
        // This must be last, after all specific routes are defined.
        config.routes.before(ctx -> {
            // @formatter:off
            String path = ctx.path();
            // Exclude API, WebSocket, health, and static resources from SPA redirect
            if (!path.startsWith("/api/") 
                && !path.contains("/api/")
                && !path.startsWith("/ws/") 
                && !path.startsWith("/health")
                && !path.startsWith("/dispatcher")
                && !path.startsWith("/web-cache/")
                && !path.startsWith("/__dev/")
                && !path.equals("/")
                && !isStaticResource(path)) {
                // Resolve context-aware SPA fallback: /<context>/anything -> /<context>/index.html
                int secondSlash = path.indexOf('/', 1);
                if (secondSlash > 0) {
                    String contextPath = path.substring(0, secondSlash);
                    LOG.debug("SPA fallback for path: {} -> {}/index.html", path, contextPath);
                    ctx.redirect(contextPath + "/index.html");
                }
            }
            // @formatter:on
        });
    }

    /**
     * Checks if a path is likely a static resource (based on extension).
     */
    private static boolean isStaticResource(String path) {
        // @formatter:off
        return path.endsWith(".js") || 
               path.endsWith(".mjs") || 
               path.endsWith(".wasm") || 
               path.endsWith(".css") || 
               path.endsWith(".html") || 
               path.endsWith(".json") || 
               path.endsWith(".map") ||
               path.endsWith(".png") || 
               path.endsWith(".jpg") || 
               path.endsWith(".gif") ||
               path.endsWith(".svg") ||
               path.endsWith(".woff") || 
               path.endsWith(".woff2") ||
               path.endsWith(".ttf") ||
               path.endsWith(".otf") ||
               path.equals("/");
        // @formatter:on
    }

    /**
     * Scans subdirectories under {@code {basedir}/frontend/} and registers each one
     * as an external static file source served at its own context path ({@code /<dirname>/}).
     * Also registers API routes under each context so SPAs can access the API
     * without cross-origin issues.
     */
    private void configureFrontendStaticFiles(JavalinConfig config) {
        Path frontendBase = ShoppingConfig.getBaseDir().resolve("frontend");
        if (!Files.isDirectory(frontendBase)) {
            LOG.info("Frontend directory not found: {} — skipping external static files", frontendBase);
            return;
        }

        try (Stream<Path> subdirs = Files.list(frontendBase)) {
            subdirs.filter(Files::isDirectory).forEach(subdir -> {
                String dirPath = subdir.toString();
                String contextName = subdir.getFileName().toString();
                String contextPrefix = "/" + contextName;
                config.staticFiles.add(staticFileConfig -> {
                    staticFileConfig.directory = dirPath;
                    staticFileConfig.location = Location.EXTERNAL;
                    staticFileConfig.hostedPath = contextPrefix;
                    staticFileConfig.precompressMaxSize = 0;
                });
                // Register API routes under this context: /<context>/api/...
                RepositoryApiRoutes.configure(config, contextPrefix);
                LOG.info("Serving frontend context '{}' from: {} (with API at {}/api/)", contextName, dirPath, contextPrefix);
            });
        } catch (IOException e) {
            LOG.warn("Failed to scan frontend directory: {}", frontendBase, e);
        }
    }

    /**
     * Starts the HTTP server.
     */
    public void start() {
        try {
            app.start(port);
            LOG.info("Javalin server started on port {}", port);
        } catch (Exception e) {
            LOG.error("Failed to start Javalin server", e);
            throw new AssertionError("Server startup failed", e);
        }
    }

    /**
     * Stops the HTTP server and cleans up resources gracefully.
     */
    public void stop() {
        try {
            if (devMode) {
                DevReloadController.stop();
            }
            businessContext.stop();

            if (app != null) {
                app.stop();
            }
            LOG.info("Javalin server stopped");
        } catch (Exception e) {
            LOG.error("Error stopping server", e);
        }
    }

    /**
     * Entry point for the application.
     */
    public static void doMain(String[] args) {
        var config = AppConfig.load();
        int port = config.getInt("server.port", DEFAULT_PORT);

        // Parse port from command line arguments if provided (overrides config)
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                LOG.warn("Invalid port number: {}, using default {}", args[0], DEFAULT_PORT);
            }
        }

        // Check for port environment variable (overrides config and args)
        String portEnv = System.getenv("SERVER_PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {
                LOG.warn("Invalid SERVER_PORT environment variable: {}, using default {}", portEnv, DEFAULT_PORT);
            }
        }

        LOG.info("Starting WeDoCode Shopping React Server on port {}", port);

        boolean devMode = config.getBoolean("server.devMode", false);
        BackendServer server = new BackendServer(port, devMode);

        // Graceful shutdown on JVM termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown signal received");
            server.stop();
        }));

        server.start();

        // Keep JVM running
        try {
            Thread.currentThread().join();
        } catch (@SuppressWarnings("java:S2142") InterruptedException ignored) {
            LOG.info("Main thread interrupted");
            server.stop();
        }
    }
}
