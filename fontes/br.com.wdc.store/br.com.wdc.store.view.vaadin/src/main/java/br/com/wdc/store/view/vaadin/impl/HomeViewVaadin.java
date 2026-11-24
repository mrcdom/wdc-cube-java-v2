package br.com.wdc.store.view.vaadin.impl;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.store.view.vaadin.AbstractViewVaadin;
import br.com.wdc.store.view.vaadin.StoreVaadinApplication;
import br.com.wdc.store.view.vaadin.util.VaadinDom;

public class HomeViewVaadin extends AbstractViewVaadin<HomeScope> {

    private boolean notRendered = true;
    private Span nickNameElm;
    private String nickNameOldValue;
    private Span cartBadge;
    private int cartCountOldValue;
    private Div contentPane;
    private Div productsPanelSlot;
    private Div purchasesPanelSlot;
    private AbstractViewVaadin<?> currentContentView;

    public HomeViewVaadin(StoreVaadinApplication app, HomeScope scope) {
        super("home", app, scope, new VerticalLayout());
    }

    @Override
    public void recreate() {
        this.element = new VerticalLayout();
        this.notRendered = true;
        this.nickNameElm = null;
        this.nickNameOldValue = null;
        this.cartBadge = null;
        this.cartCountOldValue = 0;
        this.contentPane = null;
        this.productsPanelSlot = null;
        this.purchasesPanelSlot = null;
        this.currentContentView = null;
        // Recreate sub-panel views
        if (this.app.viewOf(this.scope.productsPanel.get()) instanceof AbstractViewVaadin<?> v) {
            v.recreate();
        }
        if (this.app.viewOf(this.scope.purchasesPanel.get()) instanceof AbstractViewVaadin<?> v) {
            v.recreate();
        }
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (this.nickNameOldValue == null || !this.nickNameOldValue.equals(this.scope.nickName.get())) {
            this.nickNameElm.setText("Hello, " + this.scope.nickName.get());
            this.nickNameOldValue = this.scope.nickName.get();
        }

        if (this.cartCountOldValue != this.scope.cartItemCount.get()) {
            this.cartBadge.setText(String.valueOf(this.scope.cartItemCount.get()));
            this.cartBadge.setVisible(this.scope.cartItemCount.get() > 0);
            this.cartCountOldValue = this.scope.cartItemCount.get();
        }

        if (this.app.viewOf(this.scope.productsPanel.get()) instanceof AbstractViewVaadin<?> ppv) {
            if (!this.productsPanelSlot.getChildren().toList().contains(ppv.getElement())) {
                this.productsPanelSlot.removeAll();
                this.productsPanelSlot.add(ppv.getElement());
            }
            ppv.doUpdate();
        }

        if (this.app.viewOf(this.scope.purchasesPanel.get()) instanceof AbstractViewVaadin<?> ppv) {
            if (!this.purchasesPanelSlot.getChildren().toList().contains(ppv.getElement())) {
                this.purchasesPanelSlot.removeAll();
                this.purchasesPanelSlot.add(ppv.getElement());
            }
            ppv.doUpdate();
        }

        var newContentView = this.app.viewOf(this.scope.content.get()) instanceof AbstractViewVaadin<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentPane.removeAll();
            if (newContentView != null) {
                this.contentPane.add(newContentView.getElement());
                this.purchasesPanelSlot.setVisible(false);
            } else {
                this.contentPane.add(this.productsPanelSlot);
                this.purchasesPanelSlot.setVisible(true);
            }
            this.currentContentView = newContentView;
        }

