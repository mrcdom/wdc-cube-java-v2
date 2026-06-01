package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the state for a single view instance (identified by vsid).
 * State is received from the server as a flat key-value map.
 */
public class ViewScope {

    private final String vsid;
    private final Map<String, Object> viewState = new LinkedHashMap<>();
    private Runnable forceUpdateCallback;

    public ViewScope(String vsid) {
        this.vsid = vsid;
    }

    public String getVsid() {
        return vsid;
    }

    public Map<String, Object> getState() {
        return viewState;
    }

    public void setState(Map<String, Object> newState) {
        viewState.clear();
        if (newState != null) {
            viewState.putAll(newState);
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

    // -- State helpers --

    public String getString(String key) {
        var v = viewState.get(key);
        return v != null ? v.toString() : null;
    }

    public String getString(String key, String defaultValue) {
        var v = viewState.get(key);
        return v != null ? v.toString() : defaultValue;
    }

    public int getInt(String key) {
        var v = viewState.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    public double getDouble(String key) {
        var v = viewState.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    public boolean getBoolean(String key) {
        var v = viewState.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return "true".equalsIgnoreCase(s);
        return false;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String key) {
        var v = viewState.get(key);
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return Map.of();
    }
}
