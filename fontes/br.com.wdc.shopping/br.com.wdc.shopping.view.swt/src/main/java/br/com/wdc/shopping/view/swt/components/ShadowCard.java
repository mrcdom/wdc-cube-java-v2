package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * A rounded-corner white card with shadow — standard container for content sections.
 * Children can be added directly to this composite.
 */
public class ShadowCard extends Canvas {

    public ShadowCard(Composite parent, int marginWidth, int marginHeight, int verticalSpacing) {
        super(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);

        var layout = new GridLayout(1, false);
        layout.marginWidth = marginWidth;
        layout.marginHeight = marginHeight;
        layout.verticalSpacing = verticalSpacing;
        setLayout(layout);

        addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            var area = getClientArea();

            gc.setBackground(Theme.BG_PAGE);
            gc.fillRectangle(area);

            int x = 4, y = 2, w = area.width - 8, h = area.height - 6;

            // Shadow
            gc.setAlpha(15);
            gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            gc.fillRoundRectangle(x, y + 2, w, h, Theme.CARD_RADIUS_LARGE, Theme.CARD_RADIUS_LARGE);
            gc.setAlpha(10);
            gc.fillRoundRectangle(x - 1, y + 1, w + 2, h + 2, 26, 26);
            gc.setAlpha(255);

            // White card fill
            gc.setBackground(Theme.BG_WHITE);
            gc.fillRoundRectangle(x, y, w, h, Theme.CARD_RADIUS_LARGE, Theme.CARD_RADIUS_LARGE);

            // Border
            gc.setForeground(Theme.BORDER_LIGHT);
            gc.drawRoundRectangle(x, y, w - 1, h - 1, Theme.CARD_RADIUS_LARGE, Theme.CARD_RADIUS_LARGE);
        });
    }

    public ShadowCard(Composite parent) {
        this(parent, 32, 32, 0);
    }
}
