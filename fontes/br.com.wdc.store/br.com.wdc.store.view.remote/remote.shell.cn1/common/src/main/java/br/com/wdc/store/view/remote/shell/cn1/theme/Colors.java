package br.com.wdc.store.view.remote.shell.cn1.theme;

/**
 * Color palette used in Java code (custom painting, "baked" icons, click states) — where the CSS
 * theme does not reach. Each color is a design token ({@link StoreTokens}, generated from the store's
 * {@code tokens.json}), the same one {@code scss/_tokens.scss} reads from {@code generated/_tokens.scss}.
 * Values in RGB {@code 0xRRGGBB}, except where noted.
 */
public final class Colors {

    private Colors() {
        // NOOP
    }

    public static final int ACCENT = StoreTokens.COLOR_PRIMARY;               // $accent
    public static final int ACCENT_3 = StoreTokens.COLOR_ACCENT_SOFT;         // $accent-3 (end of the blue gradient)
    public static final int ACCENT_LIGHT = StoreTokens.COLOR_PRIMARY_LIGHT;   // $accent-light (hover, icon box, border)
    public static final int SURFACE = StoreTokens.COLOR_SURFACE;              // $surface (white)
    public static final int BG = StoreTokens.COLOR_BACKGROUND;                // $bg
    public static final int TEXT_SECONDARY = StoreTokens.COLOR_TEXT_SECONDARY; // $text-secondary

    public static final int BADGE = StoreTokens.COLOR_ERROR;                  // cart badge
    public static final int ERROR = StoreTokens.COLOR_ERROR;                  // login error text
    public static final int GRAY_DISABLED = StoreTokens.COLOR_TEXT_MUTED;     // disabled arrow (pagination)
    public static final int NEUTRAL = StoreTokens.COLOR_SURFACE_SUBTLE;       // click background of the "back" button

    /** Product image placeholder — <b>ARGB</b> ({@code 0xAARRGGBB}, opaque) for {@code Image.createImage}. */
    public static final int IMAGE_PLACEHOLDER = 0xff000000 | StoreTokens.COLOR_SURFACE_SUBTLE;
}
