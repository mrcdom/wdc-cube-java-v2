package br.com.wdc.framework.jooq.dialect;

import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Field;
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

        String parts = entries.stream()
                .map(e -> "'\"" + escapeKey(e.key()) + "\":' + " + valueExpr(e))
                .collect(Collectors.joining(" + ',' +\n  "));

        return DSL.field(
                DSL.sql("('{' +\n  " + parts + "\n+ '}')"),
                String.class);
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        // STRING_AGG disponível no SQL Server 2017+
        return DSL.field(
                DSL.sql("('[' + COALESCE(STRING_AGG(CAST(" + jsonElement + " AS nvarchar(max)), ','), '') + ']')"),
                String.class);
    }

    private String valueExpr(JsonFieldEntry e) {
        String col = e.field().toString();
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
