package br.com.wdc.shopping.view.vaadin.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.impl.home.ProductItemViewVaadin;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class ProductsPanelViewVaadin extends AbstractViewVaadin<ProductsPanelPresenter> {

    private final ProductsPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<ProductItemViewVaadin> itemViewList = new ArrayList<>();
    private BiConsumer<List<ProductInfo>, List<ProductItemViewVaadin>> contentSlot;

    public ProductsPanelViewVaadin(ShoppingVaadinApplication app, ProductsPanelPresenter presenter) {
        super("products-panel", app, presenter, new VerticalLayout());
        this.state = presenter.state;
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
        this.contentSlot.accept(this.state.products, this.itemViewList);
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("products-panel");

        dom.h3(h -> {
            h.setText("PRODUTOS");
            h.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin", "0 0 12px 0");
        });

        dom.flexLayout(pane1 -> {
            pane1.addClassName("products-grid");
            pane1.getStyle().set("gap", "12px");
            this.contentSlot = this.newListSlot(pane1, this::newItemView, this::updateItem);
        });
    }

    private ProductItemViewVaadin newItemView() {
        return new ProductItemViewVaadin(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ProductItemViewVaadin itemView, ProductInfo state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }
}
