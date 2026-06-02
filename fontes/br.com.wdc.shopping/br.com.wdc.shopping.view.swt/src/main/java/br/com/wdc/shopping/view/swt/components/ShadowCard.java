package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.view.swt.theme.Surface;

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

        addPaintListener(Surface.elevatedCard(this::getClientArea));
    }

    public ShadowCard(Composite parent) {
        this(parent, 32, 32, 0);
    }
}
