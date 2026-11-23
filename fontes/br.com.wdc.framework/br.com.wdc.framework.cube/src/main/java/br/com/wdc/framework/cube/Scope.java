package br.com.wdc.framework.cube;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The render state: what the view draws. Observed fields are {@link Prop}s made with {@link #prop}; actions are plain
 * fields ({@link Action}, {@link ActionOf}, {@link ActionOf2}) the presenter binds.
 * <p>
 * The presenter installs who is told of a change ({@link #setUpdater}, done by {@code AbstractPresenter.bind}): setting
 * a prop to a different value asks for a redraw, so the presenter does not call {@code update()} after it. A state with
 * no updater (one a remote shell fills from JSON) just keeps the values.
 * <p>
 * Changing something INSIDE a value already set (an item of the list) does not go through the prop: set a new value,
 * or call the presenter's {@code update()}.
 * <p>
 * A scope can hold smaller scopes (declared as its inner classes): segments of the view with their own state and
 * actions, but no presenter of their own. The presenter binds their actions and attaches each one to a view
 * ({@code AbstractPresenter.attach}); a change in a segment redraws only its view. For a remote client the segment is
 * a view like any other: its own {@link #skeleton(String) skeleton}, dispatching to the actions bound with an event
 * code, and the parent refers to it by id.
 */
public abstract class Scope {

    private volatile Runnable updater;

    private CubeView view;

    /** The actions a remote client fires on this scope, by event code (a segment's; a presenter keeps its own). */
    private final Map<Integer, Consumer<Map<String, Object>>> events = new HashMap<>();

    /** A prop with no value. */
    protected final <T> Prop<T> prop() {
        return new Prop<>(this, null);
    }

    /** A prop with an initial value. */
    protected final <T> Prop<T> prop(T initial) {
        return new Prop<>(this, initial);
    }

    /** Who is told when a prop changes; {@code null} tells no one. */
    public final void setUpdater(Runnable updater) {
        this.updater = updater;
    }

    /** The view that draws this scope, once a presenter attached it; {@code null} before. */
    public final CubeView view() {
        return view;
    }

    /**
     * Records the view that draws this scope. A presenter does it through {@code AbstractPresenter.attach}; a client
     * that rebuilds scopes from what a remote host sends uses it directly, with the view the host referred to.
     */
    public final void attachView(CubeView view) {
        this.view = view;
    }

    final void onEvent(int eventCode, Consumer<Map<String, Object>> handler, String owner) {
        putEvent(events, eventCode, handler, owner);
    }

    /** The skeleton of this scope as a remote view: dispatches each event to the action bound with its code. */
    public final CubeSkeleton skeleton(String classId) {
        return newSkeleton(classId, events, null, getClass().getSimpleName());
    }

    static void putEvent(Map<Integer, Consumer<Map<String, Object>>> events, int eventCode,
            Consumer<Map<String, Object>> handler, String owner) {
        if (events.putIfAbsent(eventCode, handler) != null) {
            throw new IllegalStateException(owner + " binds the event code " + eventCode + " more than once");
        }
    }

    static CubeSkeleton newSkeleton(String classId, Map<Integer, Consumer<Map<String, Object>>> events,
            Consumer<Map<String, Object>> syncState, String owner) {
        return new CubeSkeleton() {

            @Override
            public String classId() {
                return classId;
            }

            @Override
            public void submit(int eventCode, int eventQtde, Map<String, Object> formData) {
                var handler = events.get(eventCode);
                if (handler == null) {
                    throw new IllegalArgumentException("eventCode(" + eventCode + ") not handled by " + owner);
                }
                handler.accept(formData);
            }

            @Override
            public void syncState(Map<String, Object> formData) {
                if (syncState != null) {
                    syncState.accept(formData);
                }
            }
        };
    }

    final void changed() {
        var u = this.updater;
        if (u != null) {
            u.run();
        }
    }
}
