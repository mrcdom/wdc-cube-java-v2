package br.com.wdc.shopping.view.swing;

import java.awt.Dimension;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatLightLaf;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.framework.commons.sql.SqlDataSourceDelegate;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.config.AppConfig;
import br.com.wdc.shopping.persistence.RepositoryBootstrap;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;
import br.com.wdc.shopping.view.swing.util.StackPanel;
import br.com.wdc.shopping.view.swing.util.Styles;

public class ShoppingSwingMain {

    private static final Logger LOG = LoggerFactory.getLogger(ShoppingSwingMain.class);

    private ScheduledExecutorService executorService;
    private ShoppingSwingApplication app;
    private JFrame frame;
    private boolean devMode;

    public static void main(String[] args) {
        new ShoppingSwingMain().run();
    }

    private void run() {
        try {
            init();
            SwingUtilities.invokeAndWait(this::startUI);
        } catch (Exception e) {
            LOG.error("Failed to start application", e);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private void init() throws Exception {
        var config = AppConfig.load();
        ShoppingConfig.Internals.configure(config);
        this.devMode = config.getBoolean("dev.mode", false);

        this.executorService = Executors.newScheduledThreadPool(2);
        ScheduledExecutor.BEAN.set(new ScheduledExecutorSwingAdapter(this.executorService));

        var dataDir = ShoppingConfig.getDataDir();
        var dataSource = new JdbcDataSource();
        dataSource.setURL(resolveJdbcUrl(config, dataDir));
        dataSource.setUser(config.get("database.username", "sa"));
        dataSource.setPassword(config.get("database.password", "sa"));
        SqlDataSource.BEAN.set(new SqlDataSourceDelegate(dataSource));

        try (var connection = dataSource.getConnection()) {
            var command = new DBCreate().withConnection(connection);
            if (config.getBoolean("database.reset", false)) {
                command.withReset();
            }
            command.run();
        }

        RepositoryBootstrap.initialize();
        LOG.info("Backend initialized with database at {}", dataDir);
    }

    private void startUI() {
        FlatLightLaf.setup();
        Styles.applyGlobalDefaults();

        this.app = new ShoppingSwingApplication();
        this.app.setDevMode(this.devMode);

        var root = new StackPanel();
        root.setOpaque(true);
        root.setBackground(Styles.BG_PAGE);
        this.app.setRootPane(root);

        this.frame = new JFrame("WeDoCode Shopping");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setContentPane(root);
        this.frame.setPreferredSize(new Dimension(1024, 768));
        this.frame.pack();
        this.frame.setLocationRelativeTo(null);
        this.frame.setVisible(true);

        this.app.start();
        Routes.root(this.app);
    }

    private void stop() {
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

    private static String resolveJdbcUrl(AppConfig config, Path dataDir) {
        var url = config.get("database.url");
        if (url != null && !url.isBlank()) {
            return url;
        }
        return "jdbc:h2:file:" + dataDir.resolve("wedocode-shopping").toAbsolutePath();
    }
}
