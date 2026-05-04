package br.com.wdc.shopping.view.robovm;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.UIView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.CubeView;

public abstract class AbstractViewRoboVM<P> implements CubeView {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractViewRoboVM.class);

    protected final String instanceId;
    protected final ShoppingRoboVMApplication app;
    protected final P presenter;
    protected UIView rootView;

    long dirtyTimestamp;

    protected AbstractViewRoboVM(String instanceId, ShoppingRoboVMApplication app, P presenter) {
        this.instanceId = instanceId;
        this.app = app;
        this.presenter = presenter;
    }

    @Override
    public String instanceId() {
        return this.instanceId;
    }

    @Override
    public void update() {
        this.app.markDirty(this);
    }

    @Override
    public void release() {
        // Subclasses can override
    }

    public UIView getRootView() {
        return this.rootView;
    }

    public abstract void doUpdate();

    public void rebuild() {
        this.rootView = null;
        this.onRebuild();
        this.update();
    }

    protected void onRebuild() {
        // Subclasses override to reset notRendered and cached state
    }

    protected void safeAction(String context, Runnable action) {
        try {
            action.run();
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, context, caught);
        }
    }

    protected <T, V extends AbstractViewRoboVM<?>> BiConsumer<List<T>, List<V>> newListSlot(
            UIView container, Supplier<V> factory, BiConsumer<V, T> updater) {
        return (items, viewList) -> {
            this.syncListSlot(container, items, viewList, factory, updater);
        };
    }

    private <T, V extends AbstractViewRoboVM<?>> void syncListSlot(
            UIView container, List<T> items, List<V> viewList,
            Supplier<V> factory, BiConsumer<V, T> updater) {

        var newSize = items != null ? items.size() : 0;
        var oldSize = viewList.size();

        // Remove excess views
        if (oldSize > newSize) {
            for (int i = oldSize - 1; i >= newSize; i--) {
                var view = viewList.remove(i);
                if (view.getRootView() != null) {
                    view.getRootView().removeFromSuperview();
                }
            }
        }

        // Add missing views (without addSubview yet — rootView is null until doUpdate)
        while (viewList.size() < newSize) {
            viewList.add(factory.get());
        }

        // Update all, then position and ensure they are in the container
        double yOffset = 0;
        for (int i = 0; i < newSize; i++) {
            var view = viewList.get(i);
            updater.accept(view, items.get(i));
            var rv = view.getRootView();
            if (rv != null) {
                var frame = rv.getFrame();
                rv.setFrame(new CGRect(frame.getX(), yOffset, frame.getWidth(), frame.getHeight()));
                if (rv.getSuperview() == null) {
                    container.addSubview(rv);
                }
                yOffset += frame.getHeight();
            }
        }
    }
}
