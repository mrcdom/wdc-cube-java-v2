package br.com.wdc.store.view.teavm.commons.interop;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

@JSFunctor
@FunctionalInterface
public interface JsStringConsumer extends JSObject {
    void accept(String value);
}
