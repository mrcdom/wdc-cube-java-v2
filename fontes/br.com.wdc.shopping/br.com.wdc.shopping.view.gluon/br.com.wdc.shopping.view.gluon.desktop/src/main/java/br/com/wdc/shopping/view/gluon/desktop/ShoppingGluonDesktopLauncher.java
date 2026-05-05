package br.com.wdc.shopping.view.gluon.desktop;

import br.com.wdc.shopping.view.gluon.ShoppingGluonMain;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Desktop launcher — starts the Gluon application on a standard JVM and configures desktop-specific features
 * (window/taskbar icons).
 * <p>
 * Run with: {@code mvn javafx:run} or {@code mvn gluonfx:run}
 * </p>
 */
public class ShoppingGluonDesktopLauncher {

    public static void main(String[] args) {
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
        } catch (Exception e) {
            // Silently ignore — not critical
        }
    }

    public static class DesktopShoppingGluonMain extends ShoppingGluonMain {
        @Override
        public void start(Stage primaryStage) {
            super.start(primaryStage);
            loadWindowIcons(primaryStage);
        }

        private void loadWindowIcons(Stage stage) {
            for (var size : new String[] { "16", "32", "48", "256" }) {
                var stream = getClass().getResourceAsStream("/icon-" + size + ".png");
                if (stream != null) {
                    stage.getIcons().add(new Image(stream));
                }
            }
        }
    }
}
