package br.com.wdc.shopping.view.vaadin.impl;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeViewState;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class HomeViewVaadin extends AbstractViewVaadin<HomePresenter> {

    private final HomeViewState state;

    private boolean notRendered = true;
    private Span nickNameElm;
    private String nickNameOldValue;
    private Span cartBadge;
    private int cartCountOldValue;
    private Div contentPane;
    private Div productsPanelSlot;
    private Div purchasesPanelSlot;
    private AbstractViewVaadin<?> currentContentView;

    public HomeViewVaadin(ShoppingVaadinApplication app, HomePresenter presenter) {
        super("home", app, presenter, new VerticalLayout());
        this.state = presenter.state;
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
        if (this.state.productsPanelView instanceof AbstractViewVaadin<?> v) {
            v.recreate();
        }
        if (this.state.purchasesPanelView instanceof AbstractViewVaadin<?> v) {
            v.recreate();
        }
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (this.nickNameOldValue == null || !this.nickNameOldValue.equals(this.state.nickName)) {
            this.nickNameElm.setText("Bem-vindo, " + this.state.nickName + "!");
            this.nickNameOldValue = this.state.nickName;
        }

        if (this.cartCountOldValue != this.state.cartItemCount) {
            this.cartBadge.setText(String.valueOf(this.state.cartItemCount));
            this.cartBadge.setVisible(this.state.cartItemCount > 0);
            this.cartCountOldValue = this.state.cartItemCount;
        }

        if (this.state.productsPanelView instanceof AbstractViewVaadin<?> ppv) {
            if (!this.productsPanelSlot.getChildren().toList().contains(ppv.getElement())) {
                this.productsPanelSlot.removeAll();
                this.productsPanelSlot.add(ppv.getElement());
            }
            ppv.doUpdate();
        }

        if (this.state.purchasesPanelView instanceof AbstractViewVaadin<?> ppv) {
            if (!this.purchasesPanelSlot.getChildren().toList().contains(ppv.getElement())) {
                this.purchasesPanelSlot.removeAll();
                this.purchasesPanelSlot.add(ppv.getElement());
            }
            ppv.doUpdate();
        }

        var newContentView = this.state.contentView instanceof AbstractViewVaadin<?> v ? v : null;
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

        if (this.state.errorCode != 0) {
            Notification.show(this.state.errorMessage, 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            this.state.errorCode = 0;
            this.state.errorMessage = null;
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

            dom.image(img -> {
                img.setSrc("images/logo.png");
                img.setAlt("Logo");
                img.setHeight("30px");
            });

            dom.hSpacer();

            dom.span(label -> {
                this.nickNameElm = label;
                label.addClassName("welcome-label");
                label.setText("Bem-vindo, " + this.state.nickName + "!");
                this.nickNameOldValue = this.state.nickName;
            });

            dom.hSpacer(10);

            // Cart button with icon + badge
            var cartIcon = VaadinIcon.CART.create();
            cartIcon.setSize("20px");
            cartIcon.setColor("white");

            this.cartBadge = new Span(String.valueOf(this.state.cartItemCount));
            this.cartBadge.getElement().getThemeList().add("badge error pill small");
            this.cartBadge.getStyle().set("position", "absolute")
                    .set("top", "-4px").set("right", "-8px")
                    .set("font-size", "10px").set("min-width", "18px")
                    .set("text-align", "center");
            this.cartBadge.setVisible(this.state.cartItemCount > 0);
            this.cartCountOldValue = this.state.cartItemCount;

            var cartButton = new Button("Carrinho", cartIcon,
                    e -> safeAction("Open cart", this.presenter::onOpenCart));
            cartButton.addClassName("cart-button");
            cartButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            cartButton.getStyle().set("color", "white").set("position", "relative");
            cartButton.getElement().appendChild(this.cartBadge.getElement());
            header.add(cartButton);

            dom.hSpacer(10);

            // Exit button with icon
            var exitIcon = VaadinIcon.SIGN_OUT.create();
            exitIcon.setSize("16px");
            dom.button(button -> {
                button.addClassName("exit-button");
                button.setText("SAIR");
                button.setIcon(exitIcon);
                button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                button.addClickListener(e -> safeAction("Exit", this.presenter::onExit));
            });
        });

        // Body: purchases sidebar (left) + main content (right)
        var body = new HorizontalLayout();
        body.setSizeFull();
        body.setSpacing(false);
        body.setPadding(false);
        body.getStyle().set("overflow", "hidden");

        // Purchases sidebar
        this.purchasesPanelSlot = new Div();
        this.purchasesPanelSlot.addClassName("purchases-sidebar");
        body.add(this.purchasesPanelSlot);

        // Main content area with scroller
        this.productsPanelSlot = new Div();
        this.productsPanelSlot.setSizeFull();

        var scroller = new Scroller(Scroller.ScrollDirection.VERTICAL);
        this.contentPane = new Div();
        this.contentPane.addClassName("home-content");
        this.contentPane.setSizeFull();
        this.contentPane.add(this.productsPanelSlot);
        scroller.setContent(this.contentPane);
        scroller.setSizeFull();
        body.add(scroller);
        body.setFlexGrow(1, scroller);

        pane0.add(body);
        pane0.setFlexGrow(1, body);
    }
}
