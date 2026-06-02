package br.com.wdc.shopping.view.swt.theme;

import java.text.NumberFormat;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

/**
 * Centralized design tokens for the SWT Shopping application.
 * All colors, fonts, icons and dimensions are defined here.
 */
public final class Theme {

    private Theme() {
    }

    // ========== COLORS — Primary Palette ==========

    public static final Color PRIMARY_BLUE = color(0x0D, 0x66, 0xD0);
    public static final Color PRIMARY_BLUE_DARK = color(0x0B, 0x5A, 0xBA);
    public static final Color PRIMARY_BLUE_LIGHT = color(0x1A, 0x8C, 0xFF);

    // ========== COLORS — Backgrounds ==========

    public static final Color BG_PAGE = color(0xF4, 0xF6, 0xF9);
    public static final Color BG_WHITE = color(0xFF, 0xFF, 0xFF);
    public static final Color BG_HEADER = PRIMARY_BLUE;
    public static final Color BG_LOGIN_LEFT = color(0x5B, 0x8D, 0xEF);
    public static final Color BG_CARD = BG_WHITE;
    public static final Color BG_SUCCESS = color(0xE8, 0xF5, 0xE9);
    public static final Color BG_ICON_BOX = color(0xE8, 0xF1, 0xFC);
    public static final Color BG_BTN_HOVER = color(0xEE, 0xEE, 0xEE);
    public static final Color BG_ERROR = color(0xFD, 0xED, 0xED);
    public static final Color BG_IMAGE_PLACEHOLDER = color(0xEE, 0xF2, 0xF7);

    // ========== COLORS — Text ==========

    public static final Color FG_TEXT_DARK = color(0x1D, 0x1D, 0x1F);
    public static final Color FG_TEXT_SUBTLE = color(0x75, 0x75, 0x75);
    public static final Color FG_TEXT_WHITE = color(0xFF, 0xFF, 0xFF);
    public static final Color FG_TEXT_WHITE_70 = color(0xFF, 0xFF, 0xFF, 0xB3);
    public static final Color FG_TEXT_WHITE_65 = color(0xFF, 0xFF, 0xFF, 0xA6);
    public static final Color FG_PRICE = color(0xC6, 0x28, 0x28);
    public static final Color FG_LINK = PRIMARY_BLUE;
    public static final Color FG_ERROR = color(0xD3, 0x2F, 0x2F);
    public static final Color FG_SUCCESS = color(0x2E, 0x7D, 0x32);

    // ========== COLORS — Borders ==========

    public static final Color BORDER_LIGHT = color(0xE5, 0xE5, 0xEA);
    public static final Color BORDER_FIELD = color(0xCC, 0xCC, 0xCC);
    public static final Color BORDER_FIELD_FOCUS = PRIMARY_BLUE;
    public static final Color BORDER_ERROR = color(0xD3, 0x2F, 0x2F);
    public static final Color BORDER_ERROR_BOX = color(0xF5, 0xC6, 0xC6);

    // ========== FONTS ==========

    public static final Font FONT_TITLE;
    public static final Font FONT_WELCOME;
    public static final Font FONT_SUBTITLE;
    public static final Font FONT_HEADER;
    public static final Font FONT_HEADER_BOLD;
    public static final Font FONT_BODY;
    public static final Font FONT_BODY_BOLD;
    public static final Font FONT_FIELD_LABEL;
    public static final Font FONT_BUTTON;
    public static final Font FONT_PRICE;
    public static final Font FONT_PRICE_LARGE;
    public static final Font FONT_BANNER_TITLE;
    public static final Font FONT_BANNER_SUBTITLE;
    public static final Font FONT_BADGE;
    public static final Font FONT_PRODUCT_NAME;
    public static final Font FONT_NAV_SMALL;
    public static final Font FONT_NAV_TITLE;
    public static final Font FONT_PAGINATION;
    public static final Font FONT_QTY;
    public static final Font FONT_QTY_VALUE;
    public static final Font FONT_MONO;
    public static final Font FONT_MONO_BOLD;

    // ========== ICON FONTS ==========

    public static final Font FONT_ICON;
    public static final Font FONT_ICON_NAV;
    public static final Font FONT_ICON_LARGE;

    // ========== ICON CODEPOINTS (Bootstrap Icons) ==========

