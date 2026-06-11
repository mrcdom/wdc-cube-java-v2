package br.com.wdc.shopping.view.gluon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.storage.PreferencesClientStorage;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.config.AppConfig;
import br.com.wdc.shopping.persistence.client.OkHttpTransport;
import br.com.wdc.shopping.persistence.client.RestRepositoryBootstrap;
import br.com.wdc.shopping.presentation.presenter.Routes;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ShoppingGluonMain extends Application {

    private static final Log LOG = Log.getLogger(ShoppingGluonMain.class);

    private static final String DEFAULT_API_URL = "http://localhost:8080";

    private ScheduledExecutorService executorService;
    private ShoppingGluonApplication app;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        LOG.info("Initializing Gluon application...");

        // On iOS the working directory is "/" (read-only), so config/application.toml
        // cannot be found relative to CWD. Extract the bundled resource to user.home first.
        if (isIOS() && System.getProperty("shopping.config.file") == null) {
            var userHome = System.getProperty("user.home");
            if (userHome != null && !userHome.isBlank()) {
                var dest = Path.of(userHome, "config", "application.toml");
                if (!Files.exists(dest)) {
                    try (InputStream in = ShoppingGluonMain.class.getResourceAsStream("/config/application.toml")) {
                        if (in != null) {
                            Files.createDirectories(dest.getParent());
                            Files.copy(in, dest);
                        }
                    } catch (IOException e) {
                        LOG.warn("Could not extract bundled config: {}", e.getMessage());
                    }
                }
                System.setProperty("shopping.config.file", dest.toString());
            }
        }

        var config = AppConfig.load();

        // On iOS, the working directory is "/" (read-only).
        // Use user.home (set by GluonFX substrate) as the writable base directory.
        if (config.get("app.basedir") == null) {
            var userHome = System.getProperty("user.home");
            if (userHome != null && !userHome.isBlank()) {
                config = config.withOverride("app.basedir", userHome);
            }
        }

        ShoppingConfig.Internals.configure(config);

        this.executorService = Executors.newScheduledThreadPool(2);

        var apiUrl = config.get("api.url", DEFAULT_API_URL);
        var storage = new PreferencesClientStorage(ShoppingGluonMain.class);
        RestRepositoryBootstrap.initialize(new OkHttpTransport(apiUrl), storage);

        LOG.info("Backend initialized with REST client → {}", apiUrl);
    }

    @Override
    public void start(Stage primaryStage) {
        LOG.info("Starting Gluon application...");

        this.app = new ShoppingGluonApplication();

        var root = new StackPane();
        this.app.setRootPane(root);

        var isDesktop = !isIOS() && !isAndroid();

        // On desktop use a fixed window size; on mobile let GluonFX/iOS fill
        // the full screen (UILaunchScreen in Info.plist opts the app into the
        // native device resolution so no letterboxing occurs).
        var scene = isDesktop ? new Scene(root, 1024.0, 768.0) : new Scene(root);

        primaryStage.setTitle("WDC Shopping — Gluon Mobile");
        primaryStage.setScene(scene);
        primaryStage.show();

        this.app.start();

        // Ensure Routes.Place enum is class-loaded so all GoActions are registered
        // before any navigation (required when bypassing Routes.root below).
        Routes.Place.values();

        // Read saved intent BEFORE any navigation — Routes.root() would call
        // updateHistory() internally and overwrite the persisted value.
        var savedIntent = this.app.clientPersistentStore().get("session.intent");
        if (savedIntent != null && !savedIntent.isBlank() && !savedIntent.startsWith("login")) {
            // Navigate directly; ROOT/RESTRICTED steps will run tryAutoLogin internally.
            this.app.go(savedIntent);
        } else {
            Routes.root(this.app);
        }
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Stopping Gluon application...");

        if (this.app != null) {
            this.app.release();
            this.app = null;
        }

        RestRepositoryBootstrap.release();

        if (this.executorService != null) {
            this.executorService.shutdownNow();
        }
    }

    private static boolean isIOS() {
        var os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("ios") || "ios".equals(System.getProperty("javafx.platform"));
    }

    private static boolean isAndroid() {
        var os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("android") || "android".equals(System.getProperty("javafx.platform"));
    }
}
