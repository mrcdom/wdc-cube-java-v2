package br.com.wdc.shopping.view.jfx.impl;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.impl.cart.CartItemViewJfx;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import br.com.wdc.shopping.view.jfx.util.ResourceCatalog;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class CartViewJfx extends AbstractViewJfx<CartPresenter> {

    private final CartViewState state;

    private boolean notRendered = true;
    private List<CartItemViewJfx> cartItemViewList = new ArrayList<>();
    private BiConsumer<List<CartItem>, List<CartItemViewJfx>> itemsSlot;
    private int itemIdx;
    private Label itemSizeElm;
    private int itemSizeOldValue;
    private Label totalCostElm;
    private double totalCostOldValue;
    private Label errorElm;

    public CartViewJfx(ShoppingJfxApplication app, CartPresenter presenter) {
        super("cart", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            JfxDom.render((VBox) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (this.itemSizeOldValue != this.state.items.size()) {
            this.itemSizeElm.setText("[" + this.state.items.size() + "]");
            this.itemSizeOldValue = this.state.items.size();
        }

        this.itemsSlot.accept(this.state.items, this.cartItemViewList);

        var totalCostNewValue = this.computeTotalCost();
        if (totalCostNewValue != this.totalCostOldValue) {
            this.totalCostElm.setText(this.formatCurrency(totalCostNewValue));
            this.totalCostOldValue = totalCostNewValue;
        }

        var newErrorDisplay = false;
        var newErrorMessage = "";
        if (this.state.errorCode != 0) {
            newErrorDisplay = true;
            newErrorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        }
        if (!Objects.equals(this.errorElm.getText(), newErrorMessage)) {
            this.errorElm.setText(newErrorMessage);
        }
        if (this.errorElm.isVisible() != newErrorDisplay) {
            this.errorElm.setVisible(newErrorDisplay);
            this.errorElm.setManaged(newErrorDisplay);
        }
    }

    @SuppressWarnings("unused")
    private void initialRender(JfxDom dom, VBox pane0) {
        pane0.getStyleClass().add("cart-form");
        pane0.setMaxWidth(900);

        dom.hbox(pane1 -> {
            pane1.getStyleClass().add("cart-logo-pane");

            dom.img(img -> {
                img.setImage(ResourceCatalog.getImage("images/carrinho.png"));
                img.setFitWidth(24);
                img.setFitHeight(24);
            });

            dom.label(label -> {
                label.setText("Carrinho");
            });

            dom.label(label -> {
                this.itemSizeElm = label;
                this.itemSizeElm.setText("[" + this.state.items.size() + "]");
                this.itemSizeOldValue = this.state.items.size();
            });

            dom.hSpacer();
        });

        dom.label(label -> {
            label.getStyleClass().add("title");
            label.setText("LISTA DE PRODUTOS");
        });

        dom.vbox(pane1 -> {
            pane1.getStyleClass().add("content");

            dom.hbox(pane2 -> {
                pane2.getStyleClass().add("cart-header");

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
                pane2.getStyleClass().add("tbody");
                this.itemsSlot = this.newListSlot(pane2, this::newItemView, this::updateItem);
            });

            dom.hbox(pane2 -> {
                pane2.getStyleClass().add("cart-footer");

                dom.hSpacer();

                dom.label(label -> label.setText("VALOR TOTAL: "));

                dom.label(label -> {
                    var totalCostNewValue = this.computeTotalCost();
                    this.totalCostElm = label;
                    this.totalCostElm.setText(this.formatCurrency(totalCostNewValue));
                    this.totalCostOldValue = totalCostNewValue;
                });
            });
        });

        dom.label(label -> {
            this.errorElm = label;
            this.errorElm.getStyleClass().add("error");
            this.errorElm.setVisible(false);
            this.errorElm.setManaged(false);
        });

        dom.hbox(_ -> {
            dom.hSpacer();

            dom.button(button -> {
                button.getStyleClass().add("do-buy-button");
                button.setText("FINALIZAR PEDIDO");
                button.setOnAction(this::emitCommitClicked);
            });
        });

        dom.button(button -> {
            button.getStyleClass().add("back-button");
            button.setText("< VOLTAR");
            button.setOnAction(this::emitBackClicked);
        });
    }

    private double computeTotalCost() {
        return this.state.items.stream().mapToDouble(v -> v.price * v.quantity).sum();
    }

    private String formatCurrency(double value) {
        return "R$ " + NumberFormat.getInstance().format(value);
    }

    private CartItemViewJfx newItemView() {
        return new CartItemViewJfx(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(CartItemViewJfx itemView, CartItem state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }

    private void emitBackClicked(ActionEvent evt) {
        this.presenter.onOpenProducts();
    }

    private void emitCommitClicked(ActionEvent evt) {
        this.presenter.onBuy();
    }
}
