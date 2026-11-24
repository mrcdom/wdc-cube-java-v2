package br.com.wdc.framework.jooq.dialect;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Field;
import org.jooq.QueryPart;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * JSON dialect for SQL Server 2017+ (compatibility: 2016+ with limitations).
 *
 * <h2>Strategy</h2>
 *
 * <p>SQL Server has no native {@code JSON_OBJECT()} until SQL Server 2022 (CTP).
 * For maximum compatibility with SQL Server 2017+, we use manual concatenation:</p>
 *
 * <pre>{@code
 * '{' +
 *   '"id":'    + IIF(t.ID IS NULL, 'null', CAST(t.ID AS nvarchar(max))) + ',' +
 *   '"name":'  + IIF(t.NAME IS NULL, 'null', '"' + REPLACE(t.NAME, '"', '\"') + '"') + ',' +
 *   '"image":' + IIF(t.IMAGE IS NULL, 'null', '"' + CAST(t.IMAGE AS XML).value('.', 'nvarchar(max)') + '"')
 * + '}'
 * }</pre>
 *
 * <p>For binaries (BINARY/VARBINARY → Base64), we use the technique of casting to XML and extracting
 * the text value — SQL Server has no native {@code TO_BASE64()} before 2022.</p>
 *
 * <h2>Types</h2>
 * <ul>
 *   <li>{@code NUMBER}: {@code CAST(col AS nvarchar(max))} — unquoted</li>
 *   <li>{@code BOOLEAN}: {@code CAST(col AS nvarchar(1))} → '0'/'1', or IIF for true/false</li>
 *   <li>{@code STRING} / {@code DATETIME}: quotes + escaping of {@code "} and {@code \}</li>
 *   <li>{@code BINARY}: Base64 via {@code (SELECT col FOR XML PATH(''), BINARY BASE64)}</li>
 *   <li>{@code RAW_JSON}: embedded directly, unquoted</li>
 * </ul>
 *
 * <h2>jsonArrayAgg</h2>
 * <p>Uses {@code STRING_AGG}, available since SQL Server 2017.</p>
 *
 * <h2>Bind value preservation</h2>
 * <p>The fields ({@link JsonFieldEntry#field()} and the array element) are embedded via jOOQ template placeholders
 * {@code {N}} — never rendered to a raw string. This ensures that correlated subqueries with filtered criteria have
 * their bind values tracked and bound at the correct position by jOOQ. When a field is referenced more than once in
 * the expression (e.g. {@code IIF(col IS NULL, ..., col)}), the same placeholder {@code {N}} is reused — jOOQ binds
 * the QueryPart correctly at each occurrence.</p>
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
        // STRING_AGG available in SQL Server 2017+
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
                    // BIT → JSON boolean true/false
                    "IIF(" + col + " IS NULL, 'null', IIF(" + col + " = 1, 'true', 'false'))";
            case BINARY ->
                    // Base64 via FOR XML / BINARY BASE64 — works on SQL Server 2008+
                    "IIF(" + col + " IS NULL, 'null', '\"' + "
                    + "(SELECT CAST(" + col + " AS varbinary(max)) FOR XML PATH(''), BINARY BASE64)"
                    + " + '\"')";
            case RAW_JSON ->
                    "COALESCE(CAST(" + col + " AS nvarchar(max)), 'null')";
            default ->
                    // STRING, DATETIME: quotes + escaping of " and backslash
                    "IIF(" + col + " IS NULL, 'null', '\"' + REPLACE(REPLACE(CAST(" + col
                    + " AS nvarchar(max)), '\\', '\\\\'), '\"', '\\\"') + '\"')";
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''").replace("\"", "\\\"");
    }
}
