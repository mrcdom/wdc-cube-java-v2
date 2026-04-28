package br.com.wdc.shopping.presentation.function;

import br.com.wdc.framework.commons.function.ThrowingBiFunction;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.shopping.presentation.ShoppingApplication;

public interface GoAction extends ThrowingBiFunction<ShoppingApplication, CubeIntent, Boolean> {

}
