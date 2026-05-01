package br.com.wdc.shopping.view.vaadin.impl.cart;

import java.text.NumberFormat;
import java.util.Objects;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.ResourceCatalog;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class CartItemViewVaadin extends AbstractViewVaadin<CartPresenter> {

    private CartItem state;

    private boolean notRendered = true;
    private Span nameElm;
    private String nameOldValue;
    private Span priceElm;
    private double priceOldValue;
    private Span quantityElm;
    private int quantityOldValue;

    public CartItemViewVaadin(ShoppingVaadinApplication app, CartPresenter presenter, int idx) {
        super("cart-item-" + idx, app, presenter, new HorizontalLayout());
    }

    public void setState(CartItem state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((HorizontalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.nameOldValue, this.state.name)) {
            this.nameElm.setText(this.state.name);
            this.nameOldValue = this.state.name;
        }

        if (this.priceOldValue != this.state.price) {
            this.priceElm.setText("R$ " + NumberFormat.getInstance().format(this.state.price));
            this.priceOldValue = this.state.price;
        }

        if (this.quantityOldValue != this.state.quantity) {
            this.quantityElm.setText(String.valueOf(this.state.quantity));
            this.quantityOldValue = this.state.quantity;
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
                img.setSrc(ResourceCatalog.getImageResource(this.state.image));
            });

            dom.span(label -> {
                this.nameElm = label;
                this.nameElm.setText(this.state.name);
                this.nameOldValue = this.state.name;
            });
        });

        dom.span(label -> {
            label.addClassName("cell-2");
            this.priceElm = label;
            this.priceElm.setText("R$ " + NumberFormat.getInstance().format(this.state.price));
            this.priceOldValue = this.state.price;
        });

        dom.horizontalLayout(pane1 -> {
            pane1.addClassName("cell-3");
            pane1.setAlignItems(FlexComponent.Alignment.CENTER);

            dom.span(label -> {
                this.quantityElm = label;
                this.quantityElm.setText(String.valueOf(this.state.quantity));
                this.quantityOldValue = this.state.quantity;
            });

            dom.image(img -> {
                img.setSrc("images/delet.png");
                img.setAlt("Remover");
                img.setWidth("16px");
                img.setHeight("16px");
                img.getStyle().set("cursor", "pointer");
                img.addClickListener(e -> safeAction("Remove product", () -> this.presenter.onRemoveProduct(this.state.id)));
            });
        });
    }
}
