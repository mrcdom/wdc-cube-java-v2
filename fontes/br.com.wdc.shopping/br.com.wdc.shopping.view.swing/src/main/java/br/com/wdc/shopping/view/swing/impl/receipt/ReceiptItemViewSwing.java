package br.com.wdc.shopping.view.swing.impl.receipt;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.Styles;
import br.com.wdc.shopping.view.swing.util.SwingDom;

public class ReceiptItemViewSwing extends AbstractViewSwing<ReceiptPresenter> {

    private ReceiptItem state;

    private boolean notRendered = true;
    private JLabel descriptionElm;
    private String descriptionOldValue;
    private JLabel priceElm;
    private double priceOldValue;
    private JLabel quantityElm;
    private int quantityOldValue;

    public ReceiptItemViewSwing(ShoppingSwingApplication app, ReceiptPresenter presenter, int idx) {
        super("receipt-item-" + idx, app, presenter, new JPanel());
    }

    public void setState(ReceiptItem state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.descriptionElm = null;
        this.descriptionOldValue = null;
        this.priceElm = null;
        this.priceOldValue = 0;
        this.quantityElm = null;
        this.quantityOldValue = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            SwingDom.render(this.element, this::initialRender);
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

    private void initialRender(SwingDom dom, JPanel pane0) {
        dom.gridBagPane(grid -> {
            grid.setOpaque(false);
            grid.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER_SUBTLE),
                    new EmptyBorder(6, 0, 6, 0)));
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            var gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(0, 0, 0, 8);

            gbc.gridx = 0; gbc.weightx = 1.0;
            dom.constraints(gbc.clone()).label(lbl -> {
                this.descriptionElm = lbl;
                lbl.setText(this.state.description);
                lbl.setFont(Styles.FONT_RECEIPT_MONO);
                this.descriptionOldValue = this.state.description;
            });

            gbc.gridx = 1; gbc.weightx = 0; gbc.ipadx = 80;
            dom.constraints(gbc.clone()).label(lbl -> {
                this.priceElm = lbl;
                lbl.setText(NumberFormat.getCurrencyInstance().format(this.state.value));
                lbl.setFont(Styles.FONT_RECEIPT_MONO);
                this.priceOldValue = this.state.value;
            });

            gbc.gridx = 2; gbc.ipadx = 40;
            dom.constraints(gbc.clone()).label(lbl -> {
                this.quantityElm = lbl;
                lbl.setText(String.valueOf(this.state.quantity));
                lbl.setFont(Styles.FONT_RECEIPT_MONO);
                this.quantityOldValue = this.state.quantity;
            });
        });
    }
}
