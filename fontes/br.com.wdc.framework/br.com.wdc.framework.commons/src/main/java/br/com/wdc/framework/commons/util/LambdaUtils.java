package br.com.wdc.framework.commons.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class LambdaUtils {

    private LambdaUtils() {
        super();
    }
    
    public static <T> T supply(Supplier<T> supplier) {
        return supplier.get();
    }

    public static <T> T configurator(T cfg, Consumer<T> configurator) {
        configurator.accept(cfg);
        return cfg;
    }

}
