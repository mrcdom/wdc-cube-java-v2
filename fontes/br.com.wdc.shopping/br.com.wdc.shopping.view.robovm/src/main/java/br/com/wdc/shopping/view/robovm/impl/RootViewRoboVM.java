package br.com.wdc.shopping.view.robovm.impl;

import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.uikit.UIViewController;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootViewState;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.RoboVMViewSlot;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;

public class RootViewRoboVM extends AbstractViewRoboVM<RootPresenter> {

    private final RootViewState state;
    private final RoboVMViewSlot contentSlot = new RoboVMViewSlot();
    private CubeView currentContentView;

    public RootViewRoboVM(ShoppingRoboVMApplication app, RootPresenter presenter) {
        super("root", app, presenter);
        this.state = presenter.state;
    }

    public RoboVMViewSlot getContentSlot() {
        return contentSlot;
    }

    @Override
    public void doUpdate() {
        contentSlot.setView(state.contentView);
        contentSlot.flush();

        var newContentView = contentSlot.getView();
        if (currentContentView != newContentView) {
            currentContentView = newContentView;

            if (newContentView instanceof AbstractViewRoboVM<?> rvmView) {
                var rootView = rvmView.getRootView();
                if (rootView != null) {
                    var controller = app.getRootController();
                    var vc = new UIViewController();
                    vc.setView(rootView);
                    controller.setViewControllers(new NSArray<>(vc));
                } else {
                    // Child view hasn't built its UI yet; retry next frame
                    currentContentView = null;
                    app.markDirty(this);
                }
            }
        }
    }
}
