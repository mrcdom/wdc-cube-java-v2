package br.com.wdc.shopping.view.swing.impl;

import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.impl.receipt.ReceiptItemViewSwing;
import br.com.wdc.shopping.view.swing.util.Styles;
import br.com.wdc.shopping.view.swing.util.SwingDom;

public class ReceiptViewSwing extends AbstractViewSwing<ReceiptPresenter> {

    private final ReceiptViewState state;

    private boolean notRendered = true;
    private JLabel notifySuccessElm;
    private JLabel totalElm;
    private double totalOldValue;
    private List<ReceiptItemViewSwing> receiptItemViewList = new ArrayList<>();
    private BiConsumer<List<ReceiptItem>, List<ReceiptItemViewSwing>> itemsSlot;
    private int itemIdx;

    public ReceiptViewSwing(ReceiptPresenter presenter) {
        super("receipt", (ShoppingSwingApplication) presenter.app, presenter, new JPanel());
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
            SwingDom.render(this.element, this::initialRender);
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

    private void initialRender(SwingDom dom, JPanel pane0) {
        pane0.setOpaque(true);
        pane0.setBackground(Styles.BG_WHITE);
        pane0.setBorder(Styles.BORDER_EMPTY_24);
        pane0.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));

        // Success notification
        dom.label(lbl -> {
            this.notifySuccessElm = lbl;
            Styles.styleSuccessLabel(lbl);
            lbl.setVisible(this.state.notifySuccess);
            lbl.setText("COMPRA EFETUADA COM SUCESSO");
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        });

        dom.vSpacer(12);

        dom.label(titleLbl -> {
            titleLbl.setText("IMPRIMA SEU RECIBO:");
            titleLbl.setFont(Styles.FONT_TITLE);
            titleLbl.setForeground(Styles.FG_TEXT);
            titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        });

        dom.vSpacer(12);

        // Content panel
        dom.vbox(content -> {
            content.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Styles.BORDER_LIGHT, 1),
                    new EmptyBorder(12, 12, 12, 12)));
            content.setAlignmentX(Component.LEFT_ALIGNMENT);

            dom.label(caption1 -> {
                caption1.setText("WEDOCODE SHOPPING - SUA COMPRA CERTA NA INTERNET");
                caption1.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 13));
                caption1.setForeground(Styles.FG_TEXT);
                caption1.setAlignmentX(Component.LEFT_ALIGNMENT);
            });

            dom.label(caption2 -> {
                caption2.setText("Recibo de compra");
                caption2.setFont(Styles.FONT_RECEIPT_MONO);
                caption2.setForeground(Styles.FG_TEXT_LIGHT);
                caption2.setAlignmentX(Component.LEFT_ALIGNMENT);
            });

            dom.vSpacer(8);

            // Table header
            dom.gridBagPane(headerRow -> {
                headerRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER_LIGHT));
                headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

                var gbc = new java.awt.GridBagConstraints();
                gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gbc.anchor = java.awt.GridBagConstraints.WEST;
                gbc.insets = new java.awt.Insets(0, 0, 0, 8);

                gbc.gridx = 0;
                gbc.weightx = 1.0;
                dom.constraints(gbc.clone()).label(h1 -> {
                    h1.setText("ITEM");
                    h1.setFont(Styles.FONT_RECEIPT_MONO);
                });

                gbc.gridx = 1;
                gbc.weightx = 0;
                gbc.ipadx = 80;
                dom.constraints(gbc.clone()).label(h2 -> {
                    h2.setText("VALOR");
                    h2.setFont(Styles.FONT_RECEIPT_MONO);
                });

                gbc.gridx = 2;
                gbc.ipadx = 40;
                dom.constraints(gbc.clone()).label(h3 -> {
                    h3.setText("QUANTIDADE");
                    h3.setFont(Styles.FONT_RECEIPT_MONO);
                });
            });

            // Items container
            dom.vbox(tbody -> this.itemsSlot = this.newListSlot(tbody, this::newItemView, this::updateItem));

            // Footer
            dom.hbox(footer -> {
                footer.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Styles.BORDER_LIGHT),
                        new EmptyBorder(12, 0, 4, 0)));
                footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
                footer.setAlignmentX(Component.LEFT_ALIGNMENT);

                dom.label(lbl -> {
                    lbl.setText("VALOR TOTAL: ");
                    lbl.setFont(Styles.FONT_RECEIPT_MONO);
                });
                dom.label(lbl -> {
                    this.totalElm = lbl;
                    lbl.setText(NumberFormat.getCurrencyInstance().format(this.state.receipt.total));
                    lbl.setFont(Styles.FONT_RECEIPT_MONO);
                    this.totalOldValue = this.state.receipt.total;
                });
            });
        });

        dom.vSpacer(12);

        // Back button
        dom.button(backBtn -> {
            backBtn.setText("< VOLTAR");
            Styles.styleOutlineButton(backBtn, Styles.FG_PRIMARY);
            backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            backBtn.addActionListener(_ignored -> safeAction("Open products", this.presenter::onOpenProducts));
        });
    }

    private ReceiptItemViewSwing newItemView() {
        return new ReceiptItemViewSwing(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ReceiptItemViewSwing itemView, ReceiptItem state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }
}