    public static final String ICON_BAG = "\uF179";
    public static final String ICON_BAG_CHECK = "\uF171";
    public static final String ICON_BAG_CHECK_FILL = "\uF173";
    public static final String ICON_BAG_PLUS = "\uF176";
    public static final String ICON_BOX_ARROW_LEFT = "\uF1C2";
    public static final String ICON_CHEVRON_LEFT = "\uF284";
    public static final String ICON_CHEVRON_RIGHT = "\uF285";
    public static final String ICON_SHIELD_CHECK = "\uF52F";
    public static final String ICON_TRUCK = "\uF5EA";
    public static final String ICON_ARROW_REPEAT = "\uF130";
    public static final String ICON_EXCLAMATION_CIRCLE = "\uF333";
    public static final String ICON_CLOCK_HISTORY = "\uF293";
    public static final String ICON_RECEIPT = "\uF50F";
    public static final String ICON_DASH = "\uF2EA";
    public static final String ICON_PLUS = "\uF4FE";
    public static final String ICON_X = "\uF62A";
    public static final String ICON_ARROW_LEFT = "\uF12F";
    public static final String ICON_CHECK2_SQUARE = "\uF26E";
    public static final String ICON_GRID_3X3_GAP = "\uF3EE";

    // ========== DIMENSIONS ==========

    public static final int HEADER_HEIGHT = 56;
    public static final int CARD_RADIUS = 12;
    public static final int CARD_RADIUS_LARGE = 24;
    public static final int FIELD_HEIGHT = 40;
    public static final int BUTTON_HEIGHT = 44;
    public static final int SPACING_SM = 8;
    public static final int SPACING_MD = 16;
    public static final int SPACING_LG = 24;
    public static final int SPACING_XL = 32;

    // ========== INITIALIZATION ==========

    static {
        var display = Display.getDefault();
        var sysName = display.getSystemFont().getFontData()[0].getName();

        FONT_TITLE = new Font(display, new FontData(sysName, 20, SWT.BOLD));
        FONT_WELCOME = new Font(display, new FontData(sysName, 24, SWT.BOLD));
        FONT_SUBTITLE = new Font(display, new FontData(sysName, 12, SWT.NORMAL));
        FONT_HEADER = new Font(display, new FontData(sysName, 13, SWT.NORMAL));
        FONT_HEADER_BOLD = new Font(display, new FontData(sysName, 14, SWT.BOLD));
        FONT_BODY = new Font(display, new FontData(sysName, 13, SWT.NORMAL));
        FONT_BODY_BOLD = new Font(display, new FontData(sysName, 13, SWT.BOLD));
        FONT_FIELD_LABEL = new Font(display, new FontData(sysName, 12, SWT.BOLD));
        FONT_BUTTON = new Font(display, new FontData(sysName, 14, SWT.BOLD));
        FONT_PRICE = new Font(display, new FontData(sysName, 14, SWT.BOLD));
        FONT_PRICE_LARGE = new Font(display, new FontData(sysName, 18, SWT.BOLD));
        FONT_BANNER_TITLE = new Font(display, new FontData(sysName, 15, SWT.BOLD));
        FONT_BANNER_SUBTITLE = new Font(display, new FontData(sysName, 10, SWT.NORMAL));
        FONT_BADGE = new Font(display, new FontData(sysName, 10, SWT.BOLD));
        FONT_PRODUCT_NAME = new Font(display, new FontData(sysName, 12, SWT.NORMAL));
        FONT_NAV_SMALL = new Font(display, new FontData(sysName, 11, SWT.NORMAL));
        FONT_NAV_TITLE = new Font(display, new FontData(sysName, 16, SWT.BOLD));
        FONT_PAGINATION = new Font(display, new FontData(sysName, 12, SWT.BOLD));
        FONT_QTY = new Font(display, new FontData(sysName, 11, SWT.NORMAL));
        FONT_QTY_VALUE = new Font(display, new FontData(sysName, 14, SWT.BOLD));
        FONT_MONO = new Font(display, new FontData("Courier New", 12, SWT.NORMAL));
        FONT_MONO_BOLD = new Font(display, new FontData("Courier New", 12, SWT.BOLD));

        // Load Bootstrap Icons font
        var fontUrl = Theme.class.getClassLoader().getResource("fonts/bootstrap-icons.ttf");
        String iconFontName = "bootstrap-icons";
        if (fontUrl != null) {
            try {
                var path = fontUrl.getPath();
                display.loadFont(path);
                var fd = display.getFontList(null, true);
                for (var f : fd) {
                    if (f.getName().toLowerCase().contains("bootstrap")) {
                        iconFontName = f.getName();
                        break;
                    }
                }
            } catch (Exception e) {
                // Fallback: use system font
            }
        }
        FONT_ICON = new Font(display, new FontData(iconFontName, 14, SWT.NORMAL));
        FONT_ICON_NAV = new Font(display, new FontData(iconFontName, 18, SWT.NORMAL));
        FONT_ICON_LARGE = new Font(display, new FontData(iconFontName, 40, SWT.NORMAL));
    }

    // ========== HELPERS ==========

    private static final NumberFormat PRICE_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    public static String formatPrice(double value) {
        return PRICE_FORMAT.format(value);
    }

    private static Color color(int r, int g, int b) {
        return new Color(Display.getDefault(), r, g, b);
    }

    private static Color color(int r, int g, int b, int a) {
        return new Color(r, g, b, a);
    }
}
