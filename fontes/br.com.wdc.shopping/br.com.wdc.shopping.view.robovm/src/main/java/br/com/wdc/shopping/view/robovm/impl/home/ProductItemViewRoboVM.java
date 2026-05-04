package br.com.wdc.shopping.view.robovm.impl.home;

import java.util.Objects;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.UIButtonType;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIView;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class ProductItemViewRoboVM extends AbstractViewRoboVM<ProductsPanelPresenter> {

    private ProductInfo state;

    private boolean notRendered = true;
    private UILabel nameLabel;
    private String nameOldValue;
    private UILabel priceLabel;
    private double priceOldValue;

    public ProductItemViewRoboVM(ShoppingRoboVMApplication app, ProductsPanelPresenter presenter, int idx) {
        super("product-item-" + idx, app, presenter);
    }

    public void setState(ProductInfo state, boolean scheduleUpdate) {
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
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.rootView = new UIView(new CGRect(0, 0, 343, 54));
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
    }

    @SuppressWarnings("unused")
    private void initialRender(UIKitDom dom, UIView root) {
        // Wrap in a button for tap handling
        var rowButton = new org.robovm.apple.uikit.UIButton(UIButtonType.Custom);
        rowButton.setFrame(new CGRect(0, 0, 343, 54));

        dom.label(260, 22, name -> {
            this.nameLabel = name;
            name.setFrame(new CGRect(16, 8, 260, 22));
            name.setText(this.state.name);
            name.setFont(UIFont.getSystemFont(17));
            name.setUserInteractionEnabled(false);
            this.nameOldValue = this.state.name;
        });

        dom.label(150, 18, price -> {
            this.priceLabel = price;
            price.setFrame(new CGRect(16, 30, 150, 18));
            price.setText(String.format("R$ %.2f", this.state.price));
            price.setFont(UIFont.getSystemFont(15));
            price.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
            price.setUserInteractionEnabled(false);
            this.priceOldValue = this.state.price;
        });

        // Chevron
        dom.label(20, 26, chevron -> {
            chevron.setFrame(new CGRect(315, 14, 20, 26));
            chevron.setText("›");
            chevron.setFont(UIFont.getSystemFont(22));
            chevron.setTextColor(UIColor.fromRGBA(0.78, 0.78, 0.80, 1.0));
            chevron.setUserInteractionEnabled(false);
        });

        // Tap handler on root view
        final var productId = this.state.id;
        rowButton.addOnTouchUpInsideListener((c, e) ->
                safeAction("openProduct", () -> presenter.onOpenProduct(productId)));

        // Move all subviews into the button, then add button to root
        for (var sub : root.getSubviews()) {
            sub.removeFromSuperview();
            rowButton.addSubview(sub);
        }
        root.addSubview(rowButton);
    }
}
