package br.com.wdc.shopping.view.swt.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A composite that shows only one child at a time using StackLayout.
 * Equivalent to CardLayout in AWT / StackPanel in the Swing version.
 */
public class StackComposite extends Composite {

    private final StackLayout stackLayout;

    public StackComposite(Composite parent) {
        super(parent, SWT.NONE);
        this.stackLayout = new StackLayout();
        this.setLayout(this.stackLayout);
    }

    public void showControl(Control control) {
        this.stackLayout.topControl = control;
        this.layout(true, true);
    }

    public Control getTopControl() {
        return this.stackLayout.topControl;
    }
}
