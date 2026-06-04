package br.com.wdc.shopping.view.remote.javaclient.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of a single ViewState received from the Host.
 * <p>
 * The {@code "#"} field from the server JSON is exposed as {@link #instanceId()}.
 * All other fields are accessible via typed accessors.
 */
public final class ViewStateSnapshot {

    private final String instanceId;
    private final Map<String, Object> fields;

    public ViewStateSnapshot(String instanceId, Map<String, Object> fields) {
        this.instanceId = instanceId;
        this.fields = Collections.unmodifiableMap(fields);
    }

    /** The view instance ID (the {@code "#"} field from the server JSON). */
    public String instanceId() {
        return instanceId;
    }

    /** All fields as a raw map (for inspection). */
    public Map<String, Object> fields() {
        return fields;
    }

    /** Returns the field value as a String, or {@code null} if absent or not a String. */
    public String getString(String field) {
        Object v = fields.get(field);
        return v instanceof String s ? s : v != null ? String.valueOf(v) : null;
    }

    /** Returns the field value as a Long, or {@code null} if absent or not numeric. */
    public Long getLong(String field) {
        Object v = fields.get(field);
        if (v instanceof Number n) return n.longValue();
        return null;
    }

    /** Returns the field value as a Double, or {@code null} if absent or not numeric. */
    public Double getDouble(String field) {
        Object v = fields.get(field);
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }

    /** Returns the field value as a Boolean, or {@code null} if absent. */
    public Boolean getBoolean(String field) {
        Object v = fields.get(field);
        if (v instanceof Boolean b) return b;
        return null;
    }

    /**
     * Returns the field value as a {@code List<Map<String,Object>>}, or an empty list if absent.
     * Suitable for list fields (e.g., {@code items}, {@code products}).
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getList(String field) {
        Object v = fields.get(field);
        if (v instanceof List<?> l) return (List<Map<String, Object>>) l;
        return Collections.emptyList();
    }

    /** Returns {@code true} if the field is present (even if null). */
    public boolean hasField(String field) {
        return fields.containsKey(field);
    }

    @Override
    public String toString() {
        return "ViewStateSnapshot{instanceId='" + instanceId + "', fields=" + fields + "}";
    }
}
