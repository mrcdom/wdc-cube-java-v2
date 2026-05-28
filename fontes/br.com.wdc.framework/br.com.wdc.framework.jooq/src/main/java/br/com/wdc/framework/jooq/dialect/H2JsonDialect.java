package br.com.wdc.framework.jooq.dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Field;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * Dialeto JSON para H2 e HSQLDB.
 *
 * <p>Usa a sintaxe padrão SQL:2016:</p>
 * <pre>{@code
 * CAST(JSON_OBJECT(
 *     KEY 'id' VALUE t.ID,
 *     KEY 'name' VALUE t.NAME,
 *     KEY 'image' VALUE TO_BASE64(t.IMAGE)
 *     ABSENT ON NULL
 * ) AS VARCHAR)
 * }</pre>
 *
 * <p>Para Base64, requer a função {@code TO_BASE64} registrada via {@link #initialize(Connection)}.</p>
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

        String body = entries.stream()
                .map(e -> "KEY '" + escapeKey(e.key()) + "' VALUE " + valueExpr(e))
                .collect(Collectors.joining(", "));

        return DSL.field(
                DSL.sql("CAST(JSON_OBJECT(" + body + " ABSENT ON NULL) AS VARCHAR)"),
                String.class);
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        return DSL.field(
                DSL.sql("'[' || COALESCE(LISTAGG(" + jsonElement + ", ','), '') || ']'"),
                String.class);
    }

    private String valueExpr(JsonFieldEntry e) {
        return switch (e.type()) {
            case BINARY -> "TO_BASE64(" + e.field() + ")";
            case RAW_JSON -> e.field().toString() + " FORMAT JSON";
            default -> e.field().toString();
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''");
    }

    // :: H2 custom function

    /**
     * Converte byte[] para string Base64. Registrado como {@code TO_BASE64} no H2.
     */
    public static String toBase64(byte[] value) {
        if (value == null) {
            return null;
        }
        return java.util.Base64.getEncoder().encodeToString(value);
    }
}
