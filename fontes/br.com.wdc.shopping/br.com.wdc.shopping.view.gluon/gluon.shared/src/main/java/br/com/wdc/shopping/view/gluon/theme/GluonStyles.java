package br.com.wdc.shopping.view.gluon.theme;

import static br.com.wdc.shopping.view.gluon.theme.GluonColors.*;

public final class GluonStyles {

    private GluonStyles() {
    }

    // ---- Page backgrounds ----
    public static final String PAGE_BG = "-fx-background-color: " + SURFACE_DIM + ";";

    // ---- Header / AppBar ----
    public static final String HEADER_BAR = "-fx-background-color: " + SURFACE + "; " +
            "-fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;";

    // Gradient header like Flutter headerGradient
    public static final String APP_BAR_PRIMARY = "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, " + PRIMARY + ", " + PRIMARY_END + "); " +
            "-fx-effect: dropshadow(gaussian, rgba(13,102,208,0.3), 8, 0, 0, 2);";

    // ---- Back button ----
    public static final String BACK_BUTTON = "-fx-font-size: 13; -fx-background-color: transparent; " +
            "-fx-text-fill: " + PRIMARY + "; -fx-font-weight: bold; -fx-cursor: hand;";

    // ---- Page / section titles ----
    public static final String PAGE_TITLE = "-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DEFAULT + ";";
    public static final String SECTION_TITLE = "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DEFAULT + ";";
    public static final String SECTION_CAPTION = "-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MUTED + ";";

    // ---- Text styles ----
    public static final String TEXT_BODY_STYLE = "-fx-font-size: 13; -fx-text-fill: " + TEXT_BODY + ";";
    public static final String TEXT_SECONDARY_STYLE = "-fx-font-size: 12; -fx-text-fill: " + TEXT_SECONDARY + ";";
    public static final String TEXT_HINT_STYLE = "-fx-font-size: 12; -fx-text-fill: " + TEXT_HINT + ";";
    public static final String TEXT_MUTED_STYLE = "-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + ";";
    public static final String TEXT_SMALL_WHITE = "-fx-text-fill: " + TEXT_ON_PRIMARY_DIM + "; -fx-font-size: 12;";
    public static final String TEXT_WHITE_BOLD = "-fx-text-fill: " + TEXT_ON_PRIMARY + "; -fx-font-size: 16; -fx-font-weight: bold;";

    // ---- Price / emphasis ----
    public static final String PRICE_LARGE = "-fx-font-size: 22; -fx-text-fill: " + PRIMARY + "; -fx-font-weight: bold;";
    public static final String PRICE_MEDIUM = "-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY + ";";
    public static final String PRICE_SMALL = "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY + ";";
    public static final String TEXT_PRICE_LABEL = "-fx-font-size: 14; -fx-text-fill: " + TEXT_SECONDARY + ";";

    // ---- Buttons ----
    // Pill-shaped accent button — matches Flutter accentButtonStyle
    public static final String BTN_PRIMARY = "-fx-background-color: " + PRIMARY + "; -fx-text-fill: " + TEXT_ON_PRIMARY + "; " +
            "-fx-font-weight: bold; -fx-font-size: 15; -fx-padding: 14 24; " +
            "-fx-background-radius: 20; -fx-cursor: hand;";

    public static final String BTN_SUCCESS = "-fx-background-color: " + PRIMARY + "; -fx-text-fill: " + TEXT_ON_PRIMARY + "; " +
            "-fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 14 24; " +
            "-fx-background-radius: 20; -fx-cursor: hand;";

    public static final String BTN_SUCCESS_BLOCK = "-fx-background-color: " + PRIMARY + "; -fx-text-fill: " + TEXT_ON_PRIMARY + "; " +
            "-fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 14 24; -fx-background-radius: 20;";

    public static final String BTN_CIRCLE = "-fx-background-color: " + CONTROL_BG + "; -fx-background-radius: 50; " +
            "-fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32; " +
            "-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: " + CONTROL_TEXT + "; -fx-cursor: hand;";

    public static final String BTN_PAGINATION = "-fx-background-color: " + SURFACE + "; -fx-background-radius: 20; " +
            "-fx-min-width: 36; -fx-min-height: 36; -fx-font-size: 12; " +
            "-fx-effect: dropshadow(gaussian, " + SHADOW_MEDIUM + ", 4, 0, 0, 1); -fx-cursor: hand;";

    public static final String BTN_DANGER_INLINE = "-fx-background-color: transparent; -fx-text-fill: " + ERROR + "; " +
            "-fx-font-size: 14; -fx-padding: 2 6; -fx-cursor: hand;";

    public static final String BTN_GHOST_WHITE = "-fx-background-color: transparent; " +
            "-fx-text-fill: " + TEXT_ON_PRIMARY_BRIGHT + "; -fx-font-size: 12; -fx-padding: 6 10; -fx-cursor: hand;";

