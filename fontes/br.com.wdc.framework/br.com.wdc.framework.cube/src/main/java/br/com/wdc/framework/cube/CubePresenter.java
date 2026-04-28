package br.com.wdc.framework.cube;

public interface CubePresenter {

    boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest);

    void publishParameters(CubeIntent intent);

    default void commitComputedState() {
        // NOOP
    }

    void release();

}
