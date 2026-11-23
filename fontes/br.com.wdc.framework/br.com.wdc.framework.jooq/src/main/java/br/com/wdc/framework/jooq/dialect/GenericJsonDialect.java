package br.com.wdc.framework.jooq.dialect;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.jooq.Field;
import org.jooq.QueryPart;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * Generic (fallback) JSON dialect for databases without native JSON_OBJECT support.
 *
 * <p>Applies to: CUBRID, DERBY, FIREBIRD, IGNITE, TRINO, and any other unknown dialect.</p>
 *
 * <p>Uses SQL concatenation with the {@code ||} operator to build JSON manually:</p>
 * <pre>{@code
 * '{' || '"id":' || coalesce(cast(t.ID as varchar), 'null') || ',' ||
 *        '"name":' || case when t.NAME is null then 'null' else '"' || ... || '"' end || '}'
 * }</pre>
 *
 * <p>
 * For BINARY fields, this implementation <b>does not support</b> Base64 natively (there is no portable generic function).
 * Use a specific dialect or register a custom function.
 * </p>
 *
 * <p>
 * <b>Bind value preservation:</b> the fields ({@link JsonFieldEntry#field()} and the array element) are embedded via jOOQ
 * template placeholders {@code {N}} — never rendered to a raw string. This ensures that correlated subqueries with
 * filtered criteria have their bind values tracked and bound at the correct position by jOOQ.
 * </p>
 */
public final class GenericJsonDialect implements JsonDialect {

    public static final GenericJsonDialect INSTANCE = new GenericJsonDialect();

    private GenericJsonDialect() {
    }

    @Override
    public Field<String> jsonObject(List<JsonFieldEntry> entries) {
        if (entries.isEmpty()) {
            return DSL.inline("{}");
        }

        var sql = new StringBuilder("\n'{' || ");
        var args = new ArrayList<QueryPart>();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sql.append(" || ',' || \n  ");
            }
            var e = entries.get(i);
            sql.append("'\"").append(StringEscapeUtils.escapeJson(e.key())).append("\":' || ").append(valueExpr(e, args));
        }
        sql.append(" || '}'");

        return DSL.field(sql.toString(), String.class, args.toArray(new QueryPart[0]));
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        // Fallback: uses string_agg which works on PostgreSQL, H2, Trino, DuckDB.
        // For DBs that don't support it, override with a specific dialect.
        return DSL.field("'[' || COALESCE(string_agg({0}, ','), '') || ']'", String.class, jsonElement);
    }

    private String valueExpr(JsonFieldEntry e, List<QueryPart> args) {
        int idx = args.size();
        args.add(e.field());
        String f = "{" + idx + "}";
        return switch (e.type()) {
            case NUMBER -> "coalesce(cast(" + f + " as varchar), 'null')";
            case STRING -> "case when " + f + " is null then 'null' else '\"' || replace(replace("
                    + f + ", '\\', '\\\\'), '\"', '\\\"') || '\"' end";
            case BOOLEAN -> "case when " + f + " is null then 'null' when "
                    + f + " then 'true' else 'false' end";
            case DATETIME -> "case when " + f + " is null then 'null' else '\"' || cast("
                    + f + " as varchar) || '\"' end";
            case BINARY -> "'null'"; // Not supported generically — no portable base64
            case RAW_JSON -> "coalesce(" + f + ", 'null')";
        };
    }
}
