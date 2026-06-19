package br.com.wdc.framework.jooq.dialect;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Field;
import org.jooq.QueryPart;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.framework.jooq.JsonFieldEntry;

/**
 * Dialeto JSON para PostgreSQL e YugabyteDB.
 *
 * <h2>Escolha técnica: por que não usar {@code json_build_object()}?</h2>
 *
 * <p>
 * A função nativa {@code json_build_object('k1', v1, 'k2', v2, ...)} recebe dois argumentos por campo (chave + valor).
 * O PostgreSQL define {@code FUNC_MAX_ARGS = 100} como constante de compilação nos binários distribuídos pelas distros.
 * Isso limita {@code json_build_object} a <b>50 campos</b> por chamada — e alterar esse limite exige recompilar o
 * PostgreSQL a partir do código-fonte.
 * </p>
 *
 * <p>
 * Usa a combinação:
 * </p>
 *
 * <pre>{@code
 * '{' || array_to_string(array[
 *   '"id":' || coalesce(to_json(t.id), 'null'),
 *   '"name":' || coalesce(to_json(t.name), 'null'),
 *   '"image":' || coalesce(to_json(encode(t.image, 'base64')), 'null')
 * ], ',') || '}'
 * }</pre>
 *
 * <ul>
 * <li>{@code array[e1, e2, ..., eN]} é um construtor de array, não uma função — não sujeito a {@code FUNC_MAX_ARGS}.
 * </li>
 * <li>{@code array_to_string} recebe apenas 3 argumentos fixos.</li>
 * <li>{@code to_json()} serializa qualquer tipo PostgreSQL para JSON válido.</li>
 * </ul>
 *
 * <p>
 * <b>Preservação de bind values:</b> os campos ({@link JsonFieldEntry#field()} e o elemento do array) são embutidos via
 * placeholders {@code {N}} de template do jOOQ — nunca renderizados para string crua. Isso garante que subqueries
 * correlacionadas com critérios filtrados (ex.: projeção lazy 1—N com {@code ProjectionList(prj, criteria)}) tenham
 * seus bind values rastreados e vinculados na posição correta pelo jOOQ.
 * </p>
 */
public final class PostgresJsonDialect implements JsonDialect {

    public static final PostgresJsonDialect INSTANCE = new PostgresJsonDialect();

    private PostgresJsonDialect() {
    }

    @Override
    public Field<String> jsonObject(List<JsonFieldEntry> entries) {
        if (entries.isEmpty()) {
            return DSL.inline("{}");
        }

        var sql = new StringBuilder("\n'{' || array_to_string(array[\n  ");
        var args = new ArrayList<QueryPart>();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sql.append(",\n  ");
            }
            var e = entries.get(i);
            sql.append("'\"").append(escapeKey(e.key())).append("\":' || ").append(valueExpr(e, args));
        }
        sql.append("\n], ',') || '}'");

        return DSL.field(sql.toString(), String.class, args.toArray(new QueryPart[0]));
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        return DSL.field("'[' || COALESCE(string_agg({0}, ','), '') || ']'", String.class, jsonElement);
    }

    /**
     * Monta a expressão de valor JSON do campo, registrando {@link JsonFieldEntry#field()} como QueryPart (placeholder
     * {@code {N}}) na lista {@code args} — preservando eventuais bind values do campo (ex.: subqueries filtradas).
     */
    private String valueExpr(JsonFieldEntry e, List<QueryPart> args) {
        int idx = args.size();
        args.add(e.field());
        String ph = "{" + idx + "}";
        return switch (e.type()) {
            // encode() inserts newlines every 76 chars (RFC 2045); getMimeDecoder() on
            // the Java side tolerates them, so no replace() needed here.
            case BINARY -> "coalesce(to_json(encode(" + ph + ", 'base64')), 'null')";
            case RAW_JSON -> "coalesce(" + ph + ", 'null')";
            default -> "coalesce(to_json(" + ph + "), 'null')";
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''").replace("\"", "\\\"");
    }
}
