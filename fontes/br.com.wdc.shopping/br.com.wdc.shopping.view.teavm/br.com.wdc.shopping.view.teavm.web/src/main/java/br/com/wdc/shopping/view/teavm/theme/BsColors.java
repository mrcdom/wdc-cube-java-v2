package br.com.wdc.shopping.view.teavm.theme;

/**
 * Bootstrap-compatible color constants.
 * Maps the same semantic palette as GluonColors to Bootstrap CSS variables.
 */
public class BsColors {

    private BsColors() {
    }

    // ---- Brand ----
    public static final String PRIMARY = "#1976D2";
    public static final String PRIMARY_DARK = "#0D47A1";

    // ---- Surfaces ----
    public static final String SURFACE = "#FFFFFF";
    public static final String SURFACE_SECONDARY = "#F8F9FA";
    public static final String SURFACE_OVERLAY = "rgba(255,255,255,0.15)";
    public static final String CONTROL_OVERLAY = "rgba(255,255,255,0.25)";

    // ---- Text ----
    public static final String TEXT_DEFAULT = "#212529";
    public static final String TEXT_SECONDARY = "#6C757D";
    public static final String TEXT_MUTED = "#ADB5BD";
    public static final String TEXT_DISABLED = "#CED4DA";
    public static final String TEXT_ON_PRIMARY = "#FFFFFF";

    // ---- Borders ----
    public static final String BORDER = "#DEE2E6";
    public static final String DIVIDER = "#E9ECEF";

    // ---- Success ----
    public static final String SUCCESS = "#198754";
    public static final String SUCCESS_SURFACE = "#D1E7DD";
    public static final String SUCCESS_TEXT = "#0F5132";
    public static final String SUCCESS_BORDER = "#BADBCC";

    // ---- Error ----
    public static final String ERROR = "#DC3545";
    public static final String ERROR_BRIGHT = "#DC3545";

    // ---- Accent ----
    public static final String ACCENT_LIGHT = "#0D6EFD";
    public static final String ACCENT_SURFACE = "#CFE2FF";

    // ---- Shadows (CSS box-shadow values) ----
    public static final String SHADOW_LIGHT = "0 1px 3px rgba(0,0,0,0.1)";
    public static final String SHADOW_MEDIUM = "0 2px 8px rgba(0,0,0,0.12)";
    public static final String SHADOW_STRONG = "0 4px 16px rgba(0,0,0,0.15)";
}
