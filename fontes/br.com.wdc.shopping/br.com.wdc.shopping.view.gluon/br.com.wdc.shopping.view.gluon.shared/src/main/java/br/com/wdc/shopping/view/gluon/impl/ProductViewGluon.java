package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductViewState;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.util.ResourceCatalog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

    public ProductViewGluon(ShoppingGluonApplication app, ProductPresenter presenter) {
        super("product", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            buildUI();
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

    private void buildUI() {
        var root = (VBox) this.element;
        root.setPadding(new Insets(0));
        root.setSpacing(0);
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Header bar
        var backBtn = new Button("← Voltar");
        backBtn.setStyle("-fx-font-size: 13; -fx-background-color: transparent; -fx-text-fill: #1976D2; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");
        backBtn.setOnAction(e -> safeAction("Back", this.presenter::onOpenProducts));

        var headerTitle = new Label("Detalhes do Produto");
        headerTitle.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;");

        var headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        var headerBar = new HBox(12, backBtn, headerSpacer, headerTitle);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(10, 16, 10, 16));
        headerBar.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Product image - hero section
        this.imageElm = new ImageView();
        this.imageElm.setFitWidth(220);
        this.imageElm.setFitHeight(220);
        this.imageElm.setPreserveRatio(true);
        if (this.state.product != null && this.state.product.image != null) {
            this.imageElm.setImage(ResourceCatalog.getImage(this.state.product.image));
            this.imageOldValue = this.state.product.image;
        }

        var imageContainer = new VBox(this.imageElm);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPadding(new Insets(20, 0, 16, 0));
        imageContainer.setStyle("-fx-background-color: white;");

        // Product info card
        var infoCard = new VBox(14);
        infoCard.setPadding(new Insets(24, 20, 20, 20));
        infoCard.setStyle("-fx-background-color: white; -fx-background-radius: 16 16 0 0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, -2);");

        // Product name
        this.nameElm = new Label(this.state.product != null ? this.state.product.name : "");
        this.nameElm.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #222;");
        this.nameElm.setWrapText(true);
        this.nameOldValue = this.state.product != null ? this.state.product.name : null;

        // Price
        this.priceElm = new Label(this.state.product != null
                ? NumberFormat.getCurrencyInstance().format(this.state.product.price) : "");
        this.priceElm.setStyle("-fx-font-size: 22; -fx-text-fill: #1976D2; -fx-font-weight: bold;");
        this.priceOldValue = this.state.product != null ? this.state.product.price : 0;

        // Quantity selector - custom −/+ buttons
        var qtyLabel = new Label("Quantidade");
        qtyLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");

        var minusBtn = new Button("−");
        minusBtn.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 50; " +
                "-fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32; " +
                "-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #555; -fx-cursor: hand;");
        minusBtn.setOnAction(e -> {
            if (this.quantity > 1) {
                this.quantity--;
                this.quantityLabel.setText(String.valueOf(this.quantity));
            }
        });

        this.quantityLabel = new Label("1");
        this.quantityLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333; " +
                "-fx-min-width: 32; -fx-alignment: center;");

        var plusBtn = new Button("+");
        plusBtn.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 50; " +
                "-fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32; " +
                "-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #555; -fx-cursor: hand;");
        plusBtn.setOnAction(e -> {
            if (this.quantity < 99) {
                this.quantity++;
                this.quantityLabel.setText(String.valueOf(this.quantity));
            }
        });

        var qtyStepper = new HBox(8, minusBtn, this.quantityLabel, plusBtn);
        qtyStepper.setAlignment(Pos.CENTER);
        qtyStepper.setPadding(new Insets(4, 8, 4, 8));
        qtyStepper.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 20; -fx-background-radius: 20; " +
                "-fx-background-color: white;");

        var qtyBox = new VBox(6, qtyLabel, qtyStepper);

        var addBtn = new Button("🛒  Adicionar ao Carrinho");
        addBtn.setMaxWidth(220);
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 12 20; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        addBtn.setOnAction(e -> emitBuy());

        var actionSection = new HBox(16, qtyBox, addBtn);
        actionSection.setAlignment(Pos.BOTTOM_LEFT);
        actionSection.setPadding(new Insets(6, 0, 6, 0));

        // Error
        this.errorElm = new Label();
        this.errorElm.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-padding: 8 12; " +
                "-fx-background-color: #d32f2f; -fx-background-radius: 6;");
        this.errorElm.setVisible(false);
        this.errorElm.setManaged(false);
        this.errorElm.setWrapText(true);

        // Description section
        var descTitle = new Label("Descrição");
        descTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #666; -fx-padding: 12 0 6 0;");

        this.descriptionElm = new TextFlow();
        this.descriptionElm.setStyle("-fx-font-size: 13; -fx-line-spacing: 3;");
        renderDescription(this.state.product != null ? this.state.product.description : null);
        this.descriptionOldValue = this.state.product != null ? this.state.product.description : null;

        infoCard.getChildren().addAll(this.nameElm, this.priceElm, actionSection,
                this.errorElm, descTitle, this.descriptionElm);

        var contentBox = new VBox(0, imageContainer, infoCard);
        VBox.setVgrow(infoCard, Priority.ALWAYS);

        var scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(headerBar, scroll);
    }

    private void emitBuy() {
        safeAction("Add to cart", () -> {
            this.presenter.onAddToCart(this.quantity);
        });
    }

    private void renderDescription(String text) {
        this.descriptionElm.getChildren().clear();
        if (text != null && !text.isBlank()) {
            // Simple: strip tags for mobile, show plain text
            var plain = text.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            this.descriptionElm.getChildren().add(new Text(plain));
        }
    }
}
