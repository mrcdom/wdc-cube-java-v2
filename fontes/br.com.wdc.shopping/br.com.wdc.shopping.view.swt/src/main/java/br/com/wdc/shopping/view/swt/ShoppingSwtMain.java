package br.com.wdc.shopping.view.swt;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.h2.jdbcx.JdbcDataSource;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.log.Slf4jLogFactory;
import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.framework.commons.sql.SqlDataSourceDelegate;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.config.AppConfig;
import br.com.wdc.shopping.domain.security.CryptoProvider;
import br.com.wdc.shopping.domain.security.JceCryptoProvider;
import br.com.wdc.shopping.persistence.RepositoryBootstrap;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;

public class ShoppingSwtMain {

    private static final Log LOG = Log.getLogger(ShoppingSwtMain.class);

    private ScheduledExecutorService executorService;
    private ShoppingSwtApplication app;
    private boolean devMode;

    public static void main(String[] args) {
        Log.setFactory(new Slf4jLogFactory());
        new ShoppingSwtMain().run();
    }

    private void run() {
        try {
            init();
        } catch (Exception e) {
            LOG.error("Failed to initialize application", e);
            System.exit(1);
        }

        var display = Display.getDefault();

        var shell = new Shell(display, SWT.SHELL_TRIM);
        shell.setText("WeDoCode Shopping");
        shell.setSize(1024, 768);
        shell.setLayout(new StackLayout());

        this.app = new ShoppingSwtApplication(display, shell);
        this.app.setDevMode(this.devMode);
        this.app.start();
        Routes.root(this.app);

        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        stop();
        display.dispose();
    }

    private void init() throws Exception {
        var config = AppConfig.load();
        ShoppingConfig.Internals.configure(config);
        this.devMode = config.getBoolean("dev.mode", false);

        CryptoProvider.BEAN.set(new JceCryptoProvider());

        this.executorService = Executors.newScheduledThreadPool(2);
        ScheduledExecutor.BEAN.set(new ScheduledExecutorSwtAdapter(this.executorService, Display.getDefault()));

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
