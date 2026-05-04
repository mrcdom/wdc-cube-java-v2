package br.com.wdc.shopping.view.robovm.impl;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIButtonType;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UIImageView;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.NSTextAlignment;
import org.robovm.apple.uikit.UIScrollView;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.UIViewContentMode;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeViewState;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.RoboVMViewSlot;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;

public class HomeViewRoboVM extends AbstractViewRoboVM<HomePresenter> {

    private final HomeViewState state;
    private final RoboVMViewSlot contentSlot = new RoboVMViewSlot();
    private final RoboVMViewSlot productsPanelSlot = new RoboVMViewSlot();
    private final RoboVMViewSlot purchasesPanelSlot = new RoboVMViewSlot();
    private boolean built;

    private UILabel welcomeLabel;
    private UILabel cartBadgeLabel;
    private UIScrollView scrollView;
    private UIView contentContainer;
    private UIView productsPanelContainer;
    private UIView purchasesPanelContainer;
    private AbstractViewRoboVM<?> currentContentView;

    public HomeViewRoboVM(ShoppingRoboVMApplication app, HomePresenter presenter) {
        super("home", app, presenter);
        this.state = presenter.state;
    }

    public RoboVMViewSlot getContentSlot() {
        return contentSlot;
    }

    public RoboVMViewSlot getProductsPanelSlot() {
        return productsPanelSlot;
    }

    public RoboVMViewSlot getPurchasesPanelSlot() {
        return purchasesPanelSlot;
    }

    private void buildUI() {
        var container = new UIView(new CGRect(0, 0, 375, 812));
        container.setBackgroundColor(UIColor.fromRGBA(0.95, 0.95, 0.97, 1.0));

        // Header bar - bold green shopping brand
        var navBar = new UIView(new CGRect(0, 0, 375, 104));
        navBar.setBackgroundColor(UIColor.fromRGBA(0.11, 0.22, 0.45, 1.0)); // Deep navy blue

        // Logo image (top row, centered)
        var logoImage = UIImage.getImage("logo");
        if (logoImage != null) {
            var logoView = new UIImageView(new CGRect(87, 48, 200, 22));
            logoView.setImage(logoImage);
            logoView.setContentMode(UIViewContentMode.ScaleAspectFit);
            // Tint logo white for green background
            logoView.setTintColor(UIColor.white());
            navBar.addSubview(logoView);
        }

        // Welcome label (second row)
        welcomeLabel = new UILabel(new CGRect(60, 72, 255, 26));
        welcomeLabel.setTextColor(UIColor.fromRGBA(1.0, 1.0, 1.0, 0.85));
        welcomeLabel.setFont(UIFont.getSystemFont(13));
        welcomeLabel.setTextAlignment(NSTextAlignment.Center);
        navBar.addSubview(welcomeLabel);

        // Exit/logout button (top-left) - power icon, no arrow to avoid confusion with back
        var exitButton = new UIButton(UIButtonType.System);
        exitButton.setFrame(new CGRect(4, 50, 56, 36));
        exitButton.setTitle("\u23FB", UIControlState.Normal); // ⏻ power symbol
        exitButton.setTitleColor(UIColor.fromRGBA(1.0, 1.0, 1.0, 0.7), UIControlState.Normal);
        exitButton.getTitleLabel().setFont(UIFont.getSystemFont(18));
        exitButton.addOnTouchUpInsideListener((c, e) -> safeAction("exit", () -> presenter.onExit()));
        navBar.addSubview(exitButton);

        // Cart button (top-right) - white icon
        var cartButton = new UIButton(UIButtonType.System);
        cartButton.setFrame(new CGRect(315, 50, 56, 36));
        cartButton.setTitle("\uD83D\uDED2", UIControlState.Normal);
        cartButton.getTitleLabel().setFont(UIFont.getSystemFont(22));
        cartButton.addOnTouchUpInsideListener((c, e) -> safeAction("cart", () -> presenter.onOpenCart()));
        navBar.addSubview(cartButton);

        // Cart badge
        cartBadgeLabel = new UILabel(new CGRect(349, 46, 18, 18));
        cartBadgeLabel.setTextColor(UIColor.white());
        cartBadgeLabel.setFont(UIFont.getBoldSystemFont(11));
        cartBadgeLabel.setTextAlignment(NSTextAlignment.Center);
        cartBadgeLabel.setBackgroundColor(UIColor.fromRGBA(1.0, 0.23, 0.19, 1.0));
        cartBadgeLabel.getLayer().setCornerRadius(9);
        cartBadgeLabel.setClipsToBounds(true);
        navBar.addSubview(cartBadgeLabel);

        container.addSubview(navBar);

        // Doodle background pattern (WhatsApp-style) - shopping icons
        var doodleBg = createDoodleBackground(375, 708);
        doodleBg.setFrame(new CGRect(0, 104, 375, 708));
        container.addSubview(doodleBg);

        // Scrollable content area (transparent to show doodle behind)
        scrollView = new UIScrollView(new CGRect(0, 104, 375, 708));
        scrollView.setBackgroundColor(UIColor.clear());

        productsPanelContainer = new UIView(new CGRect(0, 0, 375, 400));
        productsPanelContainer.setBackgroundColor(UIColor.clear());
        scrollView.addSubview(productsPanelContainer);

        purchasesPanelContainer = new UIView(new CGRect(0, 410, 375, 200));
        purchasesPanelContainer.setBackgroundColor(UIColor.clear());
        scrollView.addSubview(purchasesPanelContainer);

        contentContainer = new UIView(new CGRect(0, 0, 375, 708));
        contentContainer.setHidden(true);
        scrollView.addSubview(contentContainer);

        scrollView.setContentSize(new org.robovm.apple.coregraphics.CGSize(375, 620));
        container.addSubview(scrollView);

        this.rootView = container;
        this.built = true;
    }

