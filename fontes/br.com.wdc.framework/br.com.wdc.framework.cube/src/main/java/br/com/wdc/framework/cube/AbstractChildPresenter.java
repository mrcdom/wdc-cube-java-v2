package br.com.wdc.framework.cube;

/**
 * Base class for presenters whose lifecycle is managed by an owner presenter
 * (not by the navigation/routing system).
 *
 * @param <A> application type
 */
public abstract class AbstractChildPresenter<A extends CubeApplication> extends AbstractPresenter<A> {

    protected AbstractChildPresenter(A app) {
        super(app);
    }

    public final CubeView getView() {
        return view;
    }

    public final CubeView initialize() {
        this.view = onCreateView();
        onInitialize();
        return this.view;
    }

    protected abstract CubeView onCreateView();

    protected abstract void onInitialize();

}
