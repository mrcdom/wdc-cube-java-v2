package br.com.wdc.shopping.view.jfx;

/**
 * Separate launcher class that does NOT extend javafx.application.Application. This bypasses the JavaFX module-path
 * check, allowing the app to run from IDEs (like Eclipse) where JavaFX jars are on the classpath instead of the module
 * path.
 */
public class Launcher {

    public static void main(String[] args) {
        ShoppingJfxMain.main(args);
    }
}
