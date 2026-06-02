package br.com.wdc.framework.jooq.dialect;

import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Field;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * Dialeto JSON para DuckDB.
 *
 * <p>Usa:</p>
 * <pre>{@code
 * CAST(json_object('id', t.ID, 'name', t.NAME, 'image', base64(t.IMAGE)) AS VARCHAR)
 * }</pre>
 *
 * <p>DuckDB suporta {@code json_object()} e {@code base64()} nativamente.</p>
 */
public final class DuckDbJsonDialect implements JsonDialect {

    public static final DuckDbJsonDialect INSTANCE = new DuckDbJsonDialect();

    private DuckDbJsonDialect() {
    }

    @Override
    public Field<String> jsonObject(List<JsonFieldEntry> entries) {
        if (entries.isEmpty()) {
            return DSL.inline("{}");
        }

        String args = entries.stream()
                .map(e -> "'" + escapeKey(e.key()) + "', " + valueExpr(e))
                .collect(Collectors.joining(", "));

        return DSL.field(
                DSL.sql("CAST(json_object(" + args + ") AS VARCHAR)"),
                String.class);
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        return DSL.field(
                DSL.sql("'[' || COALESCE(string_agg(" + jsonElement + ", ','), '') || ']'"),
                String.class);
    }

    private String valueExpr(JsonFieldEntry e) {
        return switch (e.type()) {
            case BINARY -> "base64(" + e.field() + ")";
            default -> e.field().toString();
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''");
    }
}
