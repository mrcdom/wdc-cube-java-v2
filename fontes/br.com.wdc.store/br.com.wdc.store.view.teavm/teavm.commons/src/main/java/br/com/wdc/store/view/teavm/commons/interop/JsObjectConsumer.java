package br.com.wdc.store.view.teavm.commons.interop;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

@JSFunctor
@FunctionalInterface
public interface JsObjectConsumer extends JSObject {
    void accept(JSObject obj);
}
