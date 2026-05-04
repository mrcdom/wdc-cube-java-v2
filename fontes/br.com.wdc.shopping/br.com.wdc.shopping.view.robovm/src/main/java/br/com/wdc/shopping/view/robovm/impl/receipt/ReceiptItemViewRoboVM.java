package br.com.wdc.shopping.view.robovm.impl.receipt;

import java.util.Objects;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.NSTextAlignment;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class ReceiptItemViewRoboVM extends AbstractViewRoboVM<ReceiptPresenter> {

    private ReceiptItem state;

    private boolean notRendered = true;
    private UILabel descriptionLabel;
    private String descriptionOldValue;
    private UILabel priceLabel;
    private double priceOldValue;
    private UILabel quantityLabel;
    private int quantityOldValue;

    public ReceiptItemViewRoboVM(ShoppingRoboVMApplication app, ReceiptPresenter presenter, int idx) {
        super("receipt-item-" + idx, app, presenter);
    }

    public void setState(ReceiptItem state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.descriptionLabel = null;
        this.descriptionOldValue = null;
        this.priceLabel = null;
        this.priceOldValue = 0;
        this.quantityLabel = null;
        this.quantityOldValue = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.rootView = new UIView(new CGRect(0, 0, 343, 44));
            UIKitDom.render(this.rootView, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.descriptionOldValue, this.state.description)) {
            descriptionLabel.setText(this.state.description);
            this.descriptionOldValue = this.state.description;
        }

        if (this.priceOldValue != this.state.value) {
            double subtotal = this.state.value * this.state.quantity;
            priceLabel.setText(String.format("%d × R$ %.2f", this.state.quantity, this.state.value));
            this.priceOldValue = this.state.value;
        }

        if (this.quantityOldValue != this.state.quantity) {
            quantityLabel.setText(String.format("R$ %.2f", this.state.value * this.state.quantity));
            this.quantityOldValue = this.state.quantity;
        }
    }

    @SuppressWarnings("unused")
    private void initialRender(UIKitDom dom, UIView root) {
        dom.label(180, 20, desc -> {
            this.descriptionLabel = desc;
            desc.setFrame(new CGRect(16, 6, 180, 20));
            desc.setText(this.state.description);
            desc.setFont(UIFont.getSystemFont(17));
            this.descriptionOldValue = this.state.description;
        });

        dom.label(127, 20, price -> {
            this.priceLabel = price;
            price.setFrame(new CGRect(200, 6, 127, 20));
            price.setText(String.format("%d × R$ %.2f", this.state.quantity, this.state.value));
            price.setFont(UIFont.getSystemFont(15));
            price.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
            price.setTextAlignment(NSTextAlignment.Right);
            this.priceOldValue = this.state.value;
        });

        dom.label(127, 16, subtotal -> {
            this.quantityLabel = subtotal;
            subtotal.setFrame(new CGRect(200, 26, 127, 16));
            subtotal.setText(String.format("R$ %.2f", this.state.value * this.state.quantity));
            subtotal.setFont(UIFont.getSystemFont(13));
            subtotal.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
            subtotal.setTextAlignment(NSTextAlignment.Right);
            this.quantityOldValue = this.state.quantity;
        });

        // Separator
        dom.separator(311, 0.5).setFrame(new CGRect(16, 43.5, 311, 0.5));
    }
}
