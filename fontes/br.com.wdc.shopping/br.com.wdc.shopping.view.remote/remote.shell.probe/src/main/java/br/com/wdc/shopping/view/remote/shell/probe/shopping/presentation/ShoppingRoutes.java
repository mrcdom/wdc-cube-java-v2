package br.com.wdc.shopping.view.remote.shell.probe.shopping.presentation;

/**
 * Client-side mirror of the {@code XxxKeys} classes of the Shopping presentation layer (place names and the names
 * they read from the address).
 * <p>
 * Static factory methods that build unsigned path strings for use with {@code HostClient.navigate()} — one method per
 * navigable place.
 *
 * <h3>Path format</h3> The path is the token of the deepest place in the navigation chain (same string returned by
 * {@code CubeIntent.toString()} on the server side):
 * 
 * <pre>
 *   open_login()                    → "login"
 *   restrict_home()                 → "home"
 *   restrict_home_cart()            → "cart"
 *   restrict_home_product(id)       → "product?pr-id=&lt;id&gt;"
 *   restrict_home_receipt(id)       → "receipt?rc-id=&lt;id&gt;"
 * </pre>
 *
 * <h3>Required parameters</h3>
 * <ul>
 * <li>{@link #restrict_home_product(long)} — {@code pr-id} — {@code ProductKeys.productId()}</li>
 * <li>{@link #restrict_home_receipt(long)} — {@code rc-id} — {@code ReceiptKeys.purchaseId()}</li>
 * </ul>
 */
@SuppressWarnings("java:S3400")
public final class ShoppingRoutes {

    private ShoppingRoutes() {
    }

    /** Path for the Login place. */
    public static String open_login() {
        return "login";
    }

    /** Path for the Home place. */
    public static String restrict_home() {
        return "home";
    }

    /** Path for the Cart place. */
    public static String restrict_home_cart() {
        return "cart";
    }

    /**
     * Path for the Product place.
     *
     * @param productId required — {@code pr-id}, read by {@code ProductKeys.productId()}
     */
    public static String restrict_home_product(long productId) {
        return "product?pr-id=" + productId;
    }

    /**
     * Path for the Receipt place.
     *
     * @param purchaseId required — {@code rc-id}, read by {@code ReceiptKeys.purchaseId()}
     */
    public static String restrict_home_receipt(long purchaseId) {
        return "receipt?rc-id=" + purchaseId;
    }
}
