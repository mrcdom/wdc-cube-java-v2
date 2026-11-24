package br.com.wdc.framework.cube.remote.bridge.java.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;

/**
 * Immutable snapshot of a single scope received from the Host.
 * <p>
 * The {@code "#"} field from the server JSON is exposed as {@link #instanceId()}. Its other fields (the {@link #values()}) are accessible via typed accessors.
 */
public record ScopeSnapshot(String instanceId, Map<String, Object> values) {

	public ScopeSnapshot {
		values = Collections.unmodifiableMap(values);
	}

	/** Returns the field value as a String, or {@code null} if absent or not a String. */
	public String getString(String field) {
		return CoerceUtils.asString(values.get(field));
	}

	/** Returns the field value as a Long, or {@code null} if absent or not numeric. */
	public Long getLong(String field) {
		return CoerceUtils.asLong(values.get(field));
	}

	/** Returns the field value as a Double, or {@code null} if absent or not numeric. */
	public Double getDouble(String field) {
		return CoerceUtils.asDouble(values.get(field));
	}

	/** Returns the field value as a Boolean, or {@code null} if absent. */
	@SuppressWarnings("java:S2447") // Intentionally nullable: null means "field absent"
	public Boolean getBoolean(String field) {
		return CoerceUtils.asBoolean(values.get(field));
	}

	/**
	 * Returns the field value as a {@code List<Map<String,Object>>}, or an empty list if absent. Suitable for list values (e.g., {@code items},
	 * {@code products}).
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getList(String field) {
		Object v = values.get(field);
		if (v instanceof List<?> l)
			return (List<Map<String, Object>>) l;
		return Collections.emptyList();
	}

	/**
	 * Returns the field value as a {@code Map<String,Object>}, or an empty map if absent or not a map. Suitable for nested POJO values (e.g.,
	 * {@code product}, {@code address}). Use {@link #hasField(String)} to distinguish "absent" from "present but empty".
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getMap(String field) {
		Object v = values.get(field);
		if (v instanceof Map<?, ?> m)
			return (Map<String, Object>) m;
		return Collections.emptyMap();
	}

	/** Returns {@code true} if the field is present (even if null). */
	public boolean hasField(String field) {
		return values.containsKey(field);
	}

}
