package br.com.wdc.shopping.view.swing.impl.cart;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.ResourceCatalog;
import br.com.wdc.shopping.view.swing.util.Styles;
import br.com.wdc.shopping.view.swing.util.SwingDom;

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
        super("cart-item-" + idx, app, presenter, new JPanel());
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
            SwingDom.render(this.element, this::initialRender);
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

	private void initialRender(SwingDom dom, JPanel pane0) {
        dom.gridBagPane(grid -> {
            grid.setOpaque(false);
            grid.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.BORDER_SUBTLE),
                    new EmptyBorder(10, 16, 10, 16)));
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            var gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(0, 0, 0, 8);

            // Cell 1: image + name
            gbc.gridx = 0; gbc.weightx = 1.0;
            dom.constraints(gbc.clone()).hbox(cell1 -> {
                dom.img(img -> img.setIcon(ResourceCatalog.getScaledImage(this.state.image, 42, 40)));

                dom.hSpacer(8);

                dom.label(name -> {
                    this.nameElm = name;
                    name.setText(this.state.name);
                    name.setFont(Styles.FONT_DEFAULT);
                    this.nameOldValue = this.state.name;
                });
            });

            // Cell 2: price
            gbc.gridx = 1; gbc.weightx = 0; gbc.ipadx = 80;
            dom.constraints(gbc.clone()).label(lbl -> {
                this.priceElm = lbl;
                lbl.setText("R$ " + NumberFormat.getInstance().format(this.state.price));
                lbl.setFont(Styles.FONT_DEFAULT);
                lbl.setForeground(Styles.FG_TEXT);
                this.priceOldValue = this.state.price;
            });

            // Cell 3: quantity + delete
            gbc.gridx = 2; gbc.ipadx = 40;
            dom.constraints(gbc.clone()).hbox(cell3 -> {
                dom.label(qty -> {
                    this.quantityElm = qty;
                    qty.setText(String.valueOf(this.state.quantity));
                    qty.setFont(Styles.FONT_DEFAULT);
                    this.quantityOldValue = this.state.quantity;
                });

                dom.hSpacer(12);

                dom.img(deleteIcon -> {
                    deleteIcon.setIcon(ResourceCatalog.getScaledImage("images/delet.png", 18, 18));
                    deleteIcon.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                    deleteIcon.setToolTipText("Remover item");
                    deleteIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            safeAction("Remove product", () -> presenter.onRemoveProduct(CartItemViewSwing.this.state.id));
                        }
                    });
                });
            });
        });
    }
}
