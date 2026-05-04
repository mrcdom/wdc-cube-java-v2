package br.com.wdc.shopping.view.robovm.impl.cart;

import java.util.Objects;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.NSTextAlignment;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class CartItemViewRoboVM extends AbstractViewRoboVM<CartPresenter> {

    private CartItem state;

    private boolean notRendered = true;
    private UILabel nameLabel;
    private String nameOldValue;
    private UILabel priceLabel;
    private double priceOldValue;
    private UILabel qtyLabel;
    private UILabel subtotalLabel;
    private int quantityOldValue;

    public CartItemViewRoboVM(ShoppingRoboVMApplication app, CartPresenter presenter, int idx) {
        super("cart-item-" + idx, app, presenter);
    }

    public void setState(CartItem state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.nameLabel = null;
        this.nameOldValue = null;
        this.priceLabel = null;
        this.priceOldValue = 0;
        this.qtyLabel = null;
        this.subtotalLabel = null;
        this.quantityOldValue = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.rootView = new UIView(new CGRect(0, 0, 343, 88));
            UIKitDom.render(this.rootView, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.nameOldValue, this.state.name)) {
            nameLabel.setText(this.state.name);
            this.nameOldValue = this.state.name;
        }

        if (this.priceOldValue != this.state.price) {
            priceLabel.setText(String.format("R$ %.2f", this.state.price));
            this.priceOldValue = this.state.price;
        }

        if (this.quantityOldValue != this.state.quantity) {
            qtyLabel.setText(String.valueOf(this.state.quantity));
            subtotalLabel.setText(String.format("= R$ %.2f", this.state.price * this.state.quantity));
            this.quantityOldValue = this.state.quantity;
        }
    }

    @SuppressWarnings("unused")
    private void initialRender(UIKitDom dom, UIView root) {
        dom.label(230, 22, name -> {
            this.nameLabel = name;
            name.setFrame(new CGRect(16, 10, 230, 22));
            name.setText(this.state.name);
            name.setFont(UIFont.getSystemFont(17));
            this.nameOldValue = this.state.name;
        });

        dom.label(120, 18, price -> {
            this.priceLabel = price;
            price.setFrame(new CGRect(16, 34, 120, 18));
            price.setText(String.format("R$ %.2f", this.state.price));
            price.setFont(UIFont.getSystemFont(15));
            price.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
            this.priceOldValue = this.state.price;
        });

        // Remove button
        dom.button(80, 30, removeBtn -> {
            removeBtn.setFrame(new CGRect(253, 10, 80, 30));
            removeBtn.setTitle("Remover", UIControlState.Normal);
            removeBtn.setTitleColor(UIColor.fromRGBA(1.0, 0.23, 0.19, 1.0), UIControlState.Normal);
            removeBtn.getTitleLabel().setFont(UIFont.getSystemFont(15));
            removeBtn.addOnTouchUpInsideListener((c, e) ->
                    safeAction("remove", () -> presenter.onRemoveProduct(this.state.id)));
        });

        // Stepper background
        dom.absolute(130, 28, stepperBg -> {
            stepperBg.setFrame(new CGRect(16, 56, 130, 28));
            stepperBg.setBackgroundColor(UIColor.fromRGBA(0.95, 0.95, 0.97, 1.0));
            stepperBg.getLayer().setCornerRadius(7);
        });

        // Minus button
        dom.button(40, 28, minusBtn -> {
            minusBtn.setFrame(new CGRect(16, 56, 40, 28));
            minusBtn.setTitle("\u2212", UIControlState.Normal);
            minusBtn.getTitleLabel().setFont(UIFont.getSystemFont(20));
            minusBtn.setTitleColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0), UIControlState.Normal);
            minusBtn.addOnTouchUpInsideListener((c, e) -> {
                if (this.state.quantity > 1) {
                    safeAction("qty-minus", () -> presenter.onModifyQuantity(this.state.id, this.state.quantity - 1));
                }
            });
        });

        // Quantity label
        dom.label(50, 28, qty -> {
            this.qtyLabel = qty;
            qty.setFrame(new CGRect(56, 56, 50, 28));
            qty.setText(String.valueOf(this.state.quantity));
            qty.setFont(UIFont.getBoldSystemFont(17));
            qty.setTextAlignment(NSTextAlignment.Center);
            this.quantityOldValue = this.state.quantity;
        });

        // Plus button
        dom.button(40, 28, plusBtn -> {
            plusBtn.setFrame(new CGRect(106, 56, 40, 28));
            plusBtn.setTitle("+", UIControlState.Normal);
            plusBtn.getTitleLabel().setFont(UIFont.getSystemFont(20));
            plusBtn.setTitleColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0), UIControlState.Normal);
            plusBtn.addOnTouchUpInsideListener((c, e) ->
                    safeAction("qty-plus", () -> presenter.onModifyQuantity(this.state.id, this.state.quantity + 1)));
        });

        // Subtotal
        dom.label(173, 24, subtotal -> {
            this.subtotalLabel = subtotal;
            subtotal.setFrame(new CGRect(160, 58, 173, 24));
            subtotal.setText(String.format("= R$ %.2f", this.state.price * this.state.quantity));
            subtotal.setFont(UIFont.getSystemFont(15));
            subtotal.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
            subtotal.setTextAlignment(NSTextAlignment.Right);
        });

        // Separator
        dom.separator(311, 0.5).setFrame(new CGRect(16, 87.5, 311, 0.5));
    }
}