    // ---- Tabs ----
    public static final String TAB_ACTIVE = "-fx-background-color: " + SURFACE + "; -fx-text-fill: " + PRIMARY + "; " +
            "-fx-font-weight: bold; -fx-font-size: 13; -fx-background-radius: 0; " +
            "-fx-border-color: transparent transparent " + PRIMARY + " transparent; -fx-border-width: 0 0 3 0;";

    public static final String TAB_INACTIVE = "-fx-background-color: " + SURFACE + "; -fx-text-fill: " + TEXT_MUTED + "; " +
            "-fx-font-size: 13; -fx-background-radius: 0; " +
            "-fx-border-color: transparent; -fx-border-width: 0 0 3 0;";

    // ---- Cards ----
    // Flutter cardDecoration: white, border, rounded, subtle shadow
    public static final String CARD = "-fx-background-color: " + SURFACE + "; -fx-background-radius: 12; " +
            "-fx-border-color: " + BORDER + "; -fx-border-radius: 12; -fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 3, 0, 0, 1);";

    public static final String CARD_SMALL = "-fx-background-color: " + SURFACE + "; -fx-background-radius: 12; " +
            "-fx-border-color: " + BORDER + "; -fx-border-radius: 12; -fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2); -fx-cursor: hand;";

    public static final String CARD_ITEM = "-fx-background-color: " + SURFACE + "; -fx-background-radius: 4; " +
            "-fx-effect: dropshadow(gaussian, " + SHADOW_LIGHT + ", 2, 0, 0, 1);";

    public static final String CARD_TOP_ROUND = "-fx-background-color: " + SURFACE + "; -fx-background-radius: 16 16 0 0; " +
            "-fx-effect: dropshadow(gaussian, " + SHADOW_MEDIUM + ", 8, 0, 0, -2);";

    public static final String CARD_CLICKABLE = "-fx-background-color: " + SURFACE + "; -fx-background-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2); -fx-cursor: hand;";

    // ---- Scroll ----
    public static final String SCROLL_TRANSPARENT = "-fx-background: transparent; -fx-background-color: transparent;";

    // ---- Error / status ----
    public static final String ERROR_BAR = "-fx-text-fill: " + TEXT_ON_PRIMARY + "; -fx-font-size: 12; -fx-padding: 8 16; " +
            "-fx-background-color: " + ERROR + ";";

    public static final String ERROR_INLINE = "-fx-text-fill: " + TEXT_ON_PRIMARY + "; -fx-font-size: 12; -fx-padding: 8 12; " +
            "-fx-background-color: " + ERROR + "; -fx-background-radius: 6;";

    public static final String ERROR_TEXT = "-fx-text-fill: " + ERROR + "; -fx-font-size: 12;";
    public static final String ERROR_SMALL = "-fx-text-fill: " + ERROR + "; -fx-font-size: 11; -fx-padding: 4 0 0 0;";

    public static final String SUCCESS_BANNER = "-fx-text-fill: " + SUCCESS_TEXT + "; -fx-font-size: 13; " +
            "-fx-font-weight: bold; -fx-padding: 12 16; -fx-background-color: " + SUCCESS_SURFACE + "; " +
            "-fx-border-color: " + SUCCESS_BORDER + "; -fx-border-width: 0 0 1 0;";

    // ---- Input fields ----
    public static final String INPUT_FIELD = "-fx-padding: 10; -fx-background-radius: 6; " +
            "-fx-border-color: " + BORDER + "; -fx-border-radius: 6; -fx-font-size: 13;";

    // ---- Dividers ----
    public static final String DIVIDER_BOTTOM = "-fx-border-color: " + DIVIDER + "; -fx-border-width: 0 0 1 0;";

    // ---- Bottom navigation ----
    public static final String BOTTOM_NAV = "-fx-background-color: " + SURFACE + "; " +
            "-fx-effect: dropshadow(gaussian, " + SHADOW_STRONG + ", 8, 0, 0, -2);";

    // ---- Badge ----
    // White badge with accent text — matches Flutter's white badge in header
    public static final String BADGE_CART = "-fx-background-color: white; -fx-text-fill: " + PRIMARY + "; " +
            "-fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 2 6; " +
            "-fx-background-radius: 10; -fx-min-width: 18; -fx-alignment: center;";

    public static final String BADGE_QUANTITY = "-fx-font-size: 11; -fx-text-fill: " + TEXT_ON_PRIMARY + "; " +
            "-fx-font-weight: bold; -fx-background-color: " + ACCENT_LIGHT + "; -fx-background-radius: 4; " +
            "-fx-padding: 2 6; -fx-min-width: 28; -fx-alignment: center;";

    // ---- Misc ----
    public static final String LINK_BOLD = "-fx-font-size: 16; -fx-font-weight: bold; " +
            "-fx-text-fill: " + PRIMARY + "; -fx-cursor: hand; -fx-underline: true;";

