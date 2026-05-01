package br.com.wdc.shopping.view.vaadin.impl.home;

import java.text.NumberFormat;
import java.util.Objects;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.ResourceCatalog;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class ProductItemViewVaadin extends AbstractViewVaadin<ProductsPanelPresenter> {

    private ProductInfo state;

    private boolean notRendered = true;
    private Image imageElm;
    private String imageOldValue;
    private Span nameElm;
    private String nameOldValue;
    private Span priceElm;
    private double priceOldValue;

    public ProductItemViewVaadin(ShoppingVaadinApplication app, ProductsPanelPresenter presenter, int idx) {
        super("product-item-" + idx, app, presenter, new Div());
    }

    public void setState(ProductInfo state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((Div) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.imageOldValue, this.state.image)) {
            this.imageElm.setSrc(ResourceCatalog.getImageResource(this.state.image));
            this.imageOldValue = this.state.image;
        }

        if (!Objects.equals(this.nameOldValue, this.state.name)) {
            this.nameElm.setText(this.state.name);
            this.nameOldValue = this.state.name;
        }

        if (this.priceOldValue != this.state.price) {
            this.priceElm.setText("R$ " + NumberFormat.getInstance().format(this.state.price));
            this.priceOldValue = this.state.price;
        }
    }

    private void initialRender(VaadinDom dom, Div pane0) {
        pane0.setMinWidth("200px");
        pane0.setMaxWidth("200px");
        pane0.getStyle().set("cursor", "pointer");
        pane0.addClickListener(e -> safeAction("Open product", () -> this.presenter.onOpenProduct(this.state.id)));

        dom.verticalLayout(pane1 -> {
            pane1.addClassName("product-selection-item");

            dom.div(imagePane -> {
                imagePane.addClassName("image-pane");
                dom.image(img -> {
                    img.setWidth("180px");
                    img.setHeight("140px");
                    img.getStyle().set("object-fit", "contain");
                    this.imageElm = img;
                    this.imageElm.setSrc(ResourceCatalog.getImageResource(this.state.image));
                    this.imageOldValue = this.state.image;
                });
            });

            dom.verticalLayout(pane2 -> {
                pane2.addClassName("label-group");
                dom.span(label -> {
                    label.addClassName("label-name");
                    this.nameElm = label;
                    this.nameElm.setText(this.state.name);
                    this.nameOldValue = this.state.name;
                });
                dom.span(label -> {
                    label.addClassName("label-price");
                    this.priceElm = label;
                    this.priceElm.setText("R$ " + NumberFormat.getInstance().format(this.state.price));
                    this.priceOldValue = this.state.price;
                });
            });
        });
    }
}
