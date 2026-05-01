package br.com.wdc.shopping.view.vaadin.impl.home;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import com.vaadin.flow.component.button.ButtonVariant;
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

        dom.horizontalLayout(pane1 -> {
            pane1.addClassName("order-pnl");
            pane1.setPadding(false);
            pane1.setSpacing(false);
            dom.span(label -> label.setText("Compra"));
            dom.span(label -> {
                this.idElm = label;
                this.idElm.setText("#" + this.state.id);
                this.idOldValue = this.state.id;
            });
        });

        dom.verticalLayout(pane1 -> {
            pane1.addClassName("order-info");
            pane1.setPadding(false);
            pane1.setSpacing(false);
            dom.span(label -> label.setText("Data da compra:"));
            dom.span(label -> {
                this.dateElm = label;
                this.dateElm.setText(this.getDateStr());
                this.dateOldValue = this.state.date;
            });
            dom.span(label -> label.setText("Itens adquiridos:"));
            dom.span(label -> {
                this.itemsElm = label;
                this.itemsElm.setText(this.getItemsStr());
                this.itemsOldValue = this.itemsElm.getText();
            });

            dom.horizontalLayout(pane2 -> {
                pane2.addClassName("order-total");
                pane2.setPadding(false);
                pane2.setSpacing(false);
                dom.span(text -> text.setText("Valor Total: "));
                dom.span(text -> {
                    this.totalElm = text;
                    this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.total));
                    this.totalOldValue = this.state.total;
                });
            });

            dom.button(button -> {
                button.setText("DETALHES");
                button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
                button.getStyle().set("align-self", "flex-end").set("margin-top", "var(--lumo-space-xs)");
                button.addClickListener(e -> safeAction("Open receipt", () -> this.presenter.onOpenReceipt(this.state.id)));
            });
        });
    }

    private String getDateStr() {
        var date = Instant.ofEpochMilli(this.state.date).atZone(ZoneId.systemDefault()).toLocalDate();
        return date.toString();
    }

    private String getItemsStr() {
        if (this.state.items == null || this.state.items.isEmpty()) return "";
        return String.join("; ", this.state.items);
    }
}
