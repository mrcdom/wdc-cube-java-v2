package br.com.wdc.shopping.view.teavm.commons;

/**
 * Shared component CSS class name constants. Single source of truth for reusable component-level classes used across
 * multiple views.
 * <p>
 * Equivalent to the "Shared component selectors" section of {@code global-sel.ts} in the React project.
 */
@SuppressWarnings("java:S1214")
public interface SelComponents {

    SelComponents INSTANCE = new SelComponents() {
    };

    // Alert
    String ALERT_ERROR = "alert-error";
    String ALERT_ERROR_ICON = "alert-error-icon";
    String ALERT_ERROR_TEXT = "alert-error-text";
    String ALERT_SUCCESS = "alert-success";
    String ALERT_SUCCESS_ICON = "alert-success-icon";
    String ALERT_SUCCESS_TEXT = "alert-success-text";

    // Card
    String CARD_PANEL = "card-panel";
    String CARD_PANEL_LG = "card-panel-lg";
    String CARD_HEADER_ROW = "card-header-row";
    String CARD_HEADER_ICON_BOX = "card-header-icon-box";
    String CARD_HEADER_ICON = "card-header-icon";
    String CARD_HEADER_TITLE = "card-header-title";
    String CARD_HEADER_SUBTITLE = "card-header-subtitle";

    // Empty state
    String EMPTY_STATE = "empty-state";

    // Navbar
    String NAVBAR = "navbar";
    String NAV_GROUP = "nav-group";

    // Tab navigation
    String TAB_NAV = "tab-nav";
    String TAB_ITEM = "tab-item";
    String TAB_ITEM_ACTIVE = "tab-item tab-item--active";
    String TAB_ITEM_INACTIVE = "tab-item tab-item--inactive";
    String TAB_INDICATOR = "tab-indicator";

    // Logo
    String LOGO_BOX = "logo-box";
    String LOGO_BOX_LG = "logo-box-lg";

    // Cart badge
    String CART_BADGE = "cart-badge";

    // Total row
    String TOTAL_ROW = "total-row";

    // Decorative circles
    String DECO_CIRCLE = "deco-circle";
    String DECO_CIRCLE_1 = "deco-circle deco-circle--1";
    String DECO_CIRCLE_2 = "deco-circle deco-circle--2";
    String DECO_CIRCLE_3 = "deco-circle deco-circle--3";
}
