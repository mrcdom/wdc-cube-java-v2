package br.com.wdc.shopping.view.swt.impl;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.theme.Surface;
import br.com.wdc.shopping.view.swt.theme.Theme;
import br.com.wdc.shopping.view.swt.util.SlotComposite;
import br.com.wdc.shopping.view.swt.util.StackComposite;
import br.com.wdc.shopping.view.swt.util.SwtDom;
import static br.com.wdc.shopping.view.swt.util.GridDataUtils.*;

/**
 * Home view — header + content area (products grid + purchases panel).
 * When a content overlay is active (cart/product/receipt), it replaces the default content.
 */
public class HomeViewSwt extends AbstractViewSwt<HomePresenter> {

    private final HomeViewState state;

    private boolean notRendered = true;
    private Canvas cartBadge;
    private String cartBadgeText = "0";
    private Label welcomeLabel;
    private Label nickNameLabel;
    private Canvas errorBanner;
    private String errorMessage;
    private StackComposite contentStack;
    private Composite defaultContent;
    private SlotComposite productsPanelSlot;
    private SlotComposite purchasesPanelSlot;
    private SlotComposite contentOverlaySlot;

    public HomeViewSwt(HomePresenter presenter) {
        super("home", (ShoppingSwtApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        // Welcome
        String prefix = "Bem-vindo(a),";
        if (!Objects.equals(this.welcomeLabel.getText(), prefix)) {
            this.welcomeLabel.setText(prefix);
        }
        String name = (this.state.nickName != null ? this.state.nickName : "") + "!";
        if (!Objects.equals(this.nickNameLabel.getText(), name)) {
            this.nickNameLabel.setText(name);
            this.nickNameLabel.getParent().layout(true);
        }

        // Cart badge (always visible, shows count)
        String badge = String.valueOf(this.state.cartItemCount);
        if (!Objects.equals(this.cartBadgeText, badge)) {
            this.cartBadgeText = badge;
            this.cartBadge.redraw();
        }

        // Error
        var newErrorDisplay = false;
        var newErrorMessage = "";
        if (this.state.errorCode != 0) {
            newErrorDisplay = true;
            newErrorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        }
        if (!Objects.equals(this.errorMessage, newErrorMessage)) {
            this.errorMessage = newErrorMessage;
            this.errorBanner.redraw();
        }
        if (this.errorBanner.getVisible() != newErrorDisplay) {
            this.errorBanner.setVisible(newErrorDisplay);
            ((GridData) this.errorBanner.getLayoutData()).exclude = !newErrorDisplay;
            this.element.layout(true, true);
        }

        // Content overlay
        if (this.state.contentView != null) {
            var overlay = ((AbstractViewSwt<?>) this.state.contentView).getElement();
            this.contentOverlaySlot.setContent(overlay);
            this.contentStack.showControl(this.contentOverlaySlot);
            this.purchasesPanelSlot.setVisible(false);
            ((GridData) this.purchasesPanelSlot.getLayoutData()).exclude = true;
        } else {
            this.contentOverlaySlot.setContent(null);
            this.contentStack.showControl(this.defaultContent);
            this.purchasesPanelSlot.setVisible(true);
            ((GridData) this.purchasesPanelSlot.getLayoutData()).exclude = false;
        }

        // Products panel
        if (this.state.productsPanelView != null) {
            var productsComp = ((AbstractViewSwt<?>) this.state.productsPanelView).getElement();
            this.productsPanelSlot.setContent(productsComp);
        } else {
            this.productsPanelSlot.setContent(null);
        }

        // Purchases panel
        if (this.state.purchasesPanelView != null) {
            var purchasesView = (AbstractViewSwt<?>) this.state.purchasesPanelView;
            this.purchasesPanelSlot.setContent(purchasesView.getElement());
        } else {
            this.purchasesPanelSlot.setContent(null);
        }

        // Layout first so child panels get their actual dimensions (triggers resize listeners)
        this.element.layout(true, true);
    }

    private void initialRender() {
        var layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        this.element.setLayout(layout);
        this.element.setBackground(Theme.BG_PAGE);

        SwtDom.render(this.element, (dom, root) -> {
            createNavbar(dom);
            createErrorBanner(dom);
            createContentArea(dom);
        });
    }

    // ========== NAVBAR ==========

    private void createNavbar(SwtDom dom) {
        dom.row(7, navContent -> {
            var navGd = new GridData();
            gdFillH(navGd);
            gdHeight(navGd, Theme.HEADER_HEIGHT);
            gdTop(navGd);
            navContent.setLayoutData(navGd);
            navContent.setBackgroundMode(SWT.INHERIT_FORCE);

            // Override computeSize to enforce header height
            navContent.addListener(SWT.MeasureItem, _e -> {});
            // NOTE: we use heightHint on GridData to enforce height

            // Background gradient image
            final org.eclipse.swt.graphics.Image[] bgImage = {null};
            navContent.addListener(SWT.Resize, _e -> {
                var area = navContent.getClientArea();
                if (area.width <= 0 || area.height <= 0) return;
                if (bgImage[0] != null) bgImage[0].dispose();
                bgImage[0] = new org.eclipse.swt.graphics.Image(navContent.getDisplay(), area.width, area.height);
                var gc = new org.eclipse.swt.graphics.GC(bgImage[0]);
                gc.setAntialias(SWT.ON);
                gc.setBackground(Theme.PRIMARY_BLUE_DARK);
                gc.setForeground(Theme.PRIMARY_BLUE_LIGHT);
                gc.fillGradientRectangle(0, 0, area.width, area.height, false);
                gc.dispose();
                navContent.setBackgroundImage(bgImage[0]);
            });
            navContent.addListener(SWT.Dispose, _e -> {
                if (bgImage[0] != null) bgImage[0].dispose();
            });

            var navLayout = (GridLayout) navContent.getLayout();
            navLayout.marginWidth = 16;

            // -- LEFT: Exit button
            dom.label(exitBtn -> {
                exitBtn.setFont(Theme.FONT_ICON);
                exitBtn.setText(Theme.ICON_BOX_ARROW_LEFT);
                exitBtn.setForeground(Theme.FG_TEXT_WHITE);
                exitBtn.setBackground(null);
                exitBtn.setCursor(navContent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                var exitBtnGd = new GridData();
                gdLeft(exitBtnGd);
                gdGrabV(exitBtnGd);
                exitBtn.setLayoutData(exitBtnGd);
                exitBtn.addListener(SWT.MouseDown, _e -> safeAction("exit", this.presenter::onExit));
            });

            // Welcome area: "Bem-vindo(a)," + "Nome!"
            dom.row(2, welcomeArea -> {
                welcomeArea.setBackground(null);
                var welcomeGd = new GridData();
                gdLeft(welcomeGd);
                gdGrabV(welcomeGd);
                welcomeArea.setLayoutData(welcomeGd);
                var waLayout = (GridLayout) welcomeArea.getLayout();
                waLayout.horizontalSpacing = 4;

                this.welcomeLabel = dom.label(lbl -> {
                    lbl.setFont(Theme.FONT_NAV_SMALL);
                    lbl.setForeground(Theme.FG_TEXT_WHITE_70);
                    lbl.setBackground(null);
                    lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
                });

                this.nickNameLabel = dom.label(lbl -> {
                    lbl.setFont(Theme.FONT_BODY_BOLD);
                    lbl.setForeground(Theme.FG_TEXT_WHITE);
                    lbl.setBackground(null);
                    lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
                });
            });

            // -- LEFT spacer
            dom.label(lbl -> {
                lbl.setBackground(null);
                lbl.setLayoutData(gdFill(new GridData()));
            });

            // -- CENTER: icon with rounded background
            dom.canvas(SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED, iconBox -> {
            	var iconGd = new GridData();
            	gdRight(iconGd);
                gdGrabV(iconGd);
                iconGd.widthHint = 36;
                iconGd.heightHint = 36;
                iconBox.setLayoutData(iconGd);
                iconBox.addPaintListener(ev -> paintNavIconBox(ev.gc, iconBox, navContent));
            });

            // CENTER: title block
            dom.col(centerBlock -> {
                centerBlock.setBackground(null);
                var centerGd = new GridData();
                gdLeft(centerGd);
                gdGrabV(centerGd);
                centerBlock.setLayoutData(centerGd);

                dom.label(lbl -> {
                    lbl.setFont(Theme.FONT_NAV_TITLE);
                    lbl.setText("Shopping");
                    lbl.setForeground(Theme.FG_TEXT_WHITE);
                    lbl.setBackground(null);
                    lbl.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
                });

                dom.label(lbl -> {
                    lbl.setFont(Theme.FONT_NAV_SMALL);
                    lbl.setText("By WeDoCode");
                    lbl.setForeground(Theme.FG_TEXT_WHITE_65);
                    lbl.setBackground(null);
                    lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
                });
            });

            // -- RIGHT spacer
            dom.label(lbl -> {
                lbl.setBackground(null);
                lbl.setLayoutData(gdFill(new GridData()));
            });

            // -- RIGHT: Cart area
            dom.row(3, cartArea -> {
                cartArea.setBackground(null);
                var cartGd = new GridData();
                gdRight(cartGd);
                gdGrabV(cartGd);
                cartArea.setLayoutData(cartGd);
                var cartLayout = (GridLayout) cartArea.getLayout();
                cartLayout.horizontalSpacing = 4;
                cartArea.setCursor(navContent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

                var cartIcon = dom.label(lbl -> {
                    lbl.setFont(Theme.FONT_ICON);
                    lbl.setText(Theme.ICON_BAG);
                    lbl.setForeground(Theme.FG_TEXT_WHITE);
                    lbl.setBackground(null);
                    var cartIconGd = new GridData();
                    gdLeft(cartIconGd);
                    gdGrabV(cartIconGd);
                    lbl.setLayoutData(cartIconGd);
                });

                var cartLabel = dom.label(lbl -> {
                    lbl.setFont(Theme.FONT_BODY);
                    lbl.setText("Carrinho");
                    lbl.setForeground(Theme.FG_TEXT_WHITE);
                    lbl.setBackground(null);
                    var cartLabelGd = new GridData();
                    gdLeft(cartLabelGd);
                    gdGrabV(cartLabelGd);
                    lbl.setLayoutData(cartLabelGd);
                });

                this.cartBadge = dom.canvas(SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED, badge -> {
                    var badgeGd = gdCenter(new GridData());
                    badgeGd.widthHint = 20;
                    badgeGd.heightHint = 18;
                    badge.setLayoutData(badgeGd);
                    badge.addPaintListener(ev -> paintCartBadge(ev.gc, badge.getClientArea()));
                });

                // Click on entire cart area opens cart
                var cartClickListener = (org.eclipse.swt.widgets.Listener) event ->
                        safeAction("openCart", HomeViewSwt.this.presenter::onOpenCart);
                cartArea.addListener(SWT.MouseDown, cartClickListener);
                cartIcon.addListener(SWT.MouseDown, cartClickListener);
                cartLabel.addListener(SWT.MouseDown, cartClickListener);
            });
        });
    }

    // ========== ERROR BANNER ==========

    private void createErrorBanner(SwtDom dom) {
        this.errorBanner = dom.canvas(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, canvas -> {
            var errorGd = new GridData();
            gdFillH(errorGd);
            gdTop(errorGd);
            errorGd.heightHint = 40;
            errorGd.exclude = true;
            canvas.setLayoutData(errorGd);
            canvas.setVisible(false);
            canvas.addPaintListener(Surface.errorBanner(canvas::getClientArea, () -> this.errorMessage));
        });
    }

    // ========== CONTENT AREA ==========

    private void createContentArea(SwtDom dom) {
        dom.row(2, contentRow -> {
            contentRow.setBackground(Theme.BG_PAGE);
            contentRow.setLayoutData(gdFill(new GridData()));

            // Left: content stack (default shows products, overlay replaces)
            this.contentStack = dom.stack(stack -> {
                stack.setBackground(Theme.BG_PAGE);
                stack.setLayoutData(gdFill(new GridData()));
            });

            // Default content (products slot) — created inside stack
            this.defaultContent = new Composite(this.contentStack, SWT.NONE);
            this.defaultContent.setBackground(Theme.BG_PAGE);
            var dcLayout = new GridLayout(1, false);
            dcLayout.marginWidth = 0;
            dcLayout.marginHeight = 0;
            this.defaultContent.setLayout(dcLayout);

            this.productsPanelSlot = new SlotComposite(this.defaultContent, this.app.getOffscreen());
            this.productsPanelSlot.setBackground(Theme.BG_PAGE);
            this.productsPanelSlot.setLayoutData(gdFill(new GridData()));

            // Content overlay slot (for cart/product/receipt overlays)
            this.contentOverlaySlot = new SlotComposite(this.contentStack, this.app.getOffscreen());
            this.contentOverlaySlot.setBackground(Theme.BG_PAGE);

            this.contentStack.showControl(this.defaultContent);

            // Right: purchases panel slot (fixed width)
            this.purchasesPanelSlot = dom.slot(this.app.getOffscreen(), slot -> {
                slot.setBackground(Theme.BG_WHITE);
                var slotGd = new GridData();
                gdFillV(slotGd);
                gdWidth(slotGd, 340);
                slot.setLayoutData(slotGd);
            });
        });
    }

    // ========== SURFACES ==========

    private void paintNavIconBox(GC gc, Canvas iconBox, Composite navContent) {
        gc.setAntialias(SWT.ON);
        var area = iconBox.getClientArea();
        // Draw the parent's background image portion at this position
        var parentBgImg = navContent.getBackgroundImage();
        if (parentBgImg != null) {
            var loc = iconBox.getLocation();
            gc.drawImage(parentBgImg, loc.x, loc.y, area.width, area.height, 0, 0, area.width, area.height);
        } else {
            gc.setBackground(Theme.PRIMARY_BLUE_LIGHT);
            gc.fillRectangle(0, 0, area.width, area.height);
        }
        // Rounded box: rgba(255,255,255,0.15) overlay
        gc.setAlpha(38);
        gc.setBackground(navContent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        gc.fillRoundRectangle(0, 0, area.width, area.height, 10, 10);
        gc.setAlpha(255);
        // Icon centered
        gc.setFont(Theme.FONT_ICON_NAV);
        gc.setForeground(Theme.FG_TEXT_WHITE);
        var iconText = Theme.ICON_BAG_CHECK_FILL;
        var extent = gc.textExtent(iconText);
        gc.drawText(iconText, (area.width - extent.x) / 2, (area.height - extent.y) / 2, true);
    }

    private void paintCartBadge(GC gc, Rectangle area) {
        gc.setAntialias(SWT.ON);
        Surface.drawPill(gc, area, Theme.FG_TEXT_WHITE);
        gc.setFont(Theme.FONT_BADGE);
        gc.setForeground(Theme.PRIMARY_BLUE);
        var text = this.cartBadgeText;
        var extent = gc.textExtent(text);
        gc.drawText(text, (area.width - extent.x) / 2, (area.height - extent.y) / 2, true);
    }

}
