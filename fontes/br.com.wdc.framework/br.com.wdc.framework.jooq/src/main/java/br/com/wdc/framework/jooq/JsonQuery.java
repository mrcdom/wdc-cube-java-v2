package br.com.wdc.framework.jooq;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.SelectJoinStep;
import org.jooq.SelectQuery;
import org.jooq.Table;

import com.google.gson.stream.JsonReader;

/**
 * Instância construída e pronta para uso.
 *
 * @param <B> tipo do bean
 * @param <T> tipo da tabela-schema
 */
public interface JsonQuery<B, T extends Table<?>> {

    /** Cria bean vazio. */
    B newBean();

    /**
     * Cria bean de projeção com TODOS os campos setados (non-null sentinel). Passar este bean em {@link #fetchOne} /
     * {@link #fetchToList} seleciona todas as colunas.
     */
    B newProjectionBean();

    /**
     * Cria bean de projeção total e permite "anular" campos indesejados. Campos setados para {@code null} no callback
     * NÃO serão incluídos no SELECT.
     */
    B newProjectionBean(Consumer<B> adapt);

    /** Cria nova instância de tabela-schema com alias. */
    T newTable(String alias);

    /** Monta a query SELECT (sem executar). */
    SelectQuery<Record1<String>> select(B prjBean,
            BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause);

    /** Monta a query SELECT com contexto e flag de agregação (uso interno). */
    SelectQuery<Record1<String>> select(QueryContext ctx, B prjBean,
            BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause, boolean isAgg);

    /** Gera o campo de projeção JSON para a tabela. */
    Field<String> projection(QueryContext ctx, T jooqTable, B prjBean);

    /** Parse de JSON string para bean. */
    B parseJson(String json);

    /** Parse de JSON via reader para bean. */
    B parseJson(JsonReader jsonReader);

    /** Executa e retorna um único bean (ou null). */
    B fetchOne(B prjBean, BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause);

    /** Executa e retorna stream (lazy). */
    Stream<B> fetch(B prjBean, BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause);

    /** Executa e retorna lista materializada. */
    List<B> fetchToList(B prjBean, BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause);

    /** Conta registros. */
    int fetchCount(BiConsumer<T, SelectJoinStep<Record1<Integer>>> whereClause);
}
