package br.com.wdc.framework.cube.remote.bridge.teavm.interop;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

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
