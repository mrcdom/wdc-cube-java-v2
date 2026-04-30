package br.com.wdc.shopping.view.jfx.util;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

public final class JfxUtil {

    private JfxUtil() {
        super();
    }

    public static void removeFromParent(Node node) {
        if (node != null && node.getParent() instanceof Pane pane) {
            pane.getChildren().remove(node);
        }
    }
}
