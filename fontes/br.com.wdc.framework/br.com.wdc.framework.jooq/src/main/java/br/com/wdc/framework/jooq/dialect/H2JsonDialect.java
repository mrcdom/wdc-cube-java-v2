package br.com.wdc.framework.jooq.dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jooq.Field;
import org.jooq.QueryPart;
import org.jooq.SortField;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * JSON dialect for H2 and HSQLDB.
 *
 * <p>Uses the standard SQL:2016 syntax:</p>
 * <pre>{@code
 * CAST(JSON_OBJECT(
 *     KEY 'id' VALUE t.ID,
 *     KEY 'name' VALUE t.NAME,
 *     KEY 'image' VALUE TO_BASE64(t.IMAGE)
 *     ABSENT ON NULL
 * ) AS VARCHAR)
 * }</pre>
 *
 * <p>For Base64, it requires the {@code TO_BASE64} function registered via {@link #initialize(Connection)}.</p>
 *
 * <p>
 * <b>Bind value preservation:</b> the fields ({@link JsonFieldEntry#field()} and the array element) are embedded via jOOQ
 * template placeholders {@code {N}} — never rendered to a raw string. This ensures that correlated subqueries with
 * filtered criteria have their bind values tracked and bound at the correct position by jOOQ.
 * </p>
 */
public final class H2JsonDialect implements JsonDialect {

    public static final H2JsonDialect INSTANCE = new H2JsonDialect();

    private H2JsonDialect() {
    }

    @Override
    public void initialize(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE ALIAS IF NOT EXISTS TO_BASE64 FOR "br.com.wdc.framework.jooq.dialect.H2JsonDialect.toBase64"
                    """);
        }
    }

    @Override
    public Field<String> jsonObject(List<JsonFieldEntry> entries) {
        if (entries.isEmpty()) {
            return DSL.inline("{}");
        }

        var sql = new StringBuilder("CAST(JSON_OBJECT(");
        var args = new ArrayList<QueryPart>();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            var e = entries.get(i);
            sql.append("KEY '").append(escapeKey(e.key())).append("' VALUE ").append(valueExpr(e, args));
        }
        sql.append(" ABSENT ON NULL) AS VARCHAR)");

        return DSL.field(sql.toString(), String.class, args.toArray(new QueryPart[0]));
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        return DSL.field("'[' || COALESCE(LISTAGG({0}, ','), '') || ']'", String.class, jsonElement);
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement, List<SortField<?>> order) {
        if (order == null || order.isEmpty()) {
            return jsonArrayAgg(jsonElement);
        }
        return DSL.field("'[' || COALESCE(LISTAGG({0}, ',') WITHIN GROUP (ORDER BY {1}), '') || ']'",
                String.class, jsonElement, DSL.list(order.toArray(new QueryPart[0])));
    }

    @Override
    public boolean supportsOrderedAggregation() {
        return true;
    }

    private String valueExpr(JsonFieldEntry e, List<QueryPart> args) {
        int idx = args.size();
        args.add(e.field());
        String ph = "{" + idx + "}";
        return switch (e.type()) {
            case BINARY -> "TO_BASE64(" + ph + ")";
            case RAW_JSON -> ph + " FORMAT JSON";
            default -> ph;
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''");
    }

    // :: H2 custom function

    /**
     * Converts byte[] to a Base64 string. Registered as {@code TO_BASE64} in H2.
     */
    public static String toBase64(byte[] value) {
        if (value == null) {
            return null;
        }
        return java.util.Base64.getEncoder().encodeToString(value);
    }
}
