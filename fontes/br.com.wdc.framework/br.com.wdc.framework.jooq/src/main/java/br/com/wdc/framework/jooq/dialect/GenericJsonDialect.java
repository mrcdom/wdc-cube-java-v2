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
 * Dialeto JSON genérico (fallback) para bancos sem suporte nativo a JSON_OBJECT.
 *
 * <p>Aplica-se a: CUBRID, DERBY, FIREBIRD, IGNITE, TRINO, e qualquer outro dialeto desconhecido.</p>
 *
 * <p>Usa concatenação SQL com operador {@code ||} para montar JSON manualmente:</p>
 * <pre>{@code
 * '{' || '"id":' || coalesce(cast(t.ID as varchar), 'null') || ',' ||
 *        '"name":' || case when t.NAME is null then 'null' else '"' || ... || '"' end || '}'
 * }</pre>
 *
 * <p>
 * Para campos BINARY, esta implementação <b>não suporta</b> Base64 nativamente (sem função genérica portável).
 * Use um dialeto específico ou registre uma função customizada.
 * </p>
 *
 * <p>
 * <b>Preservação de bind values:</b> os campos ({@link JsonFieldEntry#field()} e o elemento do array) são embutidos via
 * placeholders {@code {N}} de template do jOOQ — nunca renderizados para string crua. Isso garante que subqueries
 * correlacionadas com critérios filtrados tenham seus bind values rastreados e vinculados na posição correta pelo jOOQ.
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
            case BINARY -> "'null'"; // Não suportado genericamente — sem base64 portável
            case RAW_JSON -> "coalesce(" + f + ", 'null')";
        };
    }
}
