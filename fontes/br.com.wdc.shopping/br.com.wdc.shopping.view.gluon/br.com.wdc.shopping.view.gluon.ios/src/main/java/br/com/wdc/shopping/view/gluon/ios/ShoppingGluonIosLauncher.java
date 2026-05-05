package br.com.wdc.shopping.view.gluon.ios;

import br.com.wdc.shopping.view.gluon.ShoppingGluonMain;

/**
 * iOS launcher — entry point for the GraalVM native-image iOS build.
 * <p>
 * Build with: {@code mvn -f br.com.wdc.shopping.view.gluon.ios gluonfx:build gluonfx:package}
 * <br>
 * Note: iOS builds require <strong>macOS</strong> with Xcode installed.
 * </p>
 */
public class ShoppingGluonIosLauncher {

    public static void main(String[] args) {
        ShoppingGluonMain.main(args);
    }
}
