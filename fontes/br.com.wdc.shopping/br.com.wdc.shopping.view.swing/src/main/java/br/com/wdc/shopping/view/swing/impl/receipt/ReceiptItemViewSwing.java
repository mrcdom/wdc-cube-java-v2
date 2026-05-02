package br.com.wdc.shopping.view.swing.impl.receipt;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
        super("receipt-item-" + idx, app, presenter, new JPanel(new GridBagLayout()));
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
            initialRender();
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

    private void initialRender() {
        this.element.setOpaque(false);
        this.element.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER_SUBTLE),
                new EmptyBorder(6, 0, 6, 0)));
        this.element.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 8);

        this.descriptionElm = new JLabel(this.state.description);
        this.descriptionElm.setFont(Styles.FONT_RECEIPT_MONO);
        this.descriptionOldValue = this.state.description;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        this.element.add(this.descriptionElm, gbc);

        this.priceElm = new JLabel(NumberFormat.getCurrencyInstance().format(this.state.value));
        this.priceElm.setFont(Styles.FONT_RECEIPT_MONO);
        this.priceOldValue = this.state.value;
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.ipadx = 80;
        this.element.add(this.priceElm, gbc);

        this.quantityElm = new JLabel(String.valueOf(this.state.quantity));
        this.quantityElm.setFont(Styles.FONT_RECEIPT_MONO);
        this.quantityOldValue = this.state.quantity;
        gbc.gridx = 2;
        gbc.ipadx = 40;
        this.element.add(this.quantityElm, gbc);
    }
}
