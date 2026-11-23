package br.com.wdc.framework.jooq.dialect;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Field;
import org.jooq.QueryPart;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * JSON dialect for SQLite (3.38+).
 *
 * <p>Uses:</p>
 * <pre>{@code
 * json_object(
 *     'id', t.ID,
 *     'name', t.NAME
 * )
 * }</pre>
 *
 * <p>
 * SQLite has no native Base64 function. BINARY fields are represented as a hex string
 * via {@code hex()}. Applications that require Base64 should register it through a C extension or
 * use a custom dialect.
 * </p>
 *
 * <p>
 * <b>Bind value preservation:</b> the fields ({@link JsonFieldEntry#field()} and the array element) are embedded via jOOQ
 * template placeholders {@code {N}} — never rendered to a raw string. This ensures that correlated subqueries with
 * filtered criteria have their bind values tracked and bound at the correct position by jOOQ.
 * </p>
 */
public final class SQLiteJsonDialect implements JsonDialect {

    public static final SQLiteJsonDialect INSTANCE = new SQLiteJsonDialect();

    private SQLiteJsonDialect() {
    }

    @Override
    public Field<String> jsonObject(List<JsonFieldEntry> entries) {
        if (entries.isEmpty()) {
            return DSL.inline("{}");
        }

        var sql = new StringBuilder("json_object(");
        var args = new ArrayList<QueryPart>();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            var e = entries.get(i);
            sql.append("'").append(escapeKey(e.key())).append("', ").append(valueExpr(e, args));
        }
        sql.append(")");

        return DSL.field(sql.toString(), String.class, args.toArray(new QueryPart[0]));
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        return DSL.field("'[' || COALESCE(group_concat({0}, ','), '') || ']'", String.class, jsonElement);
    }

    private String valueExpr(JsonFieldEntry e, List<QueryPart> args) {
        int idx = args.size();
        args.add(e.field());
        String ph = "{" + idx + "}";
        return switch (e.type()) {
            case BINARY -> "hex(" + ph + ")";
            default -> ph;
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''");
    }
}
