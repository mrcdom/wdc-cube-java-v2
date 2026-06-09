package br.com.wdc.shopping.view.gluon.theme;

/**
 * Design tokens alinhados ao Flutter (design_tokens.dart) como fonte única de
 * verdade para cores, gradientes e valores visuais.
 */
public final class GluonColors {

    private GluonColors() {
    }

    // ---- Brand / Primary ----
    public static final String PRIMARY       = "#0D66D0"; // Flutter: appAccent
    public static final String PRIMARY_DARK  = "#0A4F9E"; // Flutter: appAccentDark
    public static final String PRIMARY_END   = "#1A8CFF"; // Flutter: headerGradient end
    public static final String PRIMARY_LIGHT = "#4DA6FF"; // Flutter: loginGradient end

    // ---- Surfaces ----
    public static final String SURFACE         = "white";
    public static final String SURFACE_DIM     = "#F4F6F9"; // Flutter: appBg
    public static final String SURFACE_OVERLAY = "#F8FAFC"; // Flutter: imageGradient start
    public static final String IMAGE_BG_END    = "#EEF2F7"; // Flutter: imageGradient end

    // ---- Text ----
    public static final String TEXT_PRIMARY           = "#1D1D1F"; // Flutter: appText
    public static final String TEXT_DEFAULT           = "#1D1D1F";
    public static final String TEXT_BODY              = "#1D1D1F";
    public static final String TEXT_SECONDARY         = "#6E6E73"; // Flutter: appTextSecondary
    public static final String TEXT_HINT              = "#6E6E73";
    public static final String TEXT_MUTED             = "#6E6E73";
    public static final String TEXT_DISABLED          = "#BBBBC0"; // Flutter: appTextDisabled
    public static final String TEXT_PLACEHOLDER       = "#BBBBC0";
    public static final String TEXT_ON_PRIMARY        = "white";
    public static final String TEXT_ON_PRIMARY_DIM    = "rgba(255,255,255,0.7)";
    public static final String TEXT_ON_PRIMARY_BRIGHT = "rgba(255,255,255,0.9)";

    // ---- Borders / Dividers ----
    public static final String BORDER        = "#E5E5EA"; // Flutter: appBorder
    public static final String DIVIDER       = "#E5E5EA";
    public static final String BORDER_ACCENT = PRIMARY;

    // ---- Success ----
    public static final String SUCCESS         = "#4CAF50";
    public static final String SUCCESS_TEXT    = "#2E7D32";
    public static final String SUCCESS_SURFACE = "#E8F5E9";
    public static final String SUCCESS_BORDER  = "#A5D6A7";

    // ---- Error / Danger ----
    public static final String ERROR       = "#EF5350"; // Flutter: appDanger
    public static final String ERROR_BRIGHT = "#EF5350";

    // ---- Accent / Info ----
    public static final String ACCENT_LIGHT   = "#90CAF9";
    public static final String ACCENT_SURFACE = "#E8F1FC"; // Flutter: appAccentLight

    // ---- Neutral controls ----
    public static final String CONTROL_BG      = "#f0f0f0";
    public static final String CONTROL_TEXT    = "#555";
    public static final String CONTROL_OVERLAY = "rgba(255,255,255,0.15)";

    // ---- Shadows ----
    public static final String SHADOW_LIGHT  = "rgba(0,0,0,0.05)";
    public static final String SHADOW_MEDIUM = "rgba(0,0,0,0.1)";
    public static final String SHADOW_STRONG = "rgba(0,0,0,0.15)";
    public static final String SHADOW_HEAVY  = "rgba(0,0,0,0.25)";
}
