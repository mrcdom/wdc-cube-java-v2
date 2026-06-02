package br.com.wdc.shopping.view.teavm.commons.interop;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * Interop com console do browser.
 */
public final class Console {

    private Console() {
    }

    @JSBody(params = {"message"}, script = "console.log(message);")
    public static native void log(String message);

    @JSBody(params = {"message"}, script = "console.warn(message);")
    public static native void warn(String message);

    @JSBody(params = {"message"}, script = "console.error(message);")
    public static native void error(String message);

    @JSBody(params = {"label", "obj"}, script = "console.log(label, obj);")
    public static native void log(String label, JSObject obj);
}
