package br.com.wdc.shopping.view.vaadin.impl.receipt;

import java.text.NumberFormat;
import java.util.Objects;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class ReceiptItemViewVaadin extends AbstractViewVaadin<ReceiptPresenter> {

    private ReceiptItem state;

    private boolean notRendered = true;
    private Span descriptionElm;
    private String descriptionOldValue;
    private Span priceElm;
    private double priceOldValue;
    private Span quantityElm;
    private int quantityOldValue;

    public ReceiptItemViewVaadin(ShoppingVaadinApplication app, ReceiptPresenter presenter, int idx) {
        super("receipt-item-" + idx, app, presenter, new HorizontalLayout());
    }

    public void setState(ReceiptItem state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((HorizontalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.descriptionOldValue, this.state.description)) {
            this.descriptionElm.setText(this.state.description);
            this.descriptionOldValue = this.state.description;
        }

        if (this.priceOldValue != this.state.value) {
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.state.value));
            this.priceOldValue = this.state.value;
        }

        if (this.quantityOldValue != this.state.quantity) {
            this.quantityElm.setText(String.valueOf(this.state.quantity));
            this.quantityOldValue = this.state.quantity;
        }
    }

    private void initialRender(VaadinDom dom, HorizontalLayout pane0) {
        pane0.addClassName("receipt-row");
        pane0.setWidthFull();
        pane0.setAlignItems(FlexComponent.Alignment.CENTER);

        dom.span(label -> {
            label.addClassName("cell-1");
            this.descriptionElm = label;
            this.descriptionElm.setText(this.state.description);
            this.descriptionOldValue = this.state.description;
        });

        dom.span(label -> {
            label.addClassName("cell-2");
            this.priceElm = label;
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.state.value));
            this.priceOldValue = this.state.value;
        });

        dom.span(label -> {
            label.addClassName("cell-3");
            this.quantityElm = label;
            this.quantityElm.setText(String.valueOf(this.state.quantity));
            this.quantityOldValue = this.state.quantity;
        });
    }
}
