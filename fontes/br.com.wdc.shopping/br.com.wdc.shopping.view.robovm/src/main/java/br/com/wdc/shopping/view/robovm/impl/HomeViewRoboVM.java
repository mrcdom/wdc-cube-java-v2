package br.com.wdc.shopping.view.robovm.impl;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.NSTextAlignment;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.UIViewContentMode;

import java.util.Objects;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeViewState;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.RoboVMViewSlot;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class HomeViewRoboVM extends AbstractViewRoboVM<HomePresenter> {

    private final HomeViewState state;
    private final RoboVMViewSlot contentSlot = new RoboVMViewSlot();
    private final RoboVMViewSlot productsPanelSlot = new RoboVMViewSlot();
    private final RoboVMViewSlot purchasesPanelSlot = new RoboVMViewSlot();

    private boolean notRendered = true;
    private UILabel welcomeLabel;
    private String nickNameOldValue;
    private UILabel cartBadgeLabel;
    private int cartCountOldValue = -1;
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

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.welcomeLabel = null;
        this.nickNameOldValue = null;
        this.cartBadgeLabel = null;
        this.cartCountOldValue = -1;
        this.contentContainer = null;
        this.productsPanelContainer = null;
        this.purchasesPanelContainer = null;
        this.currentContentView = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.rootView = new UIView(new CGRect(0, 0, 375, 812));
            UIKitDom.render(this.rootView, this::initialRender);
            this.notRendered = false;
        }

        // Sync slots
        contentSlot.setView(state.contentView);
        productsPanelSlot.setView(state.productsPanelView);
        purchasesPanelSlot.setView(state.purchasesPanelView);
        contentSlot.flush();
        productsPanelSlot.flush();
        purchasesPanelSlot.flush();

        // Update welcome text
        if (!Objects.equals(this.nickNameOldValue, state.nickName)) {
            if (state.nickName != null) {
                welcomeLabel.setText("Olá, " + state.nickName);
            }
            this.nickNameOldValue = state.nickName;
        }

        // Update cart badge
        if (this.cartCountOldValue != state.cartItemCount) {
            if (state.cartItemCount > 0) {
                cartBadgeLabel.setText(String.valueOf(state.cartItemCount));
                cartBadgeLabel.setHidden(false);
            } else {
                cartBadgeLabel.setHidden(true);
            }
            this.cartCountOldValue = state.cartItemCount;
        }

        // Determine if we have a content view (product, cart, receipt)
        var newContentView = contentSlot.getView() instanceof AbstractViewRoboVM<?> v ? v : null;

        if (newContentView != currentContentView) {
            currentContentView = newContentView;

            if (newContentView != null) {
                productsPanelContainer.setHidden(true);
                purchasesPanelContainer.setHidden(true);
                contentContainer.setHidden(false);
            } else {
                productsPanelContainer.setHidden(false);
                purchasesPanelContainer.setHidden(false);
                contentContainer.setHidden(true);
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
            app.markDirty(this);
        }
    }

    private void initialRender(UIKitDom dom, UIView root) {
        root.setBackgroundColor(UIColor.fromRGBA(0.95, 0.95, 0.97, 1.0));

        // Header bar
        dom.absolute(375, 104, navBar -> {
            navBar.setBackgroundColor(UIColor.fromRGBA(0.11, 0.22, 0.45, 1.0));

            // Logo
            var logoImage = UIImage.getImage("logo");
            if (logoImage != null) {
                dom.imageView(200, 22, logo -> {
                    logo.setFrame(new CGRect(87, 48, 200, 22));
                    logo.setImage(logoImage);
                    logo.setContentMode(UIViewContentMode.ScaleAspectFit);
                    logo.setTintColor(UIColor.white());
                });
            }

            // Welcome label
            dom.label(255, 26, label -> {
                this.welcomeLabel = label;
                label.setFrame(new CGRect(60, 72, 255, 26));
                label.setTextColor(UIColor.fromRGBA(1.0, 1.0, 1.0, 0.85));
                label.setFont(UIFont.getSystemFont(13));
                label.setTextAlignment(NSTextAlignment.Center);
            });

            // Exit button
            dom.button(56, 36, exitBtn -> {
                exitBtn.setFrame(new CGRect(4, 50, 56, 36));
                exitBtn.setTitle("\u23FB", UIControlState.Normal);
                exitBtn.setTitleColor(UIColor.fromRGBA(1.0, 1.0, 1.0, 0.7), UIControlState.Normal);
                exitBtn.getTitleLabel().setFont(UIFont.getSystemFont(18));
                exitBtn.addOnTouchUpInsideListener((c, e) -> safeAction("exit", () -> presenter.onExit()));
            });

            // Cart button
            dom.button(56, 36, cartBtn -> {
                cartBtn.setFrame(new CGRect(315, 50, 56, 36));
                cartBtn.setTitle("\uD83D\uDED2", UIControlState.Normal);
                cartBtn.getTitleLabel().setFont(UIFont.getSystemFont(22));
                cartBtn.addOnTouchUpInsideListener((c, e) -> safeAction("cart", () -> presenter.onOpenCart()));
            });

            // Cart badge
            dom.label(18, 18, badge -> {
                this.cartBadgeLabel = badge;
                badge.setFrame(new CGRect(349, 46, 18, 18));
                badge.setTextColor(UIColor.white());
                badge.setFont(UIFont.getBoldSystemFont(11));
                badge.setTextAlignment(NSTextAlignment.Center);
                badge.setBackgroundColor(UIColor.fromRGBA(1.0, 0.23, 0.19, 1.0));
                badge.getLayer().setCornerRadius(9);
                badge.setClipsToBounds(true);
            });
        });

        // Doodle background
        dom.embed(createDoodleBackground(dom, 375, 708))
                .setFrame(new CGRect(0, 104, 375, 708));

        // Scrollable content area
        dom.scrollView(375, 708, UIKitDom.LayoutMode.ABSOLUTE, scrollView -> {
            scrollView.setFrame(new CGRect(0, 104, 375, 708));
            scrollView.setBackgroundColor(UIColor.clear());

            dom.absolute(375, 400, pp -> {
                this.productsPanelContainer = pp;
                pp.setBackgroundColor(UIColor.clear());
            });

            dom.absolute(375, 200, pp -> {
                this.purchasesPanelContainer = pp;
                pp.setFrame(new CGRect(0, 410, 375, 200));
                pp.setBackgroundColor(UIColor.clear());
            });

            dom.absolute(375, 708, cc -> {
                this.contentContainer = cc;
                cc.setHidden(true);
            });

            scrollView.setContentSize(new org.robovm.apple.coregraphics.CGSize(375, 620));
        });
    }

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

    private UIView createDoodleBackground(UIKitDom dom, int width, int height) {
        var bg = new UIView(new CGRect(0, 0, width, height));
        bg.setBackgroundColor(UIColor.fromRGBA(0.78, 0.80, 0.85, 1.0));
        bg.setUserInteractionEnabled(false);

        var patternImage = UIImage.getImage("doodle-pattern");
        if (patternImage != null) {
            var imageView = new org.robovm.apple.uikit.UIImageView(new CGRect(0, 0, width, height));
            imageView.setImage(patternImage);
            imageView.setContentMode(UIViewContentMode.ScaleAspectFill);
            imageView.setAlpha(0.08);
            imageView.setUserInteractionEnabled(false);
            bg.addSubview(imageView);
        }

        return bg;
    }
}
