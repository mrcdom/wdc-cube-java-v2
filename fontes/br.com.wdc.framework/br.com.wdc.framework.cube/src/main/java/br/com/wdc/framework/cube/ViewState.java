package br.com.wdc.framework.cube;

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
 */
public abstract class ViewState {

    private volatile Runnable updater;

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

    final void changed() {
        var u = this.updater;
        if (u != null) {
            u.run();
        }
    }
}
