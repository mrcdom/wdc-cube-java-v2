package br.com.wdc.framework.domain.criteria;

/**
 * Critério de campo ordenável — número, data, timestamp.
 *
 * <p>
 * Acrescenta à igualdade o que só faz sentido onde existe ordem. A separação em subclasse é o que impede
 * {@code criteria.roles().between(...)} de compilar: quem declara o campo conhece o tipo da coluna e escolhe a classe,
 * então a restrição não custa nada a quem escreve o critério e evita o erro na origem.
 * </p>
 *
 * @param <C> tipo do {@code XxxCriteria} que contém este campo
 * @param <T> tipo do valor comparado
 */
public class ComparableCriterion<C, T> extends Criterion<C, T> {

    public ComparableCriterion(C owner, String name) {
        super(owner, name);
    }

    // Retorno covariante: sem isto, or() devolveria Criterion e o encadeamento perderia between e as comparações.
    @Override
    public ComparableCriterion<C, T> or() {
        super.or();
        return this;
    }

    @Override
    public ComparableCriterion<C, T> and() {
        super.and();
        return this;
    }

    /** Maior que. {@code null} não acrescenta pedido. */
    public C gt(T value) {
        return value == null ? owner() : add(Operator.GT, value);
    }

    /** Maior ou igual a. {@code null} não acrescenta pedido. */
    public C ge(T value) {
        return value == null ? owner() : add(Operator.GE, value);
    }

    /** Menor que. {@code null} não acrescenta pedido. */
    public C lt(T value) {
        return value == null ? owner() : add(Operator.LT, value);
    }

    /** Menor ou igual a. {@code null} não acrescenta pedido. */
    public C le(T value) {
        return value == null ? owner() : add(Operator.LE, value);
    }

    /**
     * Intervalo fechado nos dois extremos.
     *
     * <p>
     * <b>Aceita limite aberto.</b> Só o início vira {@code >=}; só o fim vira {@code <=}; nenhum dos dois não
     * acrescenta pedido. É o caso corrente de filtro por período em tela, onde costuma-se preencher uma data só —
     * exigir os dois extremos empurraria essa decisão para todo chamador.
     * </p>
     */
    public C between(T start, T end) {
        if (start == null && end == null) {
            return owner();
        }
        if (start == null) {
            return le(end);
        }
        if (end == null) {
            return ge(start);
        }
        return add(Operator.BETWEEN, start, end);
    }
}
