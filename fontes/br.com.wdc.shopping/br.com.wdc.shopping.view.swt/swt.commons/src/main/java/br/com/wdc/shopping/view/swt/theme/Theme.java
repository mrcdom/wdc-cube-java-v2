package br.com.wdc.shopping.view.swt.theme;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

    // ========== PLATFORM ==========

    /**
     * {@code true} when running on GTK/Linux.
     * GTK uses 96 DPI for font metrics; macOS uses 72 DPI.
     * The same point value therefore renders ~33 % larger on GTK.
     * Must be declared BEFORE the static initializer block.
     */
    public static final boolean IS_GTK = "gtk".equals(SWT.getPlatform());

    /**
     * Windows also uses 96 DPI metrics, so macOS-calibrated point sizes need
     * the same compensation there to keep the visual rhythm aligned.
     */
    public static final boolean IS_WIN32 = "win32".equals(SWT.getPlatform());

    /**
     * Converts a macOS-calibrated point size to the platform-appropriate equivalent.
     * On GTK a 3/4 scaling factor (72/96) is applied so fonts appear the same
     * visual size as on macOS.
     */
    private static int pt(int macSize) {
        return (IS_GTK || IS_WIN32) ? Math.max(8, macSize * 3 / 4) : macSize;
    }

    // ========== COLORS — Primary Palette ==========

    public static final Color PRIMARY_BLUE = color(0x0D, 0x66, 0xD0);
    public static final Color PRIMARY_BLUE_DARK = color(0x0B, 0x5A, 0xBA);
    public static final Color PRIMARY_BLUE_LIGHT = color(0x1A, 0x8C, 0xFF);

    // ========== COLORS — Backgrounds ==========

    public static final Color BG_PAGE = color(0xF4, 0xF6, 0xF9);
    public static final Color BG_WHITE = color(0xFF, 0xFF, 0xFF);
    public static final Color BG_HEADER = PRIMARY_BLUE;
    public static final Color BG_LOGIN_LEFT = color(0x4D, 0xA6, 0xFF);
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

        FONT_TITLE = new Font(display, new FontData(sysName, pt(20), SWT.BOLD));
        FONT_WELCOME = new Font(display, new FontData(sysName, pt(24), SWT.BOLD));
        FONT_SUBTITLE = new Font(display, new FontData(sysName, pt(12), SWT.NORMAL));
        FONT_HEADER = new Font(display, new FontData(sysName, pt(13), SWT.NORMAL));
        FONT_HEADER_BOLD = new Font(display, new FontData(sysName, pt(14), SWT.BOLD));
        FONT_BODY = new Font(display, new FontData(sysName, pt(13), SWT.NORMAL));
        FONT_BODY_BOLD = new Font(display, new FontData(sysName, pt(13), SWT.BOLD));
        FONT_FIELD_LABEL = new Font(display, new FontData(sysName, pt(12), SWT.BOLD));
        FONT_BUTTON = new Font(display, new FontData(sysName, pt(14), SWT.BOLD));
        FONT_PRICE = new Font(display, new FontData(sysName, pt(14), SWT.BOLD));
        FONT_PRICE_LARGE = new Font(display, new FontData(sysName, pt(18), SWT.BOLD));
        FONT_BANNER_TITLE = new Font(display, new FontData(sysName, pt(15), SWT.BOLD));
        FONT_BANNER_SUBTITLE = new Font(display, new FontData(sysName, pt(10), SWT.NORMAL));
        FONT_BADGE = new Font(display, new FontData(sysName, pt(10), SWT.BOLD));
        FONT_PRODUCT_NAME = new Font(display, new FontData(sysName, pt(12), SWT.NORMAL));
        FONT_NAV_SMALL = new Font(display, new FontData(sysName, pt(11), SWT.NORMAL));
        FONT_NAV_TITLE = new Font(display, new FontData(sysName, pt(16), SWT.BOLD));
        FONT_PAGINATION = new Font(display, new FontData(sysName, pt(12), SWT.BOLD));
        FONT_QTY = new Font(display, new FontData(sysName, pt(11), SWT.NORMAL));
        FONT_QTY_VALUE = new Font(display, new FontData(sysName, pt(14), SWT.BOLD));
        FONT_MONO = new Font(display, new FontData("Courier New", pt(12), SWT.NORMAL));
        FONT_MONO_BOLD = new Font(display, new FontData("Courier New", pt(12), SWT.BOLD));

        // Load Bootstrap Icons font
        // When running from a UberJar, fontUrl.getPath() is a jar-internal path that
        // display.loadFont() cannot use. Extract to a temp file if needed.
        var fontUrl = Theme.class.getClassLoader().getResource("fonts/bootstrap-icons.ttf");
        String iconFontName = "bootstrap-icons";
        if (fontUrl != null) {
            try {
                String path;
                if ("file".equals(fontUrl.getProtocol())) {
                    path = fontUrl.getPath();
                } else {
                    // jar: or other non-file URL — extract to a temp file
                    var tempFont = Files.createTempFile("bootstrap-icons-", ".ttf");
                    tempFont.toFile().deleteOnExit();
                    try (var in = fontUrl.openStream()) {
                        Files.copy(in, tempFont, StandardCopyOption.REPLACE_EXISTING);
                    }
                    path = tempFont.toAbsolutePath().toString();
                }

                // Read the real family name directly from the TTF name table.
                String ttfName = readTtfFamilyName(fontUrl, null);
                boolean loaded = display.loadFont(path);

                if (loaded && ttfName != null && !ttfName.isBlank()) {
                    iconFontName = ttfName;
                } else {
                    // Scan the font list as a fallback after loading.
                    var fd = display.getFontList(null, true);
                    for (var f : fd) {
                        var name = f.getName();
                        if (name != null && name.toLowerCase().contains("bootstrap")) {
                            iconFontName = name;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[Theme] Exception loading icon font: " + e);
            }
        } else {
            System.err.println("[Theme] bootstrap-icons.ttf not found in classpath");
        }
        FONT_ICON = new Font(display, new FontData(iconFontName, pt(14), SWT.NORMAL));
        FONT_ICON_NAV = new Font(display, new FontData(iconFontName, pt(18), SWT.NORMAL));
        FONT_ICON_LARGE = new Font(display, new FontData(iconFontName, pt(40), SWT.NORMAL));
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

    /**
     * Reads the font family name (name ID 1) directly from the TTF name table.
     * This is needed on GTK where freshly loaded fonts may not appear in
     * {@code display.getFontList()} in the same JVM session.
     *
     * @param fontUrl  URL of the TTF file (file: or jar: protocol)
     * @param fallback value to return if the name cannot be read
     */
    private static String readTtfFamilyName(URL fontUrl, String fallback) {
        try (var in = fontUrl.openStream()) {
            byte[] data = in.readAllBytes();
            var buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
            // Offset table: sfVersion(4) + numTables(2) + ...
            int numTables = buf.getShort(4) & 0xFFFF;
            for (int i = 0; i < numTables; i++) {
                int offset = 12 + i * 16;
                String tag = new String(data, offset, 4, StandardCharsets.US_ASCII);
                if ("name".equals(tag)) {
                    int tableOffset = buf.getInt(offset + 8);
                    // name table: format(2) + count(2) + stringOffset(2)
                    int count = buf.getShort(tableOffset + 2) & 0xFFFF;
                    int stringOffset = tableOffset + (buf.getShort(tableOffset + 4) & 0xFFFF);
                    for (int r = 0; r < count; r++) {
                        int recBase = tableOffset + 6 + r * 12;
                        int platformId = buf.getShort(recBase) & 0xFFFF;
                        int nameId     = buf.getShort(recBase + 6) & 0xFFFF;
                        int length     = buf.getShort(recBase + 8) & 0xFFFF;
                        int strOff     = buf.getShort(recBase + 10) & 0xFFFF;
                        // nameId == 1: Font Family; prefer platform 3 (Windows/Unicode)
                        if (nameId == 1 && (platformId == 3 || platformId == 0)) {
                            String charset = (platformId == 3) ? "UTF-16BE" : "UTF-8";
                            return new String(data, stringOffset + strOff, length, charset).trim();
                        }
                    }
                    // Fallback: pick any nameId==1 record
                    for (int r = 0; r < count; r++) {
                        int recBase = tableOffset + 6 + r * 12;
                        int nameId  = buf.getShort(recBase + 6) & 0xFFFF;
                        int length  = buf.getShort(recBase + 8) & 0xFFFF;
                        int strOff  = buf.getShort(recBase + 10) & 0xFFFF;
                        if (nameId == 1 && length > 0) {
                            return new String(data, stringOffset + strOff, length, StandardCharsets.UTF_8).trim();
                        }
                    }
                    break;
                }
            }
        } catch (Exception ignored) {
            // fall through to fallback
        }
        return fallback;
    }
}