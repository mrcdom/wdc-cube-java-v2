package br.com.wdc.store.view.vaadin.impl;

import java.util.Objects;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.store.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.store.view.vaadin.AbstractViewVaadin;
import br.com.wdc.store.view.vaadin.StoreVaadinApplication;
import br.com.wdc.store.view.vaadin.util.I18n;
import br.com.wdc.store.view.vaadin.util.VaadinDom;

public class ReceiptViewVaadin extends AbstractViewVaadin<ReceiptScope> {

    private boolean notRendered = true;
    private Span dateElm;
    private Long dateOldValue;
    private Grid<ReceiptItem> grid;
    private Span totalElm;
    private double totalOldValue;

    public ReceiptViewVaadin(StoreVaadinApplication app, ReceiptScope scope) {
        super("receipt", app, scope, new VerticalLayout());
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

        if (this.scope.notifySuccess.get()) {
            Notification.show(text(StoreMessages.RECEIPT_COMPLETED), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            // Consumed while drawing: a one-shot notice needs no redraw
            this.scope.notifySuccess.setSilently(false);
        }

        if (!Objects.equals(this.dateOldValue, this.scope.receipt.get().date)) {
            this.dateElm.setText(formatDate(this.scope.receipt.get().date));
            this.dateOldValue = this.scope.receipt.get().date;
        }

        if (this.totalOldValue != this.scope.receipt.get().total) {
            this.totalElm.setText(I18n.price(this.scope.receipt.get().total));
            this.totalOldValue = this.scope.receipt.get().total;
        }

        this.grid.setItems(this.scope.receipt.get().items);
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("receipt-form");
        pane0.setMaxWidth("900px");

        dom.h3(h -> {
            h.setText(text(StoreMessages.VAADIN_PRINT_RECEIPT));
            h.getStyle().set("color", "var(--lumo-secondary-text-color)");
        });

        dom.verticalLayout(pane1 -> {
            pane1.addClassName("content");

            dom.span(label -> {
                label.addClassName("caption-1");
                label.setText((text(StoreMessages.APP_NAME) + " - " + text(StoreMessages.APP_TAGLINE))
                        .toUpperCase(this.app.i18n().locale()));
            });
            dom.span(label -> {
                label.addClassName("caption-2");
                label.setText(text(StoreMessages.RECEIPT_TITLE));
            });

            dom.horizontalLayout(dateRow -> {
                dateRow.addClassName("receipt-date");
                dom.span(label -> {
                    label.setText(text(StoreMessages.RECEIPT_DATE));
                    label.getStyle().set("font-weight", "bold");
                });
                dom.span(label -> {
                    this.dateElm = label;
                    this.dateElm.setText(formatDate(this.scope.receipt.get().date));
                    this.dateOldValue = this.scope.receipt.get().date;
                });
            });

            // Grid
            this.grid = new Grid<>();
            this.grid.setAllRowsVisible(true);
            this.grid.addColumn(item -> item.description).setHeader(text(StoreMessages.RECEIPT_ITEM)).setFlexGrow(3);
            this.grid.addColumn(item -> I18n.price(item.value))
                    .setHeader(text(StoreMessages.RECEIPT_PRICE)).setFlexGrow(1);
            this.grid.addColumn(item -> item.quantity).setHeader(text(StoreMessages.RECEIPT_QUANTITY)).setFlexGrow(1);
            pane1.add(this.grid);

            dom.horizontalLayout(footer -> {
                footer.setWidthFull();
                footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
                footer.getStyle().set("padding-top", "12px");

                dom.span(label -> {
                    label.setText(text(StoreMessages.RECEIPT_TOTAL) + " ");
                    label.getStyle().set("font-weight", "bold");
                });

                dom.span(label -> {
                    this.totalElm = label;
                    this.totalElm.setText(I18n.price(this.scope.receipt.get().total));
                    this.totalElm.getStyle().set("font-size", "18px")
                            .set("font-weight", "bold")
                            .set("color", "var(--lumo-primary-color)");
                    this.totalOldValue = this.scope.receipt.get().total;
                });
            });
        });

        dom.button(button -> {
            button.setText(text(StoreMessages.RECEIPT_BACK_TO_PRODUCTS));
            button.setIcon(VaadinIcon.ARROW_LEFT.create());
            button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            button.addClassName("back-button");
            button.getStyle().set("margin-top", "var(--lumo-space-m)");
            button.addClickListener(e -> safeAction("Open products", this.scope.onOpenProducts::run));
        });
    }

    /** The purchase's date and time, in the server's zone (as the other hosts show it). */
    private String formatDate(Long millis) {
        return millis == null ? text(StoreMessages.FORMAT_DATE_UNAVAILABLE) : this.app.i18n().dateTime(millis);
    }
}
