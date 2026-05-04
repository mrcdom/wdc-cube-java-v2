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

import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartViewState;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;

public class CartViewRoboVM extends AbstractViewRoboVM<CartPresenter> {

    private final CartViewState state;
    private boolean built;

    private UIScrollView scrollView;
    private UILabel totalLabel;
    private UILabel errorLabel;
    private UIView emptyStateView;
    private UIButton buyButton;

    public CartViewRoboVM(ShoppingRoboVMApplication app, CartPresenter presenter) {
        super("cart", app, presenter);
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
        backLink.addOnTouchUpInsideListener((c, e) -> safeAction("products", () -> presenter.onOpenProducts()));
        container.addSubview(backLink);

        // Title - white for doodle background
        var title = new UILabel(new CGRect(16, 52, 343, 32));
        title.setText("Carrinho");
        title.setFont(UIFont.getBoldSystemFont(28));
        title.setTextColor(UIColor.white());
        title.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
        title.setShadowOffset(new org.robovm.apple.coregraphics.CGSize(0, 1));
        container.addSubview(title);

        // Error label
        errorLabel = new UILabel(new CGRect(16, 88, 343, 20));
        errorLabel.setTextColor(UIColor.fromRGBA(1.0, 0.23, 0.19, 1.0));
        errorLabel.setFont(UIFont.getSystemFont(14));
        errorLabel.setHidden(true);
        container.addSubview(errorLabel);

        // Scroll view for items (white card)
        scrollView = new UIScrollView(new CGRect(16, 116, 343, 340));
        scrollView.getLayer().setCornerRadius(10);
        scrollView.setClipsToBounds(true);
        scrollView.setBackgroundColor(UIColor.white());
        container.addSubview(scrollView);

        // Empty state - shown when cart is empty
        emptyStateView = new UIView(new CGRect(16, 116, 343, 340));
        emptyStateView.setBackgroundColor(UIColor.white());
        emptyStateView.getLayer().setCornerRadius(10);
        emptyStateView.setHidden(true);

        var emptyIcon = new UILabel(new CGRect(0, 60, 343, 60));
        emptyIcon.setText("\uD83D\uDED2");
        emptyIcon.setFont(UIFont.getSystemFont(50));
        emptyIcon.setTextAlignment(NSTextAlignment.Center);
        emptyStateView.addSubview(emptyIcon);

        var emptyTitle = new UILabel(new CGRect(16, 130, 311, 28));
        emptyTitle.setText("Seu carrinho est\u00E1 vazio");
        emptyTitle.setFont(UIFont.getBoldSystemFont(20));
        emptyTitle.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
        emptyTitle.setTextAlignment(NSTextAlignment.Center);
        emptyStateView.addSubview(emptyTitle);

        var emptySubtitle = new UILabel(new CGRect(16, 164, 311, 22));
        emptySubtitle.setText("Vamos \u00E0s compras!?");
        emptySubtitle.setFont(UIFont.getSystemFont(16));
        emptySubtitle.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
        emptySubtitle.setTextAlignment(NSTextAlignment.Center);
        emptyStateView.addSubview(emptySubtitle);

        var browseButton = new UIButton(UIButtonType.System);
        browseButton.setFrame(new CGRect(71, 210, 200, 44));
        browseButton.setTitle("Ver Produtos", UIControlState.Normal);
        browseButton.setTitleColor(UIColor.white(), UIControlState.Normal);
        browseButton.getTitleLabel().setFont(UIFont.getBoldSystemFont(16));
        browseButton.setBackgroundColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0));
        browseButton.getLayer().setCornerRadius(10);
        browseButton.addOnTouchUpInsideListener((c, e) -> safeAction("products", () -> presenter.onOpenProducts()));
        emptyStateView.addSubview(browseButton);

        container.addSubview(emptyStateView);

        // Total label
        totalLabel = new UILabel(new CGRect(16, 466, 343, 30));
        totalLabel.setFont(UIFont.getBoldSystemFont(20));
        totalLabel.setTextColor(UIColor.white());
        totalLabel.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
        totalLabel.setShadowOffset(new org.robovm.apple.coregraphics.CGSize(0, 1));
        totalLabel.setTextAlignment(NSTextAlignment.Right);
        container.addSubview(totalLabel);

        // Buy button - green iOS style
        buyButton = new UIButton(UIButtonType.System);
        buyButton.setFrame(new CGRect(16, 506, 343, 50));
        buyButton.setTitle("Comprar", UIControlState.Normal);
        buyButton.setTitleColor(UIColor.white(), UIControlState.Normal);
        buyButton.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
        buyButton.setBackgroundColor(UIColor.fromRGBA(0.20, 0.78, 0.35, 1.0));
        buyButton.getLayer().setCornerRadius(10);
        buyButton.addOnTouchUpInsideListener((c, e) -> safeAction("buy", () -> presenter.onBuy()));
        container.addSubview(buyButton);

        this.rootView = container;
        this.built = true;
    }

    @Override
    public void doUpdate() {
        if (!built) {
            buildUI();
        }

        // Sync error
        if (state.errorCode != 0) {
            errorLabel.setText(state.errorMessage);
            errorLabel.setHidden(false);
            state.errorCode = 0;
            state.errorMessage = null;
        }

        // Render cart items
        for (var sub : scrollView.getSubviews()) {
            sub.removeFromSuperview();
        }

        boolean isEmpty = state.items == null || state.items.isEmpty();
        emptyStateView.setHidden(!isEmpty);
        scrollView.setHidden(isEmpty);
        totalLabel.setHidden(isEmpty);
        buyButton.setHidden(isEmpty);

        double totalValue = 0;
        int yOffset = 0;

        if (state.items != null) {
            int count = state.items.size();
            for (int idx = 0; idx < count; idx++) {
                var item = state.items.get(idx);
                var itemView = createCartItemView(item, yOffset, idx < count - 1);
                scrollView.addSubview(itemView);
                yOffset += 88;
                totalValue += item.price * item.quantity;
            }
        }

        scrollView.setContentSize(new org.robovm.apple.coregraphics.CGSize(343, yOffset));
        totalLabel.setText(String.format("Total: R$ %.2f", totalValue));
    }

    private UIView createCartItemView(CartItem item, int yOffset, boolean showSeparator) {
        var row = new UIView(new CGRect(0, yOffset, 343, 88));

        var nameLabel = new UILabel(new CGRect(16, 10, 230, 22));
        nameLabel.setText(item.name);
        nameLabel.setFont(UIFont.getSystemFont(17));
        row.addSubview(nameLabel);

        var priceLabel = new UILabel(new CGRect(16, 34, 120, 18));
        priceLabel.setText(String.format("R$ %.2f", item.price));
        priceLabel.setFont(UIFont.getSystemFont(15));
        priceLabel.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
        row.addSubview(priceLabel);

        // Remove link - iOS text link style (right-aligned, top)
        var removeButton = new UIButton(UIButtonType.System);
        removeButton.setFrame(new CGRect(253, 10, 80, 30));
        removeButton.setTitle("Remover", UIControlState.Normal);
        removeButton.setTitleColor(UIColor.fromRGBA(1.0, 0.23, 0.19, 1.0), UIControlState.Normal);
        removeButton.getTitleLabel().setFont(UIFont.getSystemFont(15));
        removeButton.addOnTouchUpInsideListener((c, e) ->
                safeAction("remove", () -> presenter.onRemoveProduct(item.id)));
        row.addSubview(removeButton);

        // Quantity: iOS stepper-like (segmented control style)
        final var itemId = item.id;
        final var currentQty = item.quantity;

        var stepperBg = new UIView(new CGRect(16, 56, 130, 28));
        stepperBg.setBackgroundColor(UIColor.fromRGBA(0.95, 0.95, 0.97, 1.0));
        stepperBg.getLayer().setCornerRadius(7);
        row.addSubview(stepperBg);

        var minusButton = new UIButton(UIButtonType.System);
        minusButton.setFrame(new CGRect(16, 56, 40, 28));
        minusButton.setTitle("−", UIControlState.Normal);
        minusButton.getTitleLabel().setFont(UIFont.getSystemFont(20));
        minusButton.setTitleColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0), UIControlState.Normal);
        minusButton.addOnTouchUpInsideListener((c, e) -> {
            if (currentQty > 1) {
                safeAction("qty-minus", () -> presenter.onModifyQuantity(itemId, currentQty - 1));
            }
        });
        row.addSubview(minusButton);

        var qtyLabel = new UILabel(new CGRect(56, 56, 50, 28));
        qtyLabel.setText(String.valueOf(item.quantity));
        qtyLabel.setFont(UIFont.getBoldSystemFont(17));
        qtyLabel.setTextAlignment(NSTextAlignment.Center);
        row.addSubview(qtyLabel);

        var plusButton = new UIButton(UIButtonType.System);
        plusButton.setFrame(new CGRect(106, 56, 40, 28));
        plusButton.setTitle("+", UIControlState.Normal);
        plusButton.getTitleLabel().setFont(UIFont.getSystemFont(20));
        plusButton.setTitleColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0), UIControlState.Normal);
        plusButton.addOnTouchUpInsideListener((c, e) ->
                safeAction("qty-plus", () -> presenter.onModifyQuantity(itemId, currentQty + 1)));
        row.addSubview(plusButton);

        // Subtotal
        var subtotalLabel = new UILabel(new CGRect(160, 58, 173, 24));
        subtotalLabel.setText(String.format("= R$ %.2f", item.price * item.quantity));
        subtotalLabel.setFont(UIFont.getSystemFont(15));
        subtotalLabel.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
        subtotalLabel.setTextAlignment(NSTextAlignment.Right);
        row.addSubview(subtotalLabel);

        // Inset separator
        if (showSeparator) {
            var separator = new UIView(new CGRect(16, 87.5, 311, 0.5));
            separator.setBackgroundColor(UIColor.fromRGBA(0.78, 0.78, 0.80, 1.0));
            row.addSubview(separator);
        }

        return row;
    }
}
