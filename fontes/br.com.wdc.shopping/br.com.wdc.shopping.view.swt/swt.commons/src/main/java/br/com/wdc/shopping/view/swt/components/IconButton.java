package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * A 30x30 icon button with hover effect. Shows the icon centered,
 * with optional border and background on hover.
 */
public class IconButton extends Canvas {

    private static final int SIZE = 30;
    private boolean hovered;

    public IconButton(Composite parent, String icon, Color iconColor, org.eclipse.swt.graphics.Font iconFont, Color background) {
        super(parent, SWT.DOUBLE_BUFFERED);
        setBackground(background);
        setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = 30;
            int h = 30;
            if (hovered) {
                gc.setBackground(Theme.BG_BTN_HOVER);
                gc.fillRoundRectangle(0, 0, w, h, 6, 6);
                gc.setForeground(Theme.BORDER_LIGHT);
                gc.drawRoundRectangle(0, 0, w - 1, h - 1, 6, 6);
            } else {
                gc.setBackground(background);
                gc.fillRoundRectangle(0, 0, w, h, 6, 6);
            }
            gc.setFont(iconFont);
            gc.setForeground(iconColor);
            var ext = gc.textExtent(icon);
            gc.drawText(icon, (w - ext.x) / 2, (h - ext.y) / 2, true);
        });

        addListener(SWT.MouseEnter, evt -> { hovered = true; redraw(); });
        addListener(SWT.MouseExit, evt -> { hovered = false; redraw(); });
    }

    public IconButton(Composite parent, String icon, Color background) {
        this(parent, icon, Theme.FG_TEXT_DARK, Theme.FONT_ICON, background);
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        int w = (wHint != SWT.DEFAULT) ? wHint : SIZE;
        int h = (hHint != SWT.DEFAULT) ? hHint : SIZE;
        return new Point(w, h);
    }
}
