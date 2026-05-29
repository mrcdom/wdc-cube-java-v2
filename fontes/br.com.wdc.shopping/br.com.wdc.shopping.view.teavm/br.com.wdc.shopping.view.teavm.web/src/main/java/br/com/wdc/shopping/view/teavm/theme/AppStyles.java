package br.com.wdc.shopping.view.teavm.theme;

import static br.com.wdc.shopping.view.teavm.theme.BsColors.*;

/**
 * Composite inline-style constants for the TeaVM views.
 * <p>
 * Centralizes repeated style patterns so views don't embed raw color/size values.
 */
public class AppStyles {

    private AppStyles() {
    }

    // ---- Page layout ----
    public static final String PAGE_WRAPPER = "max-width:900px;margin:0 auto;padding:12px";

    // ---- Cards ----
    public static final String CARD = "background-color:" + SURFACE + ";border-radius:12px;border:1px solid "
            + BORDER + ";padding:16px";
    public static final String CARD_LARGE = "background-color:" + SURFACE + ";border-radius:12px;border:1px solid "
            + BORDER + ";padding:24px";
    public static final String LOGIN_CARD = "max-width:400px;width:calc(100% - 32px);border-radius:16px;border:none;overflow:hidden";

    // ---- Navbar ----
    public static final String NAVBAR = "background-color:" + PRIMARY + ";min-height:56px;"
            + "box-shadow:0 2px 4px rgba(0,0,0,0.1);"
            + "display:flex;flex-wrap:nowrap;align-items:center;justify-content:space-between;"
            + "padding-top:env(safe-area-inset-top, 0px)";
    public static final String NAVBAR_BUTTON = "background:none;border:none;color:" + TEXT_ON_PRIMARY_85
            + ";font-size:1.25rem;cursor:pointer;padding:4px 6px;display:flex;align-items:center";
    public static final String NAVBAR_TEXT = "color:" + TEXT_ON_PRIMARY_85 + ";font-size:0.9rem";
    public static final String NAVBAR_TEXT_BOLD = "color:" + TEXT_ON_PRIMARY + ";font-weight:600;font-size:0.9rem";

    // ---- App header (login & navbar logo) ----
    public static final String APP_HEADER = "background:" + PRIMARY_LIGHT
            + ";padding:1.5rem 2rem;display:flex;align-items:center;gap:0.75rem";
    public static final String APP_LOGO_TEXT = "color:" + TEXT_ON_PRIMARY
            + ";font-size:1.5rem;font-weight:700;letter-spacing:0.5px";
    public static final String APP_LOGO_TEXT_SM = "color:" + TEXT_ON_PRIMARY
            + ";font-size:1.1rem;font-weight:700;letter-spacing:0.5px;display:block;line-height:1.2";
    public static final String APP_LOGO_SUBTITLE = "color:" + TEXT_ON_PRIMARY_65 + ";font-size:0.75rem";
    public static final String APP_LOGO_SUBTITLE_SM = "color:" + TEXT_ON_PRIMARY_55
            + ";font-size:0.6rem;letter-spacing:0.3px";
    public static final String APP_LOGO_ICON = "color:" + ACCENT + ";font-size:1.75rem";
    public static final String APP_LOGO_ICON_SM = "color:" + ACCENT + ";font-size:1.25rem";

    // ---- Form inputs ----
    public static final String INPUT = "border-radius:8px;border:1px solid " + BORDER_INPUT + ";font-size:1rem";
    public static final String INPUT_DISABLED = INPUT + ";opacity:0.6;pointer-events:none";

    // ---- Buttons ----
    public static final String BTN_PRIMARY_LG = "border-radius:8px;padding:0.75rem;font-size:1.1rem;"
            + "background:" + PRIMARY_LIGHT + ";border:none";
    public static final String BTN_PRIMARY_LG_DISABLED = BTN_PRIMARY_LG + ";opacity:0.7;pointer-events:none";
    public static final String BTN_LINK = "color:" + PRIMARY + ";text-decoration:underline;font-size:0.85rem";

