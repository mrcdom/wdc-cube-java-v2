package br.com.wdc.shopping.view.teavm.commons;

/**
 * Utility CSS class name constants. Single source of truth for reusable utility classes.
 * <p>
 * Equivalent to {@code global-sel.ts} in the React project.
 */
@SuppressWarnings("java:S1214")
public interface SelUtility {

    SelUtility INSTANCE = new SelUtility() {
    };

    // =========================================================================
    // :: Display & Visibility
    // =========================================================================

    String HIDDEN = "hidden";
    String BLOCK = "block";
    String INLINE_FLEX = "inline-flex";
    String OVERFLOW_HIDDEN = "overflow-hidden";
    String OVERFLOW_Y_AUTO = "overflow-y-auto";
    String OVERFLOW_AUTO = "overflow-auto";

    // =========================================================================
    // :: Flex Layout
    // =========================================================================

    String FLEX = "flex";
    String FLEX_COL = "flex-col";
    String FLEX_CENTER = "flex-center";
    String FLEX_COL_CENTER = "flex-col-center";
    String FLEX_ITEMS_CENTER = "flex-items-center";
    String FLEX_BETWEEN = "flex-between";
    String FLEX_END = "flex-end";
    String FLEX_COL_STRETCH = "flex-col-stretch";

    String FLEX_1 = "flex-1";
    String FLEX_GROW = "flex-grow";
    String FLEX_SHRINK_0 = "flex-shrink-0";
    String FLEX_1_SCROLL = "flex-1-scroll";
    String FLEX_WRAP = "flex-wrap";

    // Gaps
    String GAP_4 = "gap-4";
    String GAP_6 = "gap-6";
    String GAP_8 = "gap-8";
    String GAP_10 = "gap-10";
    String GAP_12 = "gap-12";
    String GAP_16 = "gap-16";
    String GAP_20 = "gap-20";
    String GAP_24 = "gap-24";

    // Alignment modifiers
    String ITEMS_CENTER = "items-center";
    String ITEMS_START = "items-start";
    String ITEMS_END = "items-end";
    String ITEMS_STRETCH = "items-stretch";
    String JUSTIFY_CENTER = "justify-center";
    String JUSTIFY_BETWEEN = "justify-between";
    String JUSTIFY_END = "justify-end";
    String JUSTIFY_START = "justify-start";

    // =========================================================================
    // :: Spacing — Padding
    // =========================================================================

    String P_0 = "p-0";
    String P_4 = "p-4";
    String P_8 = "p-8";
    String P_10 = "p-10";
    String P_12 = "p-12";
    String P_16 = "p-16";
    String P_20 = "p-20";
    String P_24 = "p-24";
    String P_32 = "p-32";
    String P_48 = "p-48";

    String PX_8 = "px-8";
    String PX_12 = "px-12";
    String PX_16 = "px-16";
    String PX_20 = "px-20";
    String PX_24 = "px-24";

    String PY_8 = "py-8";
    String PY_10 = "py-10";
    String PY_12 = "py-12";
    String PY_16 = "py-16";
    String PY_20 = "py-20";
    String PY_24 = "py-24";
    String PY_48 = "py-48";

    String PT_8 = "pt-8";
    String PT_16 = "pt-16";
    String PT_24 = "pt-24";
    String PB_8 = "pb-8";
    String PB_16 = "pb-16";

    // =========================================================================
    // :: Spacing — Margin
    // =========================================================================

    String M_0 = "m-0";
    String MX_AUTO = "mx-auto";
    String MT_4 = "mt-4";
    String MT_8 = "mt-8";
    String MT_12 = "mt-12";
    String MT_16 = "mt-16";
    String MT_20 = "mt-20";
    String MT_24 = "mt-24";
    String MT_32 = "mt-32";
    String MT_AUTO = "mt-auto";
    String MB_4 = "mb-4";
    String MB_6 = "mb-6";
    String MB_8 = "mb-8";
    String MB_10 = "mb-10";
    String MB_12 = "mb-12";
    String MB_16 = "mb-16";
    String MB_20 = "mb-20";
    String MB_24 = "mb-24";
    String MB_28 = "mb-28";
    String ML_4 = "ml-4";
    String ML_6 = "ml-6";
    String ML_8 = "ml-8";
    String MR_4 = "mr-4";
    String MR_6 = "mr-6";
    String MR_8 = "mr-8";

    // =========================================================================
    // :: Sizing
    // =========================================================================

    String W_FULL = "w-full";
    String H_FULL = "h-full";
    String MIN_H_0 = "min-h-0";
    String MIN_H_FULL = "min-h-full";
    String MIN_W_0 = "min-w-0";
    String MAX_W_280 = "max-w-280";
    String MAX_W_320 = "max-w-320";
    String MAX_W_460 = "max-w-460";
    String MAX_W_900 = "max-w-900";

    String W_28 = "w-28";
    String H_28 = "h-28";
    String W_40 = "w-40";
    String H_40 = "h-40";
    String W_48 = "w-48";
    String H_48 = "h-48";
    String W_80 = "w-80";
    String H_80 = "h-80";
    String W_100 = "w-100";
    String H_100 = "h-100";
    String MIN_W_24 = "min-w-24";
    String MIN_W_28 = "min-w-28";
    String MAX_W_160 = "max-w-160";
    String MAX_H_160 = "max-h-160";

    // =========================================================================
    // :: Typography — Font size
    // =========================================================================

