package br.com.wdc.framework.commons.util;

/**
 * Coleção de projeção que recorta quantas linhas filhas trazer.
 *
 * <p>
 * Vive no {@code commons}, ao lado de {@link HasCriteria}, e pelo mesmo motivo: quem a implementa é o bean de
 * projeção, e o cliente REST precisa montá-la sem arrastar jOOQ para o seu classpath.
 * </p>
 *
 * <p>
 * <b>Recortar sem ordenar devolve linhas em ordem indefinida</b> — o banco não promete ordem nenhuma sem
 * {@code ORDER BY}, e as mesmas cinco linhas podem sair diferentes entre duas execuções. A ordem vem do critério que a
 * coleção carrega, via {@link HasCriteria}: é lá que mora o {@code OrderBy} da entidade filha.
 * </p>
 */
public interface HasSlice {

    /** Máximo de linhas filhas, ou {@code null} para trazer todas. */
    Integer getLimit();

    /** Quantas linhas pular antes de começar, ou {@code null} para começar da primeira. */
    Integer getOffset();
}
