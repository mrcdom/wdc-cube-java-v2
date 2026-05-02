package br.com.wdc.shopping.view.swing.util;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Centralized styling constants and helper methods, replacing the JavaFX CSS.
 */
public final class Styles {

    // Colors
    public static final Color BG_PAGE = new Color(0xf5f5f5);
    public static final Color BG_WHITE = Color.WHITE;
    public static final Color BG_HEADER = new Color(0x333333);
    public static final Color BG_LOGIN_GRADIENT_TOP = new Color(0xe3f2fd);
    public static final Color BG_PURCHASES_PANEL = new Color(0x546e7a);
    public static final Color BG_FIELD = new Color(0xfafafa);
    public static final Color BG_TABLE_HEADER = new Color(0xf5f5f5);
    public static final Color BG_TABLE_FOOTER = new Color(0xfafafa);
    public static final Color BG_PRODUCT_LABEL = new Color(0xe8e8e8);
    public static final Color BG_SUCCESS = new Color(0xe8f5e9);
    public static final Color BG_ERROR = new Color(0xffebee);
    public static final Color BG_ORDER_HEADER = new Color(0xeceff1);

    public static final Color FG_PRIMARY = new Color(0x1976d2);
    public static final Color FG_TEXT = new Color(0x424242);
    public static final Color FG_TEXT_DARK = new Color(0x212121);
    public static final Color FG_TEXT_LIGHT = new Color(0x616161);
    public static final Color FG_TEXT_SUBTLE = new Color(0x757575);
    public static final Color FG_WHITE = Color.WHITE;
    public static final Color FG_WHITE_DIM = new Color(0xeeeeee);
    public static final Color FG_RED = new Color(0xff5252);
    public static final Color FG_ERROR = new Color(0xd32f2f);
    public static final Color FG_SUCCESS = new Color(0x2e7d32);

    public static final Color BTN_PRIMARY = new Color(0x1976d2);
    public static final Color BTN_PRIMARY_HOVER = new Color(0x1565c0);
    public static final Color BTN_ORANGE = new Color(0xed6c02);
    public static final Color BTN_ORANGE_HOVER = new Color(0xe65100);

    public static final Color BORDER_LIGHT = new Color(0xe0e0e0);
    public static final Color BORDER_SUBTLE = new Color(0xeeeeee);
    public static final Color BORDER_SUCCESS = new Color(0xa5d6a7);
    public static final Color BORDER_ERROR = new Color(0xef9a9a);
    public static final Color PRICE_BADGE = new Color(0x1976d2);

    // Fonts
    public static final Font FONT_DEFAULT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    public static final Font FONT_SUBTITLE = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    public static final Font FONT_FIELD_LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_LOGIN_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    public static final Font FONT_PRODUCT_NAME = new Font(Font.SANS_SERIF, Font.PLAIN, 24);
    public static final Font FONT_PRODUCT_PRICE = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    public static final Font FONT_BUTTON = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Font FONT_BUTTON_LARGE = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    public static final Font FONT_BUTTON_SMALL = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_LOGIN_BUTTON = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font FONT_RECEIPT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public static final Font FONT_TABLE_HEADER = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_DESC_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    // Borders
    public static final Border BORDER_EMPTY_8 = new EmptyBorder(8, 8, 8, 8);
    public static final Border BORDER_EMPTY_12 = new EmptyBorder(12, 12, 12, 12);
    public static final Border BORDER_EMPTY_16 = new EmptyBorder(16, 16, 16, 16);
    public static final Border BORDER_EMPTY_24 = new EmptyBorder(24, 24, 24, 24);

    private Styles() {
        super();
    }

    public static void applyGlobalDefaults() {
        UIManager.put("Panel.background", BG_PAGE);
        UIManager.put("Label.font", FONT_DEFAULT);
        UIManager.put("Button.font", FONT_BUTTON);
        UIManager.put("TextField.font", FONT_DEFAULT);
    }

    public static void stylePrimaryButton(JButton btn) {
        btn.setBackground(BTN_PRIMARY);
        btn.setForeground(FG_WHITE);
        btn.setFont(FONT_LOGIN_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(BTN_PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BTN_PRIMARY);
            }
        });
    }

    public static void styleOrangeButton(JButton btn) {
        btn.setBackground(BTN_ORANGE);
        btn.setForeground(FG_WHITE);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(10, 24, 10, 24));
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(BTN_ORANGE_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BTN_ORANGE);
            }
        });
    }

    public static void styleOutlineButton(JButton btn, Color color) {
        btn.setBackground(BG_WHITE);
        btn.setForeground(color);
        btn.setFont(FONT_BUTTON_SMALL);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                new EmptyBorder(6, 14, 6, 14)));
        btn.setOpaque(true);
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 15));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_WHITE);
            }
        });
    }

    public static void styleHeaderButton(JButton btn) {
        btn.setBackground(new Color(0, 0, 0, 0));
        btn.setForeground(FG_WHITE_DIM);
        btn.setFont(FONT_BUTTON_SMALL);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 76), 1),
                new EmptyBorder(6, 12, 6, 12)));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    public static void styleField(JTextField field) {
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                new EmptyBorder(8, 12, 8, 12)));
        field.setBackground(BG_FIELD);
    }

    public static void styleErrorLabel(JLabel label) {
        label.setForeground(FG_ERROR);
        label.setFont(FONT_SUBTITLE);
        label.setOpaque(true);
        label.setBackground(BG_ERROR);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_ERROR, 1),
                new EmptyBorder(8, 8, 8, 8)));
    }

    public static void styleSuccessLabel(JLabel label) {
        label.setForeground(FG_SUCCESS);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setOpaque(true);
        label.setBackground(BG_SUCCESS);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUCCESS, 1),
                new EmptyBorder(14, 16, 14, 16)));
    }

    /**
     * Creates a clean border for card-like components.
     */
    public static Border createCardBorder() {
        return BorderFactory.createLineBorder(BORDER_LIGHT, 1);
    }

    /**
     * Creates a highlighted border for card hover state (same total size as card border + 1px padding).
     */
    public static Border createCardHoverBorder() {
        return BorderFactory.createLineBorder(FG_PRIMARY, 2);
    }

    /**
     * Styles a small link-like button used in sidebar panels.
     */
    public static void styleLinkButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(FG_WHITE);
        btn.setFont(FONT_SMALL);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(5, 12, 5, 12));
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        var hoverColor = color.darker();
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(color);
            }
        });
    }

}

