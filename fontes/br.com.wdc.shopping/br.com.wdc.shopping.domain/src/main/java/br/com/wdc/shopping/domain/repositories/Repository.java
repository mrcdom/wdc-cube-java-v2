package br.com.wdc.shopping.domain.repositories;

import java.util.List;

import br.com.wdc.shopping.domain.model.Page;

/**
 * Contrato base de leitura para repositórios do SGS.
 *
 * @param <E> tipo da entidade
 * @param <C> tipo do critério de pesquisa
 * @param <K> tipo da chave primária
 */
public interface Repository<E, C, K> {

    boolean insert(E bean);

    boolean update(E newBean, E oldBean);

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

    Page<E> fetchPage(C criteria, int page, int pageSize);

    E fetchById(K id, E projection);

    E newProjection();
}

