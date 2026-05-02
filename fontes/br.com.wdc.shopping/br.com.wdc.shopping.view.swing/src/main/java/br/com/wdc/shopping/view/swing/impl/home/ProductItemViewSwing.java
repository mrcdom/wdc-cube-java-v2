package br.com.wdc.shopping.view.swing.impl.home;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.ResourceCatalog;
import br.com.wdc.shopping.view.swing.util.Styles;

public class ProductItemViewSwing extends AbstractViewSwing<ProductsPanelPresenter> {

    private ProductInfo state;

    private boolean notRendered = true;
    private JLabel imageElm;
    private String imageOldValue;
    private JLabel nameElm;
    private String nameOldValue;
    private JLabel priceElm;
    private double priceOldValue;

    public ProductItemViewSwing(ShoppingSwingApplication app, ProductsPanelPresenter presenter, int idx) {
        super("product-item-" + idx, app, presenter, new JPanel(new BorderLayout()));
    }

    public void setState(ProductInfo state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.imageElm = null;
        this.imageOldValue = null;
        this.nameElm = null;
        this.nameOldValue = null;
        this.priceElm = null;
        this.priceOldValue = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        if (!Objects.equals(this.imageOldValue, this.state.image)) {
            this.imageElm.setIcon(ResourceCatalog.getScaledImage(this.state.image, 180, 140));
            this.imageOldValue = this.state.image;
        }

        if (!Objects.equals(this.nameOldValue, this.state.name)) {
            this.nameElm.setText(this.state.name);
            this.nameOldValue = this.state.name;
        }

        if (this.priceOldValue != this.state.price) {
            this.priceElm.setText("R$ " + NumberFormat.getInstance().format(this.state.price));
            this.priceOldValue = this.state.price;
        }
    }

    private static final javax.swing.border.Border CARD_BORDER_NORMAL = BorderFactory.createCompoundBorder(
            new EmptyBorder(1, 1, 1, 1),
            Styles.createCardBorder());

    private void initialRender() {
        this.element.setOpaque(true);
        this.element.setBackground(Styles.BG_WHITE);
        this.element.setPreferredSize(new Dimension(210, 240));
        this.element.setMaximumSize(new Dimension(210, 240));
        this.element.setBorder(CARD_BORDER_NORMAL);
        this.element.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Reset hover border when card becomes visible again (e.g. returning from product detail)
        this.element.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && element.isShowing()) {
                element.setBorder(CARD_BORDER_NORMAL);
            }
        });

        this.element.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                element.setBorder(CARD_BORDER_NORMAL);
                safeAction("Open product", () -> presenter.onOpenProduct(ProductItemViewSwing.this.state.id));
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                element.setBorder(Styles.createCardHoverBorder());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                var p = javax.swing.SwingUtilities.convertPoint(element, e.getPoint(), element);
                if (!element.contains(p)) {
                    element.setBorder(CARD_BORDER_NORMAL);
                }
            }
        });

        // Mouse forwarding: child panels forward clicks to the card element
        var cardClickForwarder = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                element.setBorder(CARD_BORDER_NORMAL);
                safeAction("Open product", () -> presenter.onOpenProduct(ProductItemViewSwing.this.state.id));
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                element.setBorder(Styles.createCardHoverBorder());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                // Only reset if mouse truly left the card (not just moved between children)
                var p = javax.swing.SwingUtilities.convertPoint(
                        (java.awt.Component) e.getSource(), e.getPoint(), element);
                if (!element.contains(p)) {
                    element.setBorder(CARD_BORDER_NORMAL);
                }
            }
        };

        // Image pane
        var imagePane = new JPanel(new java.awt.GridBagLayout());
        imagePane.setOpaque(true);
        imagePane.setBackground(Styles.BG_WHITE);
        imagePane.setBorder(new EmptyBorder(12, 8, 8, 8));
        imagePane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        imagePane.addMouseListener(cardClickForwarder);

        this.imageElm = new JLabel(ResourceCatalog.getScaledImage(this.state.image, 180, 140));
        this.imageElm.addMouseListener(cardClickForwarder);
        this.imageOldValue = this.state.image;
        imagePane.add(this.imageElm);
        this.element.add(imagePane, BorderLayout.CENTER);

        // Label group
        var labelGroup = new JPanel();
        labelGroup.setLayout(new BoxLayout(labelGroup, BoxLayout.Y_AXIS));
        labelGroup.setOpaque(true);
        labelGroup.setBackground(Styles.BG_WHITE);
        labelGroup.setBorder(new EmptyBorder(8, 12, 12, 12));
        labelGroup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        labelGroup.addMouseListener(cardClickForwarder);

        this.nameElm = new JLabel(this.state.name);
        this.nameElm.setFont(Styles.FONT_DEFAULT);
        this.nameElm.setForeground(Styles.FG_TEXT_DARK);
        this.nameElm.addMouseListener(cardClickForwarder);
        this.nameOldValue = this.state.name;
        labelGroup.add(this.nameElm);
        labelGroup.add(javax.swing.Box.createRigidArea(new Dimension(0, 4)));

        this.priceElm = new JLabel("R$ " + NumberFormat.getInstance().format(this.state.price));
        this.priceElm.setFont(Styles.FONT_SMALL_BOLD);
        this.priceElm.setForeground(Styles.FG_WHITE);
        this.priceElm.setOpaque(true);
        this.priceElm.setBackground(Styles.PRICE_BADGE);
        this.priceElm.setBorder(new EmptyBorder(3, 10, 3, 10));
        this.priceElm.addMouseListener(cardClickForwarder);
        this.priceOldValue = this.state.price;
        labelGroup.add(this.priceElm);

        this.element.add(labelGroup, BorderLayout.SOUTH);
    }
}
