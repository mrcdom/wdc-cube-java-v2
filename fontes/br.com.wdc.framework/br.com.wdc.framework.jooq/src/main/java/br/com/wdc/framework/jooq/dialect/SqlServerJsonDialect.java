package br.com.wdc.framework.jooq.dialect;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Field;
import org.jooq.QueryPart;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * Dialeto JSON para SQL Server 2017+ (compatibilidade: 2016+ com limitações).
 *
 * <h2>Estratégia</h2>
 *
 * <p>SQL Server não possui {@code JSON_OBJECT()} nativo até o SQL Server 2022 (CTP).
 * Para máxima compatibilidade com SQL Server 2017+, usamos concatenação manual:</p>
 *
 * <pre>{@code
 * '{' +
 *   '"id":'    + IIF(t.ID IS NULL, 'null', CAST(t.ID AS nvarchar(max))) + ',' +
 *   '"name":'  + IIF(t.NAME IS NULL, 'null', '"' + REPLACE(t.NAME, '"', '\"') + '"') + ',' +
 *   '"image":' + IIF(t.IMAGE IS NULL, 'null', '"' + CAST(t.IMAGE AS XML).value('.', 'nvarchar(max)') + '"')
 * + '}'
 * }</pre>
 *
 * <p>Para binários (BINARY/VARBINARY → Base64), usamos a técnica de cast para XML e extração
 * do valor text — SQL Server não tem {@code TO_BASE64()} nativo antes do 2022.</p>
 *
 * <h2>Tipos</h2>
 * <ul>
 *   <li>{@code NUMBER}: {@code CAST(col AS nvarchar(max))} — sem aspas</li>
 *   <li>{@code BOOLEAN}: {@code CAST(col AS nvarchar(1))} → '0'/'1', ou IIF para true/false</li>
 *   <li>{@code STRING} / {@code DATETIME}: aspas + escape de {@code "} e {@code \}</li>
 *   <li>{@code BINARY}: Base64 via {@code (SELECT col FOR XML PATH(''), BINARY BASE64)}</li>
 *   <li>{@code RAW_JSON}: embutido diretamente sem aspas</li>
 * </ul>
 *
 * <h2>jsonArrayAgg</h2>
 * <p>Usa {@code STRING_AGG}, disponível a partir do SQL Server 2017.</p>
 *
 * <h2>Preservação de bind values</h2>
 * <p>Os campos ({@link JsonFieldEntry#field()} e o elemento do array) são embutidos via placeholders {@code {N}} de
 * template do jOOQ — nunca renderizados para string crua. Isso garante que subqueries correlacionadas com critérios
 * filtrados tenham seus bind values rastreados e vinculados na posição correta pelo jOOQ. Quando um campo é referenciado
 * mais de uma vez na expressão (ex.: {@code IIF(col IS NULL, ..., col)}), o mesmo placeholder {@code {N}} é reutilizado —
 * o jOOQ vincula o QueryPart corretamente em cada ocorrência.</p>
 */
public final class SqlServerJsonDialect implements JsonDialect {

    public static final SqlServerJsonDialect INSTANCE = new SqlServerJsonDialect();

    private SqlServerJsonDialect() {
    }

    @Override
    public Field<String> jsonObject(List<JsonFieldEntry> entries) {
        if (entries.isEmpty()) {
            return DSL.inline("{}");
        }

        var sql = new StringBuilder("('{' +\n  ");
        var args = new ArrayList<QueryPart>();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sql.append(" + ',' +\n  ");
            }
            var e = entries.get(i);
            sql.append("'\"").append(escapeKey(e.key())).append("\":' + ").append(valueExpr(e, args));
        }
        sql.append("\n+ '}')");

        return DSL.field(sql.toString(), String.class, args.toArray(new QueryPart[0]));
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        // STRING_AGG disponível no SQL Server 2017+
        return DSL.field("('[' + COALESCE(STRING_AGG(CAST({0} AS nvarchar(max)), ','), '') + ']')",
                String.class, jsonElement);
    }

    private String valueExpr(JsonFieldEntry e, List<QueryPart> args) {
        int idx = args.size();
        args.add(e.field());
        String col = "{" + idx + "}";
        return switch (e.type()) {
            case NUMBER ->
                    "IIF(" + col + " IS NULL, 'null', CAST(" + col + " AS nvarchar(max)))";
            case BOOLEAN ->
                    // BIT → true/false JSON booleano
                    "IIF(" + col + " IS NULL, 'null', IIF(" + col + " = 1, 'true', 'false'))";
            case BINARY ->
                    // Base64 via FOR XML / BINARY BASE64 — funciona no SQL Server 2008+
                    "IIF(" + col + " IS NULL, 'null', '\"' + "
                    + "(SELECT CAST(" + col + " AS varbinary(max)) FOR XML PATH(''), BINARY BASE64)"
                    + " + '\"')";
            case RAW_JSON ->
                    "COALESCE(CAST(" + col + " AS nvarchar(max)), 'null')";
            default ->
                    // STRING, DATETIME: aspas + escape de " e backslash
                    "IIF(" + col + " IS NULL, 'null', '\"' + REPLACE(REPLACE(CAST(" + col
                    + " AS nvarchar(max)), '\\', '\\\\'), '\"', '\\\"') + '\"')";
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''").replace("\"", "\\\"");
    }
}
