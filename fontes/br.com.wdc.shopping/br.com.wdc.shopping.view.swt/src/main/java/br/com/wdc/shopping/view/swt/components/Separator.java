package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * A horizontal separator line (1px height) with optional vertical indent.
 */
public class Separator extends Canvas {

    public Separator(Composite parent, int verticalIndent) {
        super(parent, SWT.NONE);
        setBackground(Theme.BG_WHITE);

        addPaintListener(e -> {
            e.gc.setBackground(Theme.BORDER_LIGHT);
            e.gc.fillRectangle(0, 0, getBounds().width, 1);
        });
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        int w = (wHint != SWT.DEFAULT) ? wHint : 100;
        int h = (hHint != SWT.DEFAULT) ? hHint : 1;
        return new Point(w, h);
    }
}
