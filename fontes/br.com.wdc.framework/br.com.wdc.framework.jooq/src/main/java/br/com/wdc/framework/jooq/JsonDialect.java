package br.com.wdc.framework.jooq;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.SQLDialect;

import br.com.wdc.framework.jooq.dialect.DuckDbJsonDialect;
import br.com.wdc.framework.jooq.dialect.GenericJsonDialect;
import br.com.wdc.framework.jooq.dialect.H2JsonDialect;
import br.com.wdc.framework.jooq.dialect.MySqlJsonDialect;
import br.com.wdc.framework.jooq.dialect.PostgresJsonDialect;
import br.com.wdc.framework.jooq.dialect.SQLiteJsonDialect;
import br.com.wdc.framework.jooq.dialect.SqlServerJsonDialect;

/**
 * Abstração para geração de JSON via SQL nativo por banco de dados.
 *
 * <p>
 * Cada implementação usa as funções JSON nativas do respectivo banco (ex: {@code JSON_OBJECT},
 * {@code json_build_object}, etc.), suportando qualquer quantidade de campos.
 * </p>
 *
 * <p>
 * Uso:
 * <pre>{@code
 * var dialect = JsonDialect.of(ctx.dsl().dialect());
 * Field<String> json = dialect.jsonObject(entries);
 * }</pre>
 */
public interface JsonDialect {

    /**
     * Monta um campo SQL que produz um JSON object a partir das entries fornecidas.
     * Cada entry contém a chave, o campo raw e o tipo lógico.
     *
     * @param entries lista de campos a incluir no JSON object
     * @return Field que, ao ser avaliado, produz uma string JSON
     */
    Field<String> jsonObject(List<JsonFieldEntry> entries);

    /**
     * Monta uma expressão de agregação que produz um JSON array a partir de múltiplas linhas.
     * Usado para relações 1:N (subselect correlacionado).
     *
     * @param jsonElement expressão que produz um JSON object por linha
     * @return expressão que agrega em {@code '[' || elem1 || ',' || elem2 || ... || ']'}
     */
    Field<String> jsonArrayAgg(Field<String> jsonElement);

    /**
     * Idem, com ordem definida <b>dentro</b> da agregação.
     *
     * <p>
     * A ordem tem de entrar aqui, e não como {@code ORDER BY} da consulta: a coleção sai de uma subconsulta
     * correlacionada com a linha do pai, e envolvê-la numa tabela derivada — onde caberia um {@code ORDER BY} — põe a
     * correlação fora de alcance, porque uma derivada não enxerga o escopo externo.
     * </p>
     *
     * <p>
     * O padrão devolve a agregação <b>sem ordem</b>: nem todo banco sabe ordenar dentro do agregado, e emitir SQL que
     * ele recusaria seria pior do que devolver a coleção na ordem do banco. Sobrescrevem este método os dialetos em
     * que a construção existe — hoje H2 e PostgreSQL, os dois que a aplicação usa.
     * </p>
     */
    default Field<String> jsonArrayAgg(Field<String> jsonElement, List<SortField<?>> order) {
        return jsonArrayAgg(jsonElement);
    }

    /** Se este dialeto sabe ordenar dentro da agregação — ou seja, se {@link #jsonArrayAgg(Field, List)} honra a ordem. */
    default boolean supportsOrderedAggregation() {
        return false;
    }

    /**
     * Hook de inicialização (ex: registrar funções SQL customizadas no H2).
     * Chamado uma vez durante o bootstrap.
     *
     * @param conn conexão JDBC aberta
     */
    default void initialize(Connection conn) throws SQLException {
        // Default: nada a fazer
    }

    /**
     * Obtém a implementação de {@link JsonDialect} adequada para o dialeto jOOQ informado.
     */
    static JsonDialect of(SQLDialect dialect) {
        return switch (dialect.family()) {
            case H2, HSQLDB -> H2JsonDialect.INSTANCE;
            case POSTGRES, YUGABYTEDB -> PostgresJsonDialect.INSTANCE;
            case MYSQL, MARIADB -> MySqlJsonDialect.INSTANCE;
            case SQLITE -> SQLiteJsonDialect.INSTANCE;
            case DUCKDB -> DuckDbJsonDialect.INSTANCE;
            default -> {
                // SQL Server (SQLSERVER) só está disponível no jOOQ comercial.
                // Detectamos pelo nome da família para não depender do enum pro.
                String family = dialect.family().name();
                if (family.equals("SQLSERVER")) {
                    yield SqlServerJsonDialect.INSTANCE;
                }
                yield GenericJsonDialect.INSTANCE;
            }
        };
    }
}
