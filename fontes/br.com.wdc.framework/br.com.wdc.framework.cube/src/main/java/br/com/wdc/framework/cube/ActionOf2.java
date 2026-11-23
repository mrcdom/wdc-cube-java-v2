package br.com.wdc.framework.cube;

import br.com.wdc.framework.commons.log.Log;

/** An action the view fires with two values (an item and its new quantity). See {@link Action}. */
@FunctionalInterface
public interface ActionOf2<T, U> {

    /** An action declared and not bound, like {@link Action#NOOP}. */
    static <T, U> ActionOf2<T, U> noop() {
        return (first, second) -> Log.getLogger(ActionOf2.class).debug("Action not bound: {}, {}", first, second);
    }

    void run(T first, U second);
}
