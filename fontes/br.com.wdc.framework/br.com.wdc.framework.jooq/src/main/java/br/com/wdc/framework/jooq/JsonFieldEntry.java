package br.com.wdc.framework.jooq;

import org.jooq.Field;

/**
 * Field entry used by {@link JsonDialect} to build a JSON object.
 *
 * @param key   name of the JSON key
 * @param field raw jOOQ field (not pre-formatted)
 * @param type  logical type for serialization
 */
public record JsonFieldEntry(String key, Field<?> field, JsonFieldType type) {
}
