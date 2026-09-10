package br.com.wdc.framework.jooq;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.Table;

/**
 * A correspondência entre a chave de uma entidade associada e a coluna que a guarda deste lado.
 *
 * <p>
 * Serve a um atalho: quando a projeção da associação pede <b>apenas a chave</b>, o valor já está na linha corrente —
 * é a coluna da chave estrangeira. Ir buscá-lo do outro lado seria um subselect para descobrir o que já se sabe.
 * </p>
 *
 * <p>
 * <b>Os nomes dos métodos são os do {@link JsonQueryBuilder} de propósito.</b> A chave é declarada a partir do mesmo
 * tipo de coluna com que se declararia o campo escalar, e nomes iguais tornam impossível escolher um tipo aqui e
 * outro lá — o que produziria um JSON que a leitura do filho não reconhece.
 * </p>
 *
 * @param <T> a tabela deste lado, onde mora a coluna da chave estrangeira
 */
public final class RelationKey<T extends Table<?>> {

    private final Set<String> names = new LinkedHashSet<>();
    private final List<Function<T, Field<?>>> columns = new ArrayList<>();
    private final List<JsonFieldType> types = new ArrayList<>();

    RelationKey() {
        // Só o builder instancia.
    }

    /** Campo Long da chave do filho, guardado nesta coluna. */
    public RelationKey<T> addI64(String fn, Function<T, Field<? extends Number>> jooqField) {
        return add(fn, jooqField, JsonFieldType.NUMBER);
    }

    /** Campo Integer da chave do filho, guardado nesta coluna. */
    public RelationKey<T> addI32(String fn, Function<T, Field<? extends Number>> jooqField) {
        return add(fn, jooqField, JsonFieldType.NUMBER);
    }

    /** Campo Double da chave do filho, guardado nesta coluna. */
    public RelationKey<T> addF64(String fn, Function<T, Field<? extends Number>> jooqField) {
        return add(fn, jooqField, JsonFieldType.NUMBER);
    }

    /** Campo BigDecimal da chave do filho, guardado nesta coluna. */
    public RelationKey<T> addDec(String fn, Function<T, Field<BigDecimal>> jooqField) {
        return add(fn, jooqField, JsonFieldType.NUMBER);
    }

    /** Campo String da chave do filho, guardado nesta coluna. */
    public RelationKey<T> addStr(String fn, Function<T, Field<String>> jooqField) {
        return add(fn, jooqField, JsonFieldType.STRING);
    }

    /** Campo enum da chave do filho, guardado nesta coluna como texto. */
    public RelationKey<T> addEnm(String fn, Function<T, Field<String>> jooqField) {
        return add(fn, jooqField, JsonFieldType.STRING);
    }

    /** Campo Boolean da chave do filho, guardado nesta coluna. */
    public RelationKey<T> addBit(String fn, Function<T, Field<Boolean>> jooqField) {
        return add(fn, jooqField, JsonFieldType.BOOLEAN);
    }

    /** Campo OffsetDateTime da chave do filho, guardado nesta coluna. */
    public RelationKey<T> addOdt(String fn, Function<T, Field<OffsetDateTime>> jooqField) {
        return add(fn, jooqField, JsonFieldType.DATETIME);
    }

    /** Campo de data/hora sem fuso da chave do filho, guardado nesta coluna. */
    public RelationKey<T> addLdt(String fn, Function<T, Field<java.time.LocalDateTime>> jooqField) {
        return add(fn, jooqField, JsonFieldType.DATETIME);
    }

    /** Campo byte[] da chave do filho, guardado nesta coluna. */
    public RelationKey<T> addBin(String fn, Function<T, Field<byte[]>> jooqField) {
        return add(fn, jooqField, JsonFieldType.BINARY);
    }

    @SuppressWarnings("unchecked")
    private RelationKey<T> add(String fn, Function<T, ? extends Field<?>> jooqField, JsonFieldType type) {
        names.add(fn);
        columns.add((Function<T, Field<?>>) jooqField);
        types.add(type);
        return this;
    }

    /** Os campos que esta chave cobre — é contra eles que se pergunta se a projeção pediu mais. */
    Set<String> names() {
        return names;
    }

    boolean isEmpty() {
        return names.isEmpty();
    }

    /** O objeto da chave, montado com as colunas desta linha. */
    List<JsonFieldEntry> entries(T table) {
        var entries = new ArrayList<JsonFieldEntry>(names.size());
        int i = 0;
        for (var name : names) {
            entries.add(new JsonFieldEntry(name, columns.get(i).apply(table), types.get(i)));
            i++;
        }
        return entries;
    }
}
