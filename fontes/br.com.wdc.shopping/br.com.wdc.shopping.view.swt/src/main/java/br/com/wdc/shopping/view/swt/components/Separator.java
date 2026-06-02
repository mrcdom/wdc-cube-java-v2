package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * A horizontal separator line (1px height) with optional vertical indent.
 */
public class Separator extends Canvas {

    public Separator(Composite parent, int verticalIndent) {
        super(parent, SWT.NONE);
        var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.heightHint = 1;
        gd.verticalIndent = verticalIndent;
        setLayoutData(gd);
        setBackground(Theme.BG_WHITE);

        addPaintListener(e -> {
            e.gc.setBackground(Theme.BORDER_LIGHT);
            e.gc.fillRectangle(0, 0, getBounds().width, 1);
        });
    }
}
