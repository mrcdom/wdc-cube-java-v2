package br.com.wdc.framework.jooq;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Extensões DSL para gerar campos JSON via SQL portável (H2 e PostgreSQL).
 *
 * <h2>Escolha técnica: concatenação inline</h2>
 *
 * <p>
 * Em vez de depender de funções proprietárias ({@code to_json()}, {@code array_to_string()}), usamos SQL padrão
 * ({@code CAST}, {@code REPLACE}, {@code CASE}, operador {@code ||}) que funciona em ambos os bancos sem necessidade
 * de {@code CREATE ALIAS}.
 * </p>
 *
 * <p>
 * A única exceção é o campo BINARY (byte[]), que requer uma função custom {@code TO_JSON_BIN} para codificação Base64
 * — registrada em {@link H2Functions}.
 * </p>
 */
public final class JooqUtils {

    private JooqUtils() {
        // NOOP
    }

    // ─── Métodos de campo JSON por tipo ─────────────────────────────────────────

    /**
     * Campo numérico (BIGINT, INT, DOUBLE, DECIMAL) → JSON sem aspas.
     * <p>SQL: {@code coalesce(cast(field as varchar), 'null')}</p>
     */
    public static Pair<String, Field<String>> toJsonNum(String fn, Field<?> field) {
        return Pair.of(fn, DSL.field(
                DSL.sql("coalesce(cast(" + field + " as varchar), 'null')"), String.class));
    }

    /**
     * Campo string/enum (VARCHAR) → JSON com aspas e escape de {@code \} e {@code "}.
     * <p>SQL: {@code case when field is null then 'null' else '"' || escape(field) || '"' end}</p>
     */
    public static Pair<String, Field<String>> toJsonStr(String fn, Field<?> field) {
        return Pair.of(fn, DSL.field(
                DSL.sql("case when " + field + " is null then 'null' else '\"' || replace(replace("
                        + field + ", '\\', '\\\\'), '\"', '\\\"') || '\"' end"),
                String.class));
    }

    /**
     * Campo booleano → JSON {@code true}/{@code false}/{@code null}.
     */
    public static Pair<String, Field<String>> toJsonBool(String fn, Field<?> field) {
        return Pair.of(fn, DSL.field(
                DSL.sql("case when " + field + " is null then 'null' when " + field
                        + " then 'true' else 'false' end"),
                String.class));
    }

    /**
     * Campo OffsetDateTime (TIMESTAMP WITH TIME ZONE) → JSON string com aspas.
     */
    public static Pair<String, Field<String>> toJsonOdt(String fn, Field<?> field) {
        return Pair.of(fn, DSL.field(
                DSL.sql("case when " + field + " is null then 'null' else '\"' || cast("
                        + field + " as varchar) || '\"' end"),
                String.class));
    }

    /**
     * Campo binário (BINARY/BLOB) → JSON string Base64. Requer alias {@code TO_JSON_BIN} registrado via
     * {@link H2Functions}.
     */
    public static Pair<String, Field<String>> toJsonBin(String fn, Field<?> field) {
        return Pair.of(fn, DSL.field(
                DSL.sql("coalesce(to_json_bin(" + field + "), 'null')"), String.class));
    }

    // ─── Montagem do objeto JSON ────────────────────────────────────────────────

    /**
     * Monta um campo SQL que produz um JSON object a partir de uma lista de entries.
     *
     * <p>
     * Resultado: {@code '{' || '"k1":' || val1 || ',' || '"k2":' || val2 || ... || '}'}
     * </p>
     *
     * <p>
     * Usa concatenação simples com {@code ||} — não depende de {@code array_to_string} nem de construtores de array.
     * </p>
     */
    public static Field<String> toJsonObjectField(List<Pair<String, Field<String>>> entries) {
        String parts = entries.stream()
                .map(e -> "'\"" + StringEscapeUtils.escapeJson(e.getLeft()) + "\":' || " + e.getRight())
                .collect(Collectors.joining(" || ',' || \n  "));
        return DSL.field(DSL.sql("\n'{' || " + parts + " || '}'"), String.class);
    }

    // ─── Legado (mantido para compatibilidade) ──────────────────────────────────

    /**
     * @deprecated Substituído pelos métodos tipados ({@link #toJsonNum}, {@link #toJsonStr}, etc.)
     */
    @Deprecated
    public static Pair<String, Field<String>> toJsonField(String fn, Field<?> field) {
        return toJsonNum(fn, field);
    }
}
