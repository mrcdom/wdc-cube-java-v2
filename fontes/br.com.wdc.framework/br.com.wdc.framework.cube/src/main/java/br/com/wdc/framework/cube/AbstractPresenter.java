package br.com.wdc.framework.cube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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
 * <p>
 * An action a remote client fires is bound with its <b>event code</b>, and with how to read its values from the form
 * data ({@link FormParam}): {@code actionOf(2, FormParam.asLong("p.productId"), this::removeProduct)}. The code is the
 * contract with the client; the presenter's skeleton is then just {@code skeleton(classId)}, which dispatches by it.
 */
public abstract class AbstractPresenter<A extends CubeApplication> implements PresenterBase {

    public final A app;

    protected CubeView view;

    /** The actions a remote client fires, by event code. */
    private final Map<Integer, Consumer<Map<String, Object>>> events = new HashMap<>();

    /** The segments attached to a view, released with this presenter. */
    private final List<Scope> segments = new ArrayList<>();

    protected AbstractPresenter(A app) {
        this.app = app;
    }

    /**
     * Ties the view state to this presenter: setting one of its props to a different value calls {@link #update()}.
     * Used on the field: {@code public final CartScope state = bind(new CartScope());}
     */
    protected final <S extends Scope> S bind(S state) {
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
        for (var segment : segments) {
            segment.setUpdater(null);
            segment.view().release();
        }
        segments.clear();
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

    // :: Actions a remote client fires, by event code

    protected final Action action(int eventCode, ThrowingRunnable fn) {
        var action = action(fn);
        onEvent(eventCode, formData -> action.run());
        return action;
    }

    protected final <T> ActionOf<T> actionOf(int eventCode, FormParam<T> param, ThrowingConsumer<T> fn) {
        var action = actionOf(fn);
        onEvent(eventCode, formData -> action.run(param.read(formData)));
        return action;
    }

    protected final <T, U> ActionOf2<T, U> actionOf2(int eventCode, FormParam<T> first, FormParam<U> second,
            ThrowingBiConsumer<T, U> fn) {
        var action = actionOf2(fn);
        onEvent(eventCode, formData -> action.run(first.read(formData), second.read(formData)));
        return action;
    }

    private void onEvent(int eventCode, Consumer<Map<String, Object>> handler) {
        Scope.putEvent(events, eventCode, handler, getClass().getSimpleName());
    }

    // :: Actions of a segment (a scope inside the presenter's scope), by event code

    protected final Action action(Scope target, int eventCode, ThrowingRunnable fn) {
        var action = action(fn);
        target.onEvent(eventCode, formData -> action.run(), target.getClass().getSimpleName());
        return action;
    }

    protected final <T> ActionOf<T> actionOf(Scope target, int eventCode, FormParam<T> param, ThrowingConsumer<T> fn) {
        var action = actionOf(fn);
        target.onEvent(eventCode, formData -> action.run(param.read(formData)), target.getClass().getSimpleName());
        return action;
    }

    protected final <T, U> ActionOf2<T, U> actionOf2(Scope target, int eventCode, FormParam<T> first,
            FormParam<U> second, ThrowingBiConsumer<T, U> fn) {
        var action = actionOf2(fn);
        target.onEvent(eventCode, formData -> action.run(first.read(formData), second.read(formData)),
                target.getClass().getSimpleName());
        return action;
    }

    // :: Segments

    /**
     * Attaches a segment to a view made by the host's factory: from then on, a change in the segment redraws only that
     * view. The view is released with this presenter.
     */
    protected final <S extends Scope> S attach(S segment, BiFunction<A, S, CubeView> createView) {
        var segmentView = createView.apply(app, segment);
        segment.attachView(segmentView);
        segment.setUpdater(segmentView::update);
        segments.add(segment);
        // The first drawing: the view may need it before any change (to measure itself, for instance)
        segmentView.update();
        return segment;
    }

    /** Releases the view of a segment that left the screen (a row removed from a list). */
    protected final void detach(Scope segment) {
        if (segments.remove(segment)) {
            segment.setUpdater(null);
            segment.view().release();
        }
    }

    /** The skeleton of a remote view: dispatches each event to the action bound with its code. */
    protected final CubeSkeleton skeleton(String classId) {
        return skeleton(classId, null);
    }

    /**
     * The same, for a view that also syncs the form before the events ({@link CubeSkeleton#syncState}).
     *
     * @param syncState {@code null} when there is nothing to sync
     */
    protected final CubeSkeleton skeleton(String classId, Consumer<Map<String, Object>> syncState) {
        return Scope.newSkeleton(classId, events, syncState, getClass().getSimpleName());
    }

    /** A failure the action did not handle. */
    protected void onActionFailure(Exception caught) {
        app.alertUnexpectedError("During an action of " + getClass().getSimpleName(), caught);
    }

    private void afterAction() {
        app.updateHistory();
    }
}
