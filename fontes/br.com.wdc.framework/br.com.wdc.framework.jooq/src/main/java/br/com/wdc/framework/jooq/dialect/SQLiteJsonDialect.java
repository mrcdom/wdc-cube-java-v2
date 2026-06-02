package br.com.wdc.framework.jooq.dialect;

import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Field;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * Dialeto JSON para SQLite (3.38+).
 *
 * <p>Usa:</p>
 * <pre>{@code
 * json_object(
 *     'id', t.ID,
 *     'name', t.NAME
 * )
 * }</pre>
 *
 * <p>
 * SQLite não possui função nativa de Base64. Campos BINARY são representados como hex string
 * via {@code hex()}. Para aplicações que requerem Base64, registrar via extensão C ou
 * usar um dialect customizado.
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

        String args = entries.stream()
                .map(e -> "'" + escapeKey(e.key()) + "', " + valueExpr(e))
                .collect(Collectors.joining(", "));

        return DSL.field(
                DSL.sql("json_object(" + args + ")"),
                String.class);
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        return DSL.field(
                DSL.sql("'[' || COALESCE(group_concat(" + jsonElement + ", ','), '') || ']'"),
                String.class);
    }

    private String valueExpr(JsonFieldEntry e) {
        return switch (e.type()) {
            case BINARY -> "hex(" + e.field() + ")";
            default -> e.field().toString();
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''");
    }
}