        if (this.scope.errorCode.get() != 0) {
            Notification.show(this.scope.errorMessage.get(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            // Consumed while drawing: a redraw now would take the message away
            this.scope.errorCode.setSilently(0);
            this.scope.errorMessage.setSilently(null);
        }
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("home-view");
        pane0.setSizeFull();
        pane0.setPadding(false);
        pane0.setSpacing(false);

        // Header / AppBar
        dom.horizontalLayout(header -> {
            header.addClassName("header");
            header.setWidthFull();
            header.setAlignItems(FlexComponent.Alignment.CENTER);
            header.setPadding(true);

            // Left: Exit + Welcome
            dom.horizontalLayout(left -> {
                left.setAlignItems(FlexComponent.Alignment.CENTER);
                left.setSpacing(true);
                left.setPadding(false);
                left.getStyle().set("flex", "1");

                // Exit button with icon
                var exitIcon = VaadinIcon.POWER_OFF.create();
                exitIcon.setSize("24px");
                dom.button(button -> {
                    button.addClassName("exit-button");
                    button.setIcon(exitIcon);
                    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                    button.getElement().setAttribute("title", "Sign out");
                    button.addClickListener(e -> safeAction("Exit", this.scope.onExit::run));
                });

                dom.span(label -> {
                    this.nickNameElm = label;
                    label.addClassName("welcome-label");
                    label.setText("Hello, " + this.scope.nickName.get());
                    this.nickNameOldValue = this.scope.nickName.get();
                });
            });

            // Center: SVG Logo
            dom.horizontalLayout(center -> {
                center.setAlignItems(FlexComponent.Alignment.CENTER);
                center.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                center.setPadding(false);
                center.setSpacing(false);

                var logo = new Image("images/store-logo.svg", "WDC Store");
                logo.setHeight("32px");
                center.add(logo);
            });

            // Right: Cart button
            dom.horizontalLayout(right -> {
                right.setAlignItems(FlexComponent.Alignment.CENTER);
                right.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
                right.setPadding(false);
                right.getStyle().set("flex", "1");

                // Cart button with icon + badge
                var cartIcon = VaadinIcon.CART.create();
                cartIcon.setSize("24px");
                cartIcon.setColor("white");

                this.cartBadge = new Span(String.valueOf(this.scope.cartItemCount.get()));
                this.cartBadge.getElement().getThemeList().add("badge pill small");
                this.cartBadge.getStyle().set("position", "absolute")
                        .set("top", "-4px").set("right", "-8px")
                        .set("font-size", "10px").set("min-width", "18px")
                        .set("text-align", "center")
                        .set("background-color", "#ff9800")
                        .set("color", "white");
                this.cartBadge.setVisible(this.scope.cartItemCount.get() > 0);
                this.cartCountOldValue = this.scope.cartItemCount.get();

                var cartButton = new Button("Cart", cartIcon,
                        e -> safeAction("Open cart", this.scope.onOpenCart::run));
                cartButton.addClassName("cart-button");
                cartButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                cartButton.getStyle().set("color", "white").set("position", "relative");
                cartButton.getElement().appendChild(this.cartBadge.getElement());
                right.add(cartButton);
            });
        });

        // Body: main content (left) + purchases sidebar (right)
        var body = new HorizontalLayout();
        body.setSizeFull();
        body.setSpacing(false);
        body.setPadding(false);
        body.getStyle().set("overflow", "hidden");

        // Main content area with scroller
        this.productsPanelSlot = new Div();
        this.productsPanelSlot.setWidthFull();

        var scroller = new Scroller(Scroller.ScrollDirection.VERTICAL);
        this.contentPane = new Div();
        this.contentPane.addClassName("home-content");
        this.contentPane.setWidthFull();
        this.contentPane.add(this.productsPanelSlot);
        scroller.setContent(this.contentPane);
        scroller.setSizeFull();
        body.add(scroller);
        body.setFlexGrow(1, scroller);

        // Purchases sidebar (right)
        this.purchasesPanelSlot = new Div();
        this.purchasesPanelSlot.addClassName("purchases-sidebar");
        body.add(this.purchasesPanelSlot);

        pane0.add(body);
        pane0.setFlexGrow(1, body);
    }
}
