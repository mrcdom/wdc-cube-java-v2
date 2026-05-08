package br.com.wdc.shopping.view.gluon;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import br.com.wdc.framework.commons.log.Log;

import br.com.wdc.shopping.api.client.OkHttpTransport;
import br.com.wdc.shopping.api.client.RestConfig;
import br.com.wdc.shopping.api.client.RestRepositoryBootstrap;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.config.AppConfig;
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
        var restConfig = new RestConfig(new OkHttpTransport(apiUrl));
        RestRepositoryBootstrap.initialize(restConfig);

        LOG.info("Backend initialized with REST client → {}", apiUrl);
    }

    @Override
    public void start(Stage primaryStage) {
        LOG.info("Starting Gluon application...");

        this.app = new ShoppingGluonApplication();

        var root = new StackPane();
        this.app.setRootPane(root);

        var isDesktop = !isIOS() && !isAndroid();
        var sceneWidth = isDesktop ? 1024.0 : 360.0;
        var sceneHeight = isDesktop ? 768.0 : 640.0;

        var scene = new Scene(root, sceneWidth, sceneHeight);

        primaryStage.setTitle("WDC Shopping — Gluon Mobile");
        primaryStage.setScene(scene);
        primaryStage.show();

        this.app.start();
        Routes.root(this.app);
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
