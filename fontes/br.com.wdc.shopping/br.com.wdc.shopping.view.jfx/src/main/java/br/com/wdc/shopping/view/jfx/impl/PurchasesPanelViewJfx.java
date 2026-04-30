package br.com.wdc.shopping.view.jfx.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.impl.home.PurchaseItemViewJfx;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PurchasesPanelViewJfx extends AbstractViewJfx<PurchasesPanelPresenter> {

    private final PurchasesPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<PurchaseItemViewJfx> viewList = new ArrayList<>();
    private BiConsumer<List<PurchaseInfo>, List<PurchaseItemViewJfx>> contentSlot;
    private Label pageInfoElm;

    public PurchasesPanelViewJfx(ShoppingJfxApplication app, PurchasesPanelPresenter presenter) {
        super("purchases-panel", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            JfxDom.render((VBox) this.element, this::initialRender);
            this.notRendered = false;
        }
        this.contentSlot.accept(this.state.purchases, this.viewList);

        int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / this.state.pageSize));
        this.pageInfoElm.setText("Página " + (this.state.page + 1) + " de " + totalPages);
    }

    private void initialRender(JfxDom dom, VBox pane0) {
        pane0.getStyleClass().add("purchases-panel");

        dom.label(label -> {
            label.getStyleClass().add("caption");
            label.setText("Seu histórico de compras");
        });

        dom.vbox(pane1 -> {
            pane1.getStyleClass().add("content");
            this.contentSlot = this.newListSlot(pane1, this::newItemView, this::updateItem);
        });

        dom.hbox(pane1 -> {
            pane1.getStyleClass().add("pagination");

            dom.button(btn -> {
                btn.setText("< Anterior");
                btn.setOnAction(this::emitPrevPage);
            });

            dom.label(label -> {
                this.pageInfoElm = label;
            });

            dom.button(btn -> {
                btn.setText("Próxima >");
                btn.setOnAction(this::emitNextPage);
            });
        });
    }

    private PurchaseItemViewJfx newItemView() {
        return new PurchaseItemViewJfx(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(PurchaseItemViewJfx itemView, PurchaseInfo state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }

    private void emitPrevPage(ActionEvent evt) {
        this.presenter.onPageChange(this.state.page - 1);
    }

    private void emitNextPage(ActionEvent evt) {
        this.presenter.onPageChange(this.state.page + 1);
    }
}
