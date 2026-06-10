package br.com.wdc.framework.jooq.dialect;

import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;
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

    /**
     * Usado para renderizar referências de campo dentro dos fragmentos DSL.sql().
     * <p>
     * As classes jOOQ geradas usam nomes em maiúsculas (do H2 codegen). O PostgreSQL armazena
     * identificadores sem aspas em minúsculas. Este contexto aplica as mesmas configurações do
     * RepositoryBootstrap (withRenderSchema=false, withRenderNameCase=LOWER) para que
     * {@code e.field().toString()} produza {@code "u1"."id"} em vez de {@code "PUBLIC"."EN_USER"."ID"}.
     */
    private static final org.jooq.DSLContext RENDER_CTX = DSL.using(SQLDialect.POSTGRES,
            new Settings().withRenderSchema(false).withRenderNameCase(RenderNameCase.LOWER));

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
        String col = RENDER_CTX.render(e.field());
        return switch (e.type()) {
            // encode() inserts newlines every 76 chars (RFC 2045); getMimeDecoder() on
            // the Java side tolerates them, so no replace() needed here.
            case BINARY -> "coalesce(to_json(encode(" + col + ", 'base64')), 'null')";
            case RAW_JSON -> "coalesce(" + col + ", 'null')";
            default -> "coalesce(to_json(" + col + "), 'null')";
        };
    }

    private String escapeKey(String key) {
        return key.replace("'", "''").replace("\"", "\\\"");
    }
}
