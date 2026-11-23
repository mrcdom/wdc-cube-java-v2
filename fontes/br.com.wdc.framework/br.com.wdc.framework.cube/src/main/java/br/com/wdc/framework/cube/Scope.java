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
 * actions, but no presenter of their own. The presenter binds their actions and sets them into a prop of the owner;
 * the view layer draws each one with the view its catalog pairs with the scope class ({@link ScopeViews}, asked
 * through {@link CubeApplication#viewOf}). Until it has a view, a change in a segment redraws its owner; from then
 * on, only its own view. For a remote client the segment is a view like any other: its own
 * {@link #skeleton(String) skeleton}, dispatching to the actions bound with an event code, and the parent refers to
 * it by id.
 */
public abstract class Scope {

    private volatile Runnable updater;

    /** The scope that holds this one, when it adopted it: who is told of a change while this one has no view. */
    private volatile Scope owner;

    private volatile CubeView view;

    /** The actions a remote client fires on this scope, by event code (a presenter's scope shares the presenter's). */
    private Map<Integer, Consumer<Map<String, Object>>> events = new HashMap<>();

    private Consumer<Map<String, Object>> syncState;

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

    /** Asks for a redraw: of this scope's view, or of its owner's while it has none. */
    public final void update() {
        changed();
    }

    /** The scope that holds this one, when it adopted it. */
    public final Scope owner() {
        return owner;
    }

    /** The view that draws this scope, once the view layer made one ({@link CubeApplication#viewOf}); {@code null} before. */
    public final CubeView view() {
        return view;
    }

    /**
     * Records the view that draws this scope. The application does it when it makes the view
     * ({@link CubeApplication#viewOf}); a client that rebuilds scopes from what a remote host sends uses it directly,
     * with the view the host referred to.
     */
    public final void attachView(CubeView view) {
        this.view = view;
    }

    /**
     * A scope known only by the view that draws it: what a client rebuilding a remote host's scopes puts in a prop
     * that holds another presenter's scope, whose type the reference alone does not tell. {@code null} for no view.
     */
    public static Scope drawnBy(CubeView view) {
        if (view == null) {
            return null;
        }
        var scope = new Scope() {
        };
        scope.attachView(view);
        return scope;
    }

    /** What a remote client syncs into this scope before its events (a typed field, for instance). */
    public final void onSync(Consumer<Map<String, Object>> syncState) {
        this.syncState = syncState;
    }

    /**
     * A scope set into a prop of this one (alone or in a collection) that still has no one to tell of its changes is
     * adopted: its changes redraw this scope's view until it gets a view of its own.
     */
    final void adopt(Object value) {
        if (value instanceof Scope child) {
            adoptScope(child);
        } else if (value instanceof Iterable<?> items) {
            for (var item : items) {
                if (item instanceof Scope child) {
                    adoptScope(child);
                }
            }
        }
    }

    private void adoptScope(Scope child) {
        if (child != this && child.updater == null && child.owner == null) {
            child.owner = this;
        }
    }

    /** The presenter that binds this scope keeps the actions it binds with a code: the scope's skeleton sees them. */
    final void shareEvents(Map<Integer, Consumer<Map<String, Object>>> presenterEvents) {
        presenterEvents.putAll(this.events);
        this.events = presenterEvents;
    }

    final void onEvent(int eventCode, Consumer<Map<String, Object>> handler, String owner) {
        putEvent(events, eventCode, handler, owner);
    }

    /** The skeleton of this scope as a remote view: dispatches each event to the action bound with its code. */
    public final CubeSkeleton skeleton(String classId) {
        return newSkeleton(classId, events, syncState, getClass().getSimpleName());
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
            return;
        }
        var o = this.owner;
        if (o != null) {
            o.changed();
        }
    }
}
