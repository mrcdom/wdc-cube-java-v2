package br.com.wdc.framework.domain.criteria;

/**
 * Critério de campo textual.
 *
 * <p>
 * Herda de {@link ComparableCriterion} porque texto também se ordena — {@code name().ge("M")} é consulta legítima — e
 * acrescenta as buscas por padrão.
 * </p>
 *
 * <p>
 * <b>Os curingas fazem parte do valor.</b> {@code like("%CAFE%")} é diferente de {@code like("CAFE")}, exatamente como
 * em SQL. Acrescentá-los automaticamente pareceria conveniente, mas tiraria de quem escreve a consulta a capacidade de
 * ancorar no início — que é justamente o que decide se um índice será usado.
 * </p>
 *
 * @param <C> tipo do {@code XxxCriteria} que contém este campo
 */
public class TextCriterion<C> extends ComparableCriterion<C, String> {

    public TextCriterion(C owner, String name) {
        super(owner, name);
    }

    // Retorno covariante: sem isto, or() devolveria a superclasse e o encadeamento perderia like e ilike.
    @Override
    public TextCriterion<C> or() {
        super.or();
        return this;
    }

    @Override
    public TextCriterion<C> and() {
        super.and();
        return this;
    }

    /** {@code LIKE}, sensível a maiúsculas. Texto nulo ou vazio não acrescenta pedido. */
    public C like(String pattern) {
        return empty(pattern) ? owner() : add(Operator.LIKE, pattern);
    }

    /** {@code ILIKE}, insensível a maiúsculas. Texto nulo ou vazio não acrescenta pedido. */
    public C ilike(String pattern) {
        return empty(pattern) ? owner() : add(Operator.ILIKE, pattern);
    }

    /** {@code ILIKE '%valor%'} — o caso mais comum de busca por trecho, com os curingas postos por conveniência. */
    public C containing(String fragment) {
        return empty(fragment) ? owner() : ilike("%" + fragment + "%");
    }

    /** {@code ILIKE 'valor%'} — ancorado no início, que é o que permite ao banco usar índice. */
    public C startingWith(String prefix) {
        return empty(prefix) ? owner() : ilike(prefix + "%");
    }

    private static boolean empty(String s) {
        return s == null || s.isEmpty();
    }
}
