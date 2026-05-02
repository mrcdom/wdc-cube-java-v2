package br.com.wdc.shopping.view.swing.impl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.NumberFormat;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jsoup.Jsoup;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductViewState;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.ResourceCatalog;
import br.com.wdc.shopping.view.swing.util.Styles;

public class ProductViewSwing extends AbstractViewSwing<ProductPresenter> {

    private static final Logger LOG = LoggerFactory.getLogger(ProductViewSwing.class);

    private final ProductViewState state;

    private boolean notRendered = true;
    private JLabel breadcrumbName;
    private JLabel nameElm;
    private String nameOldValue;
    private JLabel imageElm;
    private String imageOldValue;
    private JLabel priceElm;
    private double priceOldValue;
    private JTextField quantityElm;
    private JTextPane descriptionElm;
    private String descriptionOldValue;
    private JLabel errorElm;

    public ProductViewSwing(ShoppingSwingApplication app, ProductPresenter presenter) {
        super("product", app, presenter, new JPanel());
        this.element.setLayout(new BoxLayout(this.element, BoxLayout.Y_AXIS));
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.breadcrumbName = null;
        this.nameElm = null;
        this.nameOldValue = null;
        this.imageElm = null;
        this.imageOldValue = null;
        this.priceElm = null;
        this.priceOldValue = 0;
        this.quantityElm = null;
        this.descriptionElm = null;
        this.descriptionOldValue = null;
        this.errorElm = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        if (!Objects.equals(this.nameOldValue, this.state.product.name)) {
            this.breadcrumbName.setText(this.state.product.name);
            this.nameElm.setText(this.state.product.name);
            this.nameOldValue = this.state.product.name;
        }

        if (!Objects.equals(this.imageOldValue, this.state.product.image)) {
            this.imageElm.setIcon(ResourceCatalog.getScaledImage(this.state.product.image, 300, 300));
            this.imageOldValue = this.state.product.image;
        }

        if (this.priceOldValue != this.state.product.price) {
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.state.product.price));
            this.priceOldValue = this.state.product.price;
        }

        if (!Objects.equals(this.descriptionOldValue, this.state.product.description)) {
            renderHtml(this.descriptionElm, this.state.product.description);
            this.descriptionOldValue = this.state.product.description;
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

        // Breadcrumbs
        var breadcrumbs = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        breadcrumbs.setOpaque(false);
        breadcrumbs.setBorder(new EmptyBorder(0, 0, 12, 0));
        breadcrumbs.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        var bLabel = new JLabel("Produtos");
        bLabel.setFont(Styles.FONT_SUBTITLE);
        bLabel.setForeground(Styles.FG_PRIMARY);
        bLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        bLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                safeAction("Open products", presenter::onOpenProducts);
            }
        });
        breadcrumbs.add(bLabel);
        var bSep = new JLabel(">");
        bSep.setFont(Styles.FONT_SUBTITLE);
        bSep.setForeground(Styles.FG_TEXT_SUBTLE);
        breadcrumbs.add(bSep);
        this.breadcrumbName = new JLabel(this.state.product.name);
        this.breadcrumbName.setFont(Styles.FONT_SUBTITLE);
        this.breadcrumbName.setForeground(Styles.FG_TEXT);
        breadcrumbs.add(this.breadcrumbName);
        this.element.add(breadcrumbs);

        // Main content: image left, details right
        var mainContent = new JPanel(new BorderLayout(16, 0));
        mainContent.setOpaque(false);
        mainContent.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        // Left: image
        var leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);

        var imagePane = new JPanel(new java.awt.GridBagLayout());
        imagePane.setOpaque(true);
        imagePane.setBackground(Styles.BG_FIELD);
        imagePane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.BORDER_LIGHT, 1),
                new EmptyBorder(16, 16, 16, 16)));
        imagePane.setPreferredSize(new Dimension(340, 340));

        this.imageElm = new JLabel(ResourceCatalog.getScaledImage(this.state.product.image, 300, 300));
        this.imageOldValue = this.state.product.image;
        imagePane.add(this.imageElm);
        leftCol.add(imagePane);

        mainContent.add(leftCol, BorderLayout.WEST);

        // Right: product details (use BorderLayout so description wraps to available width)
        var rightCol = new JPanel(new BorderLayout());
        rightCol.setOpaque(false);
        rightCol.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top section: name, price, quantity
        var topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        this.nameElm = new JLabel(this.state.product.name);
        this.nameElm.setFont(Styles.FONT_PRODUCT_NAME);
        this.nameElm.setForeground(Styles.FG_TEXT_DARK);
        this.nameElm.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        this.nameOldValue = this.state.product.name;
        topSection.add(this.nameElm);
        topSection.add(Box.createRigidArea(new Dimension(0, 8)));

        // Price + quantity row
        var priceRow = new JPanel();
        priceRow.setLayout(new BoxLayout(priceRow, BoxLayout.X_AXIS));
        priceRow.setOpaque(false);
        priceRow.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        this.priceElm = new JLabel(NumberFormat.getCurrencyInstance().format(this.state.product.price));
        this.priceElm.setFont(Styles.FONT_PRODUCT_PRICE);
        this.priceElm.setForeground(Styles.FG_PRIMARY);
        this.priceOldValue = this.state.product.price;
        priceRow.add(this.priceElm);
        priceRow.add(Box.createHorizontalGlue());
        var qtyLabel = new JLabel("Quantidade:");
        qtyLabel.setFont(Styles.FONT_DEFAULT);
        qtyLabel.setForeground(Styles.FG_TEXT_LIGHT);
        priceRow.add(qtyLabel);
        priceRow.add(Box.createRigidArea(new Dimension(6, 0)));

        this.quantityElm = new JTextField("1", 4);
        Styles.styleField(this.quantityElm);
        this.quantityElm.setMaximumSize(new Dimension(60, 34));
        this.quantityElm.setPreferredSize(new Dimension(60, 34));
        this.quantityElm.setHorizontalAlignment(JTextField.CENTER);
        priceRow.add(this.quantityElm);
        topSection.add(priceRow);
        topSection.add(Box.createRigidArea(new Dimension(0, 12)));

        // Description title
        var descTitle = new JLabel("DESCRIÇÃO DO PRODUTO");
        descTitle.setFont(Styles.FONT_DESC_TITLE);
        descTitle.setForeground(Styles.FG_TEXT);
        descTitle.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        descTitle.setBorder(new EmptyBorder(8, 0, 4, 0));
        topSection.add(descTitle);

        rightCol.add(topSection, BorderLayout.NORTH);

        // Description in CENTER — BorderLayout gives it the actual available width
        this.descriptionElm = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true; // force text wrapping to viewport width
            }
        };
        this.descriptionElm.setEditable(false);
        this.descriptionElm.setOpaque(false);
        renderHtml(this.descriptionElm, this.state.product.description);
        this.descriptionOldValue = this.state.product.description;

        var descScroll = new javax.swing.JScrollPane(this.descriptionElm);
        descScroll.setBorder(null);
        descScroll.setOpaque(false);
        descScroll.getViewport().setOpaque(false);
        descScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descScroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rightCol.add(descScroll, BorderLayout.CENTER);

        // Bottom section: error + buttons
        var bottomSection = new JPanel();
        bottomSection.setLayout(new BoxLayout(bottomSection, BoxLayout.Y_AXIS));
        bottomSection.setOpaque(false);
        bottomSection.setBorder(new EmptyBorder(12, 0, 0, 0));

        // Error
        this.errorElm = new JLabel();
        Styles.styleErrorLabel(this.errorElm);
        this.errorElm.setVisible(false);
        this.errorElm.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        bottomSection.add(this.errorElm);
        bottomSection.add(Box.createRigidArea(new Dimension(0, 12)));

        // Buttons row: back + buy
        var btnRow = new JPanel();
        btnRow.setLayout(new BoxLayout(btnRow, BoxLayout.X_AXIS));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        var backBtn = new JButton("< VOLTAR");
        Styles.styleOutlineButton(backBtn, Styles.FG_PRIMARY);
        backBtn.addActionListener(_ -> safeAction("Open products", this.presenter::onOpenProducts));
        btnRow.add(backBtn);
        btnRow.add(Box.createHorizontalGlue());

        var buyBtn = new JButton("Adicionar ao carrinho");
        Styles.styleOrangeButton(buyBtn);
        buyBtn.setFont(Styles.FONT_BUTTON_LARGE);
        buyBtn.addActionListener(_ -> safeAction("Add to cart", () -> {
            var quantity = 1;
            try {
                quantity = Integer.parseInt(this.quantityElm.getText().trim());
            } catch (NumberFormatException caught) {
                LOG.error("Trying to parse value: {}", this.quantityElm.getText(), caught);
            }
            this.presenter.onAddToCart(quantity);
        }));
        btnRow.add(buyBtn);
        bottomSection.add(btnRow);

        rightCol.add(bottomSection, BorderLayout.SOUTH);

        mainContent.add(rightCol, BorderLayout.CENTER);
        this.element.add(mainContent);
    }

    private static void renderHtml(JTextPane textPane, String htmlString) {
        textPane.setText("");
        if (htmlString == null || htmlString.isBlank()) {
            return;
        }
        var doc = (StyledDocument) textPane.getDocument();
        var normal = new SimpleAttributeSet();
        var bullet = new SimpleAttributeSet();
        StyleConstants.setBold(bullet, false);

        var parsed = Jsoup.parseBodyFragment(htmlString);
        parsed.body().traverse(new NodeVisitor() {
            @Override
            public void head(org.jsoup.nodes.Node node, int depth) {
                if (node instanceof TextNode textNode) {
                    var txt = textNode.text();
                    if (txt != null && !txt.isBlank()) {
                        try {
                            doc.insertString(doc.getLength(), txt.trim(), normal);
                        } catch (BadLocationException e) {
                            // ignore
                        }
                    }
                } else if (node instanceof org.jsoup.nodes.Element el) {
                    if ("li".equalsIgnoreCase(el.tagName())) {
                        try {
                            doc.insertString(doc.getLength(), "\n\u2022 ", bullet);
                        } catch (BadLocationException e) {
                            // ignore
                        }
                    }
                }
            }

            @Override
            public void tail(org.jsoup.nodes.Node node, int depth) {
                // no-op
            }
        });
    }
}
