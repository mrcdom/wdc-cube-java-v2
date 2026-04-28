package br.com.wdc.framework.commons.function;

public interface Registration {

    static Registration noop() {
        return ThrowingConsts.NOOP_REGISTRATION;
    }

    void remove();

}
