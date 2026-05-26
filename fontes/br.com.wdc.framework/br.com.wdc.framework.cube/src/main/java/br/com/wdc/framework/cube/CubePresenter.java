package br.com.wdc.framework.cube;

public interface CubePresenter extends PresenterBase {

    boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest);

    void publishParameters(CubeIntent intent);

}
