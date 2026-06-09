package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonIcons;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import br.com.wdc.shopping.view.gluon.util.ResourceCatalog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ProductViewGluon extends AbstractViewGluon<ProductPresenter> {

    private final ProductViewState state;

    private boolean notRendered = true;
    private ImageView imageElm;
    private String imageOldValue;
    private Label nameElm;
    private String nameOldValue;
    private Label priceElm;
    private double priceOldValue;
    private int quantity = 1;
    private Label quantityLabel;
    private TextFlow descriptionElm;
    private String descriptionOldValue;
    private Label errorElm;

    public ProductViewGluon(ProductPresenter presenter) {
        super("product", (ShoppingGluonApplication) presenter.app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            GluonDom.render((VBox) this.element, this::buildUI);
            this.notRendered = false;
        }

        if (!Objects.equals(this.imageOldValue, this.state.product.image)) {
            var img = ResourceCatalog.getImage(this.state.product.image);
            this.imageElm.setImage(img);
            this.imageOldValue = this.state.product.image;
        }

        if (!Objects.equals(this.nameOldValue, this.state.product.name)) {
            this.nameElm.setText(this.state.product.name);
            this.nameOldValue = this.state.product.name;
        }

        if (this.priceOldValue != this.state.product.price) {
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.state.product.price));
            this.priceOldValue = this.state.product.price;
        }

        if (!Objects.equals(this.descriptionOldValue, this.state.product.description)) {
            renderDescription(this.state.product.description);
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

    private void buildUI(GluonDom dom, VBox root) {
        root.setPadding(new Insets(0));
        root.setSpacing(0);
        root.setStyle(GluonStyles.PAGE_BG);

        // Scrollable content (no separate header bar — like Flutter PageCard)
        dom.scrollVBox((sp, content) -> {
            VBox.setVgrow(sp, Priority.ALWAYS);
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setStyle(GluonStyles.SCROLL_TRANSPARENT);
            content.setPadding(new Insets(20));
            content.setSpacing(16);
            content.setAlignment(Pos.TOP_CENTER);

            // Inner container with max-width
            dom.vbox(inner -> {
                inner.setSpacing(16);
                inner.setMaxWidth(560);

                // Title
                this.nameElm = dom.label(name -> {
                    name.setText(this.state.product != null ? this.state.product.name : "");
                    name.setStyle(GluonStyles.textBold(24, GluonColors.TEXT_PRIMARY));
                    name.setWrapText(true);
                });
                this.nameOldValue = this.state.product != null ? this.state.product.name : null;

                // Accent divider
                dom.node(buildDivider());

                // Description card
                dom.vbox(descCard -> {
                    descCard.setPadding(new Insets(20));
                    descCard.setStyle(GluonStyles.CARD);
                    descCard.setMaxWidth(Double.MAX_VALUE);

                    dom.label(descTitle -> {
                        descTitle.setText("Descrição");
                        descTitle.setStyle(GluonStyles.textBold(13, GluonColors.TEXT_SECONDARY) + " -fx-padding: 0 0 8 0;");
                    });

                    this.descriptionElm = dom.textFlow(tf -> {
                        tf.setStyle(GluonStyles.fontSize(13) + " -fx-line-spacing: 3;");
                    });
                    renderDescription(this.state.product != null ? this.state.product.description : null);
                    this.descriptionOldValue = this.state.product != null ? this.state.product.description : null;
                });

                // Price + Image row
                dom.hbox(priceImageRow -> {
                    priceImageRow.setAlignment(Pos.CENTER);
                    priceImageRow.setSpacing(16);

                    dom.vbox(leftCol -> {
                        leftCol.setSpacing(12);
                        leftCol.setAlignment(Pos.CENTER);
                        HBox.setHgrow(leftCol, Priority.NEVER);

                        // Price badge — accent light background like Flutter
                        dom.stackPane(priceBadge -> {
                            priceBadge.setStyle(GluonStyles.PRICE_BADGE);

                            this.priceElm = dom.label(price -> {
                                price.setText(this.state.product != null
                                        ? NumberFormat.getCurrencyInstance().format(this.state.product.price)
                                        : "");
                                price.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: " + GluonColors.PRIMARY + ";");
                            });
                            this.priceOldValue = this.state.product != null ? this.state.product.price : 0;
                        });

                        // Quantity selector
                        dom.hbox(qtyStepper -> {
                            qtyStepper.setAlignment(Pos.CENTER);
                            qtyStepper.setSpacing(8);
                            qtyStepper.setPadding(new Insets(4, 8, 4, 8));
                            qtyStepper.setStyle(GluonStyles.QTY_STEPPER);

                            dom.button(minusBtn -> {
                                minusBtn.setGraphic(GluonIcons.create(GluonIcons.MINUS, 14, GluonColors.CONTROL_TEXT));
                                minusBtn.setStyle(GluonStyles.BTN_CIRCLE);
                                minusBtn.setOnAction(e -> {
                                    if (this.quantity > 1) {
                                        this.quantity--;
                                        this.quantityLabel.setText(String.valueOf(this.quantity));
                                    }
                                });
                            });

                            this.quantityLabel = dom.label(qty -> {
                                qty.setText("1");
                                qty.setStyle(GluonStyles.textBold(16, GluonColors.TEXT_DEFAULT) +
                                        " -fx-min-width: 32; -fx-alignment: center;");
                            });

                            dom.button(plusBtn -> {
                                plusBtn.setGraphic(GluonIcons.create(GluonIcons.PLUS, 14, GluonColors.CONTROL_TEXT));
                                plusBtn.setStyle(GluonStyles.BTN_CIRCLE);
                                plusBtn.setOnAction(e -> {
                                    if (this.quantity < 99) {
                                        this.quantity++;
                                        this.quantityLabel.setText(String.valueOf(this.quantity));
                                    }
                                });
                            });
                        });
                    });

                    // Image box with gradient background
                    dom.stackPane(imageBox -> {
                        imageBox.setStyle(GluonStyles.IMAGE_BG + " -fx-background-radius: 8;");
                        imageBox.setPrefWidth(200);
                        imageBox.setPrefHeight(200);
                        imageBox.setMaxWidth(200);
                        imageBox.setMaxHeight(200);

                        this.imageElm = dom.imageView(img -> {
                            img.setFitWidth(160);
                            img.setFitHeight(160);
                            img.setPreserveRatio(true);
                            if (this.state.product != null && this.state.product.image != null) {
                                img.setImage(ResourceCatalog.getImage(this.state.product.image));
                                this.imageOldValue = this.state.product.image;
                            }
                        });
                    });
                });

                // Error
                this.errorElm = dom.label(err -> {
                    err.setStyle(GluonStyles.ERROR_INLINE);
                    err.setVisible(false);
                    err.setManaged(false);
                    err.setWrapText(true);
                    err.setMaxWidth(Double.MAX_VALUE);
                });

                // Action row — back button (text) + add-to-cart button (filled pill)
                dom.hbox(actionRow -> {
                    actionRow.setAlignment(Pos.CENTER);
                    actionRow.setSpacing(12);

                    dom.button(backBtn -> {
                        backBtn.setText("Voltar");
                        backBtn.setGraphic(GluonIcons.create(GluonIcons.ARROW_BACK, 18, GluonColors.PRIMARY));
                        backBtn.setStyle(GluonStyles.BACK_BUTTON + " -fx-font-size: 14;");
                        backBtn.setOnAction(e -> safeAction("Back", this.presenter::onOpenProducts));
                    });

                    dom.button(addBtn -> {
                        addBtn.setText("Adicionar ao Carrinho");
                        addBtn.setGraphic(GluonIcons.create(GluonIcons.SHOPPING_CART, 18, GluonColors.TEXT_ON_PRIMARY));
                        addBtn.setStyle(GluonStyles.BTN_SUCCESS);
                        addBtn.setOnAction(e -> emitBuy());
                    });
                });
            });
        });
    }

    private javafx.scene.shape.Line buildDivider() {
        var line = new javafx.scene.shape.Line();
        line.setStartX(0);
        line.setEndX(560);
        line.setStroke(javafx.scene.paint.Color.web(GluonColors.PRIMARY));
        line.setStrokeWidth(2);
        return line;
    }

    private void emitBuy() {
        safeAction("Add to cart", () -> {
            this.presenter.onAddToCart(this.quantity);
        });
    }

    private void renderDescription(String text) {
        this.descriptionElm.getChildren().clear();
        if (text != null && !text.isBlank()) {
            var plain = text.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            this.descriptionElm.getChildren().add(new Text(plain));
        }
    }
}
