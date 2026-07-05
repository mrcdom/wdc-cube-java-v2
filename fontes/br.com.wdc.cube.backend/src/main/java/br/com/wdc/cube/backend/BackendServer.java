package br.com.wdc.cube.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jetty.ee10.servlet.FilterHolder;

import br.com.wdc.cube.backend.controller.DevDbResetController;
import br.com.wdc.cube.backend.controller.DevGcController;
import br.com.wdc.cube.backend.controller.DevHeapController;
import br.com.wdc.cube.backend.controller.DevReloadController;
import br.com.wdc.cube.backend.controller.ImageController;
import br.com.wdc.cube.backend.controller.LandingPageController;
import br.com.wdc.cube.backend.controller.StatusController;
import br.com.wdc.cube.backend.controller.WebCacheController;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.log.Slf4jLogFactory;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.config.AppConfig;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.persistence.rest.RepositoryApiRoutes;
import br.com.wdc.shopping.view.remote.host.RemoteHostBootstrap;
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
public class BackendServer {

    public static void main(String[] args) {
        BackendServer.doMain(args);
    }

    private static final Log LOG;

    private static final String STATIC_FILES_DIR = "/META-INF/resources";
    private static final String STATIC_IMAGES_FILES_DIR = STATIC_FILES_DIR + "/images";
    private static final String STATIC_HOSTED_IMAGE_PATH = "/images";

    private static final int DEFAULT_PORT = 8080;

    /**
     * CORS allowlist applied when {@code server.cors.allowAll} is {@code false}.
     * Covers the Tauri desktop app (its three origin schemes), Android WebView and local dev hosts.
     */
    private static final List<String> DEFAULT_ALLOWED_HOSTS = List.of(
            "tauri://localhost", "https://tauri.localhost", "http://tauri.localhost",
            "http://localhost:8080", "http://shopping-wdc.localhost:8080");

    /**
     * Resolved CORS policy. When {@code allowAll} is {@code true} any origin is reflected
     * (suitable for development); otherwise only {@code allowedHosts} are accepted.
     */
    public record CorsSettings(boolean allowAll, List<String> allowedHosts, boolean allowCredentials) {
        public static CorsSettings defaults() {
            return new CorsSettings(false, DEFAULT_ALLOWED_HOSTS, true);
        }
    }

    static {
        LogBootstrap.initialize();
        Log.setFactory(new Slf4jLogFactory());
        LOG = Log.getLogger(BackendServer.class);
    }

    private final Defer cleanUp = new Defer();
    private final int port;
    private final boolean devMode;
    private final CorsSettings corsSettings;
    private final BusinessContext businessContext = new BusinessContext();
    private Javalin app;

    public BackendServer(int port, boolean devMode) {
        this(port, devMode, CorsSettings.defaults());
    }

    public BackendServer(int port, boolean devMode, CorsSettings corsSettings) {
        this.port = port;
        this.devMode = devMode;
        this.corsSettings = corsSettings;
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

            // Enable CORS. Policy resolved from config (server.cors.*): either reflect any origin
            // (allowAll, for development) or restrict to an allowlist (Tauri desktop, Android WebView, local dev).
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                if (corsSettings.allowAll()) {
                    // reflectClientOrigin echoes the request Origin instead of emitting `*`, which is the
                    // only way to allow any origin together with credentials (browsers reject `*` + credentials,
                    // and Javalin's anyHost() throws in that case).
                    LOG.info("CORS: reflecting any client origin (server.cors.allowAll=true)");
                    rule.reflectClientOrigin = true;
                } else {
                    var hosts = corsSettings.allowedHosts().isEmpty()
                            ? DEFAULT_ALLOWED_HOSTS
                            : corsSettings.allowedHosts();
                    LOG.info("CORS: restricting to allowed hosts {}", hosts);
                    rule.allowHost(hosts.get(0), hosts.subList(1, hosts.size()).toArray(new String[0]));
                }
                rule.allowCredentials = corsSettings.allowCredentials();
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
            DevReloadController.configure(config, cleanUp);
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
     * Scans subdirectories under {@code {basedir}/frontend/} and registers each one as an external static file source
     * served at its own context path ({@code /<dirname>/}). Also registers API routes under each context so SPAs can
     * access the API without cross-origin issues.
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
                LOG.info("Serving frontend context '{}' from: {} (with API at {}/api/)", contextName, dirPath,
                        contextPrefix);
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
            this.businessContext.configure(cleanUp);
            this.app = createJavalinApp();
            cleanUp.push(this.app::stop);

            this.businessContext.start(cleanUp);

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
            cleanUp.run();
            LOG.info("Javalin server stopped");
        } catch (Exception e) {
            LOG.error("Error stopping server", e);
        }
    }

    /**
     * Entry point for the application.
     */
    public static void doMain(String[] args) {
        var config = ShoppingConfig.loadConfig();
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
        CorsSettings corsSettings = resolveCorsSettings(config);
        BackendServer server = new BackendServer(port, devMode, corsSettings);

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

    /**
     * Resolves the CORS policy from configuration (section {@code [server]}):
     * <ul>
     * <li>{@code cors.allowAll} — when {@code true}, any origin is reflected (development). Default {@code false}.</li>
     * <li>{@code cors.allowedHosts} — comma-separated allowlist used when {@code allowAll} is {@code false}.
     * When absent/blank, falls back to {@link #DEFAULT_ALLOWED_HOSTS}.</li>
     * <li>{@code cors.allowCredentials} — whether to allow cookies/auth headers cross-origin. Default {@code true}.</li>
     * </ul>
     */
    static CorsSettings resolveCorsSettings(AppConfig config) {
        boolean allowAll = config.getBoolean("server.cors.allowAll", false);
        boolean allowCredentials = config.getBoolean("server.cors.allowCredentials", true);
        List<String> allowedHosts = parseHostList(config.get("server.cors.allowedHosts"), DEFAULT_ALLOWED_HOSTS);
        return new CorsSettings(allowAll, allowedHosts, allowCredentials);
    }

    /**
     * Parses a comma-separated host list, trimming entries and dropping blanks.
     * Returns {@code defaults} when the raw value is {@code null}, blank or yields no entries.
     */
    static List<String> parseHostList(String raw, List<String> defaults) {
        if (raw == null || raw.isBlank()) {
            return defaults;
        }
        List<String> hosts = Arrays.stream(raw.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
        return hosts.isEmpty() ? defaults : hosts;
    }
}
