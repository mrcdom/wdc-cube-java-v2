package br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation;

/**
 * Client-side mirror of {@code Routes} from the Shopping presentation layer.
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
 *   restrict_home_product(id)       → "product?productId=&lt;id&gt;"
 *   restrict_home_receipt(id)       → "receipt?purchaseId=&lt;id&gt;"
 * </pre>
 *
 * <h3>Required parameters</h3>
 * <ul>
 * <li>{@link #restrict_home_product(long)} — {@code productId} — read by {@code ProductPresenter.applyParameters}</li>
 * <li>{@link #restrict_home_receipt(long)} — {@code purchaseId} — read by {@code ReceiptPresenter.applyParameters}</li>
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
     * @param productId required — corresponds to {@code PlaceParameters.PRODUCT_ID}, read by
     *                  {@code ProductPresenter.applyParameters}
     */
    public static String restrict_home_product(long productId) {
        return "product?productId=" + productId;
    }

    /**
     * Path for the Receipt place.
     *
     * @param purchaseId required — corresponds to {@code PlaceParameters.PURCHASE_ID}, read by
     *                   {@code ReceiptPresenter.applyParameters}
     */
    public static String restrict_home_receipt(long purchaseId) {
        return "receipt?purchaseId=" + purchaseId;
    }
}