    public static final String CART_BTN_BOX = "-fx-background-color: " + CONTROL_OVERLAY + "; " +
            "-fx-background-radius: 20; -fx-cursor: hand;";

    public static final String QTY_STEPPER = "-fx-border-color: " + BORDER + "; -fx-border-radius: 20; " +
            "-fx-background-radius: 20; -fx-background-color: " + SURFACE + ";";

    public static final String PAGINATION_TEXT = "-fx-font-size: 13; -fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-weight: bold;";

    // ---- Footer areas ----
    public static final String FOOTER_HIGHLIGHT = "-fx-background-color: " + SURFACE_OVERLAY + "; " +
            "-fx-background-radius: 0 0 12 12; -fx-border-color: " + ACCENT_SURFACE + "; -fx-border-width: 1 0 0 0;";

    // ---- Login ----
    // Diagonal 3-stop gradient matching Flutter loginGradient
    public static final String LOGIN_GRADIENT = "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, " +
            PRIMARY + " 0%, " + PRIMARY_END + " 40%, " + PRIMARY_LIGHT + " 100%);";

    // ---- Background white ----
    public static final String BG_WHITE = "-fx-background-color: " + SURFACE + ";";

    // ---- Product image area (matches Flutter imageGradient) ----
    public static final String IMAGE_BG = "-fx-background-color: linear-gradient(from 0% 0% to 0% 100%, " + SURFACE_OVERLAY + ", " + IMAGE_BG_END + ");";

    // ---- Price badge (accent light bg, like Flutter appAccentLight container) ----
    public static final String PRICE_BADGE = "-fx-background-color: " + ACCENT_SURFACE + "; " +
            "-fx-background-radius: 8; -fx-padding: 8 16;";

    // ---- Purchases panel (white surface with left border) ----
    public static final String PURCHASES_PANEL = "-fx-background-color: " + SURFACE + "; " +
            "-fx-border-color: " + BORDER + "; -fx-border-width: 0 0 0 1;";

    // ---- Cart item divider ----
    public static final String CART_ITEM_ROW = "-fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0; -fx-padding: 12 0 12 0;";

    // ---- View header icon box (accent light circle) ----
    public static final String VIEW_HEADER_ICON_BOX = "-fx-background-color: " + ACCENT_SURFACE + "; " +
            "-fx-background-radius: 50; -fx-padding: 10;";

    // ---- Login glass icon box (semi-white on gradient) ----
    public static final String LOGIN_ICON_BOX = "-fx-background-color: rgba(255,255,255,0.12); " +
            "-fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.2); " +
            "-fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 10;";
    public static final String LOGIN_ICON_BOX_LG = "-fx-background-color: rgba(255,255,255,0.12); " +
            "-fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.2); " +
            "-fx-border-radius: 20; -fx-border-width: 1; -fx-padding: 20;";

    // ---- Demo credentials box ----
    public static final String DEMO_BOX = "-fx-background-color: " + SURFACE_DIM + "; " +
            "-fx-background-radius: 8; -fx-border-color: " + BORDER + "; -fx-border-radius: 8; " +
            "-fx-border-width: 1; -fx-padding: 12;";

    // ---- Tab navigation (compact mode, bottom border) ----
    public static final String TAB_NAV = "-fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;";

    // ---- Small transparent icon button (28x28, like Flutter IconButton w/ zero padding) ----
    public static final String BTN_ICON_SM = "-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand; " +
            "-fx-min-width: 28; -fx-min-height: 28; -fx-max-width: 28; -fx-max-height: 28;";

    // ---- Pagination pill container (appBg bg, round) ----
    public static final String PAGINATION_PILL_CONTAINER = "-fx-background-color: " + SURFACE_DIM + "; " +
            "-fx-background-radius: 20; -fx-padding: 4;";

    // ---- Pagination page info pill (white, rounded, shadow) ----
    public static final String PAGINATION_PAGE_INFO_PILL = "-fx-background-color: " + SURFACE + "; " +
            "-fx-background-radius: 12; -fx-padding: 4 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 3, 0, 0, 1);";

    // ---- Purchase item card (appBg, border, rounded) ----
    public static final String PURCHASE_ITEM_CARD = "-fx-background-color: " + SURFACE_DIM + "; " +
            "-fx-background-radius: 8; -fx-border-color: " + BORDER + "; -fx-border-radius: 8; " +
            "-fx-border-width: 1; -fx-cursor: hand;";

    // ---- Helper methods for parametric styles ----

    public static String fontSize(int size) {
        return "-fx-font-size: " + size + ";";
    }

    public static String text(int size, String color) {
        return "-fx-font-size: " + size + "; -fx-text-fill: " + color + ";";
    }

    public static String textBold(int size, String color) {
        return "-fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: " + color + ";";
    }
}
