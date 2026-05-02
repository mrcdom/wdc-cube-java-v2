package br.com.wdc.shopping.view.swing.impl;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeViewState;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.ResourceCatalog;
import br.com.wdc.shopping.view.swing.util.StackPanel;
import br.com.wdc.shopping.view.swing.util.Styles;

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

    public HomeViewSwing(ShoppingSwingApplication app, HomePresenter presenter) {
        super("home", app, presenter, new JPanel(new BorderLayout()));
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
            initialRender();
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
            this.productsPanelSlot.removeAll();
            this.productsPanelSlot.add(ppv.getElement());
            this.productsPanelSlot.revalidate();
            this.productsPanelSlot.repaint();
        }

        // Update purchases panel
        if (this.state.purchasesPanelView instanceof AbstractViewSwing<?> ppv
                && ppv.getElement().getParent() != this.purchasesPanelSlot) {
            this.purchasesPanelSlot.removeAll();
            this.purchasesPanelSlot.add(ppv.getElement());
            this.purchasesPanelSlot.revalidate();
            this.purchasesPanelSlot.repaint();
        }

        // Update content slot
        var newContentView = this.state.contentView instanceof AbstractViewSwing<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentPane.removeAll();
            if (newContentView != null) {
                this.contentPane.add(newContentView.getElement());
            } else {
                this.contentPane.add(this.defaultContentPane);
            }
            this.currentContentView = newContentView;
            this.contentPane.revalidate();
            this.contentPane.repaint();
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

    private void initialRender() {
        this.element.setBackground(Styles.BG_PAGE);

        // Header
        var header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBackground(Styles.BG_HEADER);
        header.setBorder(new EmptyBorder(8, 16, 8, 16));
        header.setPreferredSize(new Dimension(0, 56));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        var logo = new JLabel(ResourceCatalog.getScaledImage("images/logo.png", -1, 30));
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
        header.add(logo);
        header.add(Box.createHorizontalGlue());

        this.nickNameElm = new JLabel("Bem-vindo, " + this.state.nickName + "!");
        this.nickNameElm.setForeground(Styles.FG_WHITE_DIM);
        this.nickNameElm.setFont(Styles.FONT_DEFAULT);
        this.nickNameElm.setAlignmentY(0.5f);
        this.nickNameOldValue = this.state.nickName;
        header.add(this.nickNameElm);
        header.add(Box.createRigidArea(new Dimension(10, 0)));

        // Cart button
        var cartBtn = new JPanel();
        cartBtn.setLayout(new BoxLayout(cartBtn, BoxLayout.X_AXIS));
        cartBtn.setOpaque(false);
        cartBtn.setAlignmentY(0.5f);
        cartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cartBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                safeAction("Open cart", presenter::onOpenCart);
            }
        });

        var cartIcon = new JLabel(ResourceCatalog.getScaledImage("images/carrinho.png", 24, 24));
        cartIcon.setAlignmentY(0.5f);
        cartBtn.add(cartIcon);
        cartBtn.add(Box.createRigidArea(new Dimension(6, 0)));

        var cartLabel = new JLabel("Carrinho");
        cartLabel.setForeground(Styles.FG_WHITE);
        cartLabel.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 13));
        cartLabel.setAlignmentY(0.5f);
        cartBtn.add(cartLabel);
        cartBtn.add(Box.createRigidArea(new Dimension(4, 0)));

        this.cartCountElm = new JLabel("[" + this.state.cartItemCount + "]");
        this.cartCountElm.setForeground(Styles.FG_RED);
        this.cartCountElm.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 13));
        this.cartCountElm.setAlignmentY(0.5f);
        this.cartCountOldValue = this.state.cartItemCount;
        cartBtn.add(this.cartCountElm);
        header.add(cartBtn);
        header.add(Box.createRigidArea(new Dimension(10, 0)));

        var exitBtn = new JButton("SAIR");
        Styles.styleHeaderButton(exitBtn);
        exitBtn.setAlignmentY(0.5f);
        exitBtn.addActionListener(_ -> safeAction("Exit", this.presenter::onExit));
        header.add(exitBtn);

        this.element.add(header, BorderLayout.NORTH);

        // Error label
        this.errorElm = new JLabel();
        Styles.styleErrorLabel(this.errorElm);
        this.errorElm.setVisible(false);

        // Default content (products + purchases side by side)
        this.defaultContentPane = new JPanel(new BorderLayout(16, 0));
        this.defaultContentPane.setOpaque(false);

        this.productsPanelSlot = new StackPanel();
        this.defaultContentPane.add(this.productsPanelSlot, BorderLayout.CENTER);

        this.purchasesPanelSlot = new StackPanel();
        this.purchasesPanelSlot.setPreferredSize(new Dimension(230, 0));
        this.defaultContentPane.add(this.purchasesPanelSlot, BorderLayout.EAST);

        // Content pane wrapped in scroll
        this.contentPane = new StackPanel();
        this.contentPane.setBorder(new EmptyBorder(16, 16, 16, 16));
        this.contentPane.add(this.defaultContentPane);

        var scrollPane = new JScrollPane(this.contentPane);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(Styles.BG_PAGE);

        // Center panel holds error + scroll
        var center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(this.errorElm, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);

        this.element.add(center, BorderLayout.CENTER);
    }
}
