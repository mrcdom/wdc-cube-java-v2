package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ReceiptViewGluon extends AbstractViewGluon<ReceiptPresenter> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReceiptViewState state;

    private boolean notRendered = true;
    private Label dateElm;
    private Label totalElm;
    private Label successElm;
    private int itemIdx;
    private List<ReceiptItemView> viewList = new ArrayList<>();
    private BiConsumer<List<ReceiptItem>, List<ReceiptItemView>> itemsSlot;

    public ReceiptViewGluon(ShoppingGluonApplication app, ReceiptPresenter presenter) {
        super("receipt", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            buildUI();
            this.notRendered = false;
        }

        // Success notification
        if (this.state.notifySuccess) {
            this.successElm.setVisible(true);
            this.successElm.setManaged(true);
            this.state.notifySuccess = false;
        }

        // Receipt data
        if (this.state.receipt != null) {
            this.dateElm.setText(this.state.receipt.date != null
                    ? DATE_FMT.format(Instant.ofEpochMilli(this.state.receipt.date).atZone(ZoneId.systemDefault())) : "");
            this.totalElm.setText(NumberFormat.getCurrencyInstance().format(
                    this.state.receipt.total != null ? this.state.receipt.total : 0));
            this.itemsSlot.accept(this.state.receipt.items, this.viewList);
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

        var title = new Label("Comprovante");
        title.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;");

        var headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        var headerBar = new HBox(12, backBtn, headerSpacer, title);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(10, 16, 10, 16));
        headerBar.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Success banner
        this.successElm = new Label("✅  Compra realizada com sucesso!");
        this.successElm.setMaxWidth(Double.MAX_VALUE);
        this.successElm.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 13; -fx-font-weight: bold; " +
                "-fx-padding: 12 16; -fx-background-color: #E8F5E9; " +
                "-fx-border-color: #A5D6A7; -fx-border-width: 0 0 1 0;");
        this.successElm.setVisible(false);
        this.successElm.setManaged(false);

        // Receipt card
        var receiptCard = new VBox(0);
        receiptCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        VBox.setMargin(receiptCard, new Insets(16, 12, 12, 12));

        // Receipt header section
        var receiptIcon = new Label("🧾");
        receiptIcon.setStyle("-fx-font-size: 28;");

        var receiptTitle = new Label("Detalhes da Compra");
        receiptTitle.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;");

        this.dateElm = new Label();
        this.dateElm.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");

        var receiptHeader = new VBox(4, receiptIcon, receiptTitle, this.dateElm);
        receiptHeader.setAlignment(Pos.CENTER);
        receiptHeader.setPadding(new Insets(20, 16, 12, 16));
        receiptHeader.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");

        // Items section
        var itemsLabel = new Label("Itens");
        itemsLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #999; " +
                "-fx-padding: 12 16 6 16;");

        var itemsBox = new VBox(0);
        itemsBox.setPadding(new Insets(0, 12, 0, 12));
        this.itemsSlot = this.newListSlot(itemsBox, this::newItemView, this::updateItem);

        var scroll = new ScrollPane(new VBox(itemsLabel, itemsBox));
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Total footer
        var totalLabel = new Label("Total");
        totalLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666;");

        this.totalElm = new Label();
        this.totalElm.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1976D2;");

        var totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);

        var totalRow = new HBox(8, totalLabel, totalSpacer, this.totalElm);
        totalRow.setAlignment(Pos.CENTER);
        totalRow.setPadding(new Insets(14, 16, 14, 16));
        totalRow.setStyle("-fx-background-color: #F5F9FF; -fx-background-radius: 0 0 12 12; " +
                "-fx-border-color: #E3F2FD; -fx-border-width: 1 0 0 0;");

        receiptCard.getChildren().addAll(receiptHeader, scroll, totalRow);
        VBox.setVgrow(receiptCard, Priority.ALWAYS);

        root.getChildren().addAll(headerBar, this.successElm, receiptCard);
    }

    private ReceiptItemView newItemView() {
        return new ReceiptItemView(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ReceiptItemView itemView, ReceiptItem state) {
        itemView.setState(state);
        itemView.doUpdate();
    }

    // ---- Inner class ----

    public static class ReceiptItemView extends AbstractViewGluon<ReceiptPresenter> {

        private ReceiptItem item;
        private boolean notRendered = true;
        private Label descElm;
        private Label priceElm;
        private Label qtyElm;

        ReceiptItemView(ShoppingGluonApplication app, ReceiptPresenter presenter, int idx) {
            super("receipt-item-" + idx, app, presenter, new HBox());
            this.item = new ReceiptItem();
        }

        void setState(ReceiptItem item) {
            this.item = item;
        }

        @Override
        public void doUpdate() {
            if (this.notRendered) {
                buildUI();
                this.notRendered = false;
            }
            this.descElm.setText(this.item.description != null ? this.item.description : "");
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.item.value));
            this.qtyElm.setText("x" + this.item.quantity);
        }

        private void buildUI() {
            var row = (HBox) this.element;
            row.setSpacing(10);
            row.setPadding(new Insets(10, 4, 10, 4));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");

            this.qtyElm = new Label();
            this.qtyElm.setStyle("-fx-font-size: 11; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-color: #90CAF9; -fx-background-radius: 4; " +
                    "-fx-padding: 2 6; -fx-min-width: 28; -fx-alignment: center;");

            this.descElm = new Label();
            this.descElm.setStyle("-fx-font-size: 13; -fx-text-fill: #444;");
            HBox.setHgrow(this.descElm, Priority.ALWAYS);

            this.priceElm = new Label();
            this.priceElm.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333;");

            row.getChildren().addAll(this.qtyElm, this.descElm, this.priceElm);
        }
    }
}
