package br.com.wdc.shopping.view.remote.shell.teavm.interop;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

@JSFunctor
@FunctionalInterface
public interface JsRunnable extends JSObject {
    void run();
}
