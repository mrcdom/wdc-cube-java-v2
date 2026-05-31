package br.com.wdc.framework.vdom;

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
    String NAVBAR = "wdc-navbar";
    String NAV_GROUP = "wdc-navbar__group";

    // Tab navigation
    String TAB_NAV = "wdc-tab-nav";
    String TAB_ITEM = "wdc-tab-nav__item";
    String TAB_ITEM_ACTIVE = "wdc-tab-nav__item--active";
    String TAB_ITEM_INACTIVE = "wdc-tab-nav__item--inactive";
    String TAB_INDICATOR = "wdc-tab-nav__indicator";

    // Logo
    String LOGO_BOX = "wdc-logo-box";
    String LOGO_BOX_LG = "wdc-logo-box--lg";

    // Cart badge
    String CART_BADGE = "wdc-cart-badge";

    // Page layout
    String PAGE_SCROLL_ROOT = "wdc-page__scroll-root";
    String PAGE_WRAPPER = "wdc-page__wrapper";

    // Total row
    String TOTAL_ROW = "wdc-total-row";

    // Decorative circles
    String DECO_CIRCLE = "wdc-deco-circle";
    String DECO_CIRCLE_1 = "wdc-deco-circle--1";
    String DECO_CIRCLE_2 = "wdc-deco-circle--2";
    String DECO_CIRCLE_3 = "wdc-deco-circle--3";
}
