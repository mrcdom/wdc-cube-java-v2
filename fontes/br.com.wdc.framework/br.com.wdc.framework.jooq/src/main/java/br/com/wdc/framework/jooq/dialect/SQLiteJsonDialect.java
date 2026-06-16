package br.com.wdc.framework.jooq.dialect;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Field;
import org.jooq.QueryPart;
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
 *
 * <p>
 * <b>Preservação de bind values:</b> os campos ({@link JsonFieldEntry#field()} e o elemento do array) são embutidos via
 * placeholders {@code {N}} de template do jOOQ — nunca renderizados para string crua. Isso garante que subqueries
 * correlacionadas com critérios filtrados tenham seus bind values rastreados e vinculados na posição correta pelo jOOQ.
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
