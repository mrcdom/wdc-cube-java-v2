package br.com.wdc.shopping.view.vaadin.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.impl.home.PurchaseItemViewVaadin;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class PurchasesPanelViewVaadin extends AbstractViewVaadin<PurchasesPanelPresenter> {

    private final PurchasesPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<PurchaseItemViewVaadin> viewList = new ArrayList<>();
    private BiConsumer<List<PurchaseInfo>, List<PurchaseItemViewVaadin>> contentSlot;
    private Span pageInfoElm;
    private Button prevButton;
    private Button nextButton;
    private VerticalLayout contentArea;

    public PurchasesPanelViewVaadin(PurchasesPanelPresenter presenter) {
        super("purchases-panel", (ShoppingVaadinApplication) presenter.app, presenter, new VerticalLayout());
        this.state = presenter.state;
    }

    @Override
    public void recreate() {
        this.element = new VerticalLayout();
        this.notRendered = true;
        this.viewList.clear();
        this.contentSlot = null;
        this.pageInfoElm = null;
        this.prevButton = null;
        this.nextButton = null;
        this.contentArea = null;
        this.itemIdx = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
            schedulePageSizeComputation();
        }
        this.contentSlot.accept(this.state.purchases, this.viewList);

        int pageSize = Math.max(1, this.state.pageSize);
        int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / pageSize));
        this.pageInfoElm.setText((this.state.page + 1) + " / " + totalPages);
        this.prevButton.setEnabled(this.state.page > 0);
        this.nextButton.setEnabled(this.state.page < totalPages - 1);
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("purchases-panel");
        pane0.setSizeFull();
        pane0.setPadding(true);

        dom.h4(h -> {
            h.addClassName("title");
            h.setText("Seu histórico de compras");
            h.getStyle().set("margin", "0 0 12px 0");
        });

        dom.verticalLayout(pane1 -> {
            this.contentArea = pane1;
            pane1.addClassName("content");
            pane1.getStyle().set("overflow-y", "auto");
            pane0.setFlexGrow(1, pane1);
            this.contentSlot = this.newListSlot(pane1, this::newItemView, this::updateItem);
        });

        dom.horizontalLayout(pane1 -> {
            pane1.addClassName("pagination");
            pane1.setWidthFull();
            pane1.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            pane1.setJustifyContentMode(
                    com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.CENTER);
            pane1.setPadding(false);
            pane1.setSpacing(false);
            pane1.getStyle().set("gap", "var(--lumo-space-xs)");

            dom.button(btn -> {
                this.prevButton = btn;
                btn.setIcon(VaadinIcon.ANGLE_LEFT.create());
                btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
                btn.getStyle().set("color", "#1976d2");
                btn.getElement().setAttribute("aria-label", "Página anterior");
                btn.addClickListener(
                        e -> safeAction("Previous page", () -> this.presenter.onPageChange(this.state.page - 1)));
            });

            dom.span(label -> {
                this.pageInfoElm = label;
                label.getStyle().set("min-width", "50px").set("text-align", "center");
            });

            dom.button(btn -> {
                this.nextButton = btn;
                btn.setIcon(VaadinIcon.ANGLE_RIGHT.create());
                btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
                btn.getStyle().set("color", "#1976d2");
                btn.getElement().setAttribute("aria-label", "Próxima página");
                btn.addClickListener(
                        e -> safeAction("Next page", () -> this.presenter.onPageChange(this.state.page + 1)));
            });
        });
    }

    private PurchaseItemViewVaadin newItemView() {
        return new PurchaseItemViewVaadin(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(PurchaseItemViewVaadin itemView, PurchaseInfo state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }

    private static final int BASE_ITEM_HEIGHT_PX = 56;
    private static final int BASE_FONT_SIZE_PX = 16;

    private void schedulePageSizeComputation() {
        if (this.contentArea == null)
            return;
        // Initial computation + ResizeObserver with debounce for subsequent resize events.
        // Uses root font-size ratio as correction factor to adapt to different
        // devices, zoom levels and accessibility settings.
        this.contentArea.getElement().executeJs(
                "const el = this; const baseIh = $0; const baseFontPx = $1;"
                        + "function itemHeight() {"
                        + "  const rootFs = parseFloat(getComputedStyle(document.documentElement).fontSize) || baseFontPx;"
                        + "  return baseIh * (rootFs / baseFontPx);"
                        + "}"
                        + "function compute() {"
                        + "  const h = el.clientHeight;"
                        + "  if (h > 0) {"
                        + "    const ps = Math.max(1, Math.floor(h / itemHeight()));"
                        + "    if (el.__lastPs !== ps) { el.__lastPs = ps; return ps; }"
                        + "  }"
                        + "  return -1;"
                        + "}"
                        + "if (!el.__resizeObs) {"
                        + "  el.__resizeObs = new ResizeObserver(() => {"
                        + "    clearTimeout(el.__resizeTimer);"
                        + "    el.__resizeTimer = setTimeout(() => {"
                        + "      const ps = compute();"
                        + "      if (ps > 0) el.dispatchEvent(new CustomEvent('page-size', {detail: ps}));"
                        + "    }, 150);"
                        + "  });"
                        + "  el.__resizeObs.observe(el);"
                        + "}"
                        + "return new Promise(r => requestAnimationFrame(() => r(compute())));",
                BASE_ITEM_HEIGHT_PX, BASE_FONT_SIZE_PX).then(result -> {
                    int ps = (int) result.asNumber();
                    if (ps > 0) {
                        this.presenter.onItemSizeCapacityChanged(ps);
                    }
                });

        // Listen for debounced resize-triggered page size changes
        this.contentArea.getElement().addEventListener("page-size", e -> {
            var detail = e.getEventData().getNumber("event.detail");
            int ps = (int) detail;
            if (ps > 0) {
                this.presenter.onItemSizeCapacityChanged(ps);
            }
        }).addEventData("event.detail");
    }
}
