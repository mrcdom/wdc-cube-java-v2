package br.com.wdc.shopping.view.robovm;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSPathUtilities;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIApplicationDelegateAdapter;
import org.robovm.apple.uikit.UIApplicationLaunchOptions;
import org.robovm.apple.uikit.UINavigationController;
import org.robovm.apple.uikit.UIScreen;
import org.robovm.apple.uikit.UIWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.shopping.api.client.RestConfig;
import br.com.wdc.shopping.api.client.RestRepositoryBootstrap;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.presentation.presenter.Routes;

public class ShoppingRoboVMMain extends UIApplicationDelegateAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ShoppingRoboVMMain.class);

    // TODO: tornar configurável (Settings UI ou plist)
    private static final String SERVER_BASE_URL = "http://localhost:8080";

    private UIWindow window;
    private ShoppingRoboVMApplication app;
    private ScheduledExecutorService executorService;

    @SuppressWarnings("deprecation")
	@Override
    public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions launchOptions) {
        try {
            initBackend();

            app = new ShoppingRoboVMApplication();

            window = new UIWindow(UIScreen.getMainScreen().getBounds());

            var rootController = new UINavigationController();
            rootController.setNavigationBarHidden(true);
            app.setRootController(rootController);
            app.setWindow(window);

            window.setRootViewController(rootController);
            window.makeKeyAndVisible();

            // Start render loop
            RoboVMRenderLoop.start(app);

            // Navigate to root route
            Routes.root(app);

            LOG.info("iOS application started successfully");
        } catch (Exception e) {
            LOG.error("Failed to start iOS application", e);
        }

        return true;
    }

    private void initBackend() {
        // Configure directories
        var documentsDir = new File(new File(NSPathUtilities.getHomeDirectory(), "Documents"), "work");
        configureDirectories(documentsDir);

        // Scheduled executor
        this.executorService = Executors.newScheduledThreadPool(2);
        ScheduledExecutor.BEAN.set(new ScheduledExecutorRoboVMAdapter(this.executorService));

        // REST API client using HttpURLConnection (compatible with RoboVM)
        var restConfig = new RestConfig(SERVER_BASE_URL, new UrlConnectionTransport());
        RestRepositoryBootstrap.initialize(restConfig);

        LOG.info("Backend initialized with REST client → {}", SERVER_BASE_URL);
    }

    private void configureDirectories(File baseDir) {
        ShoppingConfig.Internals.setBaseDir(baseDir);
        ShoppingConfig.Internals.setConfigDir(new File(baseDir, "config"));
        ShoppingConfig.Internals.setDataDir(new File(baseDir, "data"));
        ShoppingConfig.Internals.setLogDir(new File(baseDir, "log"));
        ShoppingConfig.Internals.setTempDir(new File(baseDir, "temp"));
    }

    @Override
    public void willTerminate(UIApplication application) {
        RoboVMRenderLoop.stop();
        if (app != null) {
            app.release();
        }
        RestRepositoryBootstrap.release();
        ScheduledExecutor.BEAN.set(null);
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @SuppressWarnings("unused")
	public static void main(String[] args) {
        try (var pool = new NSAutoreleasePool()) {
            UIApplication.main(args, null, ShoppingRoboVMMain.class);
        }
    }
}
