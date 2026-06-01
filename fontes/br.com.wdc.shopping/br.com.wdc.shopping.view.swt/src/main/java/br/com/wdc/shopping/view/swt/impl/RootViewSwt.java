package br.com.wdc.shopping.view.swt.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.util.StackComposite;

public class RootViewSwt extends AbstractViewSwt<RootPresenter> {

    private final RootViewState state;

    private boolean notRendered = true;
    private StackComposite contentPane;
    private AbstractViewSwt<?> currentContentView;

    public RootViewSwt(RootPresenter presenter) {
        super("root", (ShoppingSwtApplication) presenter.app, presenter,
                new Composite(((ShoppingSwtApplication) presenter.app).getShell(), SWT.NONE));
        this.element.setLayout(new FillLayout());
        this.state = presenter.state;

        // Register this as the root pane in the application
        this.app.setRootPane(this.element);
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.contentPane = null;
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
            replaceContent(newContentView);
            this.currentContentView = newContentView;
        }
    }

    private void initialRender() {
        this.contentPane = new StackComposite(this.element);
        this.element.layout(true, true);
    }

    private void replaceContent(AbstractViewSwt<?> newView) {
        if (newView != null) {
            Control control = newView.getElement();
            // Re-parent if needed
            if (control.getParent() != this.contentPane) {
                // Can't reparent in SWT — dispose old children and re-create
                for (var child : this.contentPane.getChildren()) {
                    child.setVisible(false);
                }
                // The view's element was created with a different parent; show it
            }
            this.contentPane.showControl(control);
        }
        this.element.layout(true, true);
    }
}
