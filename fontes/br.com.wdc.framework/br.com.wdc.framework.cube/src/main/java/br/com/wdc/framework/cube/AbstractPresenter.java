package br.com.wdc.framework.cube;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import br.com.wdc.framework.commons.function.ThrowingBiConsumer;
import br.com.wdc.framework.commons.function.ThrowingConsumer;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

/**
 * What every presenter has: the application, the bound scope and the guarded actions. The presenter only makes scopes:
 * drawing them, and letting go of what drew them once they leave, is up to the rendering technology.
 * <p>
 * An action is bound once, usually in the constructor, into a field of the scope
 * ({@code scope.onBuy = action(this::buy)}). The guard catches whatever the body did not handle and hands it to
 * {@link CubeApplication#alertUnexpectedError}; at the end, failing or not, the application republishes the address,
 * since the action may have changed what the presenters publish. Expected failures (an invalid item, an offline
 * database) stay in the body, which turns them into messages in the scope.
 * <p>
 * An action a remote client fires is bound with its <b>event code</b>, and with how to read its values from the form
 * data ({@link FormParam}): {@code actionOf(2, FormParam.asLong("p.productId"), this::removeProduct)}. The code is the
 * contract with the client; the bound scope's skeleton ({@link Scope#skeleton}) dispatches by it.
 */
public abstract class AbstractPresenter<A extends CubeApplication> implements PresenterBase {

    public final A app;

    /** The actions a remote client fires, by event code. */
    private final Map<Integer, Consumer<Map<String, Object>>> events = new HashMap<>();

    /** The scope bound to this presenter ({@link #bind}). */
    private Scope scope;

    protected AbstractPresenter(A app) {
        this.app = app;
    }

    /**
     * Ties the scope to this presenter: the actions it binds with an event code are dispatched by the scope's
     * skeleton. Used on the field:
     * {@code public final CartScope scope = bind(new CartScope());}
     */
    protected final <S extends Scope> S bind(S scope) {
        scope.shareEvents(this.events);
        this.scope = scope;
        return scope;
    }

    /** The bound scope ({@link #bind}). */
    public final Scope scope() {
        return scope;
    }

    /** Asks for a redraw of the scope: for a change inside a value already set, which no prop sees. */
    public void update() {
        if (scope != null) {
            scope.update();
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

    /** A failure the action did not handle. */
    protected void onActionFailure(Exception caught) {
        app.alertUnexpectedError("During an action of " + getClass().getSimpleName(), caught);
    }

    private void afterAction() {
        app.updateHistory();
    }
}
