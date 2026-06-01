package br.com.wdc.shopping.view.swt.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.util.SlotComposite;

public class RootViewSwt extends AbstractViewSwt<RootPresenter> {

    private final RootViewState state;

    private boolean notRendered = true;
    private SlotComposite contentSlot;
    private AbstractViewSwt<?> currentContentView;

    public RootViewSwt(RootPresenter presenter) {
        super("root", (ShoppingSwtApplication) presenter.app, presenter,
                new Composite(((ShoppingSwtApplication) presenter.app).getShell(), SWT.NONE));
        this.element.setLayout(new FillLayout());
        this.state = presenter.state;

        // Register this as the root pane in the application
        this.app.setRootPane(this.element);

        // Set as topControl of the Shell's StackLayout
        var shell = this.app.getShell();
        ((StackLayout) shell.getLayout()).topControl = this.element;
        shell.layout(true, true);
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.contentSlot = null;
        this.currentContentView = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        var newContentView = this.state.contentView instanceof AbstractViewSwt<?> v ? v : null;
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
