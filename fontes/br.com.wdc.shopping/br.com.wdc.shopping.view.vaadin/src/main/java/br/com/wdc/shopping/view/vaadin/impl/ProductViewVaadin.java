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
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductViewState;
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

    public ProductViewVaadin(ShoppingVaadinApplication app, ProductPresenter presenter) {
        super("product", app, presenter, new VerticalLayout());
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

        // Breadcrumbs
        dom.horizontalLayout(pane1 -> {
            pane1.addClassName("breadcrumbs");
            pane1.setPadding(false);
            pane1.setSpacing(false);
            dom.span(text -> text.setText("Produtos > "));
            dom.span(text -> {
                this.nameElm1 = text;
                this.nameElm1.setText(this.state.product.name);
                this.nameOldValue = this.state.product.name;
            });
        });

        dom.horizontalLayout(pane1 -> {
            pane1.setWidthFull();
            pane1.setSpacing(true);

            dom.verticalLayout(leftCol -> {
                leftCol.setSpacing(false);
                leftCol.setPadding(false);
                leftCol.setWidth("auto");

                dom.div(imgPane -> {
                    imgPane.addClassName("image-pane");

                    dom.image(img -> {
                        this.imageElm = img;
                        this.imageElm.setSrc(ResourceCatalog.getImageResource(this.state.product.image));
                        this.imageOldValue = this.state.product.image;
                        img.setWidth("280px");
                        img.setHeight("280px");
                        img.getStyle().set("object-fit", "contain");
                    });
                });

                dom.button(button -> {
                    button.setText("VOLTAR");
                    button.setIcon(VaadinIcon.ARROW_LEFT.create());
                    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                    button.getStyle().set("margin-top", "var(--lumo-space-xs)");
                    button.addClickListener(e -> safeAction("Open products", this.presenter::onOpenProducts));
                });
            });

            dom.verticalLayout(pane2 -> {
                pane2.getStyle().set("flex-grow", "1");
                pane2.addClassName("content");
                pane2.setPadding(false);
                pane2.setSpacing(false);

                dom.h3(h -> {
                    this.nameElm2 = new Span();
                    this.nameElm2.setText(this.state.product.name);
                    h.add(this.nameElm2);
                    h.getStyle().set("margin", "0 0 var(--lumo-space-xs) 0");
                });

                dom.horizontalLayout(pane3 -> {
                    pane3.addClassName("pane-price-qtd");
                    pane3.setAlignItems(FlexComponent.Alignment.CENTER);
                    pane3.setWidthFull();
                    pane3.setPadding(false);

                    dom.span(label -> {
                        label.addClassName("lbl-price-val");
                        this.priceElm = label;
                        this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.state.product.price));
                        this.priceOldValue = this.state.product.price;
                    });

                    dom.hSpacer();
                });

                // IntegerField with step buttons (Vaadin native)
                dom.integerField(field -> {
                    this.quantityElm = field;
                    field.setLabel("Quantidade");
                    field.setValue(1);
                    field.setMin(1);
                    field.setStepButtonsVisible(true);
                    field.addClassName("fld-quantity");
                });

                dom.span(label -> {
                    label.setText("DESCRIÇÃO DO PRODUTO");
                    label.getStyle()
                            .set("color", "var(--lumo-secondary-text-color)")
                            .set("font-weight", "600")
                            .set("font-size", "var(--lumo-font-size-s)")
                            .set("margin-top", "var(--lumo-space-xs)");
                });

                dom.div(descDiv -> {
                    descDiv.addClassName("description");
                    this.descriptionElm = descDiv;
                    if (this.state.product.description != null) {
                        descDiv.getElement().setProperty("innerHTML", this.state.product.description);
                    }
                    this.descriptionOldValue = this.state.product.description;
                });

                dom.horizontalLayout(pane3 -> {
                    pane3.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                    pane3.setWidthFull();
                    dom.button(button -> {
                        button.setText("Adicionar ao carrinho");
                        button.setIcon(VaadinIcon.CART.create());
                        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                        button.addClickListener(e -> safeAction("Add to cart", this::emitBuyClicked));
                    });
                });
            });
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
