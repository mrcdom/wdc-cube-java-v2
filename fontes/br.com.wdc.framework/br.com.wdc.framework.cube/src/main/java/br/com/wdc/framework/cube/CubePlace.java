package br.com.wdc.framework.cube;

import java.util.function.Function;

public interface CubePlace {

    Integer getId();

    String getName();
    
    <A extends CubeApplication> Function<A, CubePresenter> presenterFactory();

}
