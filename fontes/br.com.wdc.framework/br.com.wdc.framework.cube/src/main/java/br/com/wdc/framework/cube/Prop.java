package br.com.wdc.framework.cube;

import java.util.Objects;

/**
 * An observed field of a {@link Scope}. {@link #set} keeps the value and, only when it changed
 * ({@link Objects#equals}), tells the state, which asks for the redraw.
 */
public final class Prop<T> {

    private final Scope owner;

    private volatile T value;

    Prop(Scope owner, T initial) {
        this.owner = owner;
        this.value = initial;
    }

    /** The current value. */
    public T get() {
        return value;
    }

    /** Replaces the value; tells the state only when it changed. */
    public void set(T newValue) {
        if (Objects.equals(value, newValue)) {
            return;
        }
        value = newValue;
        owner.changed();
    }

    /**
     * Replaces the value without telling the state. Only for a view that, while drawing, consumes a one-shot message
     * (an error shown once): it is already drawing, and a redraw would take the message away.
     */
    public void setSilently(T newValue) {
        value = newValue;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
