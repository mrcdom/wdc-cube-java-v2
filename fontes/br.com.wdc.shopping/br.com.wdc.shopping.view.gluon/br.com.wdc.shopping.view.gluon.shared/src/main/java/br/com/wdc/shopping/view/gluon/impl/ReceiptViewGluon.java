package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonIcons;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ReceiptViewGluon extends AbstractViewGluon<ReceiptPresenter> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReceiptViewState state;

    private boolean notRendered = true;
    private Label dateElm;
    private String dateOldValue;
    private Label totalElm;
    private String totalOldValue;
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
            GluonDom.render((VBox) this.element, this::buildUI);
            this.notRendered = false;
        }

        if (this.state.notifySuccess) {
            this.successElm.setVisible(true);
            this.successElm.setManaged(true);
            this.state.notifySuccess = false;
        }

        var items = Collections.<ReceiptItem>emptyList();
        var dateNewValue = this.dateOldValue;
        var totalNewValue = this.totalOldValue;
        if (this.state.receipt != null) {
            items = this.state.receipt.items;

            dateNewValue = this.state.receipt.date != null
                    ? DATE_FMT.format(Instant.ofEpochMilli(this.state.receipt.date).atZone(ZoneId.systemDefault()))
                    : "";

            totalNewValue = NumberFormat.getCurrencyInstance().format(this.state.receipt.total != null
                    ? this.state.receipt.total
                    : 0);
        }

        if (!Objects.equals(dateOldValue, dateNewValue)) {
            this.dateElm.setText(dateNewValue);
            this.dateOldValue = dateNewValue;
        }

        if (!Objects.equals(totalOldValue, totalNewValue)) {
            this.totalElm.setText(totalNewValue);
            this.totalOldValue = totalNewValue;
        }

        this.itemsSlot.accept(items, this.viewList);
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

            dom.label(title -> {
                title.setText("Comprovante");
                title.setStyle(GluonStyles.PAGE_TITLE);
            });
        });

        // Success banner
        this.successElm = dom.label(success -> {
            success.setText("Compra realizada com sucesso!");
            success.setGraphic(GluonIcons.create(GluonIcons.CHECK_CIRCLE, 16, GluonColors.SUCCESS_TEXT));
            success.setMaxWidth(Double.MAX_VALUE);
            success.setStyle(GluonStyles.SUCCESS_BANNER);
            success.setVisible(false);
            success.setManaged(false);
        });

        // Receipt card
        dom.vbox(card -> {
            card.setSpacing(0);
            card.setStyle(GluonStyles.CARD);
            VBox.setMargin(card, new Insets(16, 12, 12, 12));

            // Receipt header section
            dom.vbox(receiptHeader -> {
                receiptHeader.setAlignment(Pos.CENTER);
                receiptHeader.setSpacing(4);
                receiptHeader.setPadding(new Insets(20, 16, 12, 16));
                receiptHeader.setStyle(GluonStyles.DIVIDER_BOTTOM);

                dom.label(receiptTitle -> {
                    receiptTitle.setText("Detalhes da Compra");
                    receiptTitle.setStyle(GluonStyles.PAGE_TITLE);
                });

                this.dateElm = dom.label(date -> {
                    date.setStyle(GluonStyles.TEXT_HINT_STYLE);
                });
            });

            // Items section
            dom.scrollVBox((sp, scrollContent) -> {
                VBox.setVgrow(sp, Priority.ALWAYS);
                sp.setFitToWidth(true);
                sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                sp.setStyle(GluonStyles.SCROLL_TRANSPARENT);

                dom.label(itemsLabel -> {
                    itemsLabel.setText("Itens");
                    itemsLabel.setStyle(GluonStyles.SECTION_CAPTION + " -fx-padding: 12 16 6 16;");
                });

                dom.vbox(itemsBox -> {
                    itemsBox.setSpacing(0);
                    itemsBox.setPadding(new Insets(0, 12, 0, 12));
                    this.itemsSlot = this.newListSlot(itemsBox, this::newItemView, this::updateItem);
                });
            });

            // Total footer
            dom.hbox(totalRow -> {
                totalRow.setAlignment(Pos.CENTER);
                totalRow.setSpacing(8);
                totalRow.setPadding(new Insets(14, 16, 14, 16));
                totalRow.setStyle(GluonStyles.FOOTER_HIGHLIGHT);

                dom.label(totalLabel -> {
                    totalLabel.setText("Total");
                    totalLabel.setStyle(GluonStyles.TEXT_PRICE_LABEL);
                });

                dom.hSpacer();

                this.totalElm = dom.label(total -> {
                    total.setStyle(GluonStyles.PRICE_MEDIUM);
                });
            });
            VBox.setVgrow(card, Priority.ALWAYS);
        });
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
                GluonDom.render((HBox) this.element, this::buildUI);
                this.notRendered = false;
            }
            this.descElm.setText(this.item.description != null ? this.item.description : "");
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.item.value));
            this.qtyElm.setText("x" + this.item.quantity);
        }

        private void buildUI(GluonDom dom, HBox row) {
            row.setSpacing(10);
            row.setPadding(new Insets(10, 4, 10, 4));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(GluonStyles.DIVIDER_BOTTOM);

            this.qtyElm = dom.label(qty -> {
                qty.setStyle(GluonStyles.BADGE_QUANTITY);
            });

            this.descElm = dom.label(desc -> {
                desc.setStyle(GluonStyles.TEXT_BODY_STYLE);
                HBox.setHgrow(desc, Priority.ALWAYS);
            });

            this.priceElm = dom.label(price -> {
                price.setStyle(GluonStyles.textBold(13, GluonColors.TEXT_DEFAULT));
            });
        }
    }
}
