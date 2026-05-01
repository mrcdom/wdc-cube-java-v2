package br.com.wdc.shopping.view.vaadin.impl;

import java.text.NumberFormat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.ResourceCatalog;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class CartViewVaadin extends AbstractViewVaadin<CartPresenter> {

    private final CartViewState state;

    private boolean notRendered = true;
    private Grid<CartItem> grid;
    private Span totalCostElm;
    private double totalCostOldValue;

    public CartViewVaadin(ShoppingVaadinApplication app, CartPresenter presenter) {
        super("cart", app, presenter, new VerticalLayout());
        this.state = presenter.state;
    }

    @Override
    public void recreate() {
        this.element = new VerticalLayout();
        this.notRendered = true;
        this.grid = null;
        this.totalCostElm = null;
        this.totalCostOldValue = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        this.grid.setItems(this.state.items);

        var totalCostNewValue = this.computeTotalCost();
        if (totalCostNewValue != this.totalCostOldValue) {
            this.totalCostElm.setText(this.formatCurrency(totalCostNewValue));
            this.totalCostOldValue = totalCostNewValue;
        }

        if (this.state.errorCode != 0) {
            Notification.show(this.state.errorMessage, 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        }
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("cart-form");
        pane0.setMaxWidth("900px");

        // Title with cart icon
        dom.horizontalLayout(titleBar -> {
            titleBar.setAlignItems(FlexComponent.Alignment.CENTER);
            titleBar.setWidthFull();

            dom.icon(VaadinIcon.CART, icon -> {
                icon.setSize("24px");
                icon.setColor("var(--lumo-primary-color)");
            });

            dom.h3(h -> {
                h.setText("Carrinho [" + this.state.items.size() + "]");
                h.getStyle().set("margin", "0 0 0 8px");
            });
        });

        dom.h4(h -> {
            h.setText("LISTA DE PRODUTOS");
            h.getStyle().set("color", "var(--lumo-secondary-text-color)");
        });

        // Grid
        this.grid = new Grid<>();
        this.grid.setAllRowsVisible(true);
        this.grid.addColumn(new ComponentRenderer<>(item -> {
            var layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            var img = new Image(ResourceCatalog.getImageResource(item.image), item.name);
            img.setWidth("42px");
            img.setHeight("40px");
            layout.add(img, new Span(item.name));
            return layout;
        })).setHeader("ITEM").setFlexGrow(3);

        this.grid.addColumn(item -> "R$ " + NumberFormat.getInstance().format(item.price))
                .setHeader("VALOR").setFlexGrow(1);

        this.grid.addColumn(new ComponentRenderer<>(item -> {
            var layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.add(new Span(String.valueOf(item.quantity)));
            var deleteBtn = new Button(VaadinIcon.TRASH.create(),
                    e -> safeAction("Remove product", () -> this.presenter.onRemoveProduct(item.id)));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            layout.add(deleteBtn);
            return layout;
        })).setHeader("QUANTIDADE").setFlexGrow(1);

        pane0.add(this.grid);

        // Footer with total
        dom.horizontalLayout(footer -> {
            footer.setWidthFull();
            footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            footer.setAlignItems(FlexComponent.Alignment.CENTER);
            footer.getStyle().set("padding", "12px 0");

            dom.span(label -> {
                label.setText("VALOR TOTAL: ");
                label.getStyle().set("font-weight", "bold");
            });

            dom.span(label -> {
                var totalCostNewValue = this.computeTotalCost();
                this.totalCostElm = label;
                this.totalCostElm.setText(this.formatCurrency(totalCostNewValue));
                this.totalCostElm.getStyle().set("font-size", "18px")
                        .set("font-weight", "bold")
                        .set("color", "var(--lumo-primary-color)");
                this.totalCostOldValue = totalCostNewValue;
            });
        });

        // Action buttons
        dom.horizontalLayout(actions -> {
            actions.setWidthFull();
            actions.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

            dom.button(button -> {
                button.setText("VOLTAR");
                button.setIcon(VaadinIcon.ARROW_LEFT.create());
                button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                button.addClickListener(e -> safeAction("Open products", this.presenter::onOpenProducts));
            });

            dom.button(button -> {
                button.setText("FINALIZAR PEDIDO");
                button.setIcon(VaadinIcon.CHECK.create());
                button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                button.getStyle().set("background-color", "var(--lumo-success-color)");
                button.addClickListener(e -> safeAction("Buy", this.presenter::onBuy));
            });
        });
    }

    private double computeTotalCost() {
        return this.state.items.stream().mapToDouble(v -> v.price * v.quantity).sum();
    }

    private String formatCurrency(double value) {
        return "R$ " + NumberFormat.getInstance().format(value);
    }
}
