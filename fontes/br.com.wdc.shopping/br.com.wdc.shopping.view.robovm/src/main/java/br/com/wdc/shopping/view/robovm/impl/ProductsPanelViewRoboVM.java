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

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelViewState;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;

public class ProductsPanelViewRoboVM extends AbstractViewRoboVM<ProductsPanelPresenter> {

    private final ProductsPanelViewState state;
    private boolean built;

    private UIScrollView scrollView;

    public ProductsPanelViewRoboVM(ShoppingRoboVMApplication app, ProductsPanelPresenter presenter) {
        super("products-panel", app, presenter);
        this.state = presenter.state;
    }

    private void buildUI() {
        var container = new UIView(new CGRect(0, 0, 375, 400));
        container.setBackgroundColor(UIColor.clear());

        // Section header - white bold over doodle background
        var title = new UILabel(new CGRect(20, 6, 340, 22));
        title.setText("PRODUTOS");
        title.setFont(UIFont.getBoldSystemFont(14));
        title.setTextColor(UIColor.white());
        title.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
        title.setShadowOffset(new org.robovm.apple.coregraphics.CGSize(0, 1));
        container.addSubview(title);

        scrollView = new UIScrollView(new CGRect(16, 30, 343, 366));
        scrollView.getLayer().setCornerRadius(10);
        scrollView.setClipsToBounds(true);
        scrollView.setBackgroundColor(UIColor.white());
        container.addSubview(scrollView);

        this.rootView = container;
        this.built = true;
    }

    @Override
    public void doUpdate() {
        if (!built) {
            buildUI();
        }

        // Clear and rebuild product list
        for (var sub : scrollView.getSubviews()) {
            sub.removeFromSuperview();
        }

        if (state.products != null) {
            int yOffset = 0;
            int count = state.products.size();
            for (int i = 0; i < count; i++) {
                var product = state.products.get(i);

                // Entire row is a tappable button
                var rowButton = new UIButton(UIButtonType.Custom);
                rowButton.setFrame(new CGRect(0, yOffset, 343, 54));

                var nameLabel = new UILabel(new CGRect(16, 8, 260, 22));
                nameLabel.setText(product.name);
                nameLabel.setFont(UIFont.getSystemFont(17));
                nameLabel.setUserInteractionEnabled(false);
                rowButton.addSubview(nameLabel);

                var priceLabel = new UILabel(new CGRect(16, 30, 150, 18));
                priceLabel.setText(String.format("R$ %.2f", product.price));
                priceLabel.setFont(UIFont.getSystemFont(15));
                priceLabel.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
                priceLabel.setUserInteractionEnabled(false);
                rowButton.addSubview(priceLabel);

                // Chevron disclosure indicator
                var chevron = new UILabel(new CGRect(315, 14, 20, 26));
                chevron.setText("›");
                chevron.setFont(UIFont.getSystemFont(22));
                chevron.setTextColor(UIColor.fromRGBA(0.78, 0.78, 0.80, 1.0));
                chevron.setUserInteractionEnabled(false);
                rowButton.addSubview(chevron);

                final var productId = product.id;
                rowButton.addOnTouchUpInsideListener((c, e) ->
                        safeAction("openProduct", () -> presenter.onOpenProduct(productId)));

                // Inset separator (not on last item)
                if (i < count - 1) {
                    var separator = new UIView(new CGRect(16, 53.5, 327, 0.5));
                    separator.setBackgroundColor(UIColor.fromRGBA(0.78, 0.78, 0.80, 1.0));
                    separator.setUserInteractionEnabled(false);
                    rowButton.addSubview(separator);
                }

                scrollView.addSubview(rowButton);
                yOffset += 54;
            }
            scrollView.setContentSize(new org.robovm.apple.coregraphics.CGSize(343, yOffset));
        }
    }
}
