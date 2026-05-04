package br.com.wdc.shopping.view.robovm.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIScrollView;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.NSTextAlignment;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;
import br.com.wdc.shopping.view.robovm.impl.receipt.ReceiptItemViewRoboVM;
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class ReceiptViewRoboVM extends AbstractViewRoboVM<ReceiptPresenter> {

    private final ReceiptViewState state;

    private boolean notRendered = true;
    private UILabel successLabel;
    private UIScrollView itemsContainer;
    private UILabel totalLabel;
    private double totalOldValue;
    private int itemIdx;
    private List<ReceiptItemViewRoboVM> receiptItemViewList = new ArrayList<>();
    private BiConsumer<List<ReceiptItem>, List<ReceiptItemViewRoboVM>> itemsSlot;

    public ReceiptViewRoboVM(ShoppingRoboVMApplication app, ReceiptPresenter presenter) {
        super("receipt", app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.successLabel = null;
        this.itemsContainer = null;
        this.totalLabel = null;
        this.totalOldValue = 0;
        this.itemIdx = 0;
        this.receiptItemViewList.clear();
        this.itemsSlot = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.rootView = new UIView(new CGRect(0, 0, 375, 600));
            UIKitDom.render(this.rootView, this::initialRender);
            this.notRendered = false;
        }

        // Success notification
        if (state.notifySuccess) {
            successLabel.setHidden(false);
            state.notifySuccess = false;
        } else {
            successLabel.setHidden(true);
        }

        // Sync items via slot
        var receipt = state.receipt;
        if (receipt != null && receipt.items != null) {
            this.itemsSlot.accept(receipt.items, this.receiptItemViewList);
            int count = this.receiptItemViewList.size();
            itemsContainer.setContentSize(new CGSize(343, count * 44));
        }

        // Update total
        if (receipt != null && receipt.total != this.totalOldValue) {
            totalLabel.setText(String.format("Total: R$ %.2f", receipt.total));
            this.totalOldValue = receipt.total;
        }
    }

    @SuppressWarnings("unused")
	private void initialRender(UIKitDom dom, UIView root) {
        root.setBackgroundColor(UIColor.clear());

        // Back link
        dom.button(160, 44, backLink -> {
            backLink.setFrame(new CGRect(4, 4, 160, 44));
            backLink.setTitle("‹ Produtos", UIControlState.Normal);
            backLink.setTitleColor(UIColor.white(), UIControlState.Normal);
            backLink.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
            backLink.setContentHorizontalAlignment(org.robovm.apple.uikit.UIControlContentHorizontalAlignment.Left);
            backLink.addOnTouchUpInsideListener((c, e) ->
                    safeAction("products", presenter::onOpenProducts));
        });

        // Title
        dom.label(343, 32, title -> {
            title.setFrame(new CGRect(16, 52, 343, 32));
            title.setText("Recibo");
            title.setFont(UIFont.getBoldSystemFont(28));
            title.setTextColor(UIColor.white());
            title.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
            title.setShadowOffset(new CGSize(0, 1));
        });

        // Success banner
        dom.label(343, 36, success -> {
            this.successLabel = success;
            success.setFrame(new CGRect(16, 92, 343, 36));
            success.setText("✓ Compra realizada com sucesso!");
            success.setTextColor(UIColor.fromRGBA(0.20, 0.78, 0.35, 1.0));
            success.setFont(UIFont.getBoldSystemFont(15));
            success.setTextAlignment(NSTextAlignment.Center);
            success.setBackgroundColor(UIColor.white());
            success.getLayer().setCornerRadius(10);
            success.setClipsToBounds(true);
            success.setHidden(true);
        });

        // Items card
        dom.scrollView(343, 310, UIKitDom.LayoutMode.VBOX, sv -> {
            this.itemsContainer = sv;
            sv.setFrame(new CGRect(16, 140, 343, 310));
            sv.getLayer().setCornerRadius(10);
            sv.setClipsToBounds(true);
            sv.setBackgroundColor(UIColor.white());

            this.itemsSlot = this.newListSlot(sv, this::newItemView, this::updateItem);
        });

        // Total
        dom.label(343, 30, total -> {
            this.totalLabel = total;
            total.setFrame(new CGRect(16, 460, 343, 30));
            total.setFont(UIFont.getBoldSystemFont(22));
            total.setTextColor(UIColor.white());
            total.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
            total.setShadowOffset(new CGSize(0, 1));
            total.setTextAlignment(NSTextAlignment.Right);
        });
    }

    private ReceiptItemViewRoboVM newItemView() {
        return new ReceiptItemViewRoboVM(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ReceiptItemViewRoboVM itemView, ReceiptItem item) {
        itemView.setState(item, false);
        itemView.doUpdate();
    }
}
