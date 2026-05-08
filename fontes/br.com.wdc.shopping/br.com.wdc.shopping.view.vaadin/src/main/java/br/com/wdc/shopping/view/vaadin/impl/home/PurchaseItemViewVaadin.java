package br.com.wdc.shopping.view.vaadin.impl.home;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class PurchaseItemViewVaadin extends AbstractViewVaadin<PurchasesPanelPresenter> {

    private PurchaseInfo state;

    private boolean notRendered = true;
    private Span idElm;
    private long idOldValue;
    private Span dateElm;
    private long dateOldValue;
    private Span itemsElm;
    private String itemsOldValue;
    private Span totalElm;
    private double totalOldValue;

    public PurchaseItemViewVaadin(ShoppingVaadinApplication app, PurchasesPanelPresenter presenter, int idx) {
        super("purchase-item-" + idx, app, presenter, new VerticalLayout());
    }

    public void setState(PurchaseInfo state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (this.idOldValue != this.state.id) {
            this.idElm.setText("#" + this.state.id);
            this.idOldValue = this.state.id;
        }

        if (this.dateOldValue != this.state.date) {
            this.dateElm.setText(this.getDateStr());
            this.dateOldValue = this.state.date;
        }

        var itemsNewValue = this.getItemsStr();
        if (!Objects.equals(this.itemsOldValue, itemsNewValue)) {
            this.itemsElm.setText(itemsNewValue);
            this.itemsOldValue = itemsNewValue;
        }

        if (this.totalOldValue != this.state.total) {
            this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.total));
            this.totalOldValue = this.state.total;
        }
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("purchase-item");
        pane0.setPadding(false);
        pane0.setSpacing(false);
        pane0.addClickListener(e -> safeAction("Open receipt", () -> this.presenter.onOpenReceipt(this.state.id)));

        // Line 1: #id + date
        dom.horizontalLayout(pane1 -> {
            pane1.addClassName("order-pnl");
            pane1.setPadding(false);
            pane1.setSpacing(false);
            pane1.setWidthFull();
            pane1.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
            pane1.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            dom.span(label -> {
                this.idElm = label;
                this.idElm.setText("#" + this.state.id);
                this.idOldValue = this.state.id;
            });
            dom.span(label -> {
                this.dateElm = label;
                this.dateElm.setText(this.getDateStr());
                this.dateElm.getStyle().set("color", "var(--lumo-tertiary-text-color)").set("font-size", "0.7rem").set("font-weight", "normal");
                this.dateOldValue = this.state.date;
            });
        });

        // Line 2: items + total
        dom.horizontalLayout(pane1 -> {
            pane1.addClassName("order-info");
            pane1.setPadding(false);
            pane1.setSpacing(false);
            pane1.setWidthFull();
            pane1.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.BASELINE);
            pane1.getStyle().set("gap", "4px");
            dom.span(label -> {
                this.itemsElm = label;
                this.itemsElm.setText(this.getItemsStr());
                this.itemsElm.getStyle()
                        .set("flex", "1")
                        .set("min-width", "0")
                        .set("overflow", "hidden")
                        .set("text-overflow", "ellipsis")
                        .set("white-space", "nowrap");
                this.itemsOldValue = this.itemsElm.getText();
            });
            dom.span(label -> {
                this.totalElm = label;
                this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.total));
                this.totalElm.getStyle().set("font-weight", "bold").set("white-space", "nowrap").set("color", "var(--lumo-body-text-color)");
                this.totalOldValue = this.state.total;
            });
        });
    }

    private String getDateStr() {
        var date = Instant.ofEpochMilli(this.state.date).atZone(ZoneId.systemDefault()).toLocalDate();
        return date.toString();
    }

    private String getItemsStr() {
        if (this.state.items == null || this.state.items.isEmpty()) return "";
        if (this.state.items.size() == 1) return this.state.items.get(0);
        return this.state.items.get(0) + ", +" + (this.state.items.size() - 1) + "...";
    }
}
