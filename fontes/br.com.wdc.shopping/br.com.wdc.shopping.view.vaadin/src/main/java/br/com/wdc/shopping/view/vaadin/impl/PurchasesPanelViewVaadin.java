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
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelViewState;
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

    public PurchasesPanelViewVaadin(ShoppingVaadinApplication app, PurchasesPanelPresenter presenter) {
        super("purchases-panel", app, presenter, new VerticalLayout());
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
        this.itemIdx = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }
        this.contentSlot.accept(this.state.purchases, this.viewList);

        int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / this.state.pageSize));
        this.pageInfoElm.setText((this.state.page + 1) + " / " + totalPages);
        this.prevButton.setEnabled(this.state.page > 0);
        this.nextButton.setEnabled(this.state.page < totalPages - 1);
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("purchases-panel");
        pane0.setSizeFull();
        pane0.setPadding(true);

        dom.h4(h -> {
            h.setText("Seu histórico de compras");
            h.getStyle().set("color", "white").set("margin", "0 0 8px 0");
        });

        dom.verticalLayout(pane1 -> {
            pane1.addClassName("content");
            pane1.getStyle().set("overflow-y", "auto");
            pane0.setFlexGrow(1, pane1);
            this.contentSlot = this.newListSlot(pane1, this::newItemView, this::updateItem);
        });

        dom.horizontalLayout(pane1 -> {
            pane1.addClassName("pagination");
            pane1.setWidthFull();
            pane1.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            pane1.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.CENTER);
            pane1.setPadding(false);
            pane1.setSpacing(false);
            pane1.getStyle().set("gap", "var(--lumo-space-xs)");

            dom.button(btn -> {
                this.prevButton = btn;
                btn.setIcon(VaadinIcon.ANGLE_LEFT.create());
                btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
                btn.getStyle().set("color", "white");
                btn.getElement().setAttribute("aria-label", "Página anterior");
                btn.addClickListener(e -> safeAction("Previous page", () -> this.presenter.onPageChange(this.state.page - 1)));
            });

            dom.span(label -> {
                this.pageInfoElm = label;
                label.getStyle().set("min-width", "50px").set("text-align", "center");
            });

            dom.button(btn -> {
                this.nextButton = btn;
                btn.setIcon(VaadinIcon.ANGLE_RIGHT.create());
                btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
                btn.getStyle().set("color", "white");
                btn.getElement().setAttribute("aria-label", "Próxima página");
                btn.addClickListener(e -> safeAction("Next page", () -> this.presenter.onPageChange(this.state.page + 1)));
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
}
