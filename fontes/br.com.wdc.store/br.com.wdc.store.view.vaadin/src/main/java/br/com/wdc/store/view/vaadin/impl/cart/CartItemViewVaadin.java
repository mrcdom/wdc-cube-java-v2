package br.com.wdc.store.view.vaadin.impl.cart;

import java.util.Objects;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope.CartItemScope;
import br.com.wdc.store.view.vaadin.AbstractViewVaadin;
import br.com.wdc.store.view.vaadin.StoreVaadinApplication;
import br.com.wdc.store.view.vaadin.util.I18n;
import br.com.wdc.store.view.vaadin.util.ResourceCatalog;
import br.com.wdc.store.view.vaadin.util.VaadinDom;

/** The view of a cart row: a segment of the cart with its own scope and actions. */
public class CartItemViewVaadin extends AbstractViewVaadin<CartItemScope> {

    private boolean notRendered = true;
    private Span nameElm;
    private String nameOldValue;
    private Span priceElm;
    private double priceOldValue;
    private Span quantityElm;
    private int quantityOldValue;

    public CartItemViewVaadin(StoreVaadinApplication app, CartItemScope row) {
        super("cart-item-" + row.id.get(), app, row, new HorizontalLayout());
    }

    /** A reloaded page has a new UI: the row draws itself again with components of its own. */
    @Override
    public void recreate() {
        this.element = new HorizontalLayout();
        this.notRendered = true;
        this.nameElm = null;
        this.nameOldValue = null;
        this.priceElm = null;
        this.priceOldValue = 0;
        this.quantityElm = null;
        this.quantityOldValue = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((HorizontalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.nameOldValue, this.scope.name.get())) {
            this.nameElm.setText(this.scope.name.get());
            this.nameOldValue = this.scope.name.get();
        }

        if (this.priceOldValue != this.scope.price.get().doubleValue()) {
            this.priceElm.setText(I18n.price(this.scope.price.get()));
            this.priceOldValue = this.scope.price.get();
        }

        if (this.quantityOldValue != this.scope.quantity.get().intValue()) {
            this.quantityElm.setText(String.valueOf(this.scope.quantity.get()));
            this.quantityOldValue = this.scope.quantity.get();
        }
    }

    private void initialRender(VaadinDom dom, HorizontalLayout pane0) {
        pane0.addClassName("cart-row");
        pane0.setWidthFull();
        pane0.setAlignItems(FlexComponent.Alignment.CENTER);

        dom.horizontalLayout(pane1 -> {
            pane1.addClassName("cell-1");
            pane1.setAlignItems(FlexComponent.Alignment.CENTER);

            dom.image(img -> {
                img.setWidth("42px");
                img.setHeight("40px");
                img.setSrc(ResourceCatalog.getImageResource(this.scope.image.get()));
            });

            dom.span(label -> {
                this.nameElm = label;
                this.nameElm.setText(this.scope.name.get());
                this.nameOldValue = this.scope.name.get();
            });
        });

        dom.span(label -> {
            label.addClassName("cell-2");
            this.priceElm = label;
            this.priceElm.setText(I18n.price(this.scope.price.get()));
            this.priceOldValue = this.scope.price.get();
        });

        dom.horizontalLayout(pane1 -> {
            pane1.addClassName("cell-3");
            pane1.setAlignItems(FlexComponent.Alignment.CENTER);

            dom.span(label -> {
                this.quantityElm = label;
                this.quantityElm.setText(String.valueOf(this.scope.quantity.get()));
                this.quantityOldValue = this.scope.quantity.get();
            });

            dom.image(img -> {
                img.setSrc("images/delete.png");
                img.setAlt(text(StoreMessages.VAADIN_REMOVE));
                img.setWidth("16px");
                img.setHeight("16px");
                img.getStyle().set("cursor", "pointer");
                img.addClickListener(e -> safeAction("Remove product", this.scope.onRemove::run));
            });
        });
    }
}
