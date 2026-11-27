package br.com.wdc.store.view.vaadin.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope.ProductsPanelScope;
import br.com.wdc.store.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.store.view.vaadin.AbstractViewVaadin;
import br.com.wdc.store.view.vaadin.StoreVaadinApplication;
import br.com.wdc.store.view.vaadin.impl.home.ProductItemViewVaadin;
import br.com.wdc.store.view.vaadin.util.VaadinDom;

public class ProductsPanelViewVaadin extends AbstractViewVaadin<ProductsPanelScope> {

    private boolean notRendered = true;
    private int itemIdx;
    private List<ProductItemViewVaadin> itemViewList = new ArrayList<>();
    private BiConsumer<List<ProductInfo>, List<ProductItemViewVaadin>> contentSlot;

    public ProductsPanelViewVaadin(StoreVaadinApplication app, ProductsPanelScope scope) {
        super("products-panel", app, scope, new VerticalLayout());
    }

    @Override
    public void recreate() {
        this.element = new VerticalLayout();
        this.notRendered = true;
        this.itemViewList.clear();
        this.contentSlot = null;
        this.itemIdx = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }
        this.contentSlot.accept(this.scope.products.get(), this.itemViewList);
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("products-panel");

        dom.h3(h -> {
            h.setText(text(StoreMessages.HOME_PRODUCTS));
            h.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin", "0 0 12px 0");
        });

        dom.flexLayout(pane1 -> {
            pane1.addClassName("products-grid");
            pane1.getStyle().set("gap", "12px");
            this.contentSlot = this.newListSlot(pane1, this::newItemView, this::updateItem);
        });
    }

    private ProductItemViewVaadin newItemView() {
        return new ProductItemViewVaadin(this.app, this.scope, this.itemIdx++);
    }

    private void updateItem(ProductItemViewVaadin itemView, ProductInfo item) {
        itemView.setItem(item, false);
        itemView.doUpdate();
    }
}
