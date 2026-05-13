package br.com.wdc.shopping.view.gluon.desktop;

import br.com.wdc.shopping.view.gluon.ShoppingGluonMain;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Desktop-specific Application subclass — adds window/taskbar icons.
 * <p>
 * Extracted as a top-level class so GraalVM native-image can discover its no-arg constructor
 * via reflection (required by {@code Application.launch()}).
 * </p>
 */
public class DesktopShoppingGluonMain extends ShoppingGluonMain {

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
