package br.com.wdc.shopping.view.gluon.util;

/**
 * Platform detection and safe-area insets for GluonFX iOS/Android builds.
 *
 * <p>On iOS the top safe area covers the status bar + Dynamic Island / notch.
 * Without explicit padding the app bar renders under that system chrome.
 */
public final class GluonPlatform {

    private GluonPlatform() {
    }

    public static boolean isIOS() {
        var os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("ios") || "ios".equals(System.getProperty("javafx.platform"));
    }

    public static boolean isAndroid() {
        var os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("android") || "android".equals(System.getProperty("javafx.platform"));
    }

    public static boolean isMobile() {
        return isIOS() || isAndroid();
    }

    /**
     * Top safe-area inset in logical pixels.
     *
     * <p>Uses the primary screen width to distinguish iPhone from iPad:
     * <ul>
     *   <li>iPad (width ≥ 768 pt): only status bar → ~24 pt
     *   <li>iPhone with Dynamic Island (iPhone 14 Pro+): ~59 pt
     *   <li>iPhone with notch (iPhone X–13): ~44 pt
     * </ul>
     * Using 54 pt for iPhone covers notch and Dynamic Island models safely.
     */
    public static double topSafeAreaInset() {
        if (isIOS()) {
            var screenWidth = javafx.stage.Screen.getPrimary().getBounds().getWidth();
            // iPad screens are wider than 768 pt in portrait
            return screenWidth >= 768.0 ? 24.0 : 54.0;
        }
        if (isAndroid()) {
            return 24.0;
        }
        return 0.0;
    }
}
