package br.com.wdc.framework.jooq.dialect;

import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Field;
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
 * <p>Usa a combinação:</p>
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
 * <p>Todas as funções são nativas — sem necessidade de {@code initialize()}.</p>
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

        String parts = entries.stream()
                .map(e -> "'\"" + escapeKey(e.key()) + "\":' || " + valueExpr(e))
                .collect(Collectors.joining(",\n  "));

        return DSL.field(
                DSL.sql("\n'{' || array_to_string(array[\n  " + parts + "\n], ',') || '}'"),
                String.class);
    }

    @Override
    public Field<String> jsonArrayAgg(Field<String> jsonElement) {
        return DSL.field(
                DSL.sql("'[' || COALESCE(string_agg(" + jsonElement + ", ','), '') || ']'"),
                String.class);
    }

    private String valueExpr(JsonFieldEntry e) {
        return switch (e.type()) {
            case BINARY -> "coalesce(to_json(encode(" + e.field() + ", 'base64')), 'null')";
            case RAW_JSON -> "coalesce(" + e.field() + ", 'null')";
            default -> "coalesce(to_json(" + e.field() + "), 'null')";
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''").replace("\"", "\\\"");
    }
}
