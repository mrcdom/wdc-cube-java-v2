package br.com.wdc.shopping.view.jfx;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.framework.commons.sql.SqlDataSourceDelegate;
import br.com.wdc.shopping.business.impl.RepositoryBootstrap;
import br.com.wdc.shopping.business.impl.sgbd.ddl.scripts.DBCreate;
import br.com.wdc.shopping.business.shared.ShoppingConfig;
import br.com.wdc.shopping.presentation.presenter.Routes;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ShoppingJfxMain extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ShoppingJfxMain.class);

    private ScheduledExecutorService executorService;
    private ShoppingJfxApplication app;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        Path baseDir = resolveRuntimeBaseDir();
        ShoppingConfig.Internals.setBaseDir(baseDir);
        ShoppingConfig.Internals.setConfigDir(createDir(baseDir.resolve("config")));
        ShoppingConfig.Internals.setDataDir(createDir(baseDir.resolve("data")));
        ShoppingConfig.Internals.setLogDir(createDir(baseDir.resolve("log")));
        ShoppingConfig.Internals.setTempDir(createDir(baseDir.resolve("temp")));

        this.executorService = Executors.newScheduledThreadPool(2);
        ScheduledExecutor.BEAN.set(new ScheduledExecutorJfxAdapter(this.executorService));

        var dataDir = ShoppingConfig.getDataDir();
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:" + dataDir.resolve("wedocode-shopping").toAbsolutePath());
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        SqlDataSource.BEAN.set(new SqlDataSourceDelegate(dataSource));

        try (var connection = dataSource.getConnection()) {
            var command = new DBCreate().withConnection(connection);
            if (Boolean.getBoolean("wedocode.shopping.db.reset")) {
                command.withReset();
            }
            command.run();
        }

        RepositoryBootstrap.initialize();
        LOG.info("Backend initialized with database at {}", dataDir);
    }

    @Override
    public void start(Stage primaryStage) {
        this.app = new ShoppingJfxApplication();

        var root = new StackPane();
        this.app.setRootPane(root);

        var scene = new Scene(root, 1024, 768);
        scene.getStylesheets().add(this.getClass().getResource("/META-INF/resources/styles/app.css").toExternalForm());

        primaryStage.setTitle("WeDoCode Shopping");
        primaryStage.setScene(scene);
        primaryStage.show();

        this.app.start();
        Routes.root(this.app);
    }

    @Override
    public void stop() throws Exception {
        if (this.app != null) {
            this.app.release();
            this.app = null;
        }

        if (this.executorService != null) {
            this.executorService.shutdownNow();
            this.executorService = null;
        }

        RepositoryBootstrap.release();
        ScheduledExecutor.BEAN.set(null);
        SqlDataSource.BEAN.set(null);
    }

    private static Path resolveRuntimeBaseDir() {
        var dir = System.getProperty("wedocode.shopping.basedir");
        if (dir != null && !dir.isBlank()) {
            return Paths.get(dir);
        }
        return Paths.get(System.getProperty("user.dir"), "work");
    }

    private static Path createDir(Path path) throws Exception {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }
}
