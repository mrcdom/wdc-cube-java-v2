package br.com.wdc.shopping.view.remote.shell.cn1.theme;

/**
 * Color palette used in Java code (custom painting, "baked" icons, click states) — where the CSS
 * theme does not reach. <b>Mirrors</b> the tokens of {@code scss/_tokens.scss}: keep both in
 * sync (CN1's CSS does not expose Sass variables to Java). Values in RGB {@code 0xRRGGBB},
 * except where noted.
 */
public final class Colors {

    private Colors() {
        // NOOP
    }

    // -- Mirror tokens of _tokens.scss --
    public static final int ACCENT = 0x0d66d0;         // $accent
    public static final int ACCENT_3 = 0x4da6ff;       // $accent-3 (end of the blue gradient)
    public static final int ACCENT_LIGHT = 0xe8f1fc;   // $accent-light (hover, icon box, border)
    public static final int SURFACE = 0xffffff;        // $surface (white)
    public static final int BG = 0xf4f6f9;             // $bg
    public static final int TEXT_SECONDARY = 0x6e6e73; // $text-secondary

    // -- Colors without a matching token (code only) --
    public static final int BADGE = 0xff3b30;          // cart badge red (iOS systemRed)
    public static final int ERROR = 0xcc0000;          // login error text
    public static final int GRAY_DISABLED = 0xc7c7cc;  // disabled arrow (pagination)
    public static final int NEUTRAL = 0xe5e7eb;        // click background of the "back" button

    /** Product image placeholder — <b>ARGB</b> ({@code 0xAARRGGBB}, opaque) for {@code Image.createImage}.
 */
    public static final int IMAGE_PLACEHOLDER = 0xffeeeeee;
}
