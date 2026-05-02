package br.com.wdc.shopping.view.swing.impl.cart;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.ResourceCatalog;
import br.com.wdc.shopping.view.swing.util.Styles;

public class CartItemViewSwing extends AbstractViewSwing<CartPresenter> {

    private CartItem state;

    private boolean notRendered = true;
    private JLabel nameElm;
    private String nameOldValue;
    private JLabel priceElm;
    private double priceOldValue;
    private JLabel quantityElm;
    private int quantityOldValue;

    public CartItemViewSwing(ShoppingSwingApplication app, CartPresenter presenter, int idx) {
        super("cart-item-" + idx, app, presenter, new JPanel(new GridBagLayout()));
    }

    public void setState(CartItem state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.nameElm = null;
        this.nameOldValue = null;
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

        if (!Objects.equals(this.nameOldValue, this.state.name)) {
            this.nameElm.setText(this.state.name);
            this.nameOldValue = this.state.name;
        }

        if (this.priceOldValue != this.state.price) {
            this.priceElm.setText("R$ " + NumberFormat.getInstance().format(this.state.price));
            this.priceOldValue = this.state.price;
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
                new EmptyBorder(10, 16, 10, 16)));
        this.element.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 8);

        // Cell 1: image + name
        var cell1 = new JPanel();
        cell1.setLayout(new BoxLayout(cell1, BoxLayout.X_AXIS));
        cell1.setOpaque(false);

        var img = new JLabel(ResourceCatalog.getScaledImage(this.state.image, 42, 40));
        cell1.add(img);
        cell1.add(Box.createRigidArea(new Dimension(8, 0)));

        this.nameElm = new JLabel(this.state.name);
        this.nameElm.setFont(Styles.FONT_DEFAULT);
        this.nameOldValue = this.state.name;
        cell1.add(this.nameElm);

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        this.element.add(cell1, gbc);

        // Cell 2: price
        this.priceElm = new JLabel("R$ " + NumberFormat.getInstance().format(this.state.price));
        this.priceElm.setFont(Styles.FONT_DEFAULT);
        this.priceElm.setForeground(Styles.FG_TEXT);
        this.priceOldValue = this.state.price;
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.ipadx = 80;
        this.element.add(this.priceElm, gbc);

        // Cell 3: quantity + delete
        var cell3 = new JPanel();
        cell3.setLayout(new BoxLayout(cell3, BoxLayout.X_AXIS));
        cell3.setOpaque(false);

        this.quantityElm = new JLabel(String.valueOf(this.state.quantity));
        this.quantityElm.setFont(Styles.FONT_DEFAULT);
        this.quantityOldValue = this.state.quantity;
        cell3.add(this.quantityElm);
        cell3.add(Box.createRigidArea(new Dimension(12, 0)));

        var deleteIcon = new JLabel(ResourceCatalog.getScaledImage("images/delet.png", 18, 18));
        deleteIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteIcon.setToolTipText("Remover item");
        deleteIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                safeAction("Remove product", () -> presenter.onRemoveProduct(CartItemViewSwing.this.state.id));
            }
        });
        cell3.add(deleteIcon);

        gbc.gridx = 2;
        gbc.ipadx = 40;
        this.element.add(cell3, gbc);
    }
}
