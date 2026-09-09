package br.com.wdc.framework.domain.criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Critério sobre um campo: os pedidos de comparação feitos e como eles se combinam.
 *
 * <p>
 * Esta classe já cobre o campo cuja única comparação sensata é de identidade — enum, booleano, chave estrangeira.
 * Campos ordenáveis e textuais usam as subclasses, que acrescentam o que o tipo permite.
 * </p>
 *
 * <p>
 * É o singular de {@code XxxCriteria} — um critério agrega vários {@code Criterion}, um por campo filtrável, e cada
 * campo informado vira uma condição {@code AND} na consulta.
 * </p>
 *
 * <p>
 * <b>Pedidos sucessivos acumulam, e por padrão valem juntos ({@code AND}).</b> É o que faz o intervalo montado em duas
 * linhas — {@code ge(inicio)} e depois {@code le(fim)}, como sai de dois campos de tela — significar o que se espera, e
 * o que faz {@code ne(1)} seguido de {@code ne(2)} excluir os dois. Fosse {@code OR} o padrão, esses dois casos
 * passariam a devolver quase toda a tabela sem nada indicar o erro.
 * </p>
 *
 * <p>
 * <b>Alternativa se pede com {@link #or()}</b>, que marca o campo como disjuntivo:
 * </p>
 *
 * <pre>
 * criteria.name().or().startingWith("CAFE");
 * criteria.name().startingWith("CHA");
 * // (name ILIKE 'CAFE%' OR name ILIKE 'CHA%')
 * </pre>
 *
 * <p>
 * A disjunção vale <b>dentro</b> do campo; entre campos diferentes a junção é sempre {@code AND}.
 * </p>
 *
 * <p>
 * <b>Valor nulo não acrescenta pedido</b>, em vez de apagar os anteriores: é o que preserva o costume de montar filtro
 * a partir de campos de tela, onde vazio significa "não filtrar por isto". Para apagar o que já foi pedido há
 * {@link #clear()}; para comparar com nulo, {@link #isNull()}.
 * </p>
 *
 * <p>
 * Nenhum método aqui conhece jOOQ ou SQL. A tradução mora no {@code framework.jooq}, que casa {@link #name()} com a
 * coluna e converte o valor; o cliente REST apenas serializa. É o que mantém a mesma classe válida dos dois lados.
 * </p>
 *
 * @param <C> tipo do {@code XxxCriteria} que contém este campo
 * @param <T> tipo do valor comparado
 */
public class Criterion<C, T> {

    /** Um pedido de comparação: o operador e os valores que o acompanham. */
    public static final class Predicate<T> {

        private final Operator operator;
        private final List<T> values;

        Predicate(Operator operator, List<T> values) {
            this.operator = operator;
            this.values = values;
        }

        public Operator operator() {
            return operator;
        }

        public List<T> values() {
            return Collections.unmodifiableList(values);
        }

        /** Primeiro valor, ou {@code null} — atalho para os operadores de um valor só. */
        public T value() {
            return values.isEmpty() ? null : values.get(0);
        }

        @Override
        public String toString() {
            return values.isEmpty() ? operator.toString() : operator + " " + values;
        }
    }

    /**
     * O {@code XxxCriteria} a que este campo pertence, para que os pedidos possam ser encadeados.
     *
     * <p>
     * <b>Não é estado, é navegação</b> — e por isso não trafega. Serializar este campo fecharia um ciclo com o critério
     * que o contém. O que descreve o filtro é o nome, os pedidos e a disjunção; o dono se restabelece do outro lado,
     * quando o critério é remontado.
     * </p>
     */
    private transient C owner;

    private final String name;

    private final List<Predicate<T>> predicates = new ArrayList<>();

    private boolean disjunctive;

    public Criterion(C owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    /**
     * Devolve o dono a um critério que chegou sem ele — o caso de um critério reconstruído a partir do transporte.
     *
     * <p>
     * Sem dono, {@code eq()} e companhia devolveriam {@code null} e o encadeamento quebraria no primeiro uso.
     * </p>
     */
    public void rebind(C owner) {
        this.owner = owner;
    }

    /** {@code true} quando o critério ainda não sabe a que {@code XxxCriteria} pertence. */
    public boolean detached() {
        return owner == null;
    }

    /** Nome lógico do campo — é por ele que a tradução encontra a coluna correspondente. */
    public String name() {
        return name;
    }

    /** Pedidos feitos sobre este campo, na ordem em que foram feitos. */
    public List<Predicate<T>> predicates() {
        return Collections.unmodifiableList(predicates);
    }

    /** {@code true} quando os pedidos deste campo se combinam com {@code OR}. */
    public boolean disjunctive() {
        return disjunctive;
    }

    /** {@code true} quando há ao menos um pedido, ou seja, quando este campo deve virar condição. */
    public boolean isSet() {
        return !predicates.isEmpty();
    }

    /**
     * Marca o campo como disjuntivo: os pedidos passam a valer com {@code OR} entre si.
     *
     * <p>
     * Vale para o campo inteiro, e não só para o pedido seguinte — chamar uma vez basta, em qualquer ponto.
     * </p>
     */
    public Criterion<C, T> or() {
        this.disjunctive = true;
        return this;
    }

    /** Volta a exigir todos os pedidos ({@code AND}), desfazendo {@link #or()}. */
    public Criterion<C, T> and() {
        this.disjunctive = false;
        return this;
    }

    /** Apaga os pedidos deste campo, voltando a não filtrar. Não desfaz {@link #or()}. */
    public C clear() {
        predicates.clear();
        return owner;
    }

    /** Igualdade. {@code null} não acrescenta pedido. */
    public C eq(T value) {
        return value == null ? owner : add(Operator.EQ, value);
    }

    /** Diferente de. {@code null} não acrescenta pedido. */
    public C ne(T value) {
        return value == null ? owner : add(Operator.NE, value);
    }

    /** Pertence ao conjunto. Conjunto nulo ou vazio não acrescenta pedido — filtrar por "nenhum" seria inútil. */
    public C in(Collection<? extends T> values) {
        if (values == null || values.isEmpty()) {
            return owner;
        }
        predicates.add(new Predicate<>(Operator.IN, new ArrayList<>(values)));
        return owner;
    }

    /** Pertence ao conjunto. */
    @SafeVarargs
    public final C in(T... values) {
        return in(values == null ? null : Arrays.asList(values));
    }

    public C isNull() {
        return add(Operator.IS_NULL);
    }

    public C isNotNull() {
        return add(Operator.IS_NOT_NULL);
    }

    /**
     * Repõe um pedido tal como veio do transporte, sem passar pelas regras dos métodos fluentes.
     *
     * <p>
     * Existe para a desserialização, e só para ela: {@code eq(null)} não acrescenta pedido, o que é o certo para quem
     * monta o filtro a partir de uma tela, mas apagaria em silêncio um {@code EQ NULL} que o outro lado enviou.
     * </p>
     */
    public void restore(Operator operator, List<T> values) {
        predicates.add(new Predicate<>(operator, values == null ? new ArrayList<>() : new ArrayList<>(values)));
    }

    /** Marca a disjunção ao remontar o critério do transporte. */
    public void restoreDisjunctive(boolean disjunctive) {
        this.disjunctive = disjunctive;
    }

    /** Acrescenta um pedido; visível às subclasses, que oferecem os operadores conforme o tipo do campo. */
    @SafeVarargs
    protected final C add(Operator operator, T... values) {
        predicates.add(new Predicate<>(operator, new ArrayList<>(Arrays.asList(values))));
        return owner;
    }

    /** O critério dono, para as subclasses devolverem ao encadear. */
    protected C owner() {
        return owner;
    }

    @Override
    public String toString() {
        if (!isSet()) {
            return name + " (não informado)";
        }
        return name + " " + (disjunctive ? "qualquer de " : "todos de ") + predicates;
    }
}
