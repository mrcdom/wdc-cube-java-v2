package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductViewState;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonIcons;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
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

        // Header bar
        dom.hbox(headerBar -> {
            headerBar.setAlignment(Pos.CENTER_LEFT);
            headerBar.setSpacing(12);
            headerBar.setPadding(new Insets(10, 16, 10, 16));
            headerBar.setStyle(GluonStyles.HEADER_BAR);

            dom.button(backBtn -> {
                backBtn.setText("Voltar");
                backBtn.setGraphic(GluonIcons.create(GluonIcons.ARROW_BACK, 14, GluonColors.PRIMARY));
                backBtn.setStyle(GluonStyles.BACK_BUTTON);
                backBtn.setOnAction(e -> safeAction("Back", this.presenter::onOpenProducts));
            });

            dom.hSpacer();

            dom.label(headerTitle -> {
                headerTitle.setText("Detalhes do Produto");
                headerTitle.setStyle(GluonStyles.PAGE_TITLE);
            });
        });

        // Scrollable content
        dom.scrollVBox((sp, content) -> {
            VBox.setVgrow(sp, Priority.ALWAYS);
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setStyle(GluonStyles.SCROLL_TRANSPARENT);

            // Product info card
            dom.vbox(card -> {
                card.setSpacing(14);
                card.setPadding(new Insets(24, 20, 20, 20));
                card.setStyle(GluonStyles.CARD_TOP_ROUND);

                this.nameElm = dom.label(name -> {
                    name.setText(this.state.product != null ? this.state.product.name : "");
                    name.setStyle(GluonStyles.textBold(20, GluonColors.TEXT_PRIMARY));
                    name.setWrapText(true);
                });
                this.nameOldValue = this.state.product != null ? this.state.product.name : null;

                dom.label(descTitle -> {
                    descTitle.setText("Descrição");
                    descTitle.setStyle(GluonStyles.textBold(13, GluonColors.TEXT_SECONDARY) + " -fx-padding: 8 0 4 0;");
                });

                this.descriptionElm = dom.textFlow(tf -> {
                    tf.setStyle(GluonStyles.fontSize(13) + " -fx-line-spacing: 3;");
                });
                renderDescription(this.state.product != null ? this.state.product.description : null);
                this.descriptionOldValue = this.state.product != null ? this.state.product.description : null;

                // Price + quantity (left) | Image (right)
                dom.hbox(priceImageRow -> {
                    priceImageRow.setAlignment(Pos.CENTER_LEFT);
                    priceImageRow.setSpacing(16);
                    priceImageRow.setPadding(new Insets(6, 0, 6, 0));

                    dom.vbox(leftCol -> {
                        leftCol.setSpacing(10);
                        javafx.scene.layout.HBox.setHgrow(leftCol, Priority.ALWAYS);

                        this.priceElm = dom.label(price -> {
                            price.setText(this.state.product != null
                                    ? NumberFormat.getCurrencyInstance().format(this.state.product.price)
                                    : "");
                            price.setStyle(GluonStyles.PRICE_LARGE);
                        });
                        this.priceOldValue = this.state.product != null ? this.state.product.price : 0;

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
                                qty.setStyle(GluonStyles.textBold(16, GluonColors.TEXT_DEFAULT) + " " +
                                        "-fx-min-width: 32; -fx-alignment: center;");
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

                    this.imageElm = dom.imageView(img -> {
                        img.setFitWidth(110);
                        img.setFitHeight(110);
                        img.setPreserveRatio(true);
                        if (this.state.product != null && this.state.product.image != null) {
                            img.setImage(ResourceCatalog.getImage(this.state.product.image));
                            this.imageOldValue = this.state.product.image;
                        }
                    });
                });

                // Add to cart button
                dom.hbox(actionSection -> {
                    actionSection.setAlignment(Pos.CENTER);
                    actionSection.setPadding(new Insets(6, 0, 6, 0));

                    dom.button(addBtn -> {
                        addBtn.setText("Adicionar ao Carrinho");
                        addBtn.setGraphic(GluonIcons.create(GluonIcons.SHOPPING_CART, 16, GluonColors.TEXT_ON_PRIMARY));
                        addBtn.setMaxWidth(Double.MAX_VALUE);
                        addBtn.setStyle(GluonStyles.BTN_SUCCESS);
                        javafx.scene.layout.HBox.setHgrow(addBtn, Priority.ALWAYS);
                        addBtn.setOnAction(e -> emitBuy());
                    });
                });

                this.errorElm = dom.label(err -> {
                    err.setStyle(GluonStyles.ERROR_INLINE);
                    err.setVisible(false);
                    err.setManaged(false);
                    err.setWrapText(true);
                });

                VBox.setVgrow(card, Priority.ALWAYS);
            });
        });
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
