package br.com.wdc.shopping.view.teavm.commons.interop;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * Interop com APIs de timer do browser.
 */
public final class Timers {

    private Timers() {
    }

    @JSFunctor
    public interface TimerCallback extends JSObject {
        void onTimer();
    }

    @JSBody(params = {"callback", "delay"}, script = "return setTimeout(callback, delay);")
    public static native int setTimeout(TimerCallback callback, int delay);

    @JSBody(params = {"callback", "delay"}, script = "return setInterval(callback, delay);")
    public static native int setInterval(TimerCallback callback, int delay);

    @JSBody(params = {"id"}, script = "clearTimeout(id);")
    public static native void clearTimeout(int id);

    @JSBody(params = {"id"}, script = "clearInterval(id);")
    public static native void clearInterval(int id);
}
