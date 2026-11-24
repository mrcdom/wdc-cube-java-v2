package br.com.wdc.framework.jooq;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * DSL extensions that generate JSON fields through portable SQL (H2 and PostgreSQL).
 *
 * <h2>Technical choice: inline concatenation</h2>
 *
 * <p>
 * Instead of relying on proprietary functions ({@code to_json()}, {@code array_to_string()}), we use standard SQL
 * ({@code CAST}, {@code REPLACE}, {@code CASE}, the {@code ||} operator) that works on both databases without the need
 * for {@code CREATE ALIAS}.
 * </p>
 *
 * <p>
 * The only exception is the BINARY field (byte[]), which requires a custom {@code TO_JSON_BIN} function for Base64
 * encoding — registered in {@link H2Functions}.
 * </p>
 */
public final class JooqUtils {

    private JooqUtils() {
        // NOOP
    }

    // ─── JSON field methods by type ─────────────────────────────────────────────

    /**
     * Numeric field (BIGINT, INT, DOUBLE, DECIMAL) → unquoted JSON.
     * <p>SQL: {@code coalesce(cast(field as varchar), 'null')}</p>
     */
    public static Pair<String, Field<String>> toJsonNum(String fn, Field<?> field) {
        return Pair.of(fn, DSL.field(
                DSL.sql("coalesce(cast(" + field + " as varchar), 'null')"), String.class));
    }

    /**
     * String/enum field (VARCHAR) → quoted JSON, escaping {@code \} and {@code "}.
     * <p>SQL: {@code case when field is null then 'null' else '"' || escape(field) || '"' end}</p>
     */
    public static Pair<String, Field<String>> toJsonStr(String fn, Field<?> field) {
        return Pair.of(fn, DSL.field(
                DSL.sql("case when " + field + " is null then 'null' else '\"' || replace(replace("
                        + field + ", '\\', '\\\\'), '\"', '\\\"') || '\"' end"),
                String.class));
    }

    /**
     * Boolean field → JSON {@code true}/{@code false}/{@code null}.
     */
    public static Pair<String, Field<String>> toJsonBool(String fn, Field<?> field) {
        return Pair.of(fn, DSL.field(
                DSL.sql("case when " + field + " is null then 'null' when " + field
                        + " then 'true' else 'false' end"),
                String.class));
    }

    /**
     * OffsetDateTime field (TIMESTAMP WITH TIME ZONE) → quoted JSON string.
     */
    public static Pair<String, Field<String>> toJsonOdt(String fn, Field<?> field) {
        return Pair.of(fn, DSL.field(
                DSL.sql("case when " + field + " is null then 'null' else '\"' || cast("
                        + field + " as varchar) || '\"' end"),
                String.class));
    }

    /**
     * Binary field (BINARY/BLOB) → Base64 JSON string. Requires the {@code TO_JSON_BIN} alias registered via
     * {@link H2Functions}.
     */
    public static Pair<String, Field<String>> toJsonBin(String fn, Field<?> field) {
        return Pair.of(fn, DSL.field(
                DSL.sql("coalesce(to_json_bin(" + field + "), 'null')"), String.class));
    }

    // ─── JSON object assembly ───────────────────────────────────────────────────

    /**
     * Builds an SQL field that produces a JSON object from a list of entries.
     *
     * <p>
     * Result: {@code '{' || '"k1":' || val1 || ',' || '"k2":' || val2 || ... || '}'}
     * </p>
     *
     * <p>
     * Uses plain concatenation with {@code ||} — it depends neither on {@code array_to_string} nor on array constructors.
     * </p>
     */
    public static Field<String> toJsonObjectField(List<Pair<String, Field<String>>> entries) {
        String parts = entries.stream()
                .map(e -> "'\"" + StringEscapeUtils.escapeJson(e.getLeft()) + "\":' || " + e.getRight())
                .collect(Collectors.joining(" || ',' || \n  "));
        return DSL.field(DSL.sql("\n'{' || " + parts + " || '}'"), String.class);
    }

}
