package br.com.wdc.shopping.view.gluon.android;

import br.com.wdc.shopping.view.gluon.ShoppingGluonMain;

/**
 * Android launcher — entry point for the GraalVM native-image Android build.
 * <p>
 * Build with: {@code mvn -f br.com.wdc.shopping.view.gluon.android gluonfx:build gluonfx:package}
 * <br>
 * Note: Android native builds require a <strong>Linux</strong> host (or CI with Linux).
 * </p>
 */
public class ShoppingGluonAndroidLauncher {

    public static void main(String[] args) {
        ShoppingGluonMain.main(args);
    }
}
