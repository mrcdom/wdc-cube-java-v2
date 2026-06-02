package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * A colored horizontal accent line (e.g. blue divider between sections).
 */
public class AccentLine extends Canvas {

    public AccentLine(Composite parent, Color color, int height, int verticalIndent) {
        super(parent, SWT.NONE);
        setBackground(parent.getBackground());

        var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.heightHint = height;
        gd.verticalIndent = verticalIndent;
        setLayoutData(gd);

        addPaintListener(e -> {
            e.gc.setBackground(color);
            e.gc.fillRectangle(0, 0, getBounds().width, height);
        });
    }

    public AccentLine(Composite parent, int height, int verticalIndent) {
        this(parent, Theme.PRIMARY_BLUE, height, verticalIndent);
    }
}
