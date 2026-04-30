package br.com.wdc.shopping.view.jfx.impl.cart;

import java.text.NumberFormat;
import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import br.com.wdc.shopping.view.jfx.util.ResourceCatalog;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

public class CartItemViewJfx extends AbstractViewJfx<CartPresenter> {

    private CartItem state;

    private boolean notRendered = true;
    private Label nameElm;
    private String nameOldValue;
    private Label priceElm;
    private double priceOldValue;
    private Label quantityElm;
    private int quantityOldValue;

    public CartItemViewJfx(ShoppingJfxApplication app, CartPresenter presenter, int idx) {
        super("cart-item-" + idx, app, presenter, new HBox());
    }

    public void setState(CartItem state, boolean scheduleUpdate) {
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

        if (!Objects.equals(this.nameOldValue, this.state.name)) {
            this.nameElm.setText(this.state.name);
            this.nameOldValue = this.state.name;
        }

        if (this.priceOldValue != this.state.price) {
            this.priceElm.setText("R$ " + NumberFormat.getInstance().format(this.state.price));
            this.priceOldValue = this.state.price;
        }

        if (this.quantityOldValue != this.state.quantity) {
            this.quantityElm.setText(String.valueOf(this.state.quantity));
            this.quantityOldValue = this.state.quantity;
        }
    }

    private void initialRender(JfxDom dom, HBox pane0) {
        pane0.getStyleClass().add("cart-row");

        dom.hbox(pane1 -> {
            pane1.getStyleClass().add("cell-1");

            dom.img(img -> {
                img.setFitWidth(42);
                img.setFitHeight(40);
                img.setImage(ResourceCatalog.getImage(this.state.image));
            });

            dom.label(label -> {
                this.nameElm = label;
                this.nameElm.setText(this.state.name);
                this.nameOldValue = this.state.name;
            });
        });

        dom.label(label -> {
            label.getStyleClass().add("cell-2");
            this.priceElm = label;
            this.priceElm.setText("R$ " + NumberFormat.getInstance().format(this.state.price));
            this.priceOldValue = this.state.price;
        });

        dom.hbox(pane1 -> {
            pane1.getStyleClass().add("cell-3");

            dom.label(label -> {
                this.quantityElm = label;
                this.quantityElm.setText(String.valueOf(this.state.quantity));
                this.quantityOldValue = this.state.quantity;
            });

            dom.img(img -> {
                img.setImage(ResourceCatalog.getImage("images/delet.png"));
                img.setOnMouseClicked(this::emitDeleteClicked);
            });
        });
    }

    private void emitDeleteClicked(MouseEvent evt) {
        this.presenter.onRemoveProduct(this.state.id);
    }
}
