package br.com.wdc.shopping.view.react;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.shopping.domain.config.AppConfig;
import br.com.wdc.shopping.view.react.controller.DispatcherController;
import br.com.wdc.shopping.view.react.controller.ImageController;
import br.com.wdc.shopping.view.react.controller.IndexHtmlController;
import br.com.wdc.shopping.view.react.controller.StatusController;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.staticfiles.Location;

/**
 * Standalone Javalin-based HTTP server for WeDoCode Shopping React frontend.
 * 
 * This server: - Serves React static assets (HTML, CSS, JS) from classpath - Manages ApplicationReactImpl instances for
 * session state - Cleans up expired sessions periodically
 */
@SuppressWarnings({ "java:S2139", "java:S1192" })
public class JavalinApplication {

    private static final Logger LOG = LoggerFactory.getLogger(JavalinApplication.class);

    private static final String STATIC_FILES_DIR = "/META-INF/resources";
    private static final String STATIC_IMAGES_FILES_DIR = STATIC_FILES_DIR + "/images";
    private static final String STATIC_FILES_EXTERNAL_DIR_ENV = "SHOPPING_STATIC_FILES_DIR";
    private static final String STATIC_FILES_EXTERNAL_DIR_PROPERTY = "shopping.staticFilesDir";
    private static final String STATIC_HOSTED_IMAGE_PATH = "/images";
    
    private static final int DEFAULT_PORT = 8080;
    private static final record StaticFilesSettings(String directory, Location location) {}

    private final Javalin app;
    private final int port;
    private final BusinessContext businessContext = new BusinessContext();
    private final StaticFilesSettings staticFilesSettings = resolveStaticFilesSettings();

    

    public JavalinApplication(int port) {
        this.port = port;
        this.app = createJavalinApp();
    }

    public JavalinApplication() {
        this(DEFAULT_PORT);
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

            // Enable static file serving from META-INF/resources
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.directory = staticFilesSettings.directory;
                staticFileConfig.location = staticFilesSettings.location;
                staticFileConfig.precompressMaxSize = 0;
            });

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

        // Force the SPA root through /index.html so bootstrap cookies are always issued
        // before the frontend constructor tries to read app_id and app_skey.
        config.routes.get("/", ctx -> ctx.redirect("/index.html"));

        // WebSocket dispatcher for bidirectional communication
        DispatcherController.configure(config);

        // Replicates WdcAppIdFilter: generates and sets app_id + app_skey cookies
        // when the SPA entrypoint is requested so the client gets a valid,
        // server-signed session ID on the very first page load.
        IndexHtmlController.configure(config);

        // SPA fallback: redirect unmatched routes to index.html for React Router.
        // This must be last, after all specific routes are defined.
        config.routes.before(ctx -> {
            // @formatter:off
            String path = ctx.path();
            // Exclude API, WebSocket, health, and static resources from SPA redirect
            if (!path.startsWith("/api/") 
                && !path.startsWith("/ws/") 
                && !path.startsWith("/health")
                && !path.startsWith("/dispatcher")
                && !path.equals("/index.html")
                && !isStaticResource(path)) {
                LOG.debug("SPA fallback for path: {}", path);
                ctx.redirect("/index.html");
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
               path.equals("/");
        // @formatter:on
    }

    private static StaticFilesSettings resolveStaticFilesSettings() {
        var customStaticDir = System.getProperty(STATIC_FILES_EXTERNAL_DIR_PROPERTY);
        if (customStaticDir == null || customStaticDir.isBlank()) {
            customStaticDir = System.getenv(STATIC_FILES_EXTERNAL_DIR_ENV);
        }

        if (customStaticDir != null && !customStaticDir.isBlank()) {
            Path resolvedPath = Paths.get(customStaticDir).toAbsolutePath().normalize();
            if (Files.isDirectory(resolvedPath)) {
                return new StaticFilesSettings(resolvedPath.toString(), Location.EXTERNAL);
            }

            LOG.warn("External static files directory does not exist: {} (resolved to {}). Falling back to classpath.", customStaticDir, resolvedPath);
        }

        return new StaticFilesSettings(STATIC_FILES_DIR, Location.CLASSPATH);
    }

    /**
     * Starts the HTTP server.
     */
    public void start() {
        try {
            businessContext.start();
            app.start(port);
            LOG.info("Javalin server started on port {}", port);
            LOG.info("Static files served from {}: {}", staticFilesSettings.location, staticFilesSettings.directory);
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
    public static void main(String[] args) {
        var config = AppConfig.load();
        int port = config.getInt("server.port", DEFAULT_PORT);

        // Parse port from command line arguments if provided (overrides config)
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException _) {
                LOG.warn("Invalid port number: {}, using default {}", args[0], DEFAULT_PORT);
            }
        }

        // Check for port environment variable (overrides config and args)
        String portEnv = System.getenv("SERVER_PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException _) {
                LOG.warn("Invalid SERVER_PORT environment variable: {}, using default {}", portEnv, DEFAULT_PORT);
            }
        }

        LOG.info("Starting WeDoCode Shopping React Server on port {}", port);

        JavalinApplication server = new JavalinApplication(port);

        // Graceful shutdown on JVM termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown signal received");
            server.stop();
        }));

        server.start();

        // Keep JVM running
        try {
            Thread.currentThread().join();
        } catch (@SuppressWarnings("java:S2142") InterruptedException _) {
            LOG.info("Main thread interrupted");
            server.stop();
        }
    }
}
