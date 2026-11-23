package br.com.wdc.framework.cube;

public class AbstractCubePresenter<A extends CubeApplication> extends AbstractPresenter<A> implements CubePresenter {

    public AbstractCubePresenter(A app) {
        super(app);
    }

    /** The view the view layer made for this presenter's scope; {@code null} while it has none. */
    public final CubeView view() {
        var scope = scope();
        return scope != null ? scope.view() : null;
    }

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        return true;
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        // NOOP
    }

}
