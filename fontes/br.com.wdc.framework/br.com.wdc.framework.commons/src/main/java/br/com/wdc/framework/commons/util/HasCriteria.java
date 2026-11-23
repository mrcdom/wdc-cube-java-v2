package br.com.wdc.framework.commons.util;

/**
 * Marker interface for collections that carry an additional filtering criteria.
 *
 * <p>
 * Implemented by projection collections (e.g. ProjectionList, ProjectionSet) to propagate filter criteria
 * into correlated sub-queries.
 * </p>
 */
public interface HasCriteria {

    Object getCriteria();
}
