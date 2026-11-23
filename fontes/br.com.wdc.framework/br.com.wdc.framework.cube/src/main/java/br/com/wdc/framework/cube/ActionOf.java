package br.com.wdc.framework.cube;

import br.com.wdc.framework.commons.log.Log;

/** An action the view fires with a value (the clicked row, the chosen page). See {@link Action}. */
@FunctionalInterface
public interface ActionOf<T> {

    /** An action declared and not bound, like {@link Action#NOOP}. */
    static <T> ActionOf<T> noop() {
        return value -> Log.getLogger(ActionOf.class).debug("Action not bound: {}", value);
    }

    void run(T value);
}
