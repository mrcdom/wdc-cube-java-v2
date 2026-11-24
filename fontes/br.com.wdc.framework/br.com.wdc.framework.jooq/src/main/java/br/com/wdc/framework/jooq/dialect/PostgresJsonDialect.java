package br.com.wdc.framework.jooq.dialect;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Field;
import org.jooq.QueryPart;
import org.jooq.SortField;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * JSON dialect for PostgreSQL and YugabyteDB.
 *
 * <h2>Technical choice: why not use {@code json_build_object()}?</h2>
 *
 * <p>
 * The native function {@code json_build_object('k1', v1, 'k2', v2, ...)} takes two arguments per field (key + value).
 * PostgreSQL defines {@code FUNC_MAX_ARGS = 100} as a compile-time constant in the binaries shipped by the distros.
 * This limits {@code json_build_object} to <b>50 fields</b> per call — and changing that limit requires recompiling
 * PostgreSQL from source.
 * </p>
 *
 * <p>
 * It uses the combination:
 * </p>
 *
 * <pre>{@code
 * '{' || array_to_string(array[
 *   '"id":' || coalesce(to_json(t.id), 'null'),
 *   '"name":' || coalesce(to_json(t.name), 'null'),
 *   '"image":' || coalesce(to_json(encode(t.image, 'base64')), 'null')
 * ], ',') || '}'
 * }</pre>
 *
 * <ul>
 * <li>{@code array[e1, e2, ..., eN]} is an array constructor, not a function — not subject to {@code FUNC_MAX_ARGS}.
 * </li>
 * <li>{@code array_to_string} takes only 3 fixed arguments.</li>
 * <li>{@code to_json()} serializes any PostgreSQL type to valid JSON.</li>
 * </ul>
 *
 * <p>
 * <b>Bind value preservation:</b> the fields ({@link JsonFieldEntry#field()} and the array element) are embedded via jOOQ
 * template placeholders {@code {N}} — never rendered to a raw string. This ensures that correlated subqueries with
 * filtered criteria (e.g. a lazy 1—N projection with {@code ProjectionList(prj, criteria)}) have their bind values
 * tracked and bound at the correct position by jOOQ.
 * </p>
 */
public final class PostgresJsonDialect implements JsonDialect {

    public static final PostgresJsonDialect INSTANCE = new PostgresJsonDialect();

    private PostgresJsonDialect() {
    }

    @Override
    public Field<String> jsonObject(List<JsonFieldEntry> entries) {
        if (entries.isEmpty()) {
            return DSL.inline("{}");
        }

        var sql = new StringBuilder("\n'{' || array_to_string(array[\n  ");
        var args = new ArrayList<QueryPart>();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sql.append(",\n  ");
            }
            var e = entries.get(i);
            sql.append("'\"").append(escapeKey(e.key())).append("\":' || ").append(valueExpr(e, args));
        }
        sql.append("\n], ',') || '}'");

        return DSL.field(sql.toString(), String.class, args.toArray(new QueryPart[0]));
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        return DSL.field("'[' || COALESCE(string_agg({0}, ','), '') || ']'", String.class, jsonElement);
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement, List<SortField<?>> order) {
        if (order == null || order.isEmpty()) {
            return jsonArrayAgg(jsonElement);
        }
        return DSL.field("'[' || COALESCE(string_agg({0}, ',' ORDER BY {1}), '') || ']'",
                String.class, jsonElement, DSL.list(order.toArray(new QueryPart[0])));
    }

    @Override
    public boolean supportsOrderedAggregation() {
        return true;
    }

    /**
     * Builds the field's JSON value expression, registering {@link JsonFieldEntry#field()} as a QueryPart (placeholder
     * {@code {N}}) in the {@code args} list — preserving any bind values of the field (e.g. filtered subqueries).
     */
    private String valueExpr(JsonFieldEntry e, List<QueryPart> args) {
        int idx = args.size();
        args.add(e.field());
        String ph = "{" + idx + "}";
        return switch (e.type()) {
            // encode() inserts newlines every 76 chars (RFC 2045); getMimeDecoder() on
            // the Java side tolerates them, so no replace() needed here.
            case BINARY -> "coalesce(to_json(encode(" + ph + ", 'base64')), 'null')";
            case RAW_JSON -> "coalesce(" + ph + ", 'null')";
            default -> "coalesce(to_json(" + ph + "), 'null')";
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''").replace("\"", "\\\"");
    }
}
