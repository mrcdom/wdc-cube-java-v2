package br.com.wdc.shopping.view.swing.impl;

import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jsoup.Jsoup;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.ResourceCatalog;
import br.com.wdc.shopping.view.swing.util.Styles;
import br.com.wdc.shopping.view.swing.util.SwingDom;

public class ProductViewSwing extends AbstractViewSwing<ProductPresenter> {

    private static final Log LOG = Log.getLogger(ProductViewSwing.class);

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

    public ProductViewSwing(ProductPresenter presenter) {
        super("product", (ShoppingSwingApplication) presenter.app, presenter, new JPanel());
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
            SwingDom.render(this.element, this::initialRender);
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

    private void initialRender(SwingDom dom, JPanel pane0) {
        pane0.setOpaque(true);
        pane0.setBackground(Styles.BG_WHITE);
        pane0.setBorder(Styles.BORDER_EMPTY_24);
        pane0.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));

        // Breadcrumbs
        dom.hbox(breadcrumbs -> {
            breadcrumbs.setBorder(new EmptyBorder(0, 0, 12, 0));
            breadcrumbs.setAlignmentX(Component.LEFT_ALIGNMENT);
            breadcrumbs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            dom.label(bLabel -> {
                bLabel.setText("Produtos");
                bLabel.setFont(Styles.FONT_SUBTITLE);
                bLabel.setForeground(Styles.FG_PRIMARY);
                bLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                bLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        safeAction("Open products", presenter::onOpenProducts);
                    }
                });
            });

            dom.hSpacer(4);

            dom.label(bSep -> {
                bSep.setText(">");
                bSep.setFont(Styles.FONT_SUBTITLE);
                bSep.setForeground(Styles.FG_TEXT_SUBTLE);
            });

            dom.hSpacer(4);

            dom.label(bName -> {
                this.breadcrumbName = bName;
                bName.setText(this.state.product.name);
                bName.setFont(Styles.FONT_SUBTITLE);
                bName.setForeground(Styles.FG_TEXT);
            });
        });

        // Main content: image left, details right
        dom.hbox(mainContent -> {
            mainContent.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Left: image
            dom.vbox(leftCol -> {
                leftCol.setMaximumSize(new Dimension(380, Integer.MAX_VALUE));

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
            });

            // Right: product details
            dom.vbox(rightCol -> {
                rightCol.setBorder(new EmptyBorder(12, 12, 12, 12));

                dom.label(name -> {
                    this.nameElm = name;
                    name.setText(this.state.product.name);
                    name.setFont(Styles.FONT_PRODUCT_NAME);
                    name.setForeground(Styles.FG_TEXT_DARK);
                    name.setAlignmentX(Component.LEFT_ALIGNMENT);
                    name.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                    this.nameOldValue = this.state.product.name;
                });

                dom.vSpacer(8);

                // Price + quantity row
                dom.hbox(priceRow -> {
                    priceRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                    priceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

                    dom.label(price -> {
                        this.priceElm = price;
                        price.setText(NumberFormat.getCurrencyInstance().format(this.state.product.price));
                        price.setFont(Styles.FONT_PRODUCT_PRICE);
                        price.setForeground(Styles.FG_PRIMARY);
                        this.priceOldValue = this.state.product.price;
                    });

                    dom.hSpacer();

                    dom.label(qtyLabel -> {
                        qtyLabel.setText("Quantidade:");
                        qtyLabel.setFont(Styles.FONT_DEFAULT);
                        qtyLabel.setForeground(Styles.FG_TEXT_LIGHT);
                    });

                    dom.hSpacer(6);

                    dom.textField(field -> {
                        this.quantityElm = field;
                        field.setText("1");
                        field.setColumns(4);
                        Styles.styleField(field);
                        field.setMaximumSize(new Dimension(60, 34));
                        field.setPreferredSize(new Dimension(60, 34));
                        field.setHorizontalAlignment(SwingConstants.CENTER);
                    });
                });

                dom.vSpacer(12);

                // Description title
                dom.label(descTitle -> {
                    descTitle.setText("DESCRIÇÃO DO PRODUTO");
                    descTitle.setFont(Styles.FONT_DESC_TITLE);
                    descTitle.setForeground(Styles.FG_TEXT);
                    descTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
                    descTitle.setBorder(new EmptyBorder(8, 0, 4, 0));
                    descTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                });

                // Description (custom JTextPane for text wrapping)
                this.descriptionElm = new JTextPane() {
                    @Override
                    public boolean getScrollableTracksViewportWidth() {
                        return true;
                    }
                };
                this.descriptionElm.setEditable(false);
                this.descriptionElm.setOpaque(false);
                renderHtml(this.descriptionElm, this.state.product.description);
                this.descriptionOldValue = this.state.product.description;

                dom.scrollPane(descScroll -> {
                    descScroll.setViewportView(this.descriptionElm);
                    descScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    descScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                });

                // Error
                dom.label(errorLabel -> {
                    this.errorElm = errorLabel;
                    Styles.styleErrorLabel(errorLabel);
                    errorLabel.setVisible(false);
                    errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    errorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                });

                dom.vSpacer(12);

                // Buttons row: back + buy
                dom.hbox(btnRow -> {
                    btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                    btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

                    dom.button(backBtn -> {
                        backBtn.setText("< VOLTAR");
                        Styles.styleOutlineButton(backBtn, Styles.FG_PRIMARY);
                        backBtn.addActionListener(
                                _ignored -> safeAction("Open products", this.presenter::onOpenProducts));
                    });

                    dom.hSpacer();

                    dom.button(buyBtn -> {
                        buyBtn.setText("Adicionar ao carrinho");
                        Styles.styleOrangeButton(buyBtn);
                        buyBtn.setFont(Styles.FONT_BUTTON_LARGE);
                        buyBtn.addActionListener(_ignored -> safeAction("Add to cart", () -> {
                            var quantity = 1;
                            try {
                                quantity = Integer.parseInt(this.quantityElm.getText().trim());
                            } catch (NumberFormatException caught) {
                                LOG.error("Trying to parse value: {}", this.quantityElm.getText(), caught);
                            }
                            this.presenter.onAddToCart(quantity);
                        }));
                    });
                });
            });
        });
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
                } else if (node instanceof org.jsoup.nodes.Element el
                        && "li".equalsIgnoreCase(el.tagName())) {
                    try {
                        doc.insertString(doc.getLength(), "\n\u2022 ", bullet);
                    } catch (BadLocationException e) {
                        // ignore
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
