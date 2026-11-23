package br.com.wdc.framework.cube;

import br.com.wdc.framework.commons.log.Log;

/**
 * An action the view fires (a button). The presenter binds it once, with {@code action(...)}, which guards it: a
 * failure goes to the application instead of to the view. For the view, every action is just {@code run()}.
 */
@FunctionalInterface
public interface Action {

    /** An action declared and not bound: does nothing but leave a trace. */
    Action NOOP = () -> Log.getLogger(Action.class).debug("Action not bound");

    void run();
}
