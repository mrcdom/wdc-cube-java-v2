package br.com.wdc.shopping.view.swing.impl;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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

    public CartViewSwing(ShoppingSwingApplication app, CartPresenter presenter) {
        super("cart", app, presenter, new JPanel());
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
            initialRender();
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

    private void initialRender() {
        this.element.setOpaque(true);
        this.element.setBackground(Styles.BG_WHITE);
        this.element.setBorder(Styles.BORDER_EMPTY_24);
        this.element.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));

        // Logo pane
        var logoPnl = new JPanel();
        logoPnl.setLayout(new BoxLayout(logoPnl, BoxLayout.X_AXIS));
        logoPnl.setOpaque(false);
        logoPnl.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        var cartIcon = new JLabel(ResourceCatalog.getScaledImage("images/carrinho.png", 24, 24));
        logoPnl.add(cartIcon);
        logoPnl.add(Box.createRigidArea(new Dimension(8, 0)));
        logoPnl.add(new JLabel("Carrinho"));
        logoPnl.add(Box.createRigidArea(new Dimension(4, 0)));
        this.itemSizeElm = new JLabel("[" + this.state.items.size() + "]");
        this.itemSizeOldValue = this.state.items.size();
        logoPnl.add(this.itemSizeElm);
        logoPnl.add(Box.createHorizontalGlue());
        this.element.add(logoPnl);
        this.element.add(Box.createRigidArea(new Dimension(0, 12)));

        var titleLbl = new JLabel("LISTA DE PRODUTOS");
        titleLbl.setFont(Styles.FONT_TITLE);
        titleLbl.setForeground(Styles.FG_TEXT_LIGHT);
        titleLbl.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        this.element.add(titleLbl);
        this.element.add(Box.createRigidArea(new Dimension(0, 12)));

        // Content panel with border
        var content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createLineBorder(Styles.BORDER_LIGHT, 1));
        content.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        // Table header
        var headerRow = new JPanel(new GridBagLayout());
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

        var h1 = new JLabel("ITEM");
        h1.setFont(Styles.FONT_TABLE_HEADER);
        h1.setForeground(Styles.FG_TEXT);
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        headerRow.add(h1, gbc);

        var h2 = new JLabel("VALOR");
        h2.setFont(Styles.FONT_TABLE_HEADER);
        h2.setForeground(Styles.FG_TEXT);
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.ipadx = 80;
        headerRow.add(h2, gbc);

        var h3 = new JLabel("QUANTIDADE");
        h3.setFont(Styles.FONT_TABLE_HEADER);
        h3.setForeground(Styles.FG_TEXT);
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
        footer.setBackground(Styles.BG_TABLE_FOOTER);
        footer.setOpaque(true);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Styles.BORDER_LIGHT),
                new EmptyBorder(12, 16, 12, 16)));
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        footer.add(Box.createHorizontalGlue());
        var totalLabel = new JLabel("VALOR TOTAL: ");
        totalLabel.setFont(Styles.FONT_TABLE_HEADER);
        totalLabel.setForeground(Styles.FG_TEXT);
        footer.add(totalLabel);
        this.totalCostElm = new JLabel(this.formatCurrency(this.computeTotalCost()));
        this.totalCostElm.setFont(Styles.FONT_TABLE_HEADER);
        this.totalCostElm.setForeground(Styles.FG_TEXT_DARK);
        this.totalCostOldValue = this.computeTotalCost();
        footer.add(this.totalCostElm);
        content.add(footer);

        this.element.add(content);
        this.element.add(Box.createRigidArea(new Dimension(0, 12)));

        // Error
        this.errorElm = new JLabel();
        Styles.styleErrorLabel(this.errorElm);
        this.errorElm.setVisible(false);
        this.errorElm.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        this.element.add(this.errorElm);
        this.element.add(Box.createRigidArea(new Dimension(0, 12)));

        // Buttons row
        var btnRow = new JPanel();
        btnRow.setLayout(new BoxLayout(btnRow, BoxLayout.X_AXIS));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        var backBtn = new JButton("< VOLTAR");
        Styles.styleOutlineButton(backBtn, Styles.FG_PRIMARY);
        backBtn.addActionListener(_ -> safeAction("Open products", this.presenter::onOpenProducts));
        btnRow.add(backBtn);

        btnRow.add(Box.createHorizontalGlue());

        var buyBtn = new JButton("FINALIZAR PEDIDO");
        Styles.styleOrangeButton(buyBtn);
        buyBtn.addActionListener(_ -> safeAction("Buy", this.presenter::onBuy));
        btnRow.add(buyBtn);
        this.element.add(btnRow);
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
