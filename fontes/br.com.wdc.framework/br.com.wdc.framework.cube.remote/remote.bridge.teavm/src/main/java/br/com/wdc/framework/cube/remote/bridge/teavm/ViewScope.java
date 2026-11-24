package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The scope of a single view instance (identified by vsid).
 * Its values are received from the server as a flat key-value map.
 */
public class ViewScope {

    private final String vsid;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private Runnable forceUpdateCallback;

    public ViewScope(String vsid) {
        this.vsid = vsid;
    }

    public String getVsid() {
        return vsid;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> newValues) {
        values.clear();
        if (newValues != null) {
            values.putAll(newValues);
        }
        forceUpdate();
    }

    public void forceUpdate() {
        if (forceUpdateCallback != null) {
            forceUpdateCallback.run();
        }
    }

    public void setForceUpdate(Runnable callback) {
        this.forceUpdateCallback = callback;
    }

    // -- Value helpers --

    public String getString(String key) {
        var v = values.get(key);
        return v != null ? v.toString() : null;
    }

    public String getString(String key, String defaultValue) {
        var v = values.get(key);
        return v != null ? v.toString() : defaultValue;
    }

    public int getInt(String key) {
        var v = values.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    public double getDouble(String key) {
        var v = values.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    public boolean getBoolean(String key) {
        var v = values.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return "true".equalsIgnoreCase(s);
        return false;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String key) {
        var v = values.get(key);
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return Map.of();
    }
}
