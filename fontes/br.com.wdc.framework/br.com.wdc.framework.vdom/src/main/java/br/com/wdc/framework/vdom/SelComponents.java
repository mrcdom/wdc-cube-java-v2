package br.com.wdc.framework.vdom;

/**
 * Shared component CSS class name constants. Single source of truth for reusable component-level classes used across
 * multiple views.
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
}
