package br.com.wdc.shopping.view.jfx.impl.receipt;

import java.text.NumberFormat;
import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ReceiptItemViewJfx extends AbstractViewJfx<ReceiptPresenter> {

    private ReceiptItem state;

    private boolean notRendered = true;
    private Label descriptionElm;
    private String descriptionOldValue;
    private Label priceElm;
    private double priceOldValue;
    private Label quantityElm;
    private int quantityOldValue;

    public ReceiptItemViewJfx(ShoppingJfxApplication app, ReceiptPresenter presenter, int idx) {
        super("receipt-item-" + idx, app, presenter, new HBox());
    }

    public void setState(ReceiptItem state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            JfxDom.render((HBox) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.descriptionOldValue, this.state.description)) {
            this.descriptionElm.setText(this.state.description);
            this.descriptionOldValue = this.state.description;
        }

        if (this.priceOldValue != this.state.value) {
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.state.value));
            this.priceOldValue = this.state.value;
        }

        if (this.quantityOldValue != this.state.quantity) {
            this.quantityElm.setText(String.valueOf(this.state.quantity));
            this.quantityOldValue = this.state.quantity;
        }
    }

    private void initialRender(JfxDom dom, HBox pane0) {
        pane0.getStyleClass().add("receipt-row");

        dom.label(label -> {
            label.getStyleClass().add("cell-1");
            this.descriptionElm = label;
            this.descriptionElm.setText(this.state.description);
            this.descriptionOldValue = this.state.description;
        });

        dom.label(label -> {
            label.getStyleClass().add("cell-2");
            this.priceElm = label;
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.state.value));
            this.priceOldValue = this.state.value;
        });

        dom.label(label -> {
            label.getStyleClass().add("cell-3");
            this.quantityElm = label;
            this.quantityElm.setText(String.valueOf(this.state.quantity));
            this.quantityOldValue = this.state.quantity;
        });
    }
}
