package br.com.wdc.shopping.view.swt.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

/**
 * Design tokens matching the TeaVM web app visual style.
 * Colors based on the screenshots (blue header, white cards, subtle borders).
 */
public final class Styles {

    private Styles() {
    }

    // :: Colors — Primary palette (matching TeaVM web screenshots)
    public static final Color PRIMARY_BLUE = color(0x4A, 0x7A, 0xFF);
    public static final Color PRIMARY_BLUE_DARK = color(0x3B, 0x5E, 0xCC);
    public static final Color PRIMARY_BLUE_LIGHT = color(0x6B, 0x9A, 0xFF);

    // :: Colors — Backgrounds
    public static final Color BG_PAGE = color(0xF5, 0xF7, 0xFA);
    public static final Color BG_WHITE = color(0xFF, 0xFF, 0xFF);
    public static final Color BG_HEADER = PRIMARY_BLUE;
    public static final Color BG_LOGIN_LEFT = color(0x5B, 0x8D, 0xEF);
    public static final Color BG_CARD = BG_WHITE;
    public static final Color BG_SUCCESS = color(0xE8, 0xF5, 0xE9);

    // :: Colors — Text
    public static final Color FG_TEXT_DARK = color(0x21, 0x21, 0x21);
    public static final Color FG_TEXT_SUBTLE = color(0x75, 0x75, 0x75);
    public static final Color FG_TEXT_WHITE = color(0xFF, 0xFF, 0xFF);
    public static final Color FG_PRICE = color(0xC6, 0x28, 0x28);
    public static final Color FG_LINK = PRIMARY_BLUE;

    // :: Colors — Borders and Decorations
    public static final Color BORDER_LIGHT = color(0xE0, 0xE0, 0xE0);
    public static final Color BORDER_FIELD = color(0xCC, 0xCC, 0xCC);
    public static final Color BORDER_FIELD_FOCUS = PRIMARY_BLUE;
    public static final Color BORDER_ERROR = color(0xD3, 0x2F, 0x2F);
    public static final Color FG_ERROR = color(0xD3, 0x2F, 0x2F);
    public static final Color BG_ERROR = color(0xFD, 0xED, 0xED);
    public static final Color BORDER_ERROR_BOX = color(0xF5, 0xC6, 0xC6);

    // :: Fonts
    public static final Font FONT_TITLE;
    public static final Font FONT_WELCOME;
    public static final Font FONT_SUBTITLE;
    public static final Font FONT_HEADER;
    public static final Font FONT_HEADER_BOLD;
    public static final Font FONT_BODY;
    public static final Font FONT_FIELD_LABEL;
    public static final Font FONT_BUTTON;
    public static final Font FONT_PRICE;
    public static final Font FONT_BANNER_TITLE;
    public static final Font FONT_BANNER_SUBTITLE;

    // :: Icon font (Bootstrap Icons)
    public static final Font FONT_ICON;
    public static final Font FONT_ICON_LARGE;

    // :: Icon codepoints (Bootstrap Icons)
    public static final String ICON_BAG_CHECK = "\uF171";
    public static final String ICON_SHIELD_CHECK = "\uF52F";
    public static final String ICON_TRUCK = "\uF5EA";
    public static final String ICON_ARROW_REPEAT = "\uF130";
    public static final String ICON_EXCLAMATION_CIRCLE = "\uF333";

    static {
        var display = Display.getDefault();
        FONT_TITLE = new Font(display, new FontData("System", 20, SWT.BOLD));
        FONT_WELCOME = new Font(display, new FontData("System", 24, SWT.BOLD));
        FONT_SUBTITLE = new Font(display, new FontData("System", 12, SWT.NORMAL));
        FONT_HEADER = new Font(display, new FontData("System", 13, SWT.NORMAL));
        FONT_HEADER_BOLD = new Font(display, new FontData("System", 14, SWT.BOLD));
        FONT_BODY = new Font(display, new FontData("System", 12, SWT.NORMAL));
        FONT_FIELD_LABEL = new Font(display, new FontData("System", 12, SWT.BOLD));
        FONT_BUTTON = new Font(display, new FontData("System", 14, SWT.BOLD));
        FONT_PRICE = new Font(display, new FontData("System", 14, SWT.BOLD));
        FONT_BANNER_TITLE = new Font(display, new FontData("System", 15, SWT.BOLD));
        FONT_BANNER_SUBTITLE = new Font(display, new FontData("System", 10, SWT.NORMAL));

        // Load Bootstrap Icons font from resources
        var fontUrl = Styles.class.getClassLoader().getResource("fonts/bootstrap-icons.ttf");
        String iconFontName = "bootstrap-icons";
        if (fontUrl != null) {
            try {
                var path = fontUrl.getPath();
                display.loadFont(path);
                // After loading, find the registered name
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
        FONT_ICON_LARGE = new Font(display, new FontData(iconFontName, 40, SWT.NORMAL));
    }

    // :: Dimensions
    public static final int HEADER_HEIGHT = 56;
    public static final int CARD_RADIUS = 12;
    public static final int FIELD_HEIGHT = 40;
    public static final int BUTTON_HEIGHT = 44;
    public static final int SPACING_SM = 8;
    public static final int SPACING_MD = 16;
    public static final int SPACING_LG = 24;
    public static final int SPACING_XL = 32;

    private static Color color(int r, int g, int b) {
        return new Color(Display.getDefault(), r, g, b);
    }
}
