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
    private Label prevNavLabel;
    private Label nextNavLabel;
    private HBox paginationRow;

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

        // Update nav label colors based on enabled state (matching Flutter disabled color)
        boolean hasPrev = this.state.page > 0;
        boolean hasNext = this.state.totalCount > 0 && this.state.page < totalPages - 1;
        this.prevNavLabel.setStyle(buildNavLabelStyle(hasPrev ? GluonColors.TEXT_SECONDARY : GluonColors.TEXT_DISABLED));
        this.nextNavLabel.setStyle(buildNavLabelStyle(hasNext ? GluonColors.TEXT_SECONDARY : GluonColors.TEXT_DISABLED));

        // Show pagination only when there are items (matching Flutter if (totalCount > 0))
        boolean hasPagination = this.state.totalCount > 0;
        this.paginationRow.setVisible(hasPagination);
        this.paginationRow.setManaged(hasPagination);
    }

    private static String buildNavLabelStyle(String color) {
        return "-fx-font-size: 18; -fx-text-fill: " + color + "; -fx-cursor: hand; "
                + "-fx-alignment: center; -fx-min-width: 28; -fx-min-height: 28;";
    }

    private void buildUI(GluonDom dom, VBox root) {
        root.setSpacing(0);
        // White surface with left border matching Flutter's purchases panel
        root.setStyle(GluonStyles.PURCHASES_PANEL);

        // Header — history icon + title + subtitle
        dom.hbox(headerRow -> {
            headerRow.setAlignment(Pos.CENTER_LEFT);
            headerRow.setSpacing(8);
            headerRow.setPadding(new Insets(16, 16, 4, 16));

            dom.icon(GluonIcons.create(GluonIcons.HISTORY, 18, GluonColors.PRIMARY));

            dom.vbox(headerText -> {
                dom.label(caption -> {
                    caption.setText("Histórico");
                    caption.setStyle(GluonStyles.textBold(14, GluonColors.TEXT_DEFAULT));
                });
            });
        });

        dom.label(subtitle -> {
            subtitle.setText("Toque para ver detalhes");
            subtitle.setStyle(GluonStyles.text(12, GluonColors.TEXT_SECONDARY) + " -fx-padding: 0 16 12 16;");
        });

        dom.scrollVBox((sp, contentBox) -> {
            this.scrollPane = sp;
            VBox.setVgrow(sp, Priority.ALWAYS);
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setStyle(GluonStyles.SCROLL_TRANSPARENT);

            contentBox.setSpacing(8);
            contentBox.setPadding(new Insets(4, 8, 4, 8));
            this.contentSlot = this.newListSlot(contentBox, this::newItemView, this::updateItem);

            sp.heightProperty().addListener((obs, oldVal, newVal) -> {
                scheduleResize(newVal.intValue());
            });
        });

        // Pagination — matches Flutter pill-style pagination
        this.paginationRow = dom.hbox(pagination -> {
            pagination.setAlignment(Pos.CENTER);
            pagination.setPadding(new Insets(10, 0, 10, 0));
            pagination.setStyle("-fx-border-color: " + GluonColors.BORDER + "; -fx-border-width: 1 0 0 0;");

            // Pill container: appBg bg, rounded corners (matching Flutter radiusRound)
            dom.hbox(pill -> {
                pill.setAlignment(Pos.CENTER);
                pill.setStyle(GluonStyles.PAGINATION_PILL_CONTAINER);

                // Prev ‹
                this.prevNavLabel = dom.label(prev -> {
                    prev.setText("\u2039");
                    prev.setStyle(buildNavLabelStyle(GluonColors.TEXT_DISABLED));
                    prev.setMinWidth(28);
                    prev.setPrefWidth(28);
                    prev.setMinHeight(28);
                    prev.setPrefHeight(28);
                    prev.setAlignment(Pos.CENTER);
                    prev.setOnMouseClicked(e -> safeAction("Prev page",
                            () -> this.presenter.onPageChange(this.state.page - 1)));
                });

                dom.hSpacer(4);

                // Page info pill: white bg, radius 12, subtle shadow
                this.pageInfoElm = dom.label(pageInfo -> {
                    pageInfo.setText("1 / 1");
                    pageInfo.setStyle(GluonStyles.PAGINATION_PAGE_INFO_PILL
                            + GluonStyles.textBold(12, GluonColors.TEXT_DEFAULT));
                });

                dom.hSpacer(4);

                // Next ›
                this.nextNavLabel = dom.label(next -> {
                    next.setText("\u203A");
                    next.setStyle(buildNavLabelStyle(GluonColors.TEXT_DISABLED));
                    next.setMinWidth(28);
                    next.setPrefWidth(28);
                    next.setMinHeight(28);
                    next.setPrefHeight(28);
                    next.setAlignment(Pos.CENTER);
                    next.setOnMouseClicked(e -> safeAction("Next page",
                            () -> this.presenter.onPageChange(this.state.page + 1)));
                });
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
    @SuppressWarnings("java:S1450") // Detectado erroneamente pelo Sonar
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
        private Label idElm;
        private Label dateElm;
        private Label itemsElm;
        private Label totalElm;

        private String idOldValue;
        private String dateOldValue;
        private String itemsOldValue;
        private String totalOldValue;

        PurchaseItemView(ShoppingGluonApplication app, PurchasesPanelPresenter presenter, int idx) {
            super("purchase-item-" + idx, app, presenter, new VBox());
            this.purchase = new PurchaseInfo();
        }

        void setState(PurchaseInfo purchase) {
            this.purchase = purchase;
        }

        @Override
        public void doUpdate() {
            if (this.notRendered) {
                GluonDom.render((VBox) this.element, this::buildUI);
                this.notRendered = false;
            }

            // Row 1 left: #id
            var idNewValue = "#" + this.purchase.id;
            if (!Objects.equals(idOldValue, idNewValue)) {
                this.idElm.setText(idNewValue);
                this.idOldValue = idNewValue;
            }

            // Row 1 right: date
            var dateNewValue = this.purchase.date > 0
                    ? DATE_FMT.format(Instant.ofEpochMilli(this.purchase.date).atZone(ZoneId.systemDefault()))
                    : "";
            if (!Objects.equals(dateOldValue, dateNewValue)) {
                this.dateElm.setText(dateNewValue);
                this.dateOldValue = dateNewValue;
            }

            // Row 2 left: items
            var itemsNewValue = formatItems(this.purchase.items);
            if (!Objects.equals(itemsOldValue, itemsNewValue)) {
                this.itemsElm.setText(itemsNewValue);
                this.itemsOldValue = itemsNewValue;
            }

            // Row 2 right: total
            var totalNewValue = NumberFormat.getCurrencyInstance().format(this.purchase.total);
            if (!Objects.equals(totalOldValue, totalNewValue)) {
                this.totalElm.setText(totalNewValue);
                this.totalOldValue = totalNewValue;
            }
        }

        /**
         * Card layout matching Flutter _PurchaseItem:
         * Container(appBg, border, radius=8) → Column:
         *   Row: #id (accent bold 12) | Spacer | date (secondary 11)
         *   Row: items (secondary 12 ellipsis) | total (bold 12)
         */
        private void buildUI(GluonDom dom, VBox card) {
            card.setSpacing(2);
            card.setPadding(new Insets(8, 12, 8, 12));
            card.setStyle(GluonStyles.PURCHASE_ITEM_CARD);
            card.setOnMouseClicked(e -> safeAction("Open receipt",
                    () -> this.presenter.onOpenReceipt(this.purchase.id)));

            // Row 1: #id | spacer | date
            dom.hbox(row1 -> {
                row1.setAlignment(Pos.CENTER_LEFT);

                this.idElm = dom.label(lbl -> {
                    lbl.setStyle(GluonStyles.textBold(12, GluonColors.PRIMARY));
                });

                dom.hSpacer();

                this.dateElm = dom.label(lbl -> {
                    lbl.setStyle(GluonStyles.text(11, GluonColors.TEXT_SECONDARY));
                });
            });

            // Row 2: items (ellipsis) | total
            dom.hbox(row2 -> {
                row2.setAlignment(Pos.CENTER_LEFT);

                this.itemsElm = dom.label(lbl -> {
                    lbl.setStyle(GluonStyles.text(12, GluonColors.TEXT_SECONDARY));
                    lbl.setMinWidth(0);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.setEllipsisString("...");
                    HBox.setHgrow(lbl, Priority.ALWAYS);
                });

                this.totalElm = dom.label(lbl -> {
                    lbl.setStyle(GluonStyles.textBold(12, GluonColors.TEXT_DEFAULT));
                    lbl.setMinWidth(Region.USE_PREF_SIZE);
                });
            });
        }

        private static String formatItems(List<String> items) {
            if (items == null || items.isEmpty()) {
                return "";
            }
            return String.join(", ", items);
        }
    }
}
