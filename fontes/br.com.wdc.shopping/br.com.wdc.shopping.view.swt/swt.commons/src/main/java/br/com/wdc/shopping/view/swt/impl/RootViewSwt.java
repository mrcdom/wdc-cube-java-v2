package br.com.wdc.shopping.view.swt.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import java.util.function.Supplier;

import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.util.SlotComposite;

public class RootViewSwt extends AbstractViewSwt {

    public Supplier<RootViewState> stateSupplier;

    private boolean notRendered = true;
    private SlotComposite contentSlot;
    private AbstractViewSwt currentContentView;

    public RootViewSwt(SwtApp app) {
        super("root", app, new Composite(app.getShell(), SWT.NONE));
        this.element.setLayout(new FillLayout());

        // Register this as the root pane in the application
        this.app.setRootPane(this.element);

        // Set as topControl of the Shell's StackLayout
        var shell = this.app.getShell();
        ((StackLayout) shell.getLayout()).topControl = this.element;
        shell.layout(true, true);
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        var state = this.stateSupplier != null ? this.stateSupplier.get() : null;
        var newContentView = state != null && state.contentView instanceof AbstractViewSwt v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentSlot.setContent(newContentView != null ? newContentView.getElement() : null);
            this.currentContentView = newContentView;
            this.app.getShell().layout(true, true);
        }
    }

    private void initialRender() {
        this.contentSlot = new SlotComposite(this.element, this.app.getOffscreen());
        this.element.layout(true, true);
    }
}
