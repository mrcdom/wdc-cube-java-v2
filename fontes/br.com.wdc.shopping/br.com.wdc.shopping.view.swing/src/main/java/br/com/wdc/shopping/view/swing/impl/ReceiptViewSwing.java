package br.com.wdc.shopping.view.swing.impl;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.impl.receipt.ReceiptItemViewSwing;
import br.com.wdc.shopping.view.swing.util.Styles;

public class ReceiptViewSwing extends AbstractViewSwing<ReceiptPresenter> {

    private final ReceiptViewState state;

    private boolean notRendered = true;
    private JLabel notifySuccessElm;
    private JLabel totalElm;
    private double totalOldValue;
    private List<ReceiptItemViewSwing> receiptItemViewList = new ArrayList<>();
    private BiConsumer<List<ReceiptItem>, List<ReceiptItemViewSwing>> itemsSlot;
    private int itemIdx;

    public ReceiptViewSwing(ShoppingSwingApplication app, ReceiptPresenter presenter) {
        super("receipt", app, presenter, new JPanel());
        this.element.setLayout(new BoxLayout(this.element, BoxLayout.Y_AXIS));
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.notifySuccessElm = null;
        this.totalElm = null;
        this.totalOldValue = 0;
        this.receiptItemViewList.clear();
        this.itemsSlot = null;
        this.itemIdx = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        var notifySuccessDisplay = false;
        if (this.state.notifySuccess) {
            notifySuccessDisplay = true;
            this.state.notifySuccess = false;
        }
        if (this.notifySuccessElm.isVisible() != notifySuccessDisplay) {
            this.notifySuccessElm.setVisible(notifySuccessDisplay);
        }

        if (this.totalOldValue != this.state.receipt.total) {
            this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.receipt.total));
            this.totalOldValue = this.state.receipt.total;
        }

        this.itemsSlot.accept(this.state.receipt.items, this.receiptItemViewList);
    }

    private void initialRender() {
        this.element.setOpaque(true);
        this.element.setBackground(Styles.BG_WHITE);
        this.element.setBorder(Styles.BORDER_EMPTY_24);
        this.element.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));

        // Success notification
        this.notifySuccessElm = new JLabel("COMPRA EFETUADA COM SUCESSO");
        Styles.styleSuccessLabel(this.notifySuccessElm);
        this.notifySuccessElm.setVisible(this.state.notifySuccess);
        this.notifySuccessElm.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        this.element.add(this.notifySuccessElm);
        this.element.add(Box.createRigidArea(new Dimension(0, 12)));

        // Title
        var titleLbl = new JLabel("IMPRIMA SEU RECIBO:");
        titleLbl.setFont(Styles.FONT_TITLE);
        titleLbl.setForeground(Styles.FG_TEXT);
        titleLbl.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        this.element.add(titleLbl);
        this.element.add(Box.createRigidArea(new Dimension(0, 12)));

        // Content panel
        var content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.BORDER_LIGHT, 1),
                new EmptyBorder(12, 12, 12, 12)));
        content.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        var caption1 = new JLabel("WEDOCODE SHOPPING - SUA COMPRA CERTA NA INTERNET");
        caption1.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 13));
        caption1.setForeground(Styles.FG_TEXT);
        caption1.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        content.add(caption1);

        var caption2 = new JLabel("Recibo de compra");
        caption2.setFont(Styles.FONT_RECEIPT_MONO);
        caption2.setForeground(Styles.FG_TEXT_LIGHT);
        caption2.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        content.add(caption2);
        content.add(Box.createRigidArea(new Dimension(0, 8)));

        // Table header
        var headerRow = new JPanel(new GridBagLayout());
        headerRow.setOpaque(false);
        headerRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER_LIGHT));
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        headerRow.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 8);

        var h1 = new JLabel("ITEM");
        h1.setFont(Styles.FONT_RECEIPT_MONO);
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        headerRow.add(h1, gbc);

        var h2 = new JLabel("VALOR");
        h2.setFont(Styles.FONT_RECEIPT_MONO);
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.ipadx = 80;
        headerRow.add(h2, gbc);

        var h3 = new JLabel("QUANTIDADE");
        h3.setFont(Styles.FONT_RECEIPT_MONO);
        gbc.gridx = 2;
        gbc.ipadx = 40;
        headerRow.add(h3, gbc);

        content.add(headerRow);

        // Items container
        var tbody = new JPanel();
        tbody.setLayout(new BoxLayout(tbody, BoxLayout.Y_AXIS));
        tbody.setOpaque(false);
        this.itemsSlot = this.newListSlot(tbody, this::newItemView, this::updateItem);
        content.add(tbody);

        // Footer
        var footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.X_AXIS));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Styles.BORDER_LIGHT),
                new EmptyBorder(12, 0, 4, 0)));
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        footer.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        var totalLabel = new JLabel("VALOR TOTAL: ");
        totalLabel.setFont(Styles.FONT_RECEIPT_MONO);
        footer.add(totalLabel);

        this.totalElm = new JLabel(NumberFormat.getCurrencyInstance().format(this.state.receipt.total));
        this.totalElm.setFont(Styles.FONT_RECEIPT_MONO);
        this.totalOldValue = this.state.receipt.total;
        footer.add(this.totalElm);
        content.add(footer);

        this.element.add(content);
        this.element.add(Box.createRigidArea(new Dimension(0, 12)));

        // Back button
        var backBtn = new JButton("< VOLTAR");
        Styles.styleOutlineButton(backBtn, Styles.FG_PRIMARY);
        backBtn.setAlignmentX(JButton.LEFT_ALIGNMENT);
        backBtn.addActionListener(_ -> safeAction("Open products", this.presenter::onOpenProducts));
        this.element.add(backBtn);
    }

    private ReceiptItemViewSwing newItemView() {
        return new ReceiptItemViewSwing(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ReceiptItemViewSwing itemView, ReceiptItem state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }
}
