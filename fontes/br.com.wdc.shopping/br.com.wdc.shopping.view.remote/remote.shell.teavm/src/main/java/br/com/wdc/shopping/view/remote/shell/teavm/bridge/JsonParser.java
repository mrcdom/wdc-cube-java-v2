package br.com.wdc.shopping.view.remote.shell.teavm.bridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.teavm.jso.JSBody;

/**
 * Minimal JSON parser/serializer for WebSocket messages.
 * Uses browser-native JSON.parse/JSON.stringify via JSBody.
 */
public final class JsonParser {

    private JsonParser() {
    }

    /**
     * Parse a JSON string into a Map (object) or null if invalid.
     */
    public static Map<String, Object> parseObject(String json) {
        try {
            Object result = parseNative(json);
            Map<String, Object> map = convertToMap(result);
            return map != null ? map : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * Stringify any object to JSON.
     */
    public static String stringify(Object obj) {
        return stringifyNative(toJsValue(obj));
    }

    // -- Native JSON --

    @JSBody(params = {"json"}, script = "try { return JSON.parse(json); } catch(e) { return null; }")
    private static native Object parseNative(String json);

    @JSBody(params = {"obj"}, script = "return JSON.stringify(obj);")
    private static native String stringifyNative(Object obj);

    // -- Java → JS conversion --

    @JSBody(params = {}, script = "return {};")
    private static native Object newJsObject();

    @JSBody(params = {}, script = "return [];")
    private static native Object newJsArray();

    @JSBody(params = {"obj", "key", "val"}, script = "obj[key] = val;")
    private static native void setJsProp(Object obj, String key, Object val);

    @JSBody(params = {"arr", "val"}, script = "arr.push(val);")
    private static native void pushJsArray(Object arr, Object val);

    // -- Primitive conversion: TeaVM needs typed params for proper Java→JS conversion --

    @JSBody(params = {"s"}, script = "return s;")
    private static native Object stringToJs(String s);

    @JSBody(params = {"n"}, script = "return n;")
    private static native Object intToJs(int n);

    @JSBody(params = {"n"}, script = "return n;")
    private static native Object doubleToJs(double n);

    @JSBody(params = {"b"}, script = "return b;")
    private static native Object boolToJs(boolean b);

    @JSBody(params = {"obj", "key", "val"}, script = "obj[key] = val;")
    private static native void setJsPropStr(Object obj, String key, String val);

    @JSBody(params = {"obj", "key", "val"}, script = "obj[key] = val;")
    private static native void setJsPropInt(Object obj, String key, int val);

    @JSBody(params = {"obj", "key", "val"}, script = "obj[key] = val;")
    private static native void setJsPropDouble(Object obj, String key, double val);

    @JSBody(params = {"obj", "key", "val"}, script = "obj[key] = val;")
    private static native void setJsPropBool(Object obj, String key, boolean val);

    @JSBody(params = {"arr", "val"}, script = "arr.push(val);")
    private static native void pushJsArrayStr(Object arr, String val);

    @JSBody(params = {"arr", "val"}, script = "arr.push(val);")
    private static native void pushJsArrayInt(Object arr, int val);

    @JSBody(params = {"arr", "val"}, script = "arr.push(val);")
    private static native void pushJsArrayDouble(Object arr, double val);

    @JSBody(params = {"arr", "val"}, script = "arr.push(val);")
    private static native void pushJsArrayBool(Object arr, boolean val);

    @SuppressWarnings("unchecked")
    private static Object toJsValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return stringToJs(s);
        if (obj instanceof Integer i) return intToJs(i);
        if (obj instanceof Double d) return doubleToJs(d);
        if (obj instanceof Number n) return doubleToJs(n.doubleValue());
        if (obj instanceof Boolean b) return boolToJs(b);
        if (obj instanceof Map<?, ?> map) {
            var jsObj = newJsObject();
            for (var entry : ((Map<String, Object>) map).entrySet()) {
                setJsProperty(jsObj, entry.getKey(), entry.getValue());
            }
            return jsObj;
        }
        if (obj instanceof List<?> list) {
            var jsArr = newJsArray();
            for (var item : list) {
                pushJsItem(jsArr, item);
            }
            return jsArr;
        }
        return stringToJs(obj.toString());
    }

    private static void setJsProperty(Object jsObj, String key, Object value) {
        if (value == null) {
            setJsProp(jsObj, key, null);
        } else if (value instanceof String s) {
            setJsPropStr(jsObj, key, s);
        } else if (value instanceof Integer i) {
            setJsPropInt(jsObj, key, i);
        } else if (value instanceof Double d) {
            setJsPropDouble(jsObj, key, d);
        } else if (value instanceof Number n) {
            setJsPropDouble(jsObj, key, n.doubleValue());
        } else if (value instanceof Boolean b) {
            setJsPropBool(jsObj, key, b);
        } else {
            setJsProp(jsObj, key, toJsValue(value));
        }
    }

    private static void pushJsItem(Object jsArr, Object value) {
        if (value == null) {
            pushJsArray(jsArr, null);
        } else if (value instanceof String s) {
            pushJsArrayStr(jsArr, s);
        } else if (value instanceof Integer i) {
            pushJsArrayInt(jsArr, i);
        } else if (value instanceof Double d) {
            pushJsArrayDouble(jsArr, d);
        } else if (value instanceof Number n) {
            pushJsArrayDouble(jsArr, n.doubleValue());
        } else if (value instanceof Boolean b) {
            pushJsArrayBool(jsArr, b);
        } else {
            pushJsArray(jsArr, toJsValue(value));
        }
    }

    // -- JS → Java conversion --

    @JSBody(params = {"obj"}, script = "return Array.isArray(obj);")
    private static native boolean isArray(Object obj);

    @JSBody(params = {"obj"}, script = "return typeof obj === 'object' && obj !== null && !Array.isArray(obj);")
    private static native boolean isObject(Object obj);

    @JSBody(params = {"obj"}, script = "return typeof obj === 'string';")
    private static native boolean isString(Object obj);

    @JSBody(params = {"obj"}, script = "return typeof obj === 'number';")
    private static native boolean isNumber(Object obj);

    @JSBody(params = {"obj"}, script = "return typeof obj === 'boolean';")
    private static native boolean isBoolean(Object obj);

    @JSBody(params = {"obj"}, script = "return Object.keys(obj);")
    private static native String[] getKeys(Object obj);

    @JSBody(params = {"obj", "key"}, script = "return obj[key];")
    private static native Object getProp(Object obj, String key);

    @JSBody(params = {"arr"}, script = "return arr.length;")
    private static native int getLength(Object arr);

    @JSBody(params = {"arr", "i"}, script = "return arr[i];")
    private static native Object getAt(Object arr, int i);

    @JSBody(params = {"obj"}, script = "return String(obj);")
    private static native String asString(Object obj);

    @JSBody(params = {"obj"}, script = "return +obj;")
    private static native double asNumber(Object obj);

    @JSBody(params = {"obj"}, script = "return !!obj;")
    private static native boolean asBoolean(Object obj);

    static Map<String, Object> convertToMap(Object jsObj) {
        if (jsObj == null || !isObject(jsObj)) return Map.of();
        var map = new LinkedHashMap<String, Object>();
        var keys = getKeys(jsObj);
        for (var key : keys) {
            var val = getProp(jsObj, key);
            map.put(key, convertValue(val));
        }
        return map;
    }

    private static Object convertValue(Object val) {
        if (val == null) return null;
        if (isString(val)) return asString(val);
        if (isNumber(val)) return asNumber(val);
        if (isBoolean(val)) return asBoolean(val);
        if (isArray(val)) return convertToList(val);
        if (isObject(val)) return convertToMap(val);
        return asString(val);
    }

    private static List<Object> convertToList(Object jsArr) {
        int len = getLength(jsArr);
        var list = new ArrayList<Object>(len);
        for (int i = 0; i < len; i++) {
            list.add(convertValue(getAt(jsArr, i)));
        }
        return list;
    }
}
