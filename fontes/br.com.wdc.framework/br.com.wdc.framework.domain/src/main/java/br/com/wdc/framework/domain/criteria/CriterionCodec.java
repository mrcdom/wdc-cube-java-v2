package br.com.wdc.framework.domain.criteria;

import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.SerializationToken;

/**
 * Como um campo de critério trafega.
 *
 * <p>
 * Escrito uma vez, aqui, em vez de repetido em cada {@code XxxModelCodec}: o que muda de campo para campo é o tipo do
 * valor, e não a forma. Cada codec de entidade só diz qual leitor/escritor de valor usar.
 * </p>
 *
 * <p>
 * A forma é um objeto com os pedidos, e não o valor solto que trafegava antes:
 * </p>
 *
 * <pre>
 * "price": { "or": true, "p": [ { "o": "GE", "v": [10.0] }, { "o": "IS_NULL" } ] }
 * </pre>
 *
 * <p>
 * <b>O valor solto não bastaria.</b> Um campo carrega vários pedidos, cada um com seu operador e sua aridade —
 * {@code BETWEEN} leva dois valores, {@code IN} leva muitos, {@code IS NULL} nenhum —, e a disjunção é do campo, não
 * do pedido. Reduzir isso a {@code "price": 10.0} descartaria tudo menos a igualdade, e em silêncio: o outro lado
 * receberia um filtro mais frouxo do que o pedido, devolvendo linhas a mais sem nada indicar a perda.
 * </p>
 */
public final class CriterionCodec {

    /** Como escrever um valor do tipo do campo. */
    @FunctionalInterface
    public interface ValueWriter<T> {
        void write(ExtensibleObjectOutput out, T value);
    }

    /** Como ler um valor do tipo do campo. */
    @FunctionalInterface
    public interface ValueReader<T> {
        T read(ExtensibleObjectInput in);
    }

    public static final ValueWriter<Long> LONG_OUT = (out, v) -> out.value(v.longValue());
    public static final ValueReader<Long> LONG_IN = InputCoerceUtils::asLong;

    public static final ValueWriter<Integer> INT_OUT = (out, v) -> out.value(v.longValue());
    public static final ValueReader<Integer> INT_IN = InputCoerceUtils::asInteger;

    public static final ValueWriter<Double> DOUBLE_OUT = (out, v) -> out.value(v.doubleValue());
    public static final ValueReader<Double> DOUBLE_IN = InputCoerceUtils::asDouble;

    public static final ValueWriter<String> STRING_OUT = (out, v) -> out.value(v);
    public static final ValueReader<String> STRING_IN = InputCoerceUtils::asString;

    public static final ValueWriter<Boolean> BOOL_OUT = (out, v) -> out.value(v.booleanValue());
    public static final ValueReader<Boolean> BOOL_IN = InputCoerceUtils::asBoolean;

    private CriterionCodec() {
        // NOOP
    }

    /**
     * Escreve o campo, se houver o que escrever.
     *
     * <p>
     * Campo não informado não gera chave alguma — é o que mantém curto o critério que filtra por um campo só, o caso
     * corrente.
     * </p>
     */
    public static <T> void write(ExtensibleObjectOutput out, String name, Criterion<?, T> criterion,
            ValueWriter<T> valueWriter) {

        if (criterion == null || !criterion.isSet()) {
            return;
        }

        out.name(name).beginObject();
        if (criterion.disjunctive()) {
            out.name("or").value(true);
        }
        out.name("p").beginArray();
        for (var predicate : criterion.predicates()) {
            out.beginObject();
            out.name("o").value(predicate.operator().name());
            if (!predicate.values().isEmpty()) {
                out.name("v").beginArray();
                for (T value : predicate.values()) {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        valueWriter.write(out, value);
                    }
                }
                out.endArray();
            }
            out.endObject();
        }
        out.endArray();
        out.endObject();
    }

    /**
     * Lê o campo no formato acima, repondo os pedidos no {@code criterion}.
     *
     * <p>
     * Repõe por {@link Criterion#restore}, e não pelos métodos fluentes: {@code eq(null)} não acrescenta pedido — o
     * certo para quem monta filtro de tela — e apagaria em silêncio o que o outro lado enviou.
     * </p>
     *
     * <p>
     * <b>Aceita também o valor solto</b> ({@code "price": 10.0}), lendo-o como igualdade. É o formato que trafegava
     * antes, e continuar entendendo-o custa um {@code if}.
     * </p>
     */
    public static <T> void read(ExtensibleObjectInput in, Criterion<?, T> criterion, ValueReader<T> valueReader) {
        if (in.peek() != SerializationToken.BEGIN_OBJECT) {
            var value = valueReader.read(in);
            if (value != null) {
                criterion.restore(Operator.EQ, List.of(value));
            }
            return;
        }

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
            case "or" -> criterion.restoreDisjunctive(Boolean.TRUE.equals(InputCoerceUtils.asBoolean(in)));
            case "p" -> readPredicates(in, criterion, valueReader);
            default -> in.skipValue();
            }
        }
        in.endObject();
    }

    private static <T> void readPredicates(ExtensibleObjectInput in, Criterion<?, T> criterion,
            ValueReader<T> valueReader) {

        in.beginArray();
        while (in.hasNext()) {
            Operator operator = null;
            var values = new ArrayList<T>(2);

            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                case "o" -> operator = parseOperator(InputCoerceUtils.asString(in));
                case "v" -> {
                    in.beginArray();
                    while (in.hasNext()) {
                        values.add(in.peek() == SerializationToken.NULL ? in.nextNull() : valueReader.read(in));
                    }
                    in.endArray();
                }
                default -> in.skipValue();
                }
            }
            in.endObject();

            if (operator != null) {
                criterion.restore(operator, values);
            }
        }
        in.endArray();
    }

    /**
     * Operador desconhecido é descartado, e não traduzido por aproximação.
     *
     * <p>
     * Acontece quando um lado é mais novo que o outro e envia um operador que este ainda não conhece. Escolher o
     * "mais parecido" produziria um filtro diferente do pedido, sem nada assinalar; deixar o pedido de fora produz um
     * filtro mais frouxo, que é igualmente errado — mas o campo continua listado, e o descompasso aparece.
     * </p>
     */
    private static Operator parseOperator(String name) {
        if (name == null) {
            return null;
        }
        try {
            return Operator.valueOf(name);
        } catch (IllegalArgumentException _ignore) {
            return null;
        }
    }
}
