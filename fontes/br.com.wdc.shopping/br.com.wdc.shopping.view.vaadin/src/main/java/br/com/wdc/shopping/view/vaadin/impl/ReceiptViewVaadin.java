package br.com.wdc.shopping.view.vaadin.impl;

import java.text.NumberFormat;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class ReceiptViewVaadin extends AbstractViewVaadin<ReceiptPresenter> {

    private final ReceiptViewState state;

    private boolean notRendered = true;
    private Grid<ReceiptItem> grid;
    private Span totalElm;
    private double totalOldValue;

    public ReceiptViewVaadin(ShoppingVaadinApplication app, ReceiptPresenter presenter) {
        super("receipt", app, presenter, new VerticalLayout());
        this.state = presenter.state;
    }

    @Override
    public void recreate() {
        this.element = new VerticalLayout();
        this.notRendered = true;
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

        if (this.state.notifySuccess) {
            Notification.show("COMPRA EFETUADA COM SUCESSO!", 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            this.state.notifySuccess = false;
        }

        if (this.totalOldValue != this.state.receipt.total) {
            this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.receipt.total));
            this.totalOldValue = this.state.receipt.total;
        }

        this.grid.setItems(this.state.receipt.items);
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("receipt-form");
        pane0.setMaxWidth("900px");

        dom.h3(h -> {
            h.setText("IMPRIMA SEU RECIBO:");
            h.getStyle().set("color", "var(--lumo-secondary-text-color)");
        });

        dom.verticalLayout(pane1 -> {
            pane1.addClassName("content");

            dom.span(label -> {
                label.addClassName("caption-1");
                label.setText("WEDOCODE SHOPPING - SUA COMPRA CERTA NA INTERNET");
            });
            dom.span(label -> {
                label.addClassName("caption-2");
                label.setText("Recibo de compra");
            });

            // Grid
            this.grid = new Grid<>();
            this.grid.setAllRowsVisible(true);
            this.grid.addColumn(item -> item.description).setHeader("ITEM").setFlexGrow(3);
            this.grid.addColumn(item -> NumberFormat.getCurrencyInstance().format(item.value))
                    .setHeader("VALOR").setFlexGrow(1);
            this.grid.addColumn(item -> item.quantity).setHeader("QUANTIDADE").setFlexGrow(1);
            pane1.add(this.grid);

            dom.horizontalLayout(footer -> {
                footer.setWidthFull();
                footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
                footer.getStyle().set("padding-top", "12px");

                dom.span(label -> {
                    label.setText("VALOR TOTAL: ");
                    label.getStyle().set("font-weight", "bold");
                });

                dom.span(label -> {
                    this.totalElm = label;
                    this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.receipt.total));
                    this.totalElm.getStyle().set("font-size", "18px")
                            .set("font-weight", "bold")
                            .set("color", "var(--lumo-primary-color)");
                    this.totalOldValue = this.state.receipt.total;
                });
            });
        });

        dom.button(button -> {
            button.setText("VOLTAR");
            button.setIcon(VaadinIcon.ARROW_LEFT.create());
            button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> safeAction("Open products", this.presenter::onOpenProducts));
        });
    }
}
