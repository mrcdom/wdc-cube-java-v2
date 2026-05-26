package br.com.wdc.shopping.view.swing.impl;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeViewState;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.ResourceCatalog;
import br.com.wdc.shopping.view.swing.util.StackPanel;
import br.com.wdc.shopping.view.swing.util.Styles;
import br.com.wdc.shopping.view.swing.util.SwingDom;
import br.com.wdc.shopping.view.swing.util.SwingUtils;

public class HomeViewSwing extends AbstractViewSwing<HomePresenter> {

    private final HomeViewState state;

    private boolean notRendered = true;
    private JLabel nickNameElm;
    private String nickNameOldValue;
    private JLabel cartCountElm;
    private int cartCountOldValue;
    private StackPanel contentPane;
    private JPanel defaultContentPane;
    private StackPanel productsPanelSlot;
    private StackPanel purchasesPanelSlot;
    private AbstractViewSwing<?> currentContentView;
    private JLabel errorElm;

    public HomeViewSwing(HomePresenter presenter) {
        super("home", (ShoppingSwingApplication) presenter.app, presenter, new JPanel(new BorderLayout()));
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.nickNameElm = null;
        this.nickNameOldValue = null;
        this.cartCountElm = null;
        this.cartCountOldValue = 0;
        this.contentPane = null;
        this.defaultContentPane = null;
        this.productsPanelSlot = null;
        this.purchasesPanelSlot = null;
        this.currentContentView = null;
        this.errorElm = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            SwingDom.render(this.element, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.nickNameOldValue, this.state.nickName)) {
            this.nickNameElm.setText("Bem-vindo, " + this.state.nickName + "!");
            this.nickNameOldValue = this.state.nickName;
        }

        if (this.cartCountOldValue != this.state.cartItemCount) {
            this.cartCountElm.setText("[" + this.state.cartItemCount + "]");
            this.cartCountOldValue = this.state.cartItemCount;
        }

        // Update products panel
        if (this.state.productsPanelView instanceof AbstractViewSwing<?> ppv
                && ppv.getElement().getParent() != this.productsPanelSlot) {
            SwingUtils.replaceContent(this.productsPanelSlot, ppv.getElement());
        }

        // Update purchases panel
        if (this.state.purchasesPanelView instanceof AbstractViewSwing<?> ppv
                && ppv.getElement().getParent() != this.purchasesPanelSlot) {
            SwingUtils.replaceContent(this.purchasesPanelSlot, ppv.getElement());
        }

        // Update content slot
        var newContentView = this.state.contentView instanceof AbstractViewSwing<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            SwingUtils.replaceContent(this.contentPane,
                    newContentView != null ? newContentView.getElement() : this.defaultContentPane);
            this.currentContentView = newContentView;
            // Purchases panel only visible on home (no content overlay)
            this.purchasesPanelSlot.setVisible(newContentView == null);
        }

        // Error
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

    @SuppressWarnings("unused")
    private void initialRender(SwingDom dom, JPanel pane0) {
        pane0.setBackground(Styles.BG_PAGE);

        // Header
        dom.constraints(BorderLayout.NORTH).hbox(header -> {
            header.setOpaque(true);
            header.setBackground(Styles.BG_HEADER);
            header.setBorder(new EmptyBorder(8, 16, 8, 16));
            header.setPreferredSize(new Dimension(0, 56));
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

            dom.img(logo -> {
                logo.setIcon(ResourceCatalog.getScaledImage("images/logo.png", -1, 30));
                logo.setAlignmentY(0.5f);
                if (this.app.isDevMode()) {
                    logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    logo.setToolTipText("Dev: Reconstruir todas as views");
                    logo.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            safeAction("Rebuild all views", app::rebuildAllViews);
                        }
                    });
                }
            });

            dom.hSpacer();

            dom.label(label -> {
                this.nickNameElm = label;
                label.setText("Bem-vindo, " + this.state.nickName + "!");
                label.setForeground(Styles.FG_WHITE_DIM);
                label.setFont(Styles.FONT_DEFAULT);
                label.setAlignmentY(0.5f);
                this.nickNameOldValue = this.state.nickName;
            });

            dom.hSpacer(10);

            // Cart button
            dom.hbox(cartBtn -> {
                cartBtn.setAlignmentY(0.5f);
                cartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                cartBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        safeAction("Open cart", presenter::onOpenCart);
                    }
                });

                dom.img(img -> {
                    img.setIcon(ResourceCatalog.getScaledImage("images/carrinho.png", 24, 24));
                    img.setAlignmentY(0.5f);
                });

                dom.hSpacer(6);

                dom.label(label -> {
                    label.setText("Carrinho");
                    label.setForeground(Styles.FG_WHITE);
                    label.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 13));
                    label.setAlignmentY(0.5f);
                });

                dom.hSpacer(4);

                dom.label(label -> {
                    this.cartCountElm = label;
                    label.setText("[" + this.state.cartItemCount + "]");
                    label.setForeground(Styles.FG_RED);
                    label.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 13));
                    label.setAlignmentY(0.5f);
                    this.cartCountOldValue = this.state.cartItemCount;
                });
            });

            dom.hSpacer(10);

            dom.button(exitBtn -> {
                exitBtn.setText("SAIR");
                Styles.styleHeaderButton(exitBtn);
                exitBtn.setAlignmentY(0.5f);
                exitBtn.addActionListener(_ignored -> safeAction("Exit", this.presenter::onExit));
            });
        });

        // Center: error + scroll + purchases
        dom.constraints(BorderLayout.CENTER).borderPane(center -> {
            // Error
            dom.constraints(BorderLayout.NORTH).label(errorLbl -> {
                this.errorElm = errorLbl;
                Styles.styleErrorLabel(errorLbl);
                errorLbl.setVisible(false);
            });

            // Products in scroll (CENTER)
            dom.constraints(BorderLayout.CENTER).scrollPane(scroll -> {
                scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                scroll.getViewport().setOpaque(true);
                scroll.getViewport().setBackground(Styles.BG_PAGE);

                // defaultContentPane wraps productsPanelSlot (for cart/receipt switching)
                this.defaultContentPane = new JPanel(new BorderLayout());
                this.defaultContentPane.setOpaque(false);
                this.productsPanelSlot = new StackPanel();
                this.defaultContentPane.add(this.productsPanelSlot, BorderLayout.CENTER);

                this.contentPane = new StackPanel();
                this.contentPane.setBorder(new EmptyBorder(16, 16, 16, 16));
                this.contentPane.add(this.defaultContentPane);
                scroll.setViewportView(this.contentPane);
            });

            // Purchases panel outside scroll (EAST) — fixed height, not scrollable
            dom.constraints(BorderLayout.EAST).stackPane(slot -> {
                this.purchasesPanelSlot = (StackPanel) slot;
                slot.setPreferredSize(new Dimension(230, 0));
            });
        });
    }
}
