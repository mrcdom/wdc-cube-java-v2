package br.com.wdc.shopping.view.jfx.impl;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootViewState;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class RootViewJfx extends AbstractViewJfx<RootPresenter> {

    private final RootViewState state;

    private boolean notRendered = true;
    private StackPane contentPane;
    private AbstractViewJfx<?> currentContentView;

    public RootViewJfx(ShoppingJfxApplication app, RootPresenter presenter) {
        super("root", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            JfxDom.render((VBox) this.element, this::initialRender);
            this.app.getRootPane().getChildren().setAll(this.element);
            this.notRendered = false;
        }

        var newContentView = this.state.contentView instanceof AbstractViewJfx<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentPane.getChildren().clear();
            if (newContentView != null) {
                this.contentPane.getChildren().add(newContentView.getElement());
            }
            this.currentContentView = newContentView;
        }
    }

    private void initialRender(JfxDom dom, VBox pane0) {
        pane0.getStyleClass().add("root-view");

        dom.stackPane(pane1 -> {
            VBox.setVgrow(pane1, Priority.ALWAYS);
            this.contentPane = pane1;
        });
    }
}
