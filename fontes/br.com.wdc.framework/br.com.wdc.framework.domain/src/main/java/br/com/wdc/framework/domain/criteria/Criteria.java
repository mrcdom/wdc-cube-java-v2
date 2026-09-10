package br.com.wdc.framework.domain.criteria;

import java.util.List;

/**
 * Contrato comum a todo {@code XxxCriteria}: expõe os campos filtráveis como uma lista.
 *
 * <p>
 * Existe para que a tradução do critério em condição seja escrita <b>uma vez</b>. Sem ela, cada entidade precisa de um
 * {@code if} por campo, e todo operador novo significa reescrever esse bloco em todas as entidades. Com a lista, o
 * núcleo percorre os campos informados e o que resta a cada entidade é apenas dizer qual coluna corresponde a cada
 * nome.
 * </p>
 *
 * <p>
 * A ordem é a de declaração no critério, e é a ordem em que as condições entram no {@code AND} — previsível, portanto,
 * no SQL gerado.
 * </p>
 */
public interface Criteria {

    /**
     * Os campos que chegaram a ser instanciados.
     *
     * <p>
     * Um campo só nasce quando alguém o pede, então esta lista traz os tocados — informados ou não. Distinguir
     * "tocado" de "filtra" é com {@link Criterion#isSet()}, e é responsabilidade de quem traduz.
     * </p>
     */
    List<Criterion<?, ?>> criterions();
}
