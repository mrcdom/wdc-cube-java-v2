package br.com.wdc.shopping.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.log.Slf4jLogFactory;

import br.com.wdc.shopping.persistence.rest.RepositoryApiRoutes;
import br.com.wdc.shopping.domain.config.AppConfig;
import br.com.wdc.shopping.backend.controller.DispatcherController;
import br.com.wdc.shopping.backend.controller.ImageController;
import br.com.wdc.shopping.backend.controller.IndexHtmlController;
import br.com.wdc.shopping.backend.controller.LandingPageController;
import br.com.wdc.shopping.backend.controller.StatusController;
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

    public static void main(String[] args) {
        JavalinApplication.doMain(args);
    }

    private static final Log LOG;

    private static final String STATIC_FILES_DIR = "/META-INF/resources";
    private static final String STATIC_IMAGES_FILES_DIR = STATIC_FILES_DIR + "/images";
    private static final String STATIC_HOSTED_IMAGE_PATH = "/images";
    private static final String FRONTEND_DIR = "work/frontend";
    
    private static final int DEFAULT_PORT = 8080;

    static {
        Log.setFactory(new Slf4jLogFactory());
        LOG = Log.getLogger(JavalinApplication.class);
    }

    private final Javalin app;
    private final int port;
    private final BusinessContext businessContext = new BusinessContext();

    public JavalinApplication(int port) {
        this.port = port;
        this.businessContext.start();
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

            // TeaVM compiled webapp served under /teavm/
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.directory = "/META-INF/resources/teavm";
                staticFileConfig.location = Location.CLASSPATH;
                staticFileConfig.hostedPath = "/teavm";
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

        // Repository REST API for Android (and other REST clients)
		RepositoryApiRoutes.configure(config);

        // Landing page: lists available frontend contexts
        LandingPageController.configure(config);

        // WebSocket dispatcher for bidirectional communication
        DispatcherController.configure(config);

        // Replicates WdcAppIdFilter: generates and sets app_id + app_skey cookies
        // when the SPA entrypoint is requested so the client gets a valid,
        // server-signed session ID on the very first page load.
        IndexHtmlController.configure(config);

        // SPA fallback: redirect unmatched paths within a frontend context to its index.html.
        // This must be last, after all specific routes are defined.
        config.routes.before(ctx -> {
            // @formatter:off
            String path = ctx.path();
            // Exclude API, WebSocket, health, and static resources from SPA redirect
            if (!path.startsWith("/api/") 
                && !path.startsWith("/ws/") 
                && !path.startsWith("/health")
                && !path.startsWith("/dispatcher")
                && !path.startsWith("/teavm")
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

    /**
     * Scans subdirectories under {@code work/frontend/} and registers each one
     * as an external static file source served at its own context path ({@code /<dirname>/}).
     * This allows Parcel-built frontends to be served directly during development.
     */
    private void configureFrontendStaticFiles(JavalinConfig config) {
        Path frontendBase = Paths.get(FRONTEND_DIR).toAbsolutePath().normalize();
        if (!Files.isDirectory(frontendBase)) {
            LOG.info("Frontend directory not found: {} — skipping external static files", frontendBase);
            return;
        }

        try (Stream<Path> subdirs = Files.list(frontendBase)) {
            subdirs.filter(Files::isDirectory).forEach(subdir -> {
                String dirPath = subdir.toString();
                String contextName = subdir.getFileName().toString();
                config.staticFiles.add(staticFileConfig -> {
                    staticFileConfig.directory = dirPath;
                    staticFileConfig.location = Location.EXTERNAL;
                    staticFileConfig.hostedPath = "/" + contextName;
                    staticFileConfig.precompressMaxSize = 0;
                });
                LOG.info("Serving frontend context '{}' from: {}", contextName, dirPath);
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
        } catch (@SuppressWarnings("java:S2142") InterruptedException ignored) {
            LOG.info("Main thread interrupted");
            server.stop();
        }
    }
}
