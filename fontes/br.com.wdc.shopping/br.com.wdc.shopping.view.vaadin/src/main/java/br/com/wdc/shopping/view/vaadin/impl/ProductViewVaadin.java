package br.com.wdc.shopping.view.vaadin.impl;

import java.text.NumberFormat;
import java.util.Objects;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.ResourceCatalog;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class ProductViewVaadin extends AbstractViewVaadin<ProductPresenter> {

    private final ProductViewState state;

    private boolean notRendered = true;
    private Span nameElm1;
    private Span nameElm2;
    private String nameOldValue;
    private com.vaadin.flow.component.html.Image imageElm;
    private String imageOldValue;
    private Span priceElm;
    private double priceOldValue;
    private IntegerField quantityElm;
    private Div descriptionElm;
    private String descriptionOldValue;

    public ProductViewVaadin(ProductPresenter presenter) {
        super("product", (ShoppingVaadinApplication) presenter.app, presenter, new VerticalLayout());
        this.state = presenter.state;
    }

    @Override
    public void recreate() {
        this.element = new VerticalLayout();
        this.notRendered = true;
        this.nameElm1 = null;
        this.nameElm2 = null;
        this.nameOldValue = null;
        this.imageElm = null;
        this.imageOldValue = null;
        this.priceElm = null;
        this.priceOldValue = 0;
        this.quantityElm = null;
        this.descriptionElm = null;
        this.descriptionOldValue = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.nameOldValue, this.state.product.name)) {
            this.nameElm1.setText(this.state.product.name);
            this.nameElm2.setText(this.state.product.name);
            this.nameOldValue = this.state.product.name;
        }

        if (!Objects.equals(this.imageOldValue, this.state.product.image)) {
            this.imageElm.setSrc(ResourceCatalog.getImageResource(this.state.product.image));
            this.imageOldValue = this.state.product.image;
        }

        if (this.priceOldValue != this.state.product.price) {
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.state.product.price));
            this.priceOldValue = this.state.product.price;
        }

        if (!Objects.equals(this.descriptionOldValue, this.state.product.description)) {
            this.descriptionElm.getElement().setProperty("innerHTML",
                    this.state.product.description != null ? this.state.product.description : "");
            this.descriptionOldValue = this.state.product.description;
        }

        if (this.state.errorCode != 0) {
            Notification.show(this.state.errorMessage, 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        }
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("product-form");
        pane0.setMaxWidth("900px");
        pane0.setPadding(true);
        pane0.setSpacing(false);

        // Product name
        dom.h3(h -> {
            this.nameElm1 = new Span();
            this.nameElm2 = new Span();
            this.nameElm2.setText(this.state.product.name);
            this.nameElm1.setText(this.state.product.name);
            h.add(this.nameElm2);
            h.getStyle().set("margin", "var(--lumo-space-xs) 0");
        });

        // Row: info panel + image
        dom.div(row -> {
            row.addClassName("product-info-row");
            row.getStyle()
                    .set("display", "flex")
                    .set("flex-direction", "row")
                    .set("align-items", "center")
                    .set("margin-top", "var(--lumo-space-s)")
                    .set("gap", "var(--lumo-space-l)");

            // Left panel: price + qty + button (uses Div to avoid Shadow DOM)
            var infoPanel = new Div();
            infoPanel.getStyle()
                    .set("display", "flex")
                    .set("flex-direction", "column")
                    .set("align-items", "flex-start")
                    .set("justify-content", "center")
                    .set("flex", "0 1 auto");
            row.add(infoPanel);

            this.priceElm = new Span();
            this.priceElm.addClassName("lbl-price-val");
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.state.product.price));
            this.priceOldValue = this.state.product.price;
            infoPanel.add(this.priceElm);

            var actionRow = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
            actionRow.setAlignItems(FlexComponent.Alignment.BASELINE);
            actionRow.setPadding(false);
            actionRow.setSpacing(true);
            actionRow.getStyle().set("margin-top", "var(--lumo-space-s)");
            infoPanel.add(actionRow);

            this.quantityElm = new IntegerField();
            this.quantityElm.setLabel("Quantidade");
            this.quantityElm.setValue(1);
            this.quantityElm.setMin(1);
            this.quantityElm.setStepButtonsVisible(true);
            this.quantityElm.addClassName("fld-quantity");
            actionRow.add(this.quantityElm);

            var addBtn = new com.vaadin.flow.component.button.Button("Adicionar");
            addBtn.setIcon(VaadinIcon.CART.create());
            addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addBtn.addClickListener(e -> safeAction("Add to cart", this::emitBuyClicked));
            actionRow.add(addBtn);

            // Right: product image
            this.imageElm = new com.vaadin.flow.component.html.Image();
            this.imageElm.setSrc(ResourceCatalog.getImageResource(this.state.product.image));
            this.imageOldValue = this.state.product.image;
            this.imageElm.getStyle()
                    .set("flex", "0 0 auto")
                    .set("width", "240px")
                    .set("height", "240px")
                    .set("object-fit", "contain")
                    .set("padding", "var(--lumo-space-s)");
            row.add(this.imageElm);
        });

        // Description label
        dom.span(label -> {
            label.setText("Descrição");
            label.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-weight", "600")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("margin-top", "var(--lumo-space-s)");
        });

        // Description content - expands to fill available space
        dom.div(descDiv -> {
            descDiv.addClassName("description");
            descDiv.setWidthFull();
            this.descriptionElm = descDiv;
            if (this.state.product.description != null) {
                descDiv.getElement().setProperty("innerHTML", this.state.product.description);
            }
            this.descriptionOldValue = this.state.product.description;
        });

        // Back button - bottom
        dom.button(button -> {
            button.setText("Voltar aos produtos");
            button.setIcon(VaadinIcon.ARROW_LEFT.create());
            button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            button.addClassName("back-button");
            button.getStyle().set("margin-top", "var(--lumo-space-m)");
            button.addClickListener(e -> safeAction("Open products", this.presenter::onOpenProducts));
        });
    }

    private void emitBuyClicked() {
        var quantity = this.quantityElm.getValue();
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
        this.presenter.onAddToCart(quantity);
    }
}
