package br.com.wdc.shopping.view.vaadin.impl.cart;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartScope.CartItemScope;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.ResourceCatalog;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

/** The view of a cart row: a segment of the cart with its own state and actions. */
public class CartItemViewVaadin extends AbstractViewVaadin<CartItemScope> {

    private boolean notRendered = true;
    private Span nameElm;
    private String nameOldValue;
    private Span priceElm;
    private double priceOldValue;
    private Span quantityElm;
    private int quantityOldValue;

    public CartItemViewVaadin(ShoppingVaadinApplication app, CartItemScope row) {
        super("cart-item-" + row.id.get(), app, row, new HorizontalLayout());
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((HorizontalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.nameOldValue, this.state.name.get())) {
            this.nameElm.setText(this.state.name.get());
            this.nameOldValue = this.state.name.get();
        }

        if (this.priceOldValue != this.state.price.get().doubleValue()) {
            this.priceElm.setText(NumberFormat.getCurrencyInstance(Locale.US).format(this.state.price.get()));
            this.priceOldValue = this.state.price.get();
        }

        if (this.quantityOldValue != this.state.quantity.get().intValue()) {
            this.quantityElm.setText(String.valueOf(this.state.quantity.get()));
            this.quantityOldValue = this.state.quantity.get();
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
                img.setSrc(ResourceCatalog.getImageResource(this.state.image.get()));
            });

            dom.span(label -> {
                this.nameElm = label;
                this.nameElm.setText(this.state.name.get());
                this.nameOldValue = this.state.name.get();
            });
        });

        dom.span(label -> {
            label.addClassName("cell-2");
            this.priceElm = label;
            this.priceElm.setText(NumberFormat.getCurrencyInstance(Locale.US).format(this.state.price.get()));
            this.priceOldValue = this.state.price.get();
        });

        dom.horizontalLayout(pane1 -> {
            pane1.addClassName("cell-3");
            pane1.setAlignItems(FlexComponent.Alignment.CENTER);

            dom.span(label -> {
                this.quantityElm = label;
                this.quantityElm.setText(String.valueOf(this.state.quantity.get()));
                this.quantityOldValue = this.state.quantity.get();
            });

            dom.image(img -> {
                img.setSrc("images/delet.png");
                img.setAlt("Remove");
                img.setWidth("16px");
                img.setHeight("16px");
                img.getStyle().set("cursor", "pointer");
                img.addClickListener(e -> safeAction("Remove product", this.state.onRemove::run));
            });
        });
    }
}
