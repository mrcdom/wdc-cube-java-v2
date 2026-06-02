package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * A colored horizontal accent line (e.g. blue divider between sections).
 */
public class AccentLine extends Canvas {

    private final int preferredHeight;

    public AccentLine(Composite parent, Color color, int height, int verticalIndent) {
        super(parent, SWT.NONE);
        setBackground(parent.getBackground());
        this.preferredHeight = height;

        addPaintListener(e -> {
            e.gc.setBackground(color);
            e.gc.fillRectangle(0, 0, getBounds().width, height);
        });
    }

    public AccentLine(Composite parent, int height, int verticalIndent) {
        this(parent, Theme.PRIMARY_BLUE, height, verticalIndent);
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        int w = (wHint != SWT.DEFAULT) ? wHint : 100;
        int h = (hHint != SWT.DEFAULT) ? hHint : this.preferredHeight;
        return new Point(w, h);
    }
}
