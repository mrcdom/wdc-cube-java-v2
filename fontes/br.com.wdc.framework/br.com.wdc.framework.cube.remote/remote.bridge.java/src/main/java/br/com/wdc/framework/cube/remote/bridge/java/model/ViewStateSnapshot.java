package br.com.wdc.framework.cube.remote.bridge.java.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;

/**
 * Immutable snapshot of a single ViewState received from the Host.
 * <p>
 * The {@code "#"} field from the server JSON is exposed as {@link #instanceId()}. All other fields are accessible via typed accessors.
 */
public record ViewStateSnapshot(String instanceId, Map<String, Object> fields) {

	public ViewStateSnapshot {
		fields = Collections.unmodifiableMap(fields);
	}

	/** Returns the field value as a String, or {@code null} if absent or not a String. */
	public String getString(String field) {
		return CoerceUtils.asString(fields.get(field));
	}

	/** Returns the field value as a Long, or {@code null} if absent or not numeric. */
	public Long getLong(String field) {
		return CoerceUtils.asLong(fields.get(field));
	}

	/** Returns the field value as a Double, or {@code null} if absent or not numeric. */
	public Double getDouble(String field) {
		return CoerceUtils.asDouble(fields.get(field));
	}

	/** Returns the field value as a Boolean, or {@code null} if absent. */
	@SuppressWarnings("java:S2447") // Intentionally nullable: null means "field absent"
	public Boolean getBoolean(String field) {
		return CoerceUtils.asBoolean(fields.get(field));
	}

	/**
	 * Returns the field value as a {@code List<Map<String,Object>>}, or an empty list if absent. Suitable for list fields (e.g., {@code items},
	 * {@code products}).
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getList(String field) {
		Object v = fields.get(field);
		if (v instanceof List<?> l)
			return (List<Map<String, Object>>) l;
		return Collections.emptyList();
	}

	/**
	 * Returns the field value as a {@code Map<String,Object>}, or {@code null} if absent or not a map. Suitable for nested POJO fields (e.g.,
	 * {@code product}, {@code address}).
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getMap(String field) {
		Object v = fields.get(field);
		if (v instanceof Map<?, ?> m)
			return (Map<String, Object>) m;
		return null;
	}

	/** Returns {@code true} if the field is present (even if null). */
	public boolean hasField(String field) {
		return fields.containsKey(field);
	}

}
