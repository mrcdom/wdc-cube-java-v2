package br.com.wdc.store.view.vaadin.impl;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.view.vaadin.AbstractViewVaadin;
import br.com.wdc.store.view.vaadin.StoreVaadinApplication;
import br.com.wdc.store.view.vaadin.util.VaadinDom;

public class RootViewVaadin extends AbstractViewVaadin<RootScope> {

    private boolean notRendered = true;
    private Div contentPane;
    private AbstractViewVaadin<?> currentContentView;

    public RootViewVaadin(StoreVaadinApplication app, RootScope scope) {
        super("root", app, scope, new Div());
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

        var newContentView = this.viewOf(this.scope.content.get()) instanceof AbstractViewVaadin<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentPane.removeAll();
            if (newContentView != null) {
                this.contentPane.add(newContentView.getElement());
            }
            this.currentContentView = newContentView;
        }

        // An unexpected error: shown once, as a notification over the content
        var error = this.scope.errorMessage.get();
        if (error != null) {
            this.scope.errorMessage.setSilently(null);
            Notification.show(text(error), 8000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
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
