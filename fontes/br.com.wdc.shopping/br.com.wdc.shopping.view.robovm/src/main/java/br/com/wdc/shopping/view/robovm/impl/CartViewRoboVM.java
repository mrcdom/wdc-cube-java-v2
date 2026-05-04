package br.com.wdc.shopping.view.robovm.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlContentHorizontalAlignment;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIScrollView;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.NSTextAlignment;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;
import br.com.wdc.shopping.view.robovm.impl.cart.CartItemViewRoboVM;
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class CartViewRoboVM extends AbstractViewRoboVM<CartPresenter> {

    private final CartViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<CartItemViewRoboVM> cartItemViewList = new ArrayList<>();
    private BiConsumer<List<CartItem>, List<CartItemViewRoboVM>> itemsSlot;
    private UIScrollView scrollView;
    private UILabel totalLabel;
    private double totalOldValue;
    private UILabel errorLabel;
    private UIView emptyStateView;
    private UIButton buyButton;
    private boolean emptyOldValue = false;

    public CartViewRoboVM(ShoppingRoboVMApplication app, CartPresenter presenter) {
        super("cart", app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.itemIdx = 0;
        this.cartItemViewList.clear();
        this.itemsSlot = null;
        this.scrollView = null;
        this.totalLabel = null;
        this.totalOldValue = 0;
        this.errorLabel = null;
        this.emptyStateView = null;
        this.buyButton = null;
        this.emptyOldValue = false;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.rootView = new UIView(new CGRect(0, 0, 375, 600));
            UIKitDom.render(this.rootView, this::initialRender);
            this.notRendered = false;
        }

        // Sync error
        if (state.errorCode != 0) {
            errorLabel.setText(state.errorMessage);
            errorLabel.setHidden(false);
            state.errorCode = 0;
            state.errorMessage = null;
        }

        boolean isEmpty = state.items == null || state.items.isEmpty();

        if (this.emptyOldValue != isEmpty) {
            emptyStateView.setHidden(!isEmpty);
            scrollView.setHidden(isEmpty);
            totalLabel.setHidden(isEmpty);
            buyButton.setHidden(isEmpty);
            this.emptyOldValue = isEmpty;
        }

        // Sync items via slot
        this.itemsSlot.accept(this.state.items, this.cartItemViewList);

        // Update scroll content size
        int count = this.cartItemViewList.size();
        scrollView.setContentSize(new CGSize(343, count * 88));

        // Update total
        double totalValue = computeTotalCost();
        if (totalValue != this.totalOldValue) {
            totalLabel.setText(String.format("Total: R$ %.2f", totalValue));
            this.totalOldValue = totalValue;
        }
    }

    @SuppressWarnings("unused")
	private void initialRender(UIKitDom dom, UIView root) {
        root.setBackgroundColor(UIColor.clear());

        // Back link
        dom.button(160, 44, backLink -> {
            backLink.setFrame(new CGRect(4, 4, 160, 44));
            backLink.setTitle("\u2039 Produtos", UIControlState.Normal);
            backLink.setTitleColor(UIColor.white(), UIControlState.Normal);
            backLink.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
            backLink.setContentHorizontalAlignment(UIControlContentHorizontalAlignment.Left);
            backLink.addOnTouchUpInsideListener((c, e) -> safeAction("products", presenter::onOpenProducts));
        });

        // Title
        dom.label(343, 32, title -> {
            title.setFrame(new CGRect(16, 52, 343, 32));
            title.setText("Carrinho");
            title.setFont(UIFont.getBoldSystemFont(28));
            title.setTextColor(UIColor.white());
            title.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
            title.setShadowOffset(new CGSize(0, 1));
        });

        // Error label
        dom.label(343, 20, error -> {
            this.errorLabel = error;
            error.setFrame(new CGRect(16, 88, 343, 20));
            error.setTextColor(UIColor.fromRGBA(1.0, 0.23, 0.19, 1.0));
            error.setFont(UIFont.getSystemFont(14));
            error.setHidden(true);
        });

        // Items scroll view
        dom.scrollView(343, 340, UIKitDom.LayoutMode.VBOX, sv -> {
            this.scrollView = sv;
            sv.setFrame(new CGRect(16, 116, 343, 340));
            sv.getLayer().setCornerRadius(10);
            sv.setClipsToBounds(true);
            sv.setBackgroundColor(UIColor.white());

            this.itemsSlot = this.newListSlot(sv, this::newItemView, this::updateItem);
        });

        // Empty state
        dom.absolute(343, 340, empty -> {
            this.emptyStateView = empty;
            empty.setFrame(new CGRect(16, 116, 343, 340));
            empty.setBackgroundColor(UIColor.white());
            empty.getLayer().setCornerRadius(10);
            empty.setHidden(true);

            dom.label(343, 60, icon -> {
                icon.setFrame(new CGRect(0, 60, 343, 60));
                icon.setText("\uD83D\uDED2");
                icon.setFont(UIFont.getSystemFont(50));
                icon.setTextAlignment(NSTextAlignment.Center);
            });

            dom.label(311, 28, t -> {
                t.setFrame(new CGRect(16, 130, 311, 28));
                t.setText("Seu carrinho est\u00E1 vazio");
                t.setFont(UIFont.getBoldSystemFont(20));
                t.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
                t.setTextAlignment(NSTextAlignment.Center);
            });

            dom.label(311, 22, s -> {
                s.setFrame(new CGRect(16, 164, 311, 22));
                s.setText("Vamos \u00E0s compras!?");
                s.setFont(UIFont.getSystemFont(16));
                s.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
                s.setTextAlignment(NSTextAlignment.Center);
            });

            dom.button(200, 44, browseBtn -> {
                browseBtn.setFrame(new CGRect(71, 210, 200, 44));
                browseBtn.setTitle("Ver Produtos", UIControlState.Normal);
                browseBtn.setTitleColor(UIColor.white(), UIControlState.Normal);
                browseBtn.getTitleLabel().setFont(UIFont.getBoldSystemFont(16));
                browseBtn.setBackgroundColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0));
                browseBtn.getLayer().setCornerRadius(10);
                browseBtn.addOnTouchUpInsideListener((c, e) -> safeAction("products", presenter::onOpenProducts));
            });
        });

        // Total label
        dom.label(343, 30, total -> {
            this.totalLabel = total;
            total.setFrame(new CGRect(16, 466, 343, 30));
            total.setFont(UIFont.getBoldSystemFont(20));
            total.setTextColor(UIColor.white());
            total.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
            total.setShadowOffset(new CGSize(0, 1));
            total.setTextAlignment(NSTextAlignment.Right);
        });

        // Buy button
        dom.button(343, 50, buy -> {
            this.buyButton = buy;
            buy.setFrame(new CGRect(16, 506, 343, 50));
            buy.setTitle("Comprar", UIControlState.Normal);
            buy.setTitleColor(UIColor.white(), UIControlState.Normal);
            buy.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
            buy.setBackgroundColor(UIColor.fromRGBA(0.20, 0.78, 0.35, 1.0));
            buy.getLayer().setCornerRadius(10);
            buy.addOnTouchUpInsideListener((c, e) -> safeAction("buy", () -> presenter.onBuy()));
        });
    }

    private double computeTotalCost() {
        if (state.items == null) return 0;
        double total = 0;
        for (var item : state.items) {
            total += item.price * item.quantity;
        }
        return total;
    }

    private CartItemViewRoboVM newItemView() {
        return new CartItemViewRoboVM(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(CartItemViewRoboVM itemView, CartItem item) {
        itemView.setState(item, false);
        itemView.doUpdate();
    }
}
