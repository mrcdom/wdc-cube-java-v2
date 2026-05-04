package br.com.wdc.shopping.view.robovm.impl;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UIScrollView;
import org.robovm.apple.uikit.UIView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;
import br.com.wdc.shopping.view.robovm.impl.home.ProductItemViewRoboVM;
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class ProductsPanelViewRoboVM extends AbstractViewRoboVM<ProductsPanelPresenter> {

    private final ProductsPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<ProductItemViewRoboVM> itemViewList = new ArrayList<>();
    private BiConsumer<List<ProductInfo>, List<ProductItemViewRoboVM>> contentSlot;
    private UIScrollView scrollView;

    public ProductsPanelViewRoboVM(ShoppingRoboVMApplication app, ProductsPanelPresenter presenter) {
        super("products-panel", app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.itemIdx = 0;
        this.itemViewList.clear();
        this.contentSlot = null;
        this.scrollView = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.rootView = new UIView(new CGRect(0, 0, 375, 400));
            UIKitDom.render(this.rootView, this::initialRender);
            this.notRendered = false;
        }

        this.contentSlot.accept(this.state.products, this.itemViewList);

        // Update scroll content size based on item count
        double count = this.itemViewList.size();
        scrollView.setContentSize(new CGSize(343, count * 54));
    }

    private void initialRender(UIKitDom dom, UIView root) {
        root.setBackgroundColor(UIColor.clear());

        // Section header
        dom.label(340, 22, title -> {
            title.setFrame(new CGRect(20, 6, 340, 22));
            title.setText("PRODUTOS");
            title.setFont(UIFont.getBoldSystemFont(14));
            title.setTextColor(UIColor.white());
            title.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
            title.setShadowOffset(new CGSize(0, 1));
        });

        dom.scrollView(343, 366, UIKitDom.LayoutMode.VBOX, sv -> {
            this.scrollView = sv;
            sv.setFrame(new CGRect(16, 30, 343, 366));
            sv.getLayer().setCornerRadius(10);
            sv.setClipsToBounds(true);
            sv.setBackgroundColor(UIColor.white());

            this.contentSlot = this.newListSlot(sv, this::newItemView, this::updateItem);
        });
    }

    private ProductItemViewRoboVM newItemView() {
        return new ProductItemViewRoboVM(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ProductItemViewRoboVM itemView, ProductInfo item) {
        itemView.setState(item, false);
        itemView.doUpdate();
    }
}
