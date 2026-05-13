package br.com.wdc.shopping.view.gluon.impl;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootViewState;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import javafx.scene.layout.StackPane;

public class RootViewGluon extends AbstractViewGluon<RootPresenter> {

    private final RootViewState state;
    private AbstractViewGluon<?> currentContentView;

    public RootViewGluon(RootPresenter presenter) {
        super("root", (ShoppingGluonApplication) presenter.app, presenter, new StackPane());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        var pane = (StackPane) this.element;

        if (pane.getParent() == null) {
            this.app.getRootPane().getChildren().setAll(pane);
        }

        var newContentView = this.state.contentView instanceof AbstractViewGluon<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            pane.getChildren().clear();
            if (newContentView != null) {
                pane.getChildren().add(newContentView.getElement());
            }
            this.currentContentView = newContentView;
        }
    }
}
