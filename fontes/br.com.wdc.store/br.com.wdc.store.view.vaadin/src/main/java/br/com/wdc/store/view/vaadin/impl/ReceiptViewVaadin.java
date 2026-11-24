package br.com.wdc.store.view.vaadin.impl;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.store.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.store.view.vaadin.AbstractViewVaadin;
import br.com.wdc.store.view.vaadin.StoreVaadinApplication;
import br.com.wdc.store.view.vaadin.util.VaadinDom;

public class ReceiptViewVaadin extends AbstractViewVaadin<ReceiptScope> {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a",
            Locale.US);

    private boolean notRendered = true;
    private Span dateElm;
    private Long dateOldValue;
    private Grid<ReceiptItem> grid;
    private Span totalElm;
    private double totalOldValue;

    public ReceiptViewVaadin(StoreVaadinApplication app, ReceiptScope state) {
        super("receipt", app, state, new VerticalLayout());
    }

    @Override
    public void recreate() {
        this.element = new VerticalLayout();
        this.notRendered = true;
        this.dateElm = null;
        this.dateOldValue = null;
        this.grid = null;
        this.totalElm = null;
        this.totalOldValue = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (this.state.notifySuccess.get()) {
            Notification.show("PURCHASE COMPLETED SUCCESSFULLY!", 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            // Consumed while drawing: a one-shot notice needs no redraw
            this.state.notifySuccess.setSilently(false);
        }

        if (!Objects.equals(this.dateOldValue, this.state.receipt.get().date)) {
            this.dateElm.setText(formatDate(this.state.receipt.get().date));
            this.dateOldValue = this.state.receipt.get().date;
        }

        if (this.totalOldValue != this.state.receipt.get().total) {
            this.totalElm.setText(NumberFormat.getCurrencyInstance(Locale.US).format(this.state.receipt.get().total));
            this.totalOldValue = this.state.receipt.get().total;
        }

        this.grid.setItems(this.state.receipt.get().items);
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("receipt-form");
        pane0.setMaxWidth("900px");

        dom.h3(h -> {
            h.setText("PRINT YOUR RECEIPT:");
            h.getStyle().set("color", "var(--lumo-secondary-text-color)");
        });

        dom.verticalLayout(pane1 -> {
            pane1.addClassName("content");

            dom.span(label -> {
                label.addClassName("caption-1");
                label.setText("WEDOCODE STORE - YOUR TRUSTED ONLINE STORE");
            });
            dom.span(label -> {
                label.addClassName("caption-2");
                label.setText("Purchase Receipt");
            });

            dom.horizontalLayout(dateRow -> {
                dateRow.addClassName("receipt-date");
                dom.span(label -> {
                    label.setText("Date:");
                    label.getStyle().set("font-weight", "bold");
                });
                dom.span(label -> {
                    this.dateElm = label;
                    this.dateElm.setText(formatDate(this.state.receipt.get().date));
                    this.dateOldValue = this.state.receipt.get().date;
                });
            });

            // Grid
            this.grid = new Grid<>();
            this.grid.setAllRowsVisible(true);
            this.grid.addColumn(item -> item.description).setHeader("ITEM").setFlexGrow(3);
            this.grid.addColumn(item -> NumberFormat.getCurrencyInstance(Locale.US).format(item.value))
                    .setHeader("PRICE").setFlexGrow(1);
            this.grid.addColumn(item -> item.quantity).setHeader("QUANTITY").setFlexGrow(1);
            pane1.add(this.grid);

            dom.horizontalLayout(footer -> {
                footer.setWidthFull();
                footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
                footer.getStyle().set("padding-top", "12px");

                dom.span(label -> {
                    label.setText("TOTAL: ");
                    label.getStyle().set("font-weight", "bold");
                });

                dom.span(label -> {
                    this.totalElm = label;
                    this.totalElm.setText(NumberFormat.getCurrencyInstance(Locale.US).format(this.state.receipt.get().total));
                    this.totalElm.getStyle().set("font-size", "18px")
                            .set("font-weight", "bold")
                            .set("color", "var(--lumo-primary-color)");
                    this.totalOldValue = this.state.receipt.get().total;
                });
            });
        });

        dom.button(button -> {
            button.setText("Back to products");
            button.setIcon(VaadinIcon.ARROW_LEFT.create());
            button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            button.addClassName("back-button");
            button.getStyle().set("margin-top", "var(--lumo-space-m)");
            button.addClickListener(e -> safeAction("Open products", this.state.onOpenProducts::run));
        });
    }

    /** The purchase's date and time, in the server's zone (as the other hosts show it). */
    private static String formatDate(Long millis) {
        return millis == null ? "" : DATE_TIME_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }
}
