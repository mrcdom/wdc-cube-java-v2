package br.com.wdc.framework.domain.pagination;

import java.util.List;

/**
 * Paginated result of a query.
 *
 * @param items      items of the current page
 * @param page       page number (0-based)
 * @param totalPages total number of pages
 * @param totalItems total number of items without pagination
 */
public record Page<T>(List<T> items, int page, int totalPages, int totalItems) {

    /**
     * Builds a {@code Page} computing {@code totalPages} from {@code pageSize}.
     *
     * @param items      items of the page
     * @param page       page number (0-based)
     * @param pageSize   page size (must be &gt; 0)
     * @param totalItems total number of items without pagination
     */
    public static <T> Page<T> of(List<T> items, int page, int pageSize, int totalItems) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalItems / pageSize) : 0;
        return new Page<>(List.copyOf(items), page, totalPages, totalItems);
    }
}