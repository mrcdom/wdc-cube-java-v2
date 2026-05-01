package br.com.wdc.shopping.view.jfx.impl;

import java.text.NumberFormat;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductViewState;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import br.com.wdc.shopping.view.jfx.util.ResourceCatalog;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.converter.NumberStringConverter;

public class ProductViewJfx extends AbstractViewJfx<ProductPresenter> {

    private static final Logger LOG = LoggerFactory.getLogger(ProductViewJfx.class);

    private final ProductViewState state;

    private boolean notRendered = true;
    private Text nameElm1;
    private Label nameElm2;
    private String nameOldValue;
    private ImageView imageElm;
    private String imageOldValue;
    private Label priceElm;
    private double priceOldValue;
    private TextField quantityElm;
    private TextFlow descriptionElm;
    private String descriptionOldValue;
    private Label errorElm;

    public ProductViewJfx(ShoppingJfxApplication app, ProductPresenter presenter) {
        super("product", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            JfxDom.render((VBox) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.nameOldValue, this.state.product.name)) {
            this.nameElm1.setText(this.state.product.name);
            this.nameElm2.setText(this.state.product.name);
            this.nameOldValue = this.state.product.name;
        }

        if (!Objects.equals(this.imageOldValue, this.state.product.image)) {
            this.imageElm.setImage(ResourceCatalog.getImage(this.state.product.image));
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
            this.errorElm.setManaged(newErrorDisplay);
        }
    }

    private void initialRender(JfxDom dom, VBox pane0) {
        pane0.getStyleClass().add("product-form");
        pane0.setMaxWidth(900);

        dom.textFlow(pane1 -> {
            pane1.getStyleClass().add("breadcrumbs");

            dom.text(text -> text.setText("Produtos > "));

            dom.text(text -> {
                this.nameElm1 = text;
                this.nameElm1.setText(this.state.product.name);
                this.nameOldValue = this.state.product.name;
            });
        });

        dom.hbox(_ -> {
            dom.vbox(_ -> {
                dom.stackPane(imgPane -> {
                    imgPane.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-color: #fafafa; -fx-background-radius: 4; -fx-padding: 8;");

                    dom.img(img -> {
                        this.imageElm = img;
                        this.imageElm.setImage(ResourceCatalog.getImage(this.state.product.image));
                        this.imageOldValue = this.state.product.image;
                        img.setFitWidth(300);
                        img.setFitHeight(300);
                        img.setPreserveRatio(true);
                    });
                });

                dom.vSpacer(16);

                dom.button(button -> {
                    button.getStyleClass().add("back-button");
                    button.setText("< VOLTAR");
                    button.setOnAction(this::emitBackClicked);
                });
            });

            dom.vbox(pane2 -> {
                HBox.setHgrow(pane2, Priority.ALWAYS);
                pane2.getStyleClass().add("content");

                dom.label(label -> {
                    label.getStyleClass().add("lbl-name-val");
                    this.nameElm2 = label;
                    this.nameElm2.setText(this.state.product.name);
                });

                dom.hbox(pane3 -> {
                    pane3.getStyleClass().add("pane-price-qtd");

                    dom.label(label -> {
                        label.getStyleClass().add("lbl-price-val");
                        this.priceElm = label;
                        this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.state.product.price));
                        this.priceOldValue = this.state.product.price;
                    });

                    dom.hSpacer();

                    dom.label(label -> label.setText("Quantidade:"));

                    dom.textField(field -> {
                        field.getStyleClass().add("fld-quantity");
                        field.setTextFormatter(new TextFormatter<>(new NumberStringConverter()));
                        this.quantityElm = field;
                        this.quantityElm.setText("1");
                    });
                });

                dom.label(label -> {
                    label.getStyleClass().add("description-title");
                    label.setText("DESCRIÇÃO DO PRODUTO");
                });

                dom.textFlow(pane3 -> {
                    pane3.getStyleClass().add("description");
                    this.descriptionElm = pane3;
                    renderHtml(this.descriptionElm, this.state.product.description);
                    this.descriptionOldValue = this.state.product.description;
                });

                dom.label(label -> {
                    this.errorElm = label;
                    this.errorElm.getStyleClass().add("error");
                    this.errorElm.setVisible(false);
                    this.errorElm.setManaged(false);
                });

                dom.hbox(pane3 -> {
                    pane3.setAlignment(Pos.CENTER);

                    dom.button(button -> {
                        button.getStyleClass().add("buy-button");
                        button.setText("Adicionar ao carrinho");
                        button.setOnAction(this::emitBuyClicked);
                    });
                });
            });
        });
    }

    private void emitBackClicked(ActionEvent evt) {
        this.presenter.onOpenProducts();
    }

    private void emitBuyClicked(ActionEvent evt) {
        var quantity = 1;
        try {
            quantity = Integer.parseInt(this.quantityElm.getText());
        } catch (NumberFormatException caught) {
            LOG.error("Trying to parse value: {}", this.quantityElm.getText(), caught);
        }
        this.presenter.onAddToCart(quantity);
    }

    private static void renderHtml(Pane pane, String htmlString) {
        pane.getChildren().clear();
        if (htmlString == null || htmlString.isBlank()) {
            return;
        }
        var doc = Jsoup.parseBodyFragment(htmlString);
        doc.body().traverse(new SimpleHtmlRenderer(pane));
    }

    private static class SimpleHtmlRenderer implements NodeVisitor {

        private final Deque<Pane> stack = new LinkedList<>();

        SimpleHtmlRenderer(Pane pane) {
            this.stack.push(pane);
        }

        @Override
        public void head(org.jsoup.nodes.Node node, int depth) {
            var pane = this.stack.peek();

            if (node instanceof TextNode textNode) {
                var txt = textNode.text();
                if (txt != null && !txt.isBlank()) {
                    pane.getChildren().add(new Text(txt.trim()));
                }
            } else if (node instanceof org.jsoup.nodes.Element htmlElm) {
                if ("ul".equalsIgnoreCase(htmlElm.tagName())) {
                    var ul = new VBox();
                    ul.getStyleClass().add("ul");
                    pane.getChildren().add(ul);
                    this.stack.push(ul);
                } else if ("li".equalsIgnoreCase(htmlElm.tagName())) {
                    var li = new TextFlow();
                    li.getStyleClass().add("li");
                    li.getChildren().add(new Text("\u2022 "));
                    pane.getChildren().add(li);
                    this.stack.push(li);
                }
            }
        }

        @Override
        public void tail(org.jsoup.nodes.Node node, int depth) {
            if (node instanceof org.jsoup.nodes.Element) {
                this.stack.pop();
            }
        }
    }
}
