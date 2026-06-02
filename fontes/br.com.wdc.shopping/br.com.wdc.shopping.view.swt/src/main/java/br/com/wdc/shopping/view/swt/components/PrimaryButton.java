package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * A filled primary button (blue background, white text) with an optional leading icon.
 * Used for primary actions like "Finalizar pedido" or "Adicionar ao Carrinho".
 */
public class PrimaryButton extends Canvas {

    private boolean hovered;
    private final int preferredWidth;
    private final int preferredHeight;

    public PrimaryButton(Composite parent, String icon, String text, Color background) {
        super(parent, SWT.DOUBLE_BUFFERED);
        setBackground(background);
        setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        // Compute dimensions
        var tmpGc = new GC(this);
        tmpGc.setFont(Theme.FONT_BODY_BOLD);
        var textExt = tmpGc.textExtent(text);
        tmpGc.setFont(Theme.FONT_ICON);
        var iconExt = tmpGc.textExtent(icon);
        tmpGc.dispose();
        this.preferredWidth = iconExt.x + textExt.x + 36;
        this.preferredHeight = 42;

        final Point cachedTextExt = textExt;

        addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = getBounds().width;
            int h = getBounds().height;

            // Filled blue background (darker on hover)
            gc.setBackground(hovered ? Theme.PRIMARY_BLUE_DARK : Theme.PRIMARY_BLUE);
            gc.fillRoundRectangle(0, 0, w, h, 8, 8);

            // Icon
            gc.setFont(Theme.FONT_ICON);
            gc.setForeground(Theme.BG_WHITE);
            var ie = gc.textExtent(icon);
            int x = 16;
            gc.drawText(icon, x, (h - ie.y) / 2, true);

            // Text
            gc.setFont(Theme.FONT_BODY_BOLD);
            gc.setForeground(Theme.BG_WHITE);
            gc.drawText(text, x + ie.x + 6, (h - cachedTextExt.y) / 2, true);
        });

        addListener(SWT.MouseEnter, evt -> { hovered = true; redraw(); });
        addListener(SWT.MouseExit, evt -> { hovered = false; redraw(); });
    }

    public PrimaryButton(Composite parent, String icon, String text) {
        this(parent, icon, text, Theme.BG_WHITE);
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        int w = (wHint != SWT.DEFAULT) ? wHint : this.preferredWidth;
        int h = (hHint != SWT.DEFAULT) ? hHint : this.preferredHeight;
        return new Point(w, h);
    }
}
