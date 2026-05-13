package br.com.wdc.shopping.view.gluon.desktop;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.log.Slf4jLogFactory;
import javafx.application.Application;

/**
 * Desktop launcher — starts the Gluon application on a standard JVM and configures desktop-specific features
 * (window/taskbar icons).
 * <p>
 * Run with: {@code mvn javafx:run} or {@code mvn gluonfx:run}
 * </p>
 */
public class ShoppingGluonDesktopLauncher {

    public static void main(String[] args) {
        Log.setFactory(new Slf4jLogFactory());
        setDockIcon();
        Application.launch(DesktopShoppingGluonMain.class, args);
    }

    private static void setDockIcon() {
        try {
            if (java.awt.Taskbar.isTaskbarSupported()) {
                var taskbar = java.awt.Taskbar.getTaskbar();
                if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                    var iconStream = ShoppingGluonDesktopLauncher.class.getResourceAsStream("/icon-256.png");
                    if (iconStream != null) {
                        taskbar.setIconImage(javax.imageio.ImageIO.read(iconStream));
                    }
                }
            }
        } catch (Throwable e) {
            // Silently ignore — AWT is not available in GluonFX native images
        }
    }
}
