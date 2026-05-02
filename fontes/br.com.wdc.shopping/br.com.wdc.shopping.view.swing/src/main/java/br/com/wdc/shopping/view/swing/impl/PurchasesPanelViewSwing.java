package br.com.wdc.shopping.view.swing.impl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.impl.home.PurchaseItemViewSwing;
import br.com.wdc.shopping.view.swing.util.Styles;

public class PurchasesPanelViewSwing extends AbstractViewSwing<PurchasesPanelPresenter> {

    private final PurchasesPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<PurchaseItemViewSwing> viewList = new ArrayList<>();
    private BiConsumer<List<PurchaseInfo>, List<PurchaseItemViewSwing>> contentSlot;
    private JLabel pageInfoElm;
    private JButton prevButton;
    private JButton nextButton;
    private JPanel paginationPanel;

    public PurchasesPanelViewSwing(ShoppingSwingApplication app, PurchasesPanelPresenter presenter) {
        super("purchases-panel", app, presenter, new JPanel(new BorderLayout()));
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.itemIdx = 0;
        this.viewList.clear();
        this.contentSlot = null;
        this.pageInfoElm = null;
        this.prevButton = null;
        this.nextButton = null;
        this.paginationPanel = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }
        this.contentSlot.accept(this.state.purchases, this.viewList);

        int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / this.state.pageSize));
        this.pageInfoElm.setText((this.state.page + 1) + " / " + totalPages);

        boolean hasPrev = this.state.page > 0;
        this.prevButton.setEnabled(hasPrev);
        this.prevButton.setOpaque(hasPrev);
        this.prevButton.setForeground(hasPrev ? Styles.FG_WHITE : new Color(0, 0, 0, 0));

        boolean hasNext = this.state.page < totalPages - 1;
        this.nextButton.setEnabled(hasNext);
        this.nextButton.setOpaque(hasNext);
        this.nextButton.setForeground(hasNext ? Styles.FG_WHITE : new Color(0, 0, 0, 0));

        this.paginationPanel.setVisible(totalPages > 1);
    }

    private void initialRender() {
        this.element.setOpaque(true);
        this.element.setBackground(Styles.BG_PURCHASES_PANEL);
        this.element.setBorder(new EmptyBorder(12, 12, 12, 12));
        this.element.setPreferredSize(new Dimension(230, 0));
        this.element.setMinimumSize(new Dimension(230, 0));
        this.element.setMaximumSize(new Dimension(230, Integer.MAX_VALUE));

        var caption = new JLabel("Seu histórico de compras");
        caption.setForeground(Styles.FG_WHITE);
        caption.setFont(Styles.FONT_SMALL_BOLD);
        caption.setBorder(new EmptyBorder(0, 0, 12, 0));
        this.element.add(caption, BorderLayout.NORTH);

        var contentBox = new JPanel();
        contentBox.setLayout(new BoxLayout(contentBox, BoxLayout.Y_AXIS));
        contentBox.setOpaque(false);
        contentBox.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.contentSlot = this.newListSlot(contentBox, this::newItemView, this::updateItem);
        this.element.add(contentBox, BorderLayout.CENTER);

        // Pagination — use BorderLayout to guarantee all 3 components fit
        var pagination = new JPanel(new BorderLayout(4, 0));
        pagination.setOpaque(false);
        pagination.setBorder(new EmptyBorder(10, 0, 0, 0));

        this.prevButton = new JButton("\u25C0");
        this.prevButton.setFont(Styles.FONT_SMALL);
        this.prevButton.setForeground(Styles.FG_WHITE);
        this.prevButton.setBackground(new Color(255, 255, 255, 38));
        this.prevButton.setFocusPainted(false);
        this.prevButton.setBorderPainted(false);
        this.prevButton.setOpaque(true);
        this.prevButton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        this.prevButton.addActionListener(_ -> safeAction("Previous page", () -> this.presenter.onPageChange(this.state.page - 1)));
        pagination.add(this.prevButton, BorderLayout.WEST);

        this.pageInfoElm = new JLabel("", javax.swing.SwingConstants.CENTER);
        this.pageInfoElm.setForeground(Styles.FG_WHITE);
        this.pageInfoElm.setFont(Styles.FONT_SMALL);
        pagination.add(this.pageInfoElm, BorderLayout.CENTER);

        this.nextButton = new JButton("\u25B6");
        this.nextButton.setFont(Styles.FONT_SMALL);
        this.nextButton.setForeground(Styles.FG_WHITE);
        this.nextButton.setBackground(new Color(255, 255, 255, 38));
        this.nextButton.setFocusPainted(false);
        this.nextButton.setBorderPainted(false);
        this.nextButton.setOpaque(true);
        this.nextButton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        this.nextButton.addActionListener(_ -> safeAction("Next page", () -> this.presenter.onPageChange(this.state.page + 1)));
        pagination.add(this.nextButton, BorderLayout.EAST);

        this.paginationPanel = pagination;
        this.element.add(pagination, BorderLayout.SOUTH);
    }

    private PurchaseItemViewSwing newItemView() {
        return new PurchaseItemViewSwing(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(PurchaseItemViewSwing itemView, PurchaseInfo state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }
}
