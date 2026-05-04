package br.com.wdc.shopping.view.robovm.impl;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIScrollView;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.NSTextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;
import br.com.wdc.shopping.view.robovm.impl.home.PurchaseItemViewRoboVM;
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class PurchasesPanelViewRoboVM extends AbstractViewRoboVM<PurchasesPanelPresenter> {

    private final PurchasesPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<PurchaseItemViewRoboVM> itemViewList = new ArrayList<>();
    private BiConsumer<List<PurchaseInfo>, List<PurchaseItemViewRoboVM>> contentSlot;
    private UIScrollView scrollView;
    private UILabel pageLabel;
    private String pageLabelOldValue;

    public PurchasesPanelViewRoboVM(ShoppingRoboVMApplication app, PurchasesPanelPresenter presenter) {
        super("purchases-panel", app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.itemIdx = 0;
        this.itemViewList.clear();
        this.contentSlot = null;
        this.scrollView = null;
        this.pageLabel = null;
        this.pageLabelOldValue = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.rootView = new UIView(new CGRect(0, 0, 375, 200));
            UIKitDom.render(this.rootView, this::initialRender);
            this.notRendered = false;
        }

        this.contentSlot.accept(this.state.purchases, this.itemViewList);

        // Update scroll content size
        double count = this.itemViewList.size();
        scrollView.setContentSize(new CGSize(343, count * 44));

        // Update pagination label
        int totalPages = state.pageSize > 0 ? (state.totalCount + state.pageSize - 1) / state.pageSize : 0;
        var newPageText = String.format("P\u00e1gina %d de %d", state.page + 1, Math.max(1, totalPages));
        if (!Objects.equals(newPageText, this.pageLabelOldValue)) {
            pageLabel.setText(newPageText);
            this.pageLabelOldValue = newPageText;
        }
    }

    @SuppressWarnings("unused")
	private void initialRender(UIKitDom dom, UIView root) {
        root.setBackgroundColor(UIColor.clear());

        // Section header
        dom.label(340, 22, title -> {
            title.setFrame(new CGRect(20, 2, 340, 22));
            title.setText("COMPRAS RECENTES");
            title.setFont(UIFont.getBoldSystemFont(14));
            title.setTextColor(UIColor.white());
            title.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
            title.setShadowOffset(new CGSize(0, 1));
        });

        // White card for items
        dom.scrollView(343, 130, UIKitDom.LayoutMode.VBOX, sv -> {
            this.scrollView = sv;
            sv.setFrame(new CGRect(16, 26, 343, 130));
            sv.getLayer().setCornerRadius(10);
            sv.setClipsToBounds(true);
            sv.setBackgroundColor(UIColor.white());

            this.contentSlot = this.newListSlot(sv, this::newItemView, this::updateItem);
        });

        // Pagination buttons
        dom.button(70, 36, prevBtn -> {
            prevBtn.setFrame(new CGRect(16, 162, 70, 36));
            prevBtn.setTitle("‹ Ant", UIControlState.Normal);
            prevBtn.getTitleLabel().setFont(UIFont.getSystemFont(15));
            prevBtn.setTitleColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0), UIControlState.Normal);
            prevBtn.addOnTouchUpInsideListener((c, e) ->
                    safeAction("prevPage", () -> {
                        if (state.page > 0) {
                            presenter.onPageChange(state.page - 1);
                        }
                    }));
        });

        dom.label(155, 36, pl -> {
            this.pageLabel = pl;
            pl.setFrame(new CGRect(110, 162, 155, 36));
            pl.setTextAlignment(NSTextAlignment.Center);
            pl.setFont(UIFont.getSystemFont(13));
            pl.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
        });

        dom.button(70, 36, nextBtn -> {
            nextBtn.setFrame(new CGRect(290, 162, 70, 36));
            nextBtn.setTitle("Próx ›", UIControlState.Normal);
            nextBtn.getTitleLabel().setFont(UIFont.getSystemFont(15));
            nextBtn.setTitleColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0), UIControlState.Normal);
            nextBtn.addOnTouchUpInsideListener((c, e) ->
                    safeAction("nextPage", () -> {
                        int totalPages = (state.totalCount + state.pageSize - 1) / state.pageSize;
                        if (state.page < totalPages - 1) {
                            presenter.onPageChange(state.page + 1);
                        }
                    }));
        });
    }

    private PurchaseItemViewRoboVM newItemView() {
        return new PurchaseItemViewRoboVM(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(PurchaseItemViewRoboVM itemView, PurchaseInfo item) {
        itemView.setState(item, false);
        itemView.doUpdate();
    }
}
