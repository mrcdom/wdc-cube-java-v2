package br.com.wdc.shopping.view.teavm.commons.interop;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

@JSFunctor
@FunctionalInterface
public interface JsIntConsumer extends JSObject {
    void accept(int value);
}
