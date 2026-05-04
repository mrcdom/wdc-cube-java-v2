package br.com.wdc.shopping.view.robovm.impl;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIButtonType;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIScrollView;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.NSTextAlignment;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptViewState;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;

public class ReceiptViewRoboVM extends AbstractViewRoboVM<ReceiptPresenter> {

    private final ReceiptViewState state;
    private boolean built;

    private UILabel successLabel;
    private UIScrollView itemsContainer;
    private UILabel totalLabel;

    public ReceiptViewRoboVM(ShoppingRoboVMApplication app, ReceiptPresenter presenter) {
        super("receipt", app, presenter);
        this.state = presenter.state;
    }

    private void buildUI() {
        var container = new UIView(new CGRect(0, 0, 375, 600));
        container.setBackgroundColor(UIColor.clear());

        // Back link - white for doodle background
        var backLink = new UIButton(UIButtonType.System);
        backLink.setFrame(new CGRect(4, 4, 160, 44));
        backLink.setTitle("‹ Produtos", UIControlState.Normal);
        backLink.setTitleColor(UIColor.white(), UIControlState.Normal);
        backLink.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
        backLink.setContentHorizontalAlignment(org.robovm.apple.uikit.UIControlContentHorizontalAlignment.Left);
        backLink.addOnTouchUpInsideListener((c, e) ->
                safeAction("products", () -> presenter.onOpenProducts()));
        container.addSubview(backLink);

        // Title - white for doodle background
        var title = new UILabel(new CGRect(16, 52, 343, 32));
        title.setText("Recibo");
        title.setFont(UIFont.getBoldSystemFont(28));
        title.setTextColor(UIColor.white());
        title.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
        title.setShadowOffset(new org.robovm.apple.coregraphics.CGSize(0, 1));
        container.addSubview(title);

        // Success banner - white card with green text for readability on doodle bg
        successLabel = new UILabel(new CGRect(16, 92, 343, 36));
        successLabel.setText("✓ Compra realizada com sucesso!");
        successLabel.setTextColor(UIColor.fromRGBA(0.20, 0.78, 0.35, 1.0));
        successLabel.setFont(UIFont.getBoldSystemFont(15));
        successLabel.setTextAlignment(NSTextAlignment.Center);
        successLabel.setBackgroundColor(UIColor.white());
        successLabel.getLayer().setCornerRadius(10);
        successLabel.setClipsToBounds(true);
        successLabel.setHidden(true);
        container.addSubview(successLabel);

        // Items card
        itemsContainer = new UIScrollView(new CGRect(16, 140, 343, 310));
        itemsContainer.getLayer().setCornerRadius(10);
        itemsContainer.setClipsToBounds(true);
        itemsContainer.setBackgroundColor(UIColor.white());
        container.addSubview(itemsContainer);

        // Total
        totalLabel = new UILabel(new CGRect(16, 460, 343, 30));
        totalLabel.setFont(UIFont.getBoldSystemFont(22));
        totalLabel.setTextColor(UIColor.white());
        totalLabel.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
        totalLabel.setShadowOffset(new org.robovm.apple.coregraphics.CGSize(0, 1));
        totalLabel.setTextAlignment(NSTextAlignment.Right);
        container.addSubview(totalLabel);

        this.rootView = container;
        this.built = true;
    }

    @Override
    public void doUpdate() {
        if (!built) {
            buildUI();
        }

        // Success notification
        successLabel.setHidden(!state.notifySuccess);

        // Render receipt items
        for (var sub : itemsContainer.getSubviews()) {
            sub.removeFromSuperview();
        }

        var receipt = state.receipt;
        if (receipt != null) {
            int yOffset = 0;
            double total = 0;

            if (receipt.items != null) {
                int count = receipt.items.size();
                for (int i = 0; i < count; i++) {
                    var item = receipt.items.get(i);
                    var row = new UIView(new CGRect(0, yOffset, 343, 44));

                    var name = new UILabel(new CGRect(16, 6, 180, 20));
                    name.setText(item.description);
                    name.setFont(UIFont.getSystemFont(17));
                    row.addSubview(name);

                    double subtotal = item.value * item.quantity;
                    var price = new UILabel(new CGRect(200, 6, 127, 20));
                    price.setText(String.format("%d × R$ %.2f", item.quantity, item.value));
                    price.setFont(UIFont.getSystemFont(15));
                    price.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
                    price.setTextAlignment(NSTextAlignment.Right);
                    row.addSubview(price);

                    var subtotalLabel = new UILabel(new CGRect(200, 26, 127, 16));
                    subtotalLabel.setText(String.format("R$ %.2f", subtotal));
                    subtotalLabel.setFont(UIFont.getSystemFont(13));
                    subtotalLabel.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
                    subtotalLabel.setTextAlignment(NSTextAlignment.Right);
                    row.addSubview(subtotalLabel);

                    // Inset separator
                    if (i < count - 1) {
                        var separator = new UIView(new CGRect(16, 43.5, 311, 0.5));
                        separator.setBackgroundColor(UIColor.fromRGBA(0.78, 0.78, 0.80, 1.0));
                        row.addSubview(separator);
                    }

                    itemsContainer.addSubview(row);
                    yOffset += 44;
                    total += subtotal;
                }
            }

            itemsContainer.setContentSize(new org.robovm.apple.coregraphics.CGSize(343, yOffset));
            totalLabel.setText(String.format("Total: R$ %.2f", total));
        }
    }
}
