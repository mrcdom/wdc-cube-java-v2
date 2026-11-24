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
 * The mapping between the key of an associated entity and the column that holds it on this side.
 *
 * <p>
 * It serves a shortcut: when the association's projection asks for <b>only the key</b>, the value is already in the
 * current row — it is the foreign key column. Fetching it from the other side would be a subselect to find out what is
 * already known.
 * </p>
 *
 * <p>
 * <b>The method names are those of {@link JsonQueryBuilder} on purpose.</b> The key is declared from the same column
 * type with which the scalar field would be declared, and identical names make it impossible to pick one type here and
 * another there — which would produce JSON that the child's reader does not recognize.
 * </p>
 *
 * @param <T> the table on this side, where the foreign key column lives
 */
public final class RelationKey<T extends Table<?>> {

    private final Set<String> names = new LinkedHashSet<>();
    private final List<Function<T, Field<?>>> columns = new ArrayList<>();
    private final List<JsonFieldType> types = new ArrayList<>();

    RelationKey() {
        // Only the builder instantiates it.
    }

    /** Long field of the child key, stored in this column. */
    public RelationKey<T> addI64(String fn, Function<T, Field<? extends Number>> jooqField) {
        return add(fn, jooqField, JsonFieldType.NUMBER);
    }

    /** Integer field of the child key, stored in this column. */
    public RelationKey<T> addI32(String fn, Function<T, Field<? extends Number>> jooqField) {
        return add(fn, jooqField, JsonFieldType.NUMBER);
    }

    /** Double field of the child key, stored in this column. */
    public RelationKey<T> addF64(String fn, Function<T, Field<? extends Number>> jooqField) {
        return add(fn, jooqField, JsonFieldType.NUMBER);
    }

    /** BigDecimal field of the child key, stored in this column. */
    public RelationKey<T> addDec(String fn, Function<T, Field<BigDecimal>> jooqField) {
        return add(fn, jooqField, JsonFieldType.NUMBER);
    }

    /** String field of the child key, stored in this column. */
    public RelationKey<T> addStr(String fn, Function<T, Field<String>> jooqField) {
        return add(fn, jooqField, JsonFieldType.STRING);
    }

    /** Enum field of the child key, stored in this column as text. */
    public RelationKey<T> addEnm(String fn, Function<T, Field<String>> jooqField) {
        return add(fn, jooqField, JsonFieldType.STRING);
    }

    /** Boolean field of the child key, stored in this column. */
    public RelationKey<T> addBit(String fn, Function<T, Field<Boolean>> jooqField) {
        return add(fn, jooqField, JsonFieldType.BOOLEAN);
    }

    /** OffsetDateTime field of the child key, stored in this column. */
    public RelationKey<T> addOdt(String fn, Function<T, Field<OffsetDateTime>> jooqField) {
        return add(fn, jooqField, JsonFieldType.DATETIME);
    }

    /** Date/time field without time zone of the child key, stored in this column. */
    public RelationKey<T> addLdt(String fn, Function<T, Field<java.time.LocalDateTime>> jooqField) {
        return add(fn, jooqField, JsonFieldType.DATETIME);
    }

    /** byte[] field of the child key, stored in this column. */
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

    /** The fields this key covers — they are what is checked to tell whether the projection asked for more. */
    Set<String> names() {
        return names;
    }

    boolean isEmpty() {
        return names.isEmpty();
    }

    /** The key object, built from the columns of this row. */
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
