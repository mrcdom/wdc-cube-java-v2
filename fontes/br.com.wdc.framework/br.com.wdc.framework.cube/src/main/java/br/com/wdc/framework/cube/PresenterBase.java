package br.com.wdc.framework.cube;

public interface PresenterBase {

    default void commitComputedState() {
        // NOOP
    }

    void release();

}
