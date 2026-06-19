package br.com.wdc.framework.jooq.dialect;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Field;
import org.jooq.QueryPart;
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
 *
 * <p>
 * <b>Preservação de bind values:</b> os campos ({@link JsonFieldEntry#field()} e o elemento do array) são embutidos via
 * placeholders {@code {N}} de template do jOOQ — nunca renderizados para string crua. Isso garante que subqueries
 * correlacionadas com critérios filtrados tenham seus bind values rastreados e vinculados na posição correta pelo jOOQ.
 * </p>
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

        var sql = new StringBuilder("CAST(JSON_OBJECT(");
        var args = new ArrayList<QueryPart>();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            var e = entries.get(i);
            sql.append("'").append(escapeKey(e.key())).append("', ").append(valueExpr(e, args));
        }
        sql.append(") AS CHAR)");

        return DSL.field(sql.toString(), String.class, args.toArray(new QueryPart[0]));
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        return DSL.field("CONCAT('[', COALESCE(GROUP_CONCAT({0} SEPARATOR ','), ''), ']')", String.class, jsonElement);
    }

    private String valueExpr(JsonFieldEntry e, List<QueryPart> args) {
        int idx = args.size();
        args.add(e.field());
        String ph = "{" + idx + "}";
        return switch (e.type()) {
            case BINARY -> "TO_BASE64(" + ph + ")";
            default -> ph;
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''");
    }
}
