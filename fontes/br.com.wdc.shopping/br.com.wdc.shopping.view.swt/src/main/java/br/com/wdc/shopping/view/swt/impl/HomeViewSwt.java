package br.com.wdc.shopping.view.swt.impl;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.util.SlotComposite;
import br.com.wdc.shopping.view.swt.util.StackComposite;
import br.com.wdc.shopping.view.swt.util.Styles;

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
        super("home", (ShoppingSwtApplication) presenter.app, presenter,
                createRootComposite((ShoppingSwtApplication) presenter.app));
        this.state = presenter.state;
    }

    private static Composite createRootComposite(ShoppingSwtApplication app) {
        return new Composite(app.getOffscreen(), SWT.NONE);
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
        this.element.layout(true, true);

        // Products panel
        if (this.state.productsPanelView != null) {
            var productsComp = ((AbstractViewSwt<?>) this.state.productsPanelView).getElement();
            this.productsPanelSlot.setContent(productsComp);
        } else {
            this.productsPanelSlot.setContent(null);
        }

        // Purchases panel
        if (this.state.purchasesPanelView != null) {
            var purchasesComp = ((AbstractViewSwt<?>) this.state.purchasesPanelView).getElement();
            this.purchasesPanelSlot.setContent(purchasesComp);
        } else {
            this.purchasesPanelSlot.setContent(null);
        }
    }

    private void initialRender() {
        var layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        this.element.setLayout(layout);
        this.element.setBackground(Styles.BG_PAGE);

        createNavbar(this.element);
        createErrorBanner(this.element);
        createContentArea(this.element);
    }

    // ========== NAVBAR ==========

    private void createNavbar(Composite parent) {
        var navContent = new Composite(parent, SWT.NONE) {
            @Override
            public Point computeSize(int wHint, int hHint, boolean changed) {
                var size = super.computeSize(wHint, hHint, changed);
                size.y = Styles.HEADER_HEIGHT;
                return size;
            }
        };
        var gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.heightHint = Styles.HEADER_HEIGHT;
        navContent.setLayoutData(gd);
        navContent.setBackgroundMode(SWT.INHERIT_FORCE);

        // Use a background image (gradient) so INHERIT_FORCE gives children the correct
        // portion of the gradient as their background (instead of a solid color).
        final org.eclipse.swt.graphics.Image[] bgImage = {null};
        navContent.addListener(SWT.Resize, _e -> {
            var area = navContent.getClientArea();
            if (area.width <= 0 || area.height <= 0) return;
            if (bgImage[0] != null) bgImage[0].dispose();
            bgImage[0] = new org.eclipse.swt.graphics.Image(navContent.getDisplay(), area.width, area.height);
            var gc = new org.eclipse.swt.graphics.GC(bgImage[0]);
            gc.setAntialias(SWT.ON);
            gc.setBackground(Styles.PRIMARY_BLUE_DARK);
            gc.setForeground(Styles.PRIMARY_BLUE_LIGHT);
            gc.fillGradientRectangle(0, 0, area.width, area.height, false);
            gc.dispose();
            navContent.setBackgroundImage(bgImage[0]);
        });
        navContent.addListener(SWT.Dispose, _e -> {
            if (bgImage[0] != null) bgImage[0].dispose();
        });

        var navLayout = new GridLayout(7, false);
        navLayout.marginWidth = 16;
        navLayout.marginHeight = 0;
        navLayout.verticalSpacing = 0;
        navContent.setLayout(navLayout);

        // -- LEFT: Exit button
        var exitBtn = new Label(navContent, SWT.NONE);
        exitBtn.setFont(Styles.FONT_ICON);
        exitBtn.setText(Styles.ICON_BOX_ARROW_LEFT);
        exitBtn.setForeground(Styles.FG_TEXT_WHITE);
        exitBtn.setBackground(null);
        exitBtn.setCursor(navContent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        exitBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
        exitBtn.addListener(SWT.MouseDown, _e -> safeAction("exit", () -> this.presenter.onExit()));

        // Welcome area: "Bem-vindo(a)," + "Nome!"
        var welcomeArea = new Composite(navContent, SWT.NONE);
        welcomeArea.setBackground(null);
        var waLayout = new GridLayout(2, false);
        waLayout.marginWidth = 0;
        waLayout.marginHeight = 0;
        waLayout.horizontalSpacing = 4;
        welcomeArea.setLayout(waLayout);
        welcomeArea.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));

        this.welcomeLabel = new Label(welcomeArea, SWT.NONE);
        this.welcomeLabel.setFont(Styles.FONT_NAV_SMALL);
        this.welcomeLabel.setForeground(Styles.FG_TEXT_WHITE_70);
        this.welcomeLabel.setBackground(null);
        this.welcomeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        this.nickNameLabel = new Label(welcomeArea, SWT.NONE);
        this.nickNameLabel.setFont(Styles.FONT_BODY_BOLD);
        this.nickNameLabel.setForeground(Styles.FG_TEXT_WHITE);
        this.nickNameLabel.setBackground(null);
        this.nickNameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        // -- LEFT spacer
        var spacerLeft = new Label(navContent, SWT.NONE);
        spacerLeft.setBackground(null);
        spacerLeft.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        // -- CENTER: icon with rounded background + title block
        var iconBox = new Canvas(navContent, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);
        var iconBoxGd = new GridData(SWT.RIGHT, SWT.CENTER, false, true);
        iconBoxGd.widthHint = 36;
        iconBoxGd.heightHint = 36;
        iconBox.setLayoutData(iconBoxGd);
        iconBox.addPaintListener(ev -> {
            var gc = ev.gc;
            gc.setAntialias(SWT.ON);
            var area = iconBox.getClientArea();
            // Draw the parent's background image portion at this position
            var parentBgImg = navContent.getBackgroundImage();
            if (parentBgImg != null) {
                var loc = iconBox.getLocation();
                gc.drawImage(parentBgImg, loc.x, loc.y, area.width, area.height, 0, 0, area.width, area.height);
            } else {
                gc.setBackground(Styles.PRIMARY_BLUE_LIGHT);
                gc.fillRectangle(0, 0, area.width, area.height);
            }
            // Rounded box: rgba(255,255,255,0.15) overlay
            gc.setAlpha(38); // 0.15 * 255 ≈ 38
            gc.setBackground(navContent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
            gc.fillRoundRectangle(0, 0, area.width, area.height, 10, 10);
            gc.setAlpha(255);
            // Draw the icon centered
            gc.setFont(Styles.FONT_ICON_NAV);
            gc.setForeground(Styles.FG_TEXT_WHITE);
            var iconText = Styles.ICON_BAG_CHECK_FILL;
            var extent = gc.textExtent(iconText);
            int ix = (area.width - extent.x) / 2;
            int iy = (area.height - extent.y) / 2;
            gc.drawText(iconText, ix, iy, true);
        });

        var centerBlock = new Composite(navContent, SWT.NONE);
        centerBlock.setBackground(null);
        var cbLayout = new GridLayout(1, false);
        cbLayout.marginWidth = 0;
        cbLayout.marginHeight = 0;
        cbLayout.verticalSpacing = 0;
        centerBlock.setLayout(cbLayout);
        centerBlock.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));

        var titleLabel = new Label(centerBlock, SWT.NONE);
        titleLabel.setFont(Styles.FONT_NAV_TITLE);
        titleLabel.setText("Shopping");
        titleLabel.setForeground(Styles.FG_TEXT_WHITE);
        titleLabel.setBackground(null);
        titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));

        var subtitleLabel = new Label(centerBlock, SWT.NONE);
        subtitleLabel.setFont(Styles.FONT_NAV_SMALL);
        subtitleLabel.setText("By WeDoCode");
        subtitleLabel.setForeground(Styles.FG_TEXT_WHITE_65);
        subtitleLabel.setBackground(null);
        subtitleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        // -- RIGHT spacer
        var spacerRight = new Label(navContent, SWT.NONE);
        spacerRight.setBackground(null);
        spacerRight.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        // -- RIGHT: Cart area
        var cartArea = new Composite(navContent, SWT.NONE);
        cartArea.setBackground(null);
        var cartLayout = new GridLayout(3, false);
        cartLayout.marginWidth = 0;
        cartLayout.marginHeight = 0;
        cartLayout.horizontalSpacing = 4;
        cartArea.setLayout(cartLayout);
        cartArea.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, true));
        cartArea.setCursor(navContent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        var cartIcon = new Label(cartArea, SWT.NONE);
        cartIcon.setFont(Styles.FONT_ICON);
        cartIcon.setText(Styles.ICON_BAG);
        cartIcon.setForeground(Styles.FG_TEXT_WHITE);
        cartIcon.setBackground(null);
        cartIcon.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));

        var cartLabel = new Label(cartArea, SWT.NONE);
        cartLabel.setFont(Styles.FONT_BODY);
        cartLabel.setText("Carrinho");
        cartLabel.setForeground(Styles.FG_TEXT_WHITE);
        cartLabel.setBackground(null);
        cartLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));

        this.cartBadge = new Canvas(cartArea, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);
        var badgeGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        badgeGd.widthHint = 20;
        badgeGd.heightHint = 18;
        this.cartBadge.setLayoutData(badgeGd);
        this.cartBadge.addPaintListener(ev -> {
            var gc = ev.gc;
            var area = this.cartBadge.getClientArea();
            gc.setAntialias(SWT.ON);
            // Pill-shaped white background
            gc.setBackground(Styles.FG_TEXT_WHITE);
            gc.fillRoundRectangle(0, 0, area.width, area.height, area.height, area.height);
            // Centered text
            gc.setFont(Styles.FONT_BADGE);
            gc.setForeground(Styles.PRIMARY_BLUE);
            var text = this.cartBadgeText;
            var extent = gc.textExtent(text);
            int tx = (area.width - extent.x) / 2;
            int ty = (area.height - extent.y) / 2;
            gc.drawText(text, tx, ty, true);
        });

        // Click on entire cart area opens cart
        var cartClickListener = new org.eclipse.swt.widgets.Listener() {
            @Override
            public void handleEvent(org.eclipse.swt.widgets.Event event) {
                safeAction("openCart", () -> HomeViewSwt.this.presenter.onOpenCart());
            }
        };
        cartArea.addListener(SWT.MouseDown, cartClickListener);
        cartIcon.addListener(SWT.MouseDown, cartClickListener);
        cartLabel.addListener(SWT.MouseDown, cartClickListener);
    }

    // ========== ERROR BANNER ==========

    private void createErrorBanner(Composite parent) {
        this.errorBanner = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.heightHint = 40;
        gd.exclude = true;
        this.errorBanner.setLayoutData(gd);
        this.errorBanner.setVisible(false);
        this.errorBanner.addPaintListener(ev -> {
            var gc = ev.gc;
            var area = this.errorBanner.getClientArea();
            gc.setAntialias(SWT.ON);
            gc.setBackground(Styles.BG_ERROR);
            gc.fillRectangle(0, 0, area.width, area.height);
            gc.setForeground(Styles.BORDER_ERROR_BOX);
            gc.drawLine(0, area.height - 1, area.width, area.height - 1);
            // Icon
            gc.setForeground(Styles.FG_ERROR);
            gc.setFont(Styles.FONT_ICON);
            Point iconSz = gc.textExtent(Styles.ICON_EXCLAMATION_CIRCLE);
            int iconX = 16;
            int iconY = (area.height - iconSz.y) / 2;
            gc.drawText(Styles.ICON_EXCLAMATION_CIRCLE, iconX, iconY, true);
            // Text
            gc.setFont(Styles.FONT_BODY);
            String msg = this.errorMessage != null ? this.errorMessage : "";
            gc.drawText(msg, iconX + iconSz.x + 10, (area.height - gc.textExtent(msg).y) / 2, true);
        });
    }

    // ========== CONTENT AREA ==========

    private void createContentArea(Composite parent) {
        // Main content row: products (flex) + purchases (fixed 280px)
        var contentRow = new Composite(parent, SWT.NONE);
        contentRow.setBackground(Styles.BG_PAGE);
        contentRow.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        var rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        rowLayout.horizontalSpacing = 0;
        contentRow.setLayout(rowLayout);

        // Left: content stack (default shows products, overlay replaces)
        this.contentStack = new StackComposite(contentRow);
        this.contentStack.setBackground(Styles.BG_PAGE);
        this.contentStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Default content (products slot)
        this.defaultContent = new Composite(this.contentStack, SWT.NONE);
        this.defaultContent.setBackground(Styles.BG_PAGE);
        var dcLayout = new GridLayout(1, false);
        dcLayout.marginWidth = 0;
        dcLayout.marginHeight = 0;
        this.defaultContent.setLayout(dcLayout);

        this.productsPanelSlot = new SlotComposite(this.defaultContent, this.app.getOffscreen());
        this.productsPanelSlot.setBackground(Styles.BG_PAGE);
        this.productsPanelSlot.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Content overlay slot (for cart/product/receipt overlays)
        this.contentOverlaySlot = new SlotComposite(this.contentStack, this.app.getOffscreen());
        this.contentOverlaySlot.setBackground(Styles.BG_PAGE);

        this.contentStack.showControl(this.defaultContent);

        // Right: purchases panel slot (fixed width)
        this.purchasesPanelSlot = new SlotComposite(contentRow, this.app.getOffscreen());
        this.purchasesPanelSlot.setBackground(Styles.BG_WHITE);
        var purchGd = new GridData(SWT.FILL, SWT.FILL, false, true);
        purchGd.widthHint = 340;
        this.purchasesPanelSlot.setLayoutData(purchGd);
    }
}
