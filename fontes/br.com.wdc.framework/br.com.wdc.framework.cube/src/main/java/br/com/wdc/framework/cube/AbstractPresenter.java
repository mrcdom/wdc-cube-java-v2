package br.com.wdc.framework.cube;

import br.com.wdc.framework.commons.function.ThrowingBiConsumer;
import br.com.wdc.framework.commons.function.ThrowingConsumer;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

/**
 * What every presenter has: the application, the view, the bound view state and the guarded actions.
 * <p>
 * An action is bound once, usually in the constructor, into a field of the view state
 * ({@code state.onBuy = action(this::buy)}). The guard catches whatever the body did not handle and hands it to
 * {@link CubeApplication#alertUnexpectedError}; at the end, failing or not, the application republishes the address,
 * since the action may have changed what the presenters publish. Expected failures (an invalid item, an offline
 * database) stay in the body, which turns them into messages in the view state.
 */
public abstract class AbstractPresenter<A extends CubeApplication> implements PresenterBase {

    public final A app;

    protected CubeView view;

    protected AbstractPresenter(A app) {
        this.app = app;
    }

    /**
     * Ties the view state to this presenter: setting one of its props to a different value calls {@link #update()}.
     * Used on the field: {@code public final CartViewState state = bind(new CartViewState());}
     */
    protected final <S extends ViewState> S bind(S state) {
        state.setUpdater(this::update);
        return state;
    }

    public void update() {
        if (view != null) {
            view.update();
        }
    }

    @Override
    public void release() {
        if (view != null) {
            view.release();
            view = null;
        }
    }

    // :: Actions

    protected final Action action(ThrowingRunnable fn) {
        return () -> {
            try {
                fn.runThrows();
            } catch (Exception caught) {
                this.onActionFailure(caught);
            }
            this.afterAction();
        };
    }

    protected final <T> ActionOf<T> actionOf(ThrowingConsumer<T> fn) {
        return value -> {
            try {
                fn.acceptThrows(value);
            } catch (Exception caught) {
                this.onActionFailure(caught);
            }
            this.afterAction();
        };
    }

    protected final <T, U> ActionOf2<T, U> actionOf2(ThrowingBiConsumer<T, U> fn) {
        return (first, second) -> {
            try {
                fn.acceptThrows(first, second);
            } catch (Exception caught) {
                this.onActionFailure(caught);
            }
            this.afterAction();
        };
    }

    /** A failure the action did not handle. */
    protected void onActionFailure(Exception caught) {
        app.alertUnexpectedError("During an action of " + getClass().getSimpleName(), caught);
    }

    private void afterAction() {
        app.updateHistory();
    }
}
