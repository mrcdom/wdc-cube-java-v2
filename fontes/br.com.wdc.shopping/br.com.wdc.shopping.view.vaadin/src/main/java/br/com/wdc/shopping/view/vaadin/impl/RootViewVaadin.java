package br.com.wdc.shopping.view.vaadin.impl;

import com.vaadin.flow.component.html.Div;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootViewState;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class RootViewVaadin extends AbstractViewVaadin<RootPresenter> {

    private final RootViewState state;

    private boolean notRendered = true;
    private Div contentPane;
    private AbstractViewVaadin<?> currentContentView;

    public RootViewVaadin(ShoppingVaadinApplication app, RootPresenter presenter) {
        super("root", app, presenter, new Div());
        this.state = presenter.state;
    }

    @Override
    public void recreate() {
        this.element = new Div();
        this.notRendered = true;
        this.contentPane = null;
        this.currentContentView = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((Div) this.element, this::initialRender);
            this.app.getRootContainer().removeAll();
            this.app.getRootContainer().add(this.element);
            this.notRendered = false;
        }

        var newContentView = this.state.contentView instanceof AbstractViewVaadin<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentPane.removeAll();
            if (newContentView != null) {
                this.contentPane.add(newContentView.getElement());
            }
            this.currentContentView = newContentView;
        }
    }

    private void initialRender(VaadinDom dom, Div pane0) {
        pane0.addClassName("root-view");
        pane0.setSizeFull();

        dom.div(pane1 -> {
            pane1.setSizeFull();
            this.contentPane = pane1;
        });
    }
}