    String TEXT_XXS = "text-xxs";
    String TEXT_XS = "text-xs";
    String TEXT_SM = "text-sm";
    String TEXT_BASE = "text-base";
    String TEXT_LG = "text-lg";
    String TEXT_XL = "text-xl";
    String TEXT_2XL = "text-2xl";
    String TEXT_3XL = "text-3xl";
    String TEXT_4XL = "text-4xl";

    // =========================================================================
    // :: Typography — Font weight
    // =========================================================================

    String FONT_NORMAL = "font-normal";
    String FONT_MEDIUM = "font-medium";
    String FONT_SEMIBOLD = "font-semibold";
    String FONT_BOLD = "font-bold";
    String FONT_EXTRABOLD = "font-extrabold";

    // =========================================================================
    // :: Typography — Alignment, line-height, spacing
    // =========================================================================

    String TEXT_CENTER = "text-center";
    String TEXT_LEFT = "text-left";
    String TEXT_RIGHT = "text-right";

    String LEADING_TIGHT = "leading-tight";
    String LEADING_NORMAL = "leading-normal";
    String LEADING_RELAXED = "leading-relaxed";

    String TRACKING_TIGHT = "tracking-tight";
    String TRACKING_TIGHTER = "tracking-tighter";
    String TRACKING_WIDE = "tracking-wide";

    // =========================================================================
    // :: Typography — Colors
    // =========================================================================

    String TEXT_PRIMARY = "text-primary";
    String TEXT_SECONDARY = "text-secondary";
    String TEXT_ACCENT = "text-accent";
    String TEXT_WHITE = "text-white";
    String TEXT_WHITE_65 = "text-white-65";
    String TEXT_WHITE_70 = "text-white-70";
    String TEXT_WHITE_80 = "text-white-80";

    String TEXT_SM_SECONDARY = "text-sm-secondary";
    String TEXT_XS_SECONDARY = "text-xs-secondary";

    // =========================================================================
    // :: Borders & Radius
    // =========================================================================

    String BORDER = "border";
    String BORDER_T = "border-t";
    String BORDER_B = "border-b";
    String BORDER_L = "border-l";
    String BORDER_NONE = "border-none";
    String BORDER_WHITE_20 = "border-white-20";
    String BORDER_DASHED = "border-dashed";
    String BORDER_DOTTED = "border-dotted";
    String BORDER_T_ACCENT = "border-t-accent";

    String ROUNDED = "rounded";
    String ROUNDED_SM = "rounded-sm";
    String ROUNDED_10 = "rounded-10";
    String ROUNDED_12 = "rounded-12";
    String ROUNDED_20 = "rounded-20";
    String ROUNDED_FULL = "rounded-full";

    // =========================================================================
    // :: Background & Shadows
    // =========================================================================

    String BG_SURFACE = "bg-surface";
    String BG_DEFAULT = "bg-default";
    String BG_ACCENT_LIGHT = "bg-accent-light";
    String BG_WHITE = "bg-white";
    String BG_NONE = "bg-none";
    String BG_TRANSPARENT = "bg-transparent";

    String BG_GRADIENT_PRIMARY = "bg-gradient-primary";
    String BG_GRADIENT_PRIMARY_EXTENDED = "bg-gradient-primary-extended";

    String BG_WHITE_04 = "bg-white-04";
    String BG_WHITE_05 = "bg-white-05";
    String BG_WHITE_06 = "bg-white-06";
    String BG_WHITE_12 = "bg-white-12";
    String BG_WHITE_15 = "bg-white-15";

    String SHADOW_SM = "shadow-sm";
    String SHADOW_MD = "shadow-md";
    String SHADOW_LG = "shadow-lg";
    String SHADOW_BLUE = "shadow-blue";

    String BACKDROP_BLUR_SM = "backdrop-blur-sm";
    String BACKDROP_BLUR_MD = "backdrop-blur-md";

    // =========================================================================
    // :: Position
    // =========================================================================

    String RELATIVE = "relative";
    String ABSOLUTE = "absolute";
    String Z_1 = "z-1";
    String Z_10 = "z-10";

    // =========================================================================
    // :: Cursor & Interaction
    // =========================================================================

    String CURSOR_POINTER = "cursor-pointer";
    String POINTER_EVENTS_NONE = "pointer-events-none";

    // =========================================================================
    // :: Transitions
    // =========================================================================

    String TRANSITION_COLORS = "transition-colors";
    String TRANSITION_TRANSFORM = "transition-transform";
    String TRANSITION_ALL = "transition-all";
    String TRANSITION_TRANSFORM_SPRING = "transition-transform-spring";

    // =========================================================================
    // :: Text utilities
    // =========================================================================

    String WHITESPACE_NOWRAP = "whitespace-nowrap";
    String TEXT_ELLIPSIS = "text-ellipsis";
    String TEXT_UPPERCASE = "text-uppercase";

    String OBJECT_CONTAIN = "object-contain";
    String ASPECT_1 = "aspect-1";
    String BOX_BORDER = "box-border";

    // =========================================================================
    // :: Responsive
    // =========================================================================

    String SM_SHOW = "sm-show";
    String MD_SHOW = "md-show";
    String MD_HIDE = "md-hide";
    String MD_ROW = "md-row";
    String MD_GROW_0 = "md-grow-0";

    // =========================================================================
    // :: Page Layout
    // =========================================================================

    String PAGE_SCROLL_ROOT = "page-scroll-root";
    String PAGE_WRAPPER = "page-wrapper";
}
