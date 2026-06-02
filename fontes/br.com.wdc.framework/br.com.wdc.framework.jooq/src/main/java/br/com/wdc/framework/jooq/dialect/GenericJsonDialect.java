package br.com.wdc.framework.jooq.dialect;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.jooq.Field;
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

        String parts = entries.stream()
                .map(e -> "'\"" + StringEscapeUtils.escapeJson(e.key()) + "\":' || " + valueExpr(e))
                .collect(Collectors.joining(" || ',' || \n  "));

        return DSL.field(DSL.sql("\n'{' || " + parts + " || '}'"), String.class);
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        // Fallback: uses string_agg which works on PostgreSQL, H2, Trino, DuckDB.
        // For DBs that don't support it, override with a specific dialect.
        return DSL.field(
                DSL.sql("'[' || COALESCE(string_agg(" + jsonElement + ", ','), '') || ']'"),
                String.class);
    }

    private String valueExpr(JsonFieldEntry e) {
        String field = e.field().toString();
        return switch (e.type()) {
            case NUMBER -> "coalesce(cast(" + field + " as varchar), 'null')";
            case STRING -> "case when " + field + " is null then 'null' else '\"' || replace(replace("
                    + field + ", '\\', '\\\\'), '\"', '\\\"') || '\"' end";
            case BOOLEAN -> "case when " + field + " is null then 'null' when "
                    + field + " then 'true' else 'false' end";
            case DATETIME -> "case when " + field + " is null then 'null' else '\"' || cast("
                    + field + " as varchar) || '\"' end";
            case BINARY -> "'null'"; // Não suportado genericamente — sem base64 portável
            case RAW_JSON -> "coalesce(" + field + ", 'null')";
        };
    }
}
