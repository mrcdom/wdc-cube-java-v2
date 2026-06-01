package br.com.wdc.shopping.view.swt.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * A composite rendered as a rounded-corner white card with shadow —
 * mimicking the CSS card style from the TeaVM web version.
 */
public class CardComposite extends Composite {

    public CardComposite(Composite parent) {
        super(parent, SWT.DOUBLE_BUFFERED);
        setBackground(Styles.BG_PAGE);

        var layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);

        var canvas = new Canvas(this, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        canvas.addPaintListener(this::paintCard);
    }

    private void paintCard(PaintEvent e) {
        GC gc = e.gc;
        Rectangle bounds = ((Canvas) e.widget).getBounds();

        gc.setAdvanced(true);
        gc.setAntialias(SWT.ON);

        // Shadow (subtle offset)
        gc.setAlpha(30);
        gc.setBackground(e.display.getSystemColor(SWT.COLOR_BLACK));
        gc.fillRoundRectangle(4, 4, bounds.width - 8, bounds.height - 6, Styles.CARD_RADIUS, Styles.CARD_RADIUS);

        // Card body
        gc.setAlpha(255);
        gc.setBackground(Styles.BG_CARD);
        gc.fillRoundRectangle(2, 2, bounds.width - 8, bounds.height - 8, Styles.CARD_RADIUS, Styles.CARD_RADIUS);

        // Border
        gc.setForeground(Styles.BORDER_LIGHT);
        gc.drawRoundRectangle(2, 2, bounds.width - 8, bounds.height - 8, Styles.CARD_RADIUS, Styles.CARD_RADIUS);
    }
}