    @Override
    public void doUpdate() {
        if (!built) {
            buildUI();
        }

        // Sync slots
        contentSlot.setView(state.contentView);
        productsPanelSlot.setView(state.productsPanelView);
        purchasesPanelSlot.setView(state.purchasesPanelView);
        contentSlot.flush();
        productsPanelSlot.flush();
        purchasesPanelSlot.flush();

        // Update welcome text
        if (state.nickName != null) {
            welcomeLabel.setText("Olá, " + state.nickName);
        }

        // Update cart badge
        if (state.cartItemCount > 0) {
            cartBadgeLabel.setText(String.valueOf(state.cartItemCount));
            cartBadgeLabel.setHidden(false);
        } else {
            cartBadgeLabel.setHidden(true);
        }

        // Determine if we have a content view (product, cart, receipt)
        var newContentView = contentSlot.getView() instanceof AbstractViewRoboVM<?> v ? v : null;

        if (newContentView != currentContentView) {
            currentContentView = newContentView;

            if (newContentView != null) {
                // Hide panels, show content
                productsPanelContainer.setHidden(true);
                purchasesPanelContainer.setHidden(true);
                contentContainer.setHidden(false);
            } else {
                // Show panels, hide content
                productsPanelContainer.setHidden(false);
                purchasesPanelContainer.setHidden(false);
                contentContainer.setHidden(true);
                // Clear content container
                for (var sub : contentContainer.getSubviews()) {
                    sub.removeFromSuperview();
                }
            }
        }

        // Embed sub-views
        boolean pending = false;
        pending |= !embedView(productsPanelContainer, productsPanelSlot.getView());
        pending |= !embedView(purchasesPanelContainer, purchasesPanelSlot.getView());
        if (newContentView != null) {
            pending |= !embedView(contentContainer, newContentView);
        }
        if (pending) {
            // Child view(s) haven't built UI yet; retry next frame
            app.markDirty(this);
        }
    }

    /**
     * @return true if the view was embedded (or cubeView was null), false if rootView was null (pending)
     */
    private boolean embedView(UIView container, CubeView cubeView) {
        if (cubeView instanceof AbstractViewRoboVM<?> rvmView) {
            var uiView = rvmView.getRootView();
            if (uiView == null) {
                return false;
            }
            if (uiView.getSuperview() != container) {
                for (var sub : container.getSubviews()) {
                    sub.removeFromSuperview();
                }
                uiView.setFrame(container.getBounds());
                container.addSubview(uiView);
            }
        }
        return true;
    }

    private UIView createDoodleBackground(int width, int height) {
        var bg = new UIView(new CGRect(0, 0, width, height));
        // Darker blue-gray background - white panels will really pop
        bg.setBackgroundColor(UIColor.fromRGBA(0.78, 0.80, 0.85, 1.0));
        bg.setUserInteractionEnabled(false);

        // Use doodle pattern image as background texture
        var patternImage = UIImage.getImage("doodle-pattern");
        if (patternImage != null) {
            var imageView = new UIImageView(new CGRect(0, 0, width, height));
            imageView.setImage(patternImage);
            imageView.setContentMode(UIViewContentMode.ScaleAspectFill);
            imageView.setAlpha(0.08);
            imageView.setUserInteractionEnabled(false);
            bg.addSubview(imageView);
        }

        return bg;
    }
}
