package br.com.wdc.shopping.view.swing.impl.home;

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
import br.com.wdc.shopping.view.swing.util.SwingDom;

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
		super("product-item-" + idx, app, presenter, new JPanel());
		this.element.setLayout(new BoxLayout(this.element, BoxLayout.Y_AXIS));
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
			SwingDom.render(this.element, this::initialRender);
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

	private void initialRender(SwingDom dom, JPanel pane0) {
		pane0.setOpaque(true);
		pane0.setBackground(Styles.BG_WHITE);
		pane0.setPreferredSize(new Dimension(210, 240));
		pane0.setMaximumSize(new Dimension(210, 240));
		pane0.setBorder(CARD_BORDER_NORMAL);
		pane0.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		pane0.addHierarchyListener(this.hierarchyListener);
		pane0.addMouseListener(this.cardMouseListener);

		// Image pane
		dom.gridBagPane(imagePane -> {
			imagePane.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
			imagePane.setOpaque(true);
			imagePane.setBackground(Styles.BG_WHITE);
			imagePane.setBorder(new EmptyBorder(12, 8, 8, 8));
			imagePane.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			imagePane.addMouseListener(this.cardClickForwarder);

			dom.img(img -> {
				this.imageElm = img;
				img.setIcon(ResourceCatalog.getScaledImage(this.state.image, 180, 140));
				img.addMouseListener(this.cardClickForwarder);
				this.imageOldValue = this.state.image;
			});
		});

		// Label group
		dom.vbox(labelGroup -> {
			labelGroup.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
			labelGroup.setOpaque(true);
			labelGroup.setBackground(Styles.BG_WHITE);
			labelGroup.setBorder(new EmptyBorder(8, 12, 12, 12));
			labelGroup.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			labelGroup.addMouseListener(this.cardClickForwarder);

			dom.label(name -> {
				this.nameElm = name;
				name.setText(this.state.name);
				name.setFont(Styles.FONT_DEFAULT);
				name.setForeground(Styles.FG_TEXT_DARK);
				name.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
				name.addMouseListener(this.cardClickForwarder);
				this.nameOldValue = this.state.name;
			});

			dom.vSpacer(4);

			dom.label(price -> {
				this.priceElm = price;
				price.setText("R$ " + NumberFormat.getInstance().format(this.state.price));
				price.setFont(Styles.FONT_SMALL_BOLD);
				price.setForeground(Styles.FG_WHITE);
				price.setOpaque(true);
				price.setBackground(Styles.PRICE_BADGE);
				price.setBorder(new EmptyBorder(3, 10, 3, 10));
				price.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
				price.addMouseListener(this.cardClickForwarder);
				this.priceOldValue = this.state.price;
			});
		});
	}

	private static final javax.swing.border.Border CARD_BORDER_NORMAL =
			BorderFactory.createCompoundBorder(new EmptyBorder(1, 1, 1, 1), Styles.createCardBorder());

	private final java.awt.event.HierarchyListener hierarchyListener = e -> {
		if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && element.isShowing()) {
			element.setBorder(CARD_BORDER_NORMAL);
		}
	};

	private final java.awt.event.MouseAdapter cardMouseListener = new java.awt.event.MouseAdapter() {
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
	};

	private final java.awt.event.MouseAdapter cardClickForwarder = new java.awt.event.MouseAdapter() {
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
			var p = javax.swing.SwingUtilities.convertPoint((java.awt.Component) e.getSource(), e.getPoint(), element);
			if (!element.contains(p)) {
				element.setBorder(CARD_BORDER_NORMAL);
			}
		}
	};

}
