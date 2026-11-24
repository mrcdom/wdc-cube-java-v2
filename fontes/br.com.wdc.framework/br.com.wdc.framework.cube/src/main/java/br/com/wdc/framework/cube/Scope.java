package br.com.wdc.framework.cube;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The state of a piece of screen: what is drawn. Observed fields are {@link Prop}s made with {@link #prop}; actions are
 * plain fields ({@link Action}, {@link ActionOf}, {@link ActionOf2}) the presenter binds.
 * <p>
 * The rendering technology installs who is told of a change ({@link #setUpdater}) when it starts drawing the scope, and
 * takes it back ({@link #clearUpdater}) when it stops: setting a prop to a different value asks for a redraw, so the
 * presenter does not call {@code update()} after it. A scope with no updater (one a remote shell fills from JSON) just
 * keeps the values.
 * <p>
 * Changing something INSIDE a value already set (an item of the list) does not go through the prop: set a new value,
 * or call the presenter's {@code update()}.
 * <p>
 * A scope can hold smaller scopes (declared as its inner classes): segments of the screen with their own props and
 * actions, but no presenter of their own. The presenter binds their actions and sets them into a prop of the owner;
 * the rendering technology draws each one as it draws any scope. Until something draws it, a change in a segment
 * redraws its owner; from then on, only what draws the segment. When a scope leaves the props that held it, it is the
 * rendering technology that lets go of what drew it: the presenter does not take part. For a remote client the segment
 * has its own {@link #skeleton(String) skeleton}, dispatching to the actions bound with an event code.
 */
public abstract class Scope {

    private volatile Runnable updater;

    /** The scope that holds this one, when it adopted it: who is told of a change while nothing draws this one. */
    private volatile Scope owner;

    /** The actions a remote client fires on this scope, by event code (a presenter's scope shares the presenter's). */
    private Map<Integer, Consumer<Map<String, Object>>> events = new HashMap<>();

    private Consumer<Map<String, Object>> syncScope;

    /** A prop with no value. */
    protected final <T> Prop<T> prop() {
        return new Prop<>(this, null);
    }

    /** A prop with an initial value. */
    protected final <T> Prop<T> prop(T initial) {
        return new Prop<>(this, initial);
    }

    /** Who is told when a prop changes: what draws this scope. {@code null} tells no one. */
    public final void setUpdater(Runnable updater) {
        this.updater = updater;
    }

    /** Takes the updater back, if it is still the one given: what drew this scope no longer does. */
    public final void clearUpdater(Runnable updater) {
        if (this.updater == updater) {
            this.updater = null;
        }
    }

    /** Asks for a redraw: of what draws this scope, or of its owner while nothing does. */
    public final void update() {
        changed();
    }

    /** The scope that holds this one, when it adopted it. */
    public final Scope owner() {
        return owner;
    }

    /** What a remote client syncs into this scope before its events (a typed field, for instance). */
    public final void onSync(Consumer<Map<String, Object>> syncScope) {
        this.syncScope = syncScope;
    }

    /**
     * A scope set into a prop of this one (alone or in a collection) that still has no one to tell of its changes is
     * adopted: its changes redraw this scope until something draws it.
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

    /** The skeleton of this scope for a remote client: dispatches each event to the action bound with its code. */
    public final CubeSkeleton skeleton(String classId) {
        return newSkeleton(classId, events, syncScope, getClass().getSimpleName());
    }

    static void putEvent(Map<Integer, Consumer<Map<String, Object>>> events, int eventCode,
            Consumer<Map<String, Object>> handler, String owner) {
        if (events.putIfAbsent(eventCode, handler) != null) {
            throw new IllegalStateException(owner + " binds the event code " + eventCode + " more than once");
        }
    }

    static CubeSkeleton newSkeleton(String classId, Map<Integer, Consumer<Map<String, Object>>> events,
            Consumer<Map<String, Object>> syncScope, String owner) {
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
            public void syncScope(Map<String, Object> formData) {
                if (syncScope != null) {
                    syncScope.accept(formData);
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
