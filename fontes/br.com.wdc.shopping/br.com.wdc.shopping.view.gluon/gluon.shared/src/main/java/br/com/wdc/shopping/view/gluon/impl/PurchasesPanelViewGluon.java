package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonIcons;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class PurchasesPanelViewGluon extends AbstractViewGluon<PurchasesPanelPresenter> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int ITEM_HEIGHT_PX = 58;

    private final PurchasesPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<PurchaseItemView> viewList = new ArrayList<>();
    private BiConsumer<List<PurchaseInfo>, List<PurchaseItemView>> contentSlot;
    private ScrollPane scrollPane;
    private Label pageInfoElm;

    public PurchasesPanelViewGluon(PurchasesPanelPresenter presenter) {
        super("purchases-panel", (ShoppingGluonApplication) presenter.app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            GluonDom.render((VBox) this.element, this::buildUI);
            this.notRendered = false;
        }
        if (this.state.pageSize < 0 && this.scrollPane != null) {
            Platform.runLater(() -> {
                int h = (int) this.scrollPane.getHeight();
                if (h > 0) {
                    computePageSize(h);
                }
            });
        }
        this.contentSlot.accept(this.state.purchases, this.viewList);

        int pageSize = Math.max(1, this.state.pageSize);
        int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / pageSize));
        this.pageInfoElm.setText((this.state.page + 1) + " / " + totalPages);
    }

    private void buildUI(GluonDom dom, VBox root) {
        root.setPadding(new Insets(16, 12, 8, 12));
        root.setSpacing(12);
        root.setStyle(GluonStyles.PAGE_BG);

        dom.vbox(headerBox -> {
            headerBox.setSpacing(2);

            dom.label(caption -> {
                caption.setText("Histórico de Compras");
                caption.setStyle(GluonStyles.textBold(16, GluonColors.TEXT_DEFAULT));
            });

            dom.label(subtitle -> {
                subtitle.setText("Toque em uma compra para ver os detalhes");
                subtitle.setStyle(GluonStyles.TEXT_MUTED_STYLE);
            });
        });

        dom.scrollVBox((sp, contentBox) -> {
            this.scrollPane = sp;
            VBox.setVgrow(sp, Priority.ALWAYS);
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setStyle(GluonStyles.SCROLL_TRANSPARENT);

            contentBox.setSpacing(10);
            contentBox.setPadding(new Insets(4, 0, 4, 0));
            this.contentSlot = this.newListSlot(contentBox, this::newItemView, this::updateItem);

            sp.heightProperty().addListener((obs, oldVal, newVal) -> {
                scheduleResize(newVal.intValue());
            });
        });

        // Pagination
        dom.hbox(pagination -> {
            pagination.setAlignment(Pos.CENTER);
            pagination.setSpacing(12);
            pagination.setPadding(new Insets(8, 4, 4, 4));

            dom.button(prevBtn -> {
                prevBtn.setGraphic(GluonIcons.create(GluonIcons.CHEVRON_LEFT, 16, GluonColors.TEXT_DEFAULT));
                prevBtn.setStyle(GluonStyles.BTN_PAGINATION);
                prevBtn.setOnAction(e -> safeAction("Prev page",
                        () -> this.presenter.onPageChange(this.state.page - 1)));
            });

            dom.hSpacer();

            this.pageInfoElm = dom.label(pageInfo -> {
                pageInfo.setText("1 / 1");
                pageInfo.setStyle(GluonStyles.PAGINATION_TEXT);
            });

            dom.hSpacer();

            dom.button(nextBtn -> {
                nextBtn.setGraphic(GluonIcons.create(GluonIcons.CHEVRON_RIGHT, 16, GluonColors.TEXT_DEFAULT));
                nextBtn.setStyle(GluonStyles.BTN_PAGINATION);
                nextBtn.setOnAction(e -> safeAction("Next page",
                        () -> this.presenter.onPageChange(this.state.page + 1)));
            });
        });
    }

    private PurchaseItemView newItemView() {
        return new PurchaseItemView(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(PurchaseItemView itemView, PurchaseInfo state) {
        itemView.setState(state);
        itemView.doUpdate();
    }

    private PauseTransition resizeDebounce;
    private int pendingContainerHeight;

    private void scheduleResize(int containerHeight) {
        this.pendingContainerHeight = containerHeight;
        if (this.resizeDebounce == null) {
            this.resizeDebounce = new PauseTransition(Duration.millis(150));
            this.resizeDebounce.setOnFinished(e -> computePageSize(this.pendingContainerHeight));
        }
        this.resizeDebounce.playFromStart();
    }

    private void computePageSize(int containerHeight) {
        if (containerHeight <= 0)
            return;
        int capacity = Math.max(1, containerHeight / ITEM_HEIGHT_PX);
        this.presenter.onItemSizeCapacityChanged(capacity);
    }

    // ---- Inner class ----

    public static class PurchaseItemView extends AbstractViewGluon<PurchasesPanelPresenter> {

        private PurchaseInfo purchase;
        private boolean notRendered = true;
        private Label dateElm;
        private Label itemsElm;
        private Label totalElm;

        private String dateOldValue;
        private String itemsOldValue;
        private String totalOldValue;

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
                GluonDom.render((HBox) this.element, this::buildUI);
                this.notRendered = false;
            }

            var dateNewValue = this.purchase.date > 0
                    ? DATE_FMT.format(Instant.ofEpochMilli(this.purchase.date).atZone(ZoneId.systemDefault()))
                    : "";
            if (!Objects.equals(dateOldValue, dateNewValue)) {
                this.dateElm.setText(dateNewValue);
                this.dateOldValue = dateNewValue;
            }

            var itemsNewValue = formatItems(this.purchase.items);
            if (!Objects.equals(itemsOldValue, itemsNewValue)) {
                this.itemsElm.setText(itemsNewValue);
                this.itemsOldValue = itemsNewValue;
            }

            var totalNewValue = NumberFormat.getCurrencyInstance().format(this.purchase.total);
            if (!Objects.equals(totalOldValue, totalNewValue)) {
                this.totalElm.setText(totalNewValue);
                this.totalOldValue = totalNewValue;
            }
        }

        private void buildUI(GluonDom dom, HBox row) {
            row.setSpacing(12);
            row.setPadding(new Insets(14, 16, 14, 16));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(GluonStyles.CARD_CLICKABLE);
            row.setOnMouseClicked(e -> safeAction("Open receipt",
                    () -> this.presenter.onOpenReceipt(this.purchase.id)));

            dom.vbox(dateBox -> {
                dateBox.setSpacing(2);
                dateBox.setMinWidth(Region.USE_PREF_SIZE);

                this.dateElm = dom.label(date -> {
                    date.setStyle(GluonStyles.TEXT_BODY_STYLE);
                });
            });

            this.itemsElm = dom.label(items -> {
                items.setStyle(GluonStyles.TEXT_MUTED_STYLE);
                items.setMinWidth(0);
                items.setMaxWidth(Double.MAX_VALUE);
                items.setEllipsisString("...");
                HBox.setHgrow(items, Priority.ALWAYS);
            });

            this.totalElm = dom.label(total -> {
                total.setStyle(GluonStyles.PRICE_SMALL);
                total.setMinWidth(Region.USE_PREF_SIZE);
            });

            dom.icon(GluonIcons.create(GluonIcons.CHEVRON_RIGHT, 16, GluonColors.TEXT_PLACEHOLDER));
        }

        private static String formatItems(List<String> items) {
            if (items == null || items.isEmpty()) {
                return "";
            }
            return String.join(", ", items);
        }
    }
}
