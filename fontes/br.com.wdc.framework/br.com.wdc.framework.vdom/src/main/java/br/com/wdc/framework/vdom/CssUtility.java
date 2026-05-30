package br.com.wdc.framework.vdom;

/**
 * Utility CSS class name constants. Single source of truth for reusable utility classes.
 */
@SuppressWarnings("java:S1214")
public interface CssUtility {

    CssUtility INSTANCE = new CssUtility() {
    };

    // Layout
    String FLEX = "flex";
    String FLEX_COL = "flex-col";
    String FLEX_GROW = "flex-grow";
    String FLEX_1 = "flex-1";
    String FLEX_ITEMS_CENTER = "flex-items-center";
    String H_FULL = "h-full";
    String MIN_H_0 = "min-h-0";
    String OVERFLOW_AUTO = "overflow-auto";
    String OVERFLOW_HIDDEN = "overflow-hidden";
    String RELATIVE = "relative";

    // Spacing
    String ML_6 = "ml-6";
    String MR_6 = "mr-6";
    String MB_12 = "mb-12";
    String MB_16 = "mb-16";
    String MB_24 = "mb-24";
    String MT_12 = "mt-12";
    String PY_48 = "py-48";

    // Typography
    String FONT_BOLD = "font-bold";
    String FONT_MEDIUM = "font-medium";
    String FONT_NORMAL = "font-normal";
    String FONT_SEMIBOLD = "font-semibold";
    String TEXT_BASE = "text-base";
    String TEXT_LG = "text-lg";
    String TEXT_SM = "text-sm";
    String TEXT_XL = "text-xl";
    String TEXT_XS = "text-xs";
    String TEXT_WHITE = "text-white";
    String TEXT_WHITE_65 = "text-white-65";
    String TEXT_WHITE_70 = "text-white-70";
    String LEADING_TIGHT = "leading-tight";
    String TRACKING_TIGHT = "tracking-tight";
    String TRACKING_WIDE = "tracking-wide";

    // Responsive
    String SM_SHOW = "sm-show";
    String MD_SHOW = "md-show";
    String MD_HIDE = "md-hide";
    String MD_ROW = "md-row";

    // Background
    String BG_DEFAULT = "bg-default";

    // Visibility
    String HIDDEN = "hidden";

    // Page Layout
    String PAGE_SCROLL_ROOT = "page-scroll-root";
    String PAGE_WRAPPER = "page-wrapper";
}
