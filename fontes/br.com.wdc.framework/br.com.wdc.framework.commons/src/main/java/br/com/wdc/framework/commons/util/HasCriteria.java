package br.com.wdc.framework.commons.util;

/**
 * Interface marcadora para coleções que carregam um critério de filtragem adicional.
 *
 * <p>
 * Implementada por coleções de projeção (ex: ProjectionList, ProjectionSet) para propagar critérios de filtro
 * em sub-queries correlacionadas.
 * </p>
 */
public interface HasCriteria {

    Object getCriteria();
}
