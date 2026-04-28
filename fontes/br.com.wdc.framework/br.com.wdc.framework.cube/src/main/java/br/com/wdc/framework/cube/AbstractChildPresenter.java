package br.com.wdc.framework.cube;

/**
 * Base class for presenters whose lifecycle is managed by an owner presenter
 * (not by the navigation/routing system).
 *
 * @param <A> application type
 * @param <O> owner presenter type
 * @param <S> state type
 */
public abstract class AbstractChildPresenter<A extends CubeApplication> {

    public final A app;

    protected CubeView view;

    protected AbstractChildPresenter(A app) {
        this.app = app;
    }

    public final CubeView getView() {
        return view;
    }

    public final CubeView initialize() {
        this.view = onCreateView();
        onInitialize();
        return this.view;
    }

    public void release() {
        if (view != null) {
            view.release();
            view = null;
        }
    }

    public void update() {
        if (view != null) {
            view.update();
        }
    }

    protected abstract CubeView onCreateView();

    protected abstract void onInitialize();

}
