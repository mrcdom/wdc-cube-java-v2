package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * A text button with an optional leading icon. Shows hover background + border on mouse over.
 * Used for secondary actions like "Continuar comprando" or "Voltar".
 */
public class ActionButton extends Canvas {

    private boolean hovered;

    public ActionButton(Composite parent, String icon, String text, Color background) {
        super(parent, SWT.DOUBLE_BUFFERED);
        setBackground(background);
        setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        // Compute dimensions
        int iconTextGap = 6;
        var tmpGc = new GC(this);
        tmpGc.setFont(Theme.FONT_BODY);
        var textExt = tmpGc.textExtent(text);
        tmpGc.setFont(Theme.FONT_ICON);
        var iconExt = tmpGc.textExtent(icon);
        tmpGc.dispose();
        int btnW = iconExt.x + iconTextGap + textExt.x + 24;
        int btnH = 36;

        var gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gd.widthHint = btnW;
        gd.heightHint = btnH;
        setLayoutData(gd);

        final Point cachedTextExt = textExt;

        addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = getBounds().width;
            int h = getBounds().height;

            if (hovered) {
                gc.setBackground(Theme.BG_BTN_HOVER);
                gc.fillRoundRectangle(0, 0, w, h, 8, 8);
                gc.setForeground(Theme.BORDER_LIGHT);
                gc.drawRoundRectangle(0, 0, w - 1, h - 1, 8, 8);
            } else {
                gc.setBackground(background);
                gc.fillRoundRectangle(0, 0, w, h, 8, 8);
            }

            // Icon
            gc.setFont(Theme.FONT_ICON);
            gc.setForeground(Theme.FG_TEXT_DARK);
            var ie = gc.textExtent(icon);
            gc.drawText(icon, 10, (h - ie.y) / 2, true);

            // Text
            gc.setFont(Theme.FONT_BODY);
            gc.setForeground(Theme.FG_TEXT_DARK);
            gc.drawText(text, 10 + ie.x + iconTextGap, (h - cachedTextExt.y) / 2, true);
        });

        addListener(SWT.MouseEnter, evt -> { hovered = true; redraw(); });
        addListener(SWT.MouseExit, evt -> { hovered = false; redraw(); });
    }
}
