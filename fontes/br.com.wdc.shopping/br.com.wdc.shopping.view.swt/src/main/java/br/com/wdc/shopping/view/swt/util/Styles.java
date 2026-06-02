package br.com.wdc.shopping.view.swt.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * @deprecated Use {@link Theme} directly. This class delegates all tokens to Theme for backward compatibility.
 */
@Deprecated
public final class Styles {

    private Styles() {
    }

    // :: Colors
    public static final Color PRIMARY_BLUE = Theme.PRIMARY_BLUE;
    public static final Color PRIMARY_BLUE_DARK = Theme.PRIMARY_BLUE_DARK;
    public static final Color PRIMARY_BLUE_LIGHT = Theme.PRIMARY_BLUE_LIGHT;
    public static final Color BG_PAGE = Theme.BG_PAGE;
    public static final Color BG_WHITE = Theme.BG_WHITE;
    public static final Color BG_HEADER = Theme.BG_HEADER;
    public static final Color BG_LOGIN_LEFT = Theme.BG_LOGIN_LEFT;
    public static final Color BG_CARD = Theme.BG_CARD;
    public static final Color BG_SUCCESS = Theme.BG_SUCCESS;
    public static final Color FG_TEXT_DARK = Theme.FG_TEXT_DARK;
    public static final Color FG_TEXT_SUBTLE = Theme.FG_TEXT_SUBTLE;
    public static final Color FG_TEXT_WHITE = Theme.FG_TEXT_WHITE;
    public static final Color FG_TEXT_WHITE_70 = Theme.FG_TEXT_WHITE_70;
    public static final Color FG_TEXT_WHITE_65 = Theme.FG_TEXT_WHITE_65;
    public static final Color FG_PRICE = Theme.FG_PRICE;
    public static final Color FG_LINK = Theme.FG_LINK;
    public static final Color BORDER_LIGHT = Theme.BORDER_LIGHT;
    public static final Color BORDER_FIELD = Theme.BORDER_FIELD;
    public static final Color BORDER_FIELD_FOCUS = Theme.BORDER_FIELD_FOCUS;
    public static final Color BORDER_ERROR = Theme.BORDER_ERROR;
    public static final Color FG_ERROR = Theme.FG_ERROR;
    public static final Color BG_ERROR = Theme.BG_ERROR;
    public static final Color BORDER_ERROR_BOX = Theme.BORDER_ERROR_BOX;

    // :: Fonts
    public static final Font FONT_TITLE = Theme.FONT_TITLE;
    public static final Font FONT_WELCOME = Theme.FONT_WELCOME;
    public static final Font FONT_SUBTITLE = Theme.FONT_SUBTITLE;
    public static final Font FONT_HEADER = Theme.FONT_HEADER;
    public static final Font FONT_HEADER_BOLD = Theme.FONT_HEADER_BOLD;
    public static final Font FONT_BODY = Theme.FONT_BODY;
    public static final Font FONT_BODY_BOLD = Theme.FONT_BODY_BOLD;
    public static final Font FONT_FIELD_LABEL = Theme.FONT_FIELD_LABEL;
    public static final Font FONT_BUTTON = Theme.FONT_BUTTON;
    public static final Font FONT_PRICE = Theme.FONT_PRICE;
    public static final Font FONT_BANNER_TITLE = Theme.FONT_BANNER_TITLE;
    public static final Font FONT_BANNER_SUBTITLE = Theme.FONT_BANNER_SUBTITLE;
    public static final Font FONT_BADGE = Theme.FONT_BADGE;
    public static final Font FONT_PRODUCT_NAME = Theme.FONT_PRODUCT_NAME;
    public static final Font FONT_NAV_SMALL = Theme.FONT_NAV_SMALL;
    public static final Font FONT_NAV_TITLE = Theme.FONT_NAV_TITLE;
    public static final Font FONT_PAGINATION = Theme.FONT_PAGINATION;
    public static final Font FONT_ICON = Theme.FONT_ICON;
    public static final Font FONT_ICON_NAV = Theme.FONT_ICON_NAV;
    public static final Font FONT_ICON_LARGE = Theme.FONT_ICON_LARGE;

    // :: Icons
    public static final String ICON_BAG_CHECK = Theme.ICON_BAG_CHECK;
    public static final String ICON_BAG = Theme.ICON_BAG;
    public static final String ICON_BOX_ARROW_LEFT = Theme.ICON_BOX_ARROW_LEFT;
    public static final String ICON_CHEVRON_LEFT = Theme.ICON_CHEVRON_LEFT;
    public static final String ICON_CHEVRON_RIGHT = Theme.ICON_CHEVRON_RIGHT;
    public static final String ICON_SHIELD_CHECK = Theme.ICON_SHIELD_CHECK;
    public static final String ICON_TRUCK = Theme.ICON_TRUCK;
    public static final String ICON_ARROW_REPEAT = Theme.ICON_ARROW_REPEAT;
    public static final String ICON_EXCLAMATION_CIRCLE = Theme.ICON_EXCLAMATION_CIRCLE;
    public static final String ICON_CLOCK_HISTORY = Theme.ICON_CLOCK_HISTORY;
    public static final String ICON_BAG_CHECK_FILL = Theme.ICON_BAG_CHECK_FILL;
    public static final String ICON_RECEIPT = Theme.ICON_RECEIPT;

    // :: Dimensions
    public static final int HEADER_HEIGHT = Theme.HEADER_HEIGHT;
    public static final int CARD_RADIUS = Theme.CARD_RADIUS;
    public static final int FIELD_HEIGHT = Theme.FIELD_HEIGHT;
    public static final int BUTTON_HEIGHT = Theme.BUTTON_HEIGHT;
    public static final int SPACING_SM = Theme.SPACING_SM;
    public static final int SPACING_MD = Theme.SPACING_MD;
    public static final int SPACING_LG = Theme.SPACING_LG;
    public static final int SPACING_XL = Theme.SPACING_XL;
}
