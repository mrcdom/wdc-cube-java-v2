package br.com.wdc.shopping.view.jfx.impl;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.impl.receipt.ReceiptItemViewJfx;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ReceiptViewJfx extends AbstractViewJfx<ReceiptPresenter> {

    private final ReceiptViewState state;

    private boolean notRendered = true;
    private Label notifySuccessElm;
    private Label totalElm;
    private double totalOldValue;
    private List<ReceiptItemViewJfx> receiptItemViewList = new ArrayList<>();
    private BiConsumer<List<ReceiptItem>, List<ReceiptItemViewJfx>> itemsSlot;
    private int itemIdx;

    public ReceiptViewJfx(ShoppingJfxApplication app, ReceiptPresenter presenter) {
        super("receipt", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            JfxDom.render((VBox) this.element, this::initialRender);
            this.notRendered = false;
        }

        var notifySuccessDisplay = false;
        if (this.state.notifySuccess) {
            notifySuccessDisplay = true;
            this.state.notifySuccess = false;
        }
        if (this.notifySuccessElm.isVisible() != notifySuccessDisplay) {
            this.notifySuccessElm.setVisible(notifySuccessDisplay);
            this.notifySuccessElm.setManaged(notifySuccessDisplay);
        }

        if (this.totalOldValue != this.state.receipt.total) {
            this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.receipt.total));
            this.totalOldValue = this.state.receipt.total;
        }

        this.itemsSlot.accept(this.state.receipt.items, this.receiptItemViewList);
    }

    private void initialRender(JfxDom dom, VBox pane0) {
        pane0.getStyleClass().add("receipt-form");
        pane0.setMaxWidth(900);

        dom.label(label -> {
            label.getStyleClass().add("success-lbl");
            this.notifySuccessElm = label;
            this.notifySuccessElm.setVisible(this.state.notifySuccess);
            this.notifySuccessElm.setManaged(this.state.notifySuccess);
            this.notifySuccessElm.setText("COMPRA EFETUADA COM SUCESSO");
        });

        dom.label(label -> {
            label.getStyleClass().add("title");
            label.setText("IMPRIMA SEU RECIBO:");
        });

        dom.vbox(pane1 -> {
            pane1.getStyleClass().add("content");

            dom.label(label -> {
                label.getStyleClass().add("caption-1");
                label.setText("WEDOCODE SHOPPING - SUA COMPRA CERTA NA INTERNET");
            });

            dom.label(label -> {
                label.getStyleClass().add("caption-2");
                label.setText("Recibo de compra");
            });

            dom.hbox(pane2 -> {
                pane2.getStyleClass().add("receipt-header");

                dom.label(label -> {
                    label.getStyleClass().add("cell-1");
                    label.setText("ITEM");
                });

                dom.label(label -> {
                    label.getStyleClass().add("cell-2");
                    label.setText("VALOR");
                });

                dom.label(label -> {
                    label.getStyleClass().add("cell-3");
                    label.setText("QUANTIDADE");
                });
            });

            dom.vbox(pane2 -> {
                pane2.getStyleClass().add("items");
                this.itemsSlot = this.newListSlot(pane2, this::newItemView, this::updateItem);
            });

            dom.hbox(pane2 -> {
                pane2.getStyleClass().add("receipt-footer");

                dom.label(label -> label.setText("VALOR TOTAL: "));

                dom.label(label -> {
                    this.totalElm = label;
                    this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.receipt.total));
                    this.totalOldValue = this.state.receipt.total;
                });
            });
        });

        dom.button(button -> {
            button.getStyleClass().add("back-button");
            button.setText("< VOLTAR");
            button.setOnAction(this::emitBackClicked);
        });
    }

    private ReceiptItemViewJfx newItemView() {
        return new ReceiptItemViewJfx(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ReceiptItemViewJfx itemView, ReceiptItem state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }

    private void emitBackClicked(ActionEvent evt) {
        this.presenter.onOpenProducts();
    }
}
