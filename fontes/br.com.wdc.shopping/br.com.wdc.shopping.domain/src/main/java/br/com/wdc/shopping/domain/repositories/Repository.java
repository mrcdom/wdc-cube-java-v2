package br.com.wdc.shopping.domain.repositories;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import br.com.wdc.shopping.domain.pagination.Page;

/**
 * Contrato base de leitura para repositórios do SGS.
 *
 * @param <E> tipo da entidade
 * @param <C> tipo do critério de pesquisa
 * @param <K> tipo da chave primária
 */
public interface Repository<E, C, K> {

    boolean insert(E bean);

    /**
     * Atualiza uma entidade.
     *
     * @param newBean    valores novos
     * @param oldBean    valores antigos para optimistic locking (pode ser {@code null} para ignorar)
     * @param projection indica quais campos considerar — campos não-nulos na projeção serão atualizados
     */
    boolean update(E newBean, E oldBean, E projection);

    /** Atualiza considerando apenas campos não-nulos do newBean (retrocompatível). */
    default boolean update(E newBean) {
        return update(newBean, null, null);
    }

    /** Atualiza considerando apenas campos não-nulos do newBean (retrocompatível). */
    default boolean update(E newBean, E oldBean) {
        return update(newBean, oldBean, null);
    }

    /** Insere se {@code oldBean} for {@code null}; atualiza caso contrário. */
    default boolean insertOrUpdate(E newBean, E oldBean) {
        return oldBean == null ? insert(newBean) : update(newBean, oldBean);
    }

    int delete(C criteria);

    int count(C criteria);

    List<E> fetch(C criteria, int offset, int limit);

    /** Busca sem restrição de quantidade. */
    default List<E> fetch(C criteria) {
        return fetch(criteria, 0, 0);
    }

    /** Busca com limite, sem offset (ex.: autocomplete, "top N"). */
    default List<E> fetch(C criteria, int limit) {
        return fetch(criteria, 0, limit);
    }

    default Page<E> fetchPage(C criteria, int page, int pageSize) {
        var total = count(criteria);
        var items = fetch(criteria, page * pageSize, pageSize);
        return Page.of(items, page, pageSize, total);
    }

    E fetchById(K id, E projection);

    E newProjection();

    /**
     * Verifica se um campo deve ser incluído no UPDATE.
     * Retorna {@code true} quando a projeção indica o campo (não-nulo) E o valor mudou em relação ao oldBean.
     *
     * @param newBean    entidade com valores novos
     * @param oldBean    entidade com valores antigos (pode ser {@code null})
     * @param projection projeção indicando campos a atualizar
     * @param getter     extrai o valor do campo de interesse
     */
    static <E, V> boolean changed(E newBean, E oldBean, E projection, Function<E, V> getter) {
        return getter.apply(projection) != null
                && (oldBean == null || !Objects.equals(getter.apply(newBean), getter.apply(oldBean)));
    }
}
