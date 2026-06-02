package br.com.wdc.framework.jooq.dialect;

import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Field;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * Dialeto JSON para MySQL e MariaDB.
 *
 * <p>Usa:</p>
 * <pre>{@code
 * JSON_OBJECT(
 *     'id', t.ID,
 *     'name', t.NAME,
 *     'image', TO_BASE64(t.IMAGE)
 * )
 * }</pre>
 *
 * <p>{@code TO_BASE64} e {@code JSON_OBJECT} são funções nativas do MySQL 5.7+ e MariaDB 10.2+.</p>
 */
public final class MySqlJsonDialect implements JsonDialect {

    public static final MySqlJsonDialect INSTANCE = new MySqlJsonDialect();

    private MySqlJsonDialect() {
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
                DSL.sql("CAST(JSON_OBJECT(" + args + ") AS CHAR)"),
                String.class);
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        return DSL.field(
                DSL.sql("CONCAT('[', COALESCE(GROUP_CONCAT(" + jsonElement + " SEPARATOR ','), ''), ']')"),
                String.class);
    }

    private String valueExpr(JsonFieldEntry e) {
        return switch (e.type()) {
            case BINARY -> "TO_BASE64(" + e.field() + ")";
            default -> e.field().toString();
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''");
    }
}
