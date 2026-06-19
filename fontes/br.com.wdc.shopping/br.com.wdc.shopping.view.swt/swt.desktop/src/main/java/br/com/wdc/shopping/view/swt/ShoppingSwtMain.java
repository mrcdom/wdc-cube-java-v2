package br.com.wdc.shopping.view.swt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.h2.jdbcx.JdbcDataSource;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.log.Slf4jLogFactory;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.config.AppConfig;
import br.com.wdc.framework.domain.security.CryptoProvider;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.persistence.impl.ShoppingRepositoryBootstrap;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;

public class ShoppingSwtMain {

    private static final Log LOG = Log.getLogger(ShoppingSwtMain.class);

    /** Kept alive for the app lifetime — NSApplication retains it after setApplicationIconImage. */
    @SuppressWarnings({"unused", "java:S1450", "java:S1068"})
    private static Image dockIconImage;

    private final Defer cleanUp = new Defer();
    private ScheduledExecutorService executorService;
    private ShoppingSwtApplication app;
    private boolean devMode;

    public static void main(String[] args) {
        Log.setFactory(new Slf4jLogFactory());
        installGtkFonts();
        new ShoppingSwtMain().run();
    }

    /**
     * On GTK/Linux, SWT's {@code display.loadFont()} adds the font to fontconfig
     * app-fonts but Pango's font map cache is NOT refreshed in the same JVM session.
     * Pre-installing fonts to {@code ~/.local/share/fonts/} and running
     * {@code fc-cache -f} ensures they are visible to Pango before Display is created.
     */
    private static void installGtkFonts() {
        if (!"gtk".equals(org.eclipse.swt.SWT.getPlatform())) return;
        try {
            var fontsDir = Path.of(System.getProperty("user.home"), ".local", "share", "fonts");
            Files.createDirectories(fontsDir);
            var fontResource = ShoppingSwtMain.class.getClassLoader()
                    .getResourceAsStream("fonts/bootstrap-icons.ttf");
            if (fontResource == null) {
                LOG.warn("bootstrap-icons.ttf not found in classpath, icons will not render");
                return;
            }
            var dest = fontsDir.resolve("bootstrap-icons.ttf");
            try (fontResource) {
                Files.copy(fontResource, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            var result = new ProcessBuilder("fc-cache", "-f", fontsDir.toString())
                    .redirectErrorStream(true)
                    .start();
            result.waitFor();
            LOG.info("GTK fonts installed to {}", dest);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Failed to pre-install GTK fonts: {}", e.getMessage());
        } catch (IOException e) {
            LOG.warn("Failed to pre-install GTK fonts: {}", e.getMessage());
        }
    }

    private void run() {
        try {
            init(cleanUp);
        } catch (Exception e) {
            LOG.error("Failed to initialize application", e);
            System.exit(1);
        }

        var display = Display.getDefault();
        // macOS: must be called AFTER Display.getDefault() — Display's constructor calls
        // NSApplicationLoad() which reinitialises NSApplication and overrides any
        // icon set via -Xdock:icon or apple.awt.application.icon.
        var appIcon = loadAppIcon(display);
        setDockIcon(appIcon);

        var shell = new Shell(display, SWT.SHELL_TRIM);
        if (appIcon != null) {
            shell.setImage(appIcon);
        }
        shell.setText("WeDoCode Shopping");
        shell.setSize(800, 820);

        var monitor = display.getPrimaryMonitor().getBounds();
        var size = shell.getSize();
        shell.setLocation((monitor.width - size.x) / 2, (monitor.height - size.y) / 2);

        shell.setLayout(new StackLayout());

        this.app = new ShoppingSwtApplication(display, shell);
        cleanUp.push(this.app::release);

        this.app.setDevMode(this.devMode);
        this.app.start();
        tryRestoreSession();

        // Ensure Routes.Place enum is class-loaded (registers GoActions)
        Routes.Place.values();

        var savedIntent = tryRestoreIntent();
        if (savedIntent != null) {
            this.app.safeGo(savedIntent);
        } else {
            this.app.runPresenterAction(() -> Routes.root(this.app));
        }
        this.app.runPresenterAction(() -> this.app.markAllViewsDirty());

        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        stop();
        display.dispose();
    }

    private void init(Defer cleanUp) throws Exception {
        var config = ShoppingConfig.loadConfig();
        ShoppingConfig.Internals.configure(config);
        this.devMode = config.getBoolean("dev.mode", false);

        CryptoProvider.BEAN.set(new JceCryptoProvider());
        cleanUp.push(() -> CryptoProvider.BEAN.set(null));

        this.executorService = Executors.newScheduledThreadPool(2);
        cleanUp.push(() -> {
            this.executorService.shutdownNow();
            this.executorService = null;
        });

        ScheduledExecutor.BEAN.set(new ScheduledExecutorSwtAdapter(this.executorService, Display.getDefault()));
        cleanUp.push(() -> ScheduledExecutor.BEAN.set(null));

        var dataDir = ShoppingConfig.getDataDir();
        var dataSource = new JdbcDataSource();
        dataSource.setURL(resolveJdbcUrl(config, dataDir));
        dataSource.setUser(config.get("database.username", "sa"));
        dataSource.setPassword(config.get("database.password", "sa"));

        ShoppingRepositoryBootstrap.initialize(dataSource, cleanUp);

        try (var connection = dataSource.getConnection()) {
            var command = new DBCreate().withConnection(connection);
            if (config.getBoolean("database.reset", false)) {
                command.withReset();
            }
            command.run();
        }

        LOG.info("Backend initialized with database at {}", dataDir);
    }

    /**
     * Sets the macOS Dock icon via {@code NSApplication.setApplicationIconImage()}.
     *
     * <p>Must be called <em>after</em> {@code Display.getDefault()} — SWT's Display constructor
     * calls {@code NSApplicationLoad()} which reinitialises {@code NSApplication} and resets any
     * icon previously set through {@code -Xdock:icon} or {@code apple.awt.application.icon}.
     *
     * <p>Uses SWT's own Cocoa internal bindings via reflection. The created {@code Image} is kept
     * in {@link #dockIconImage} for the app lifetime; {@code NSApplication} retains the underlying
     * {@code NSImage} after the call.
     */
    @SuppressWarnings("java:S3011")
    private static void setDockIcon(Image appIcon) {
        if (appIcon == null) {
            return;
        }
        dockIconImage = appIcon;
        try {
            var nsImageClass = Class.forName("org.eclipse.swt.internal.cocoa.NSImage");
            var handleField  = Image.class.getDeclaredField("handle");
            handleField.setAccessible(true);
            var nsImage = nsImageClass.getDeclaredConstructor(long.class)
                    .newInstance(handleField.getLong(appIcon));

            var nsAppClass = Class.forName("org.eclipse.swt.internal.cocoa.NSApplication");
            var sharedApp  = nsAppClass.getMethod("sharedApplication").invoke(null);
            nsAppClass.getMethod("setApplicationIconImage", nsImageClass).invoke(sharedApp, nsImage);
        } catch (Exception e) {
            LOG.debug("Could not set Dock icon via NSApplication: {}", e.getMessage());
        }
    }

    private static Image loadAppIcon(Display display) {
        try (var stream = ShoppingSwtMain.class.getResourceAsStream("/images/app-icon.png")) {
            if (stream == null) {
                LOG.debug("app-icon.png not found in classpath");
                return null;
            }
            return new Image(display, new ImageData(stream).scaledTo(512, 512));
        } catch (IOException e) {
            LOG.debug("Could not load app icon: {}", e.getMessage());
            return null;
        }
    }

    private void stop() {
        cleanUp.run();
    }

    private void tryRestoreSession() {
        if (this.app == null) return;
        var storage = this.app.clientPersistentStore();

        var savedUserId = storage.get("session.userId");
        if (savedUserId == null) return;

        try {
            var userId = Long.parseLong(savedUserId);
            var users = this.app.getUserRepository().fetch(
                    new UserCriteria().withUserId(userId).withProjection(Subject.projection()), 0, 1);
            if (!users.isEmpty()) {
                this.app.setSubject(Subject.create(users.get(0)));
                LOG.info("Session restored for userId={}", userId);
            } else {
                storage.remove("session.userId");
            }
        } catch (Exception e) {
            LOG.warn("Session restore failed: {}", e.getMessage());
            storage.remove("session.userId");
        }
    }

    private String tryRestoreIntent() {
        if (this.app.getSubject() == null) return null;

        var storage = this.app.clientPersistentStore();

        var savedIntent = storage.get("session.intent");
        if (savedIntent == null || savedIntent.isBlank()) return null;

        // Don't restore login intent — let Routes.root decide
        if (savedIntent.startsWith("login")) return null;

        return savedIntent;
    }

    private static String resolveJdbcUrl(AppConfig config, Path dataDir) {
        var url = config.get("database.url");
        if (url != null && !url.isBlank()) {
            return url;
        }
        return "jdbc:h2:file:" + dataDir.resolve("wedocode-shopping").toAbsolutePath();
    }
}
