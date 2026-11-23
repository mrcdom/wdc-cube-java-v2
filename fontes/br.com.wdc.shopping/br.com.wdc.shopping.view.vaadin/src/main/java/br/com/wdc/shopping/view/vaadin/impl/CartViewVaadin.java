package br.com.wdc.shopping.view.vaadin.impl;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartScope.CartItemScope;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class CartViewVaadin extends AbstractViewVaadin<CartScope> {

    private boolean notRendered = true;
    private HorizontalLayout headerPane;
    private VerticalLayout rowsPane;
    private Span totalCostElm;
    private double totalCostOldValue;
    private Div emptyCartPane;
    private HorizontalLayout footerPane;
    private HorizontalLayout actionsPane;

    public CartViewVaadin(ShoppingVaadinApplication app, CartScope state) {
        super("cart", app, state, new VerticalLayout());
    }

    @Override
    public void recreate() {
        this.element = new VerticalLayout();
        this.notRendered = true;
        this.headerPane = null;
        this.rowsPane = null;
        this.totalCostElm = null;
        this.totalCostOldValue = 0;
        this.emptyCartPane = null;
        this.footerPane = null;
        this.actionsPane = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        var rows = this.state.rows.get();
        this.syncRows(rows);

        boolean empty = rows.isEmpty();
        this.emptyCartPane.setVisible(empty);
        this.headerPane.setVisible(!empty);
        this.rowsPane.setVisible(!empty);
        this.footerPane.setVisible(!empty);
        if (this.actionsPane != null) {
            // Show only "Back to products" in actions when empty
            this.actionsPane.getChildren().forEach(c -> {
                if (c instanceof Button btn && "CHECKOUT".equals(btn.getText())) {
                    btn.setVisible(!empty);
                }
            });
        }

        var totalCostNewValue = this.state.total.get();
        if (totalCostNewValue != this.totalCostOldValue) {
            this.totalCostElm.setText(this.formatCurrency(totalCostNewValue));
            this.totalCostOldValue = totalCostNewValue;
        }

        if (this.state.errorCode.get() != 0) {
            Notification.show(this.state.errorMessage.get(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            // Consumed while drawing: a redraw now would take the message away
            this.state.errorCode.setSilently(0);
            this.state.errorMessage.setSilently(null);
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
                h.setText("Cart");
                h.getStyle().set("margin", "0 0 0 8px");
            });
        });

        dom.h4(h -> {
            h.setText("PRODUCT LIST");
            h.getStyle().set("color", "var(--lumo-secondary-text-color)");
        });

        // Rows: each one a view of its own (the cart's segments), laid out in the cart's order
        dom.horizontalLayout(header -> {
            this.headerPane = header;
            header.addClassName("cart-row");
            header.setWidthFull();
            header.getStyle().set("font-weight", "bold").set("color", "var(--lumo-secondary-text-color)");
            dom.span(label -> { label.addClassName("cell-1"); label.setText("ITEM"); });
            dom.span(label -> { label.addClassName("cell-2"); label.setText("PRICE"); });
            dom.span(label -> { label.addClassName("cell-3"); label.setText("QUANTITY"); });
        });

        dom.verticalLayout(rows -> {
            this.rowsPane = rows;
            rows.setWidthFull();
            rows.setPadding(false);
            rows.setSpacing(false);
        });

        // Empty cart message
        this.emptyCartPane = new Div();
        this.emptyCartPane.addClassName("cart-empty-message");
        this.emptyCartPane.setVisible(false);
        this.emptyCartPane.setWidthFull();
        this.emptyCartPane.getStyle()
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("padding", "var(--lumo-space-xl) 0");
        var emptyCartIcon = new Div();
        emptyCartIcon.addClassName("cart-empty-icon");
        this.emptyCartPane.add(emptyCartIcon);
        var emptyText = new Span("Your cart is empty");
        emptyText.getStyle().set("display", "block").set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "1.1rem").set("margin-top", "var(--lumo-space-m)");
        this.emptyCartPane.add(emptyText);
        var goShoppingBtn = new Button("Let's go shopping!",
                e -> safeAction("Open products", this.state.onOpenProducts::run));
        goShoppingBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        goShoppingBtn.getStyle().set("font-weight", "bold").set("font-size", "1rem").set("margin-top",
                "var(--lumo-space-s)");
        this.emptyCartPane.add(goShoppingBtn);
        pane0.add(this.emptyCartPane);

        // Footer with total
        dom.horizontalLayout(footer -> {
            this.footerPane = footer;
            footer.setWidthFull();
            footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            footer.setAlignItems(FlexComponent.Alignment.CENTER);
            footer.getStyle().set("padding", "12px 0");

            dom.span(label -> {
                label.setText("TOTAL: ");
                label.getStyle().set("font-weight", "bold");
            });

            dom.span(label -> {
                var totalCostNewValue = this.state.total.get();
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
            this.actionsPane = actions;
            actions.setWidthFull();
            actions.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

            dom.button(button -> {
                button.setText("Back to products");
                button.setIcon(VaadinIcon.ARROW_LEFT.create());
                button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                button.addClassName("back-button");
                button.addClickListener(e -> safeAction("Open products", this.state.onOpenProducts::run));
            });

            dom.button(button -> {
                button.setText("CHECKOUT");
                button.setIcon(VaadinIcon.CHECK.create());
                button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                button.getStyle().set("background-color", "var(--lumo-success-color)");
                button.addClickListener(e -> safeAction("Buy", this.state.onBuy::run));
            });
        });
    }

    /** Puts the rows' views in the pane, in the cart's order; a row view is reused while its row stays. */
    private void syncRows(List<CartItemScope> rows) {
        var wanted = new ArrayList<Component>(rows.size());
        for (var row : rows) {
            if (this.app.viewOf(row) instanceof AbstractViewVaadin<?> rowView) {
                wanted.add(rowView.getElement());
            }
        }
        var current = this.rowsPane.getChildren().toList();
        if (!current.equals(wanted)) {
            this.rowsPane.removeAll();
            wanted.forEach(this.rowsPane::add);
        }
    }

    private String formatCurrency(double value) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(value);
    }
}
