package br.com.wdc.shopping.view.swing.impl;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.impl.cart.CartItemViewSwing;
import br.com.wdc.shopping.view.swing.util.ResourceCatalog;
import br.com.wdc.shopping.view.swing.util.Styles;
import br.com.wdc.shopping.view.swing.util.SwingDom;

public class CartViewSwing extends AbstractViewSwing<CartPresenter> {

    private final CartViewState state;

    private boolean notRendered = true;
    private List<CartItemViewSwing> cartItemViewList = new ArrayList<>();
    private BiConsumer<List<CartItem>, List<CartItemViewSwing>> itemsSlot;
    private int itemIdx;
    private JLabel itemSizeElm;
    private int itemSizeOldValue;
    private JLabel totalCostElm;
    private double totalCostOldValue;
    private JLabel errorElm;

    public CartViewSwing(CartPresenter presenter) {
        super("cart", (ShoppingSwingApplication) presenter.app, presenter, new JPanel());
        this.element.setLayout(new BoxLayout(this.element, BoxLayout.Y_AXIS));
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.cartItemViewList.clear();
        this.itemsSlot = null;
        this.itemIdx = 0;
        this.itemSizeElm = null;
        this.itemSizeOldValue = 0;
        this.totalCostElm = null;
        this.totalCostOldValue = 0;
        this.errorElm = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            SwingDom.render(this.element, this::initialRender);
            this.notRendered = false;
        }

        if (this.itemSizeOldValue != this.state.items.size()) {
            this.itemSizeElm.setText("[" + this.state.items.size() + "]");
            this.itemSizeOldValue = this.state.items.size();
        }

        this.itemsSlot.accept(this.state.items, this.cartItemViewList);

        var totalCostNewValue = this.computeTotalCost();
        if (totalCostNewValue != this.totalCostOldValue) {
            this.totalCostElm.setText(this.formatCurrency(totalCostNewValue));
            this.totalCostOldValue = totalCostNewValue;
        }

        var newErrorDisplay = false;
        var newErrorMessage = "";
        if (this.state.errorCode != 0) {
            newErrorDisplay = true;
            newErrorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        }
        if (!Objects.equals(this.errorElm.getText(), newErrorMessage)) {
            this.errorElm.setText(newErrorMessage);
        }
        if (this.errorElm.isVisible() != newErrorDisplay) {
            this.errorElm.setVisible(newErrorDisplay);
        }
    }

    private void initialRender(SwingDom dom, JPanel pane0) {
        pane0.setOpaque(true);
        pane0.setBackground(Styles.BG_WHITE);
        pane0.setBorder(Styles.BORDER_EMPTY_24);
        pane0.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));

        // Logo pane
        dom.hbox(logoPnl -> {
            logoPnl.setAlignmentX(Component.LEFT_ALIGNMENT);

            dom.img(img -> img.setIcon(ResourceCatalog.getScaledImage("images/carrinho.png", 24, 24)));
            dom.hSpacer(8);
            dom.label(lbl -> lbl.setText("Carrinho"));
            dom.hSpacer(4);
            dom.label(lbl -> {
                this.itemSizeElm = lbl;
                lbl.setText("[" + this.state.items.size() + "]");
                this.itemSizeOldValue = this.state.items.size();
            });
            dom.hSpacer();
        });

        dom.vSpacer(12);

        dom.label(titleLbl -> {
            titleLbl.setText("LISTA DE PRODUTOS");
            titleLbl.setFont(Styles.FONT_TITLE);
            titleLbl.setForeground(Styles.FG_TEXT_LIGHT);
            titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        });

        dom.vSpacer(12);

        // Content panel with border
        dom.vbox(content -> {
            content.setOpaque(true);
            content.setBorder(BorderFactory.createLineBorder(Styles.BORDER_LIGHT, 1));
            content.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Table header
            dom.gridBagPane(headerRow -> {
                headerRow.setBackground(Styles.BG_TABLE_HEADER);
                headerRow.setOpaque(true);
                headerRow.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER_LIGHT),
                        new EmptyBorder(12, 16, 12, 16)));
                headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

                var gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(0, 0, 0, 8);

                gbc.gridx = 0; gbc.weightx = 1.0;
                dom.constraints(gbc.clone()).label(h1 -> {
                    h1.setText("ITEM");
                    h1.setFont(Styles.FONT_TABLE_HEADER);
                    h1.setForeground(Styles.FG_TEXT);
                });

                gbc.gridx = 1; gbc.weightx = 0; gbc.ipadx = 80;
                dom.constraints(gbc.clone()).label(h2 -> {
                    h2.setText("VALOR");
                    h2.setFont(Styles.FONT_TABLE_HEADER);
                    h2.setForeground(Styles.FG_TEXT);
                });

                gbc.gridx = 2; gbc.ipadx = 40;
                dom.constraints(gbc.clone()).label(h3 -> {
                    h3.setText("QUANTIDADE");
                    h3.setFont(Styles.FONT_TABLE_HEADER);
                    h3.setForeground(Styles.FG_TEXT);
                });
            });

            // Items container
            dom.vbox(tbody -> this.itemsSlot = this.newListSlot(tbody, this::newItemView, this::updateItem));

            // Footer
            dom.hbox(footer -> {
                footer.setBackground(Styles.BG_TABLE_FOOTER);
                footer.setOpaque(true);
                footer.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Styles.BORDER_LIGHT),
                        new EmptyBorder(12, 16, 12, 16)));
                footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

                dom.hSpacer();
                dom.label(lbl -> {
                    lbl.setText("VALOR TOTAL: ");
                    lbl.setFont(Styles.FONT_TABLE_HEADER);
                    lbl.setForeground(Styles.FG_TEXT);
                });
                dom.label(lbl -> {
                    this.totalCostElm = lbl;
                    lbl.setText(this.formatCurrency(this.computeTotalCost()));
                    lbl.setFont(Styles.FONT_TABLE_HEADER);
                    lbl.setForeground(Styles.FG_TEXT_DARK);
                    this.totalCostOldValue = this.computeTotalCost();
                });
            });
        });

        dom.vSpacer(12);

        // Error
        dom.label(errorLabel -> {
            this.errorElm = errorLabel;
            Styles.styleErrorLabel(errorLabel);
            errorLabel.setVisible(false);
            errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        });

        dom.vSpacer(12);

        // Buttons row
        dom.hbox(btnRow -> {
            btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            dom.button(backBtn -> {
                backBtn.setText("< VOLTAR");
                Styles.styleOutlineButton(backBtn, Styles.FG_PRIMARY);
                backBtn.addActionListener(_ignored -> safeAction("Open products", this.presenter::onOpenProducts));
            });

            dom.hSpacer();

            dom.button(buyBtn -> {
                buyBtn.setText("FINALIZAR PEDIDO");
                Styles.styleOrangeButton(buyBtn);
                buyBtn.addActionListener(_ignored -> safeAction("Buy", this.presenter::onBuy));
            });
        });
    }

    private double computeTotalCost() {
        return this.state.items.stream().mapToDouble(v -> v.price * v.quantity).sum();
    }

    private String formatCurrency(double value) {
        return "R$ " + NumberFormat.getInstance().format(value);
    }

    private CartItemViewSwing newItemView() {
        return new CartItemViewSwing(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(CartItemViewSwing itemView, CartItem state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }
}
