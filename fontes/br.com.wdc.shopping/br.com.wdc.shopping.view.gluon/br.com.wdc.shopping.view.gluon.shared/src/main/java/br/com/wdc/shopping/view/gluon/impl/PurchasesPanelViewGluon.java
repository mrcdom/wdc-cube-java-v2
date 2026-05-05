package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
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

public class PurchasesPanelViewGluon extends AbstractViewGluon<PurchasesPanelPresenter> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PurchasesPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<PurchaseItemView> viewList = new ArrayList<>();
    private BiConsumer<List<PurchaseInfo>, List<PurchaseItemView>> contentSlot;
    private Label pageInfoElm;

    public PurchasesPanelViewGluon(ShoppingGluonApplication app, PurchasesPanelPresenter presenter) {
        super("purchases-panel", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            buildUI();
            this.notRendered = false;
        }
        this.contentSlot.accept(this.state.purchases, this.viewList);

        int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / this.state.pageSize));
        this.pageInfoElm.setText((this.state.page + 1) + " / " + totalPages);
    }

    private void buildUI() {
        var root = (VBox) this.element;
        root.setPadding(new Insets(16, 12, 8, 12));
        root.setSpacing(12);
        root.setStyle("-fx-background-color: #f5f5f5;");

        var caption = new Label("Histórico de Compras");
        caption.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333;");

        var subtitle = new Label("Toque em uma compra para ver os detalhes");
        subtitle.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");

        var headerBox = new VBox(2, caption, subtitle);

        var contentBox = new VBox(10);
        contentBox.setPadding(new Insets(4, 0, 4, 0));
        this.contentSlot = this.newListSlot(contentBox, this::newItemView, this::updateItem);

        var scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Pagination - Material Design style
        var prevBtn = new Button("◀");
        prevBtn.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-min-width: 36; -fx-min-height: 36; -fx-font-size: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1); -fx-cursor: hand;");
        prevBtn.setOnAction(e -> safeAction("Prev page", () -> this.presenter.onPageChange(this.state.page - 1)));

        this.pageInfoElm = new Label("1 / 1");
        this.pageInfoElm.setStyle("-fx-font-size: 13; -fx-text-fill: #666; -fx-font-weight: bold;");

        var nextBtn = new Button("▶");
        nextBtn.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-min-width: 36; -fx-min-height: 36; -fx-font-size: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1); -fx-cursor: hand;");
        nextBtn.setOnAction(e -> safeAction("Next page", () -> this.presenter.onPageChange(this.state.page + 1)));

        var spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        var spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        var pagination = new HBox(12, prevBtn, spacer1, this.pageInfoElm, spacer2, nextBtn);
        pagination.setAlignment(Pos.CENTER);
        pagination.setPadding(new Insets(8, 4, 4, 4));

        root.getChildren().addAll(headerBox, scrollPane, pagination);
    }

    private PurchaseItemView newItemView() {
        return new PurchaseItemView(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(PurchaseItemView itemView, PurchaseInfo state) {
        itemView.setState(state);
        itemView.doUpdate();
    }

    // ---- Inner class ----

    public static class PurchaseItemView extends AbstractViewGluon<PurchasesPanelPresenter> {

        private PurchaseInfo purchase;
        private boolean notRendered = true;
        private Label dateElm;
        private Label totalElm;

        PurchaseItemView(ShoppingGluonApplication app, PurchasesPanelPresenter presenter, int idx) {
            super("purchase-item-" + idx, app, presenter, new HBox());
            this.purchase = new PurchaseInfo();
        }

        void setState(PurchaseInfo purchase) {
            this.purchase = purchase;
        }

        @Override
        public void doUpdate() {
            if (this.notRendered) {
                buildUI();
                this.notRendered = false;
            }
            this.dateElm.setText(this.purchase.date > 0
                    ? DATE_FMT.format(Instant.ofEpochMilli(this.purchase.date).atZone(ZoneId.systemDefault())) : "");
            this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.purchase.total));
        }

        private void buildUI() {
            var row = (HBox) this.element;
            row.setSpacing(12);
            row.setPadding(new Insets(14, 16, 14, 16));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2); -fx-cursor: hand;");
            row.setOnMouseClicked(e -> safeAction("Open receipt",
                    () -> this.presenter.onOpenReceipt(this.purchase.id)));

            var icon = new Label("🧾");
            icon.setStyle("-fx-font-size: 22;");

            this.dateElm = new Label();
            this.dateElm.setStyle("-fx-font-size: 13; -fx-text-fill: #444;");

            var dateBox = new VBox(2, this.dateElm);

            var spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            this.totalElm = new Label();
            this.totalElm.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1976D2;");

            var arrow = new Label("›");
            arrow.setStyle("-fx-font-size: 20; -fx-text-fill: #ccc;");

            row.getChildren().addAll(icon, dateBox, spacer, this.totalElm, arrow);
        }
    }
}
