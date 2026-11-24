package br.com.wdc.store.view.vaadin.impl.home;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;

import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope.ProductsPanelScope;
import br.com.wdc.store.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.store.view.vaadin.AbstractViewVaadin;
import br.com.wdc.store.view.vaadin.StoreVaadinApplication;
import br.com.wdc.store.view.vaadin.util.ResourceCatalog;
import br.com.wdc.store.view.vaadin.util.VaadinDom;

public class ProductItemViewVaadin extends AbstractViewVaadin<ProductsPanelScope> {

    private ProductInfo item;

    private boolean notRendered = true;
    private Image imageElm;
    private String imageOldValue;
    private Span nameElm;
    private String nameOldValue;
    private Span priceElm;
    private double priceOldValue;

    public ProductItemViewVaadin(StoreVaadinApplication app, ProductsPanelScope panel, int idx) {
        super("product-item-" + idx, app, panel, new Div());
    }

    public void setItem(ProductInfo item, boolean scheduleUpdate) {
        this.item = item;
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

        if (!Objects.equals(this.imageOldValue, this.item.image)) {
            this.imageElm.setSrc(ResourceCatalog.getImageResource(this.item.image));
            this.imageOldValue = this.item.image;
        }

        if (!Objects.equals(this.nameOldValue, this.item.name)) {
            this.nameElm.setText(this.item.name);
            this.nameOldValue = this.item.name;
        }

        if (this.priceOldValue != this.item.price) {
            this.priceElm.setText(NumberFormat.getCurrencyInstance(Locale.US).format(this.item.price));
            this.priceOldValue = this.item.price;
        }
    }

    private void initialRender(VaadinDom dom, Div pane0) {
        pane0.getStyle()
                .set("cursor", "pointer")
                .set("flex", "1 1 calc(50% - 12px)")
                .set("min-width", "250px")
                .set("max-width", "calc(50% - 12px)");
        pane0.addClickListener(e -> safeAction("Open product", () -> this.scope.onOpenProduct.run(this.item.id)));

        dom.verticalLayout(pane1 -> {
            pane1.addClassName("product-selection-item");

            dom.div(imagePane -> {
                imagePane.addClassName("image-pane");
                dom.image(img -> {
                    img.setWidth("180px");
                    img.setHeight("140px");
                    img.getStyle().set("object-fit", "contain");
                    this.imageElm = img;
                    this.imageElm.setSrc(ResourceCatalog.getImageResource(this.item.image));
                    this.imageOldValue = this.item.image;
                });
            });

            dom.verticalLayout(pane2 -> {
                pane2.addClassName("label-group");
                dom.span(label -> {
                    label.addClassName("label-name");
                    this.nameElm = label;
                    this.nameElm.setText(this.item.name);
                    this.nameOldValue = this.item.name;
                });
                dom.span(label -> {
                    label.addClassName("label-price");
                    this.priceElm = label;
                    this.priceElm.setText(NumberFormat.getCurrencyInstance(Locale.US).format(this.item.price));
                    this.priceOldValue = this.item.price;
                });
            });
        });
    }
}