    // ---- Section labels ----
    public static final String SECTION_LABEL = "color:" + TEXT_SECONDARY + ";font-size:0.85rem";

    // ---- Prices ----
    public static final String PRICE = "font-weight:bold;color:" + PRIMARY;
    public static final String PRICE_LG = "font-size:1.5rem;font-weight:bold;color:" + PRIMARY + ";margin:0 0 12px 0";
    public static final String PRICE_MD = "font-size:1.1rem;font-weight:bold;color:" + PRIMARY;

    // ---- Cart badge ----
    public static final String CART_BADGE = "position:absolute;top:-2px;right:-6px;font-size:10px;min-width:18px;"
            + "text-align:center;background-color:" + ACCENT + ";color:white;border-radius:50%;padding:2px 5px";

    // ---- Cart button (navbar) ----
    public static final String CART_BUTTON = "background:none;border:none;color:" + TEXT_ON_PRIMARY_85
            + ";cursor:pointer;padding:4px 8px;display:flex;align-items:center;gap:6px;position:relative";

    // ---- Empty state ----
    public static final String EMPTY_STATE_ICON = "width:120px;height:120px;background-color:" + PRIMARY_SURFACE_LIGHT
            + ";border-radius:50%;display:flex;align-items:center;justify-content:center;margin-bottom:16px;"
            + "font-size:48px;color:" + PRIMARY;

    // ---- Lock icon (login) ----
    public static final String LOCK_ICON = "display:inline-flex;align-items:center;justify-content:center;"
            + "width:48px;height:48px;border-radius:50%;background:" + PRIMARY_SURFACE + ";color:" + PRIMARY
            + ";font-size:1.5rem";

    // ---- List items ----
    public static final String LIST_ITEM = "display:flex;align-items:center;padding:10px 0;border-bottom:1px solid "
            + BORDER_LIGHT;
    public static final String PURCHASE_ITEM = "width:100%;box-sizing:border-box;background-color:" + SURFACE_TERTIARY
            + ";border-radius:6px;border-left:3px solid " + PRIMARY
            + ";cursor:pointer;transition:all 0.15s;margin-bottom:6px;overflow:hidden";

    // ---- Product card ----
    public static final String PRODUCT_CARD = "background-color:" + SURFACE + ";border-radius:12px;border:1px solid "
            + BORDER + ";cursor:pointer;transition:all 0.25s cubic-bezier(0.4,0,0.2,1);overflow:hidden";
    public static final String PRODUCT_IMAGE_PANE = "background-color:" + SURFACE_SECONDARY
            + ";padding:16px;border-bottom:1px solid " + BORDER_LIGHT;

    // ---- Success banner ----
    public static final String SUCCESS_BANNER = "background-color:" + SUCCESS_SURFACE + ";border:1px solid "
            + SUCCESS_BORDER + ";border-radius:8px;padding:12px 16px;color:" + SUCCESS_DARK + ";font-size:1.2rem";
    public static final String SUCCESS_TEXT_STYLE = "color:" + SUCCESS_DARK + ";font-weight:bold;font-size:1rem";

    // ---- Receipt ----
    public static final String RECEIPT_BOX = "border:1px solid " + BORDER_MEDIUM + ";border-radius:8px;padding:16px;"
            + "font-family:'Courier New',Courier,monospace;font-size:0.85rem";

    // ---- Separator ----
    public static final String SEPARATOR = "border-top:1px solid " + BORDER;

    // ---- Hint box (login) ----
    public static final String HINT_BOX = "border:1px dashed " + BORDER_INPUT
            + ";border-radius:8px;padding:0.6rem 1rem;color:" + TEXT_MUTED + ";font-size:0.85rem";

    // ---- Content pane ----
    public static final String CONTENT_PANE = "background-color:" + SURFACE_MUTED + ";min-height:0";
}
