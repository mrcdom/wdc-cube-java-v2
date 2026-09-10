package br.com.wdc.framework.jooq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import br.com.wdc.framework.domain.criteria.Criterion;
import br.com.wdc.framework.domain.criteria.Operator;

/**
 * Traduz um campo de critério em condição jOOQ.
 *
 * <p>
 * Escrito uma vez, aqui, em vez de repetido por entidade. Antes cada {@code ApplyConditions} carregava um {@code if}
 * por campo filtrável, o que multiplicava por entidade qualquer mudança na forma de filtrar. Acrescentar um operador
 * novo agora é mexer neste arquivo.
 * </p>
 *
 * <p>
 * O que continua por entidade é só o que ela sabe e o framework não: qual coluna corresponde a cada campo e, quando o
 * tipo do domínio difere do da coluna, como converter o valor — com os tipos conferidos na compilação.
 * </p>
 *
 * <p>
 * <b>Campo não informado devolve {@code null}</b> e não entra na consulta; critério inteiramente vazio resulta em
 * {@code noCondition()} — nunca uma condição falsa, que transformaria "sem filtro" em "nenhum resultado".
 * </p>
 */
public final class CriterionTranslator {

    private CriterionTranslator() {
        // NOOP
    }

    /** Condição de um campo cujo valor vai ao banco como está. */
    public static <V> Condition translate(Field<V> column, Criterion<?, V> criterion) {
        return translate(column, criterion, null);
    }

    /**
     * Condição de um campo cujo valor precisa ser convertido antes de chegar à coluna.
     *
     * @param <A> tipo do valor no domínio — o enum, {@code OffsetDateTime}, {@code Double}
     * @param <B> tipo da coluna — {@code String}, {@code LocalDateTime}, {@code BigDecimal}
     */
    public static <A, B> Condition translate(Field<B> column, Criterion<?, A> criterion, Function<A, B> converter) {
        if (criterion == null || !criterion.isSet()) {
            return null;
        }
        var ofField = new ArrayList<Condition>();
        for (var predicate : criterion.predicates()) {
            var c = condition(column, predicate, converter);
            if (c != null) {
                ofField.add(c);
            }
        }
        if (ofField.isEmpty()) {
            return null;
        }
        if (ofField.size() == 1) {
            return ofField.get(0);
        }
        // AND por padrão — é o que faz ge(inicio) com le(fim) significar intervalo, e ne(1) com ne(2) excluir os dois.
        // Só vira OR quando o campo foi marcado com or(), e a disjunção fica contida no campo.
        return criterion.disjunctive() ? DSL.or(ofField) : DSL.and(ofField);
    }

    /**
     * Condição de um campo sobre a tabela informada, e não sobre a que declarou a coluna.
     *
     * <p>
     * Serve ao subselect correlacionado, onde a instância da tabela tem alias próprio. A coluna entra como
     * <b>nome</b>: o campo que vai para a consulta é o resolvido em {@code t}, já qualificado pelo alias dela.
     * </p>
     */
    public static <V> Condition translate(Table<?> t, TableField<?, V> column, Criterion<?, V> criterion) {
        if (criterion == null || !criterion.isSet()) {
            return null;
        }
        return translate(column(t, column), criterion, null);
    }

    /** Como {@link #translate(Table, TableField, Criterion)}, para campo que precisa de conversão. */
    public static <A, B> Condition translate(Table<?> t, TableField<?, B> column, Criterion<?, A> criterion,
            Function<A, B> converter) {
        if (criterion == null || !criterion.isSet()) {
            return null;
        }
        return translate(column(t, column), criterion, converter);
    }

    /**
     * A coluna, resolvida em {@code t}.
     *
     * <p>
     * <b>Recusa quando não existe, em vez de ignorar o filtro.</b> Devolver {@code noCondition()} aí entregaria o
     * conjunto inteiro como se o filtro tivesse sido aplicado — resposta errada, e silenciosa.
     * </p>
     */
    public static <V> Field<V> column(Table<?> t, TableField<?, V> column) {
        var field = t.field(column);
        if (field == null) {
            throw new IllegalArgumentException(
                    "a coluna " + column.getName() + " não existe em " + t.getName());
        }
        return field;
    }

    /** Junta as condições que sobraram; lista vazia devolve {@code noCondition()}. */
    public static Condition and(List<Condition> conditions) {
        var useful = new ArrayList<Condition>();
        for (var c : conditions) {
            if (c != null) {
                useful.add(c);
            }
        }
        return useful.isEmpty() ? DSL.noCondition() : DSL.and(useful);
    }

    private static <A, B> Condition condition(Field<B> column, Criterion.Predicate<A> predicate,
            Function<A, B> converter) {

        var op = predicate.operator();

        if (op == Operator.IS_NULL) {
            return column.isNull();
        }
        if (op == Operator.IS_NOT_NULL) {
            return column.isNotNull();
        }

        var values = convert(predicate.values(), converter);
        if (!op.accepts(values.size())) {
            // Operador e valores em desacordo — ignorar é melhor do que emitir SQL que o banco recusaria.
            return null;
        }

        return switch (op) {
        case EQ -> column.eq(values.get(0));
        case NE -> column.ne(values.get(0));
        case GT -> column.gt(values.get(0));
        case GE -> column.ge(values.get(0));
        case LT -> column.lt(values.get(0));
        case LE -> column.le(values.get(0));
        case LIKE -> textual(column).like((String) values.get(0));
        case ILIKE -> textual(column).likeIgnoreCase((String) values.get(0));
        case BETWEEN -> column.between(values.get(0), values.get(1));
        case IN -> column.in(values);
        default -> null;
        };
    }

    /**
     * {@code like} e {@code ilike} só são oferecidos em {@code TextCriterion}, que quem declara o campo escolhe pelo
     * tipo da coluna — então esta conversão só é alcançada para coluna textual.
     */
    @SuppressWarnings("unchecked")
    private static <B> Field<String> textual(Field<B> column) {
        return (Field<String>) column;
    }

    private static <A, B> List<B> convert(Collection<A> values, Function<A, B> converter) {
        var result = new ArrayList<B>(values.size());
        for (var v : values) {
            @SuppressWarnings("unchecked")
            var converted = converter == null ? (B) v : converter.apply(v);
            result.add(converted);
        }
        return result;
    }
}
