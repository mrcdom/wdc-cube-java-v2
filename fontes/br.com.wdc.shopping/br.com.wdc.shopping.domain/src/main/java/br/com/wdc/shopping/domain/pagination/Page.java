package br.com.wdc.shopping.domain.pagination;

import java.util.List;

/**
 * Resultado paginado de uma consulta.
 *
 * @param items      itens da página atual
 * @param page       número da página (0-based)
 * @param totalPages total de páginas
 * @param totalItems total de itens sem paginação
 */
public record Page<T>(List<T> items, int page, int totalPages, int totalItems) {

    /**
     * Constrói uma {@code Page} calculando {@code totalPages} a partir de {@code pageSize}.
     *
     * @param items      itens da página
     * @param page       número da página (0-based)
     * @param pageSize   tamanho da página (deve ser &gt; 0)
     * @param totalItems total de itens sem paginação
     */
    public static <T> Page<T> of(List<T> items, int page, int pageSize, int totalItems) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalItems / pageSize) : 0;
        return new Page<>(List.copyOf(items), page, totalPages, totalItems);
    }
}