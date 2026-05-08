package br.com.wdc.shopping.view.swing.impl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

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
import br.com.wdc.shopping.view.swing.util.SwingDom;

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
    private JPanel contentBox;

    public PurchasesPanelViewSwing(PurchasesPanelPresenter presenter) {
        super("purchases-panel", (ShoppingSwingApplication) presenter.app, presenter, new JPanel(new BorderLayout()));
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
        this.contentBox = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            SwingDom.render(this.element, this::initialRender);
            this.notRendered = false;
        }
        if (this.state.pageSize < 0 && this.contentBox != null) {
            javax.swing.SwingUtilities.invokeLater(this::computePageSize);
        }
        this.contentSlot.accept(this.state.purchases, this.viewList);

        int pageSize = Math.max(1, this.state.pageSize);
        int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / pageSize));
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

    private void initialRender(SwingDom dom, JPanel pane0) {
        pane0.setOpaque(true);
        pane0.setBackground(Styles.BG_PURCHASES_PANEL);
        pane0.setBorder(new EmptyBorder(12, 12, 12, 12));
        pane0.setPreferredSize(new Dimension(230, 0));
        pane0.setMinimumSize(new Dimension(230, 0));
        pane0.setMaximumSize(new Dimension(230, Integer.MAX_VALUE));

        dom.constraints(BorderLayout.NORTH).label(caption -> {
            caption.setText("Seu histórico de compras");
            caption.setForeground(Styles.FG_WHITE);
            caption.setFont(Styles.FONT_SMALL_BOLD);
            caption.setBorder(new EmptyBorder(0, 0, 12, 0));
        });

        dom.constraints(BorderLayout.CENTER).vbox(contentBox -> {
            this.contentBox = contentBox;
            contentBox.setBorder(new EmptyBorder(0, 0, 0, 0));
            this.contentSlot = this.newListSlot(contentBox, this::newItemView, this::updateItem);
            contentBox.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    scheduleResize();
                }
            });
        });

        // Pagination
        dom.constraints(BorderLayout.SOUTH).hbox(pagination -> {
            pagination.setBorder(new EmptyBorder(10, 0, 0, 0));
            this.paginationPanel = pagination;

            dom.button(prevBtn -> {
                this.prevButton = prevBtn;
                prevBtn.setText("\u25C0");
                prevBtn.setFont(Styles.FONT_SMALL);
                prevBtn.setForeground(Styles.FG_WHITE);
                prevBtn.setBackground(new Color(255, 255, 255, 38));
                prevBtn.setFocusPainted(false);
                prevBtn.setBorderPainted(false);
                prevBtn.setOpaque(true);
                prevBtn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                prevBtn.addActionListener(_ignored -> safeAction("Previous page", () -> this.presenter.onPageChange(this.state.page - 1)));
            });

            dom.hSpacer();

            dom.label(pageInfo -> {
                this.pageInfoElm = pageInfo;
                pageInfo.setForeground(Styles.FG_WHITE);
                pageInfo.setFont(Styles.FONT_SMALL);
            });

            dom.hSpacer();

            dom.button(nextBtn -> {
                this.nextButton = nextBtn;
                nextBtn.setText("\u25B6");
                nextBtn.setFont(Styles.FONT_SMALL);
                nextBtn.setForeground(Styles.FG_WHITE);
                nextBtn.setBackground(new Color(255, 255, 255, 38));
                nextBtn.setFocusPainted(false);
                nextBtn.setBorderPainted(false);
                nextBtn.setOpaque(true);
                nextBtn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                nextBtn.addActionListener(_ignored -> safeAction("Next page", () -> this.presenter.onPageChange(this.state.page + 1)));
            });
        });
    }

    private PurchaseItemViewSwing newItemView() {
        return new PurchaseItemViewSwing(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(PurchaseItemViewSwing itemView, PurchaseInfo state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }

    private static final int ITEM_HEIGHT_PX = 130;

    private javax.swing.Timer resizeTimer;

    private void scheduleResize() {
        if (this.resizeTimer == null) {
            this.resizeTimer = new javax.swing.Timer(150, e -> computePageSize());
            this.resizeTimer.setRepeats(false);
        }
        this.resizeTimer.restart();
    }

    private void computePageSize() {
        if (this.contentBox == null) return;
        int containerHeight = this.contentBox.getHeight();
        if (containerHeight <= 0) return;
        int itemHeight = ITEM_HEIGHT_PX;
        if (!this.viewList.isEmpty()) {
            int measured = this.viewList.get(0).getElement().getHeight();
            if (measured > 0) itemHeight = measured;
        }
        int capacity = Math.max(1, containerHeight / itemHeight);
        this.presenter.onItemSizeCapacityChanged(capacity);
    }
}
