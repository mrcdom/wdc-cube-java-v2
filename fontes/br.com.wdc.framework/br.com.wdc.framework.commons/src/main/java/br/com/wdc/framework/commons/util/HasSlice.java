package br.com.wdc.framework.commons.util;

/**
 * Projection collection that slices how many child rows to fetch.
 *
 * <p>
 * Lives in {@code commons}, next to {@link HasCriteria}, for the same reason: the projection bean is what implements
 * it, and the REST client needs to build it without dragging jOOQ onto its classpath.
 * </p>
 *
 * <p>
 * <b>Slicing without ordering returns rows in undefined order</b> — the database promises no order at all without
 * {@code ORDER BY}, and the same five rows may come out differently between two executions. The order comes from the
 * criteria the collection carries, via {@link HasCriteria}: that is where the child entity's {@code OrderBy} lives.
 * </p>
 */
public interface HasSlice {

    /** Maximum number of child rows, or {@code null} to fetch all of them. */
    Integer getLimit();

    /** How many rows to skip before starting, or {@code null} to start from the first one. */
    Integer getOffset();
}
