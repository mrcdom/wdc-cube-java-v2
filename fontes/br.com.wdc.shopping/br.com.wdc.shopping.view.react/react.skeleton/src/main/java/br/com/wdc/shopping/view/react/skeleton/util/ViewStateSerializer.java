package br.com.wdc.shopping.view.react.skeleton.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.ViewState;

/**
 * Classe comando responsável por serializar um {@link ViewState} para JSON.
 *
 * <p>
 * Cada instância encapsula o contexto de uma única execução de serialização: o {@link ExtensibleObjectOutput} de
 * destino e o mapa {@code ViewState → vsid} fornecido pelo chamador. Não há estado estático.
 * </p>
 *
 * <ul>
 * <li>Injeta {@code "vsid"} como primeiro campo (instanceId da view).</li>
 * <li>Campos {@code transient} e {@code static} são ignorados.</li>
 * <li>Campos do tipo {@link CubeView} ou {@link ViewState} são serializados como {@code "<nome>Id": vsid} com sufixo
 * {@code "Id"}.</li>
 * <li>Tipos suportados em campos e em coleções: todos os primitivos Java e seus wrappers, {@link String},
 * {@link Character}, datas ({@link java.util.Date} e extensões SQL), {@link Collection} (List/Set), {@link Map} e
 * Records Java.</li>
 * <li>Datas são formatadas como ISO 8601.</li>
 * <li>Strings em branco em campos diretos são omitidas.</li>
 * <li>Primitivos numéricos com valor zero em campos diretos são omitidos.</li>
 * <li>Booleanos primitivos são sempre serializados (o cliente precisa saber quando mudam para false).</li>
 * </ul>
 */
public final class ViewStateSerializer {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ISO_LOCAL_TIME;

    private final ExtensibleObjectOutput json;

    /**
     * @param json destino da serialização
     */
    public ViewStateSerializer(ExtensibleObjectOutput json) {
        this.json = json;
    }

    public void write(Object state, String instanceId) {
        try {
            json.beginObject();
            json.name("#").value(instanceId);

            for (Field field : state.getClass().getFields()) {
                if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
                    continue;

                Object value = field.get(state);
                writeField(field, value);
            }

            json.endObject();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Falha ao serializar ViewState: " + state.getClass().getSimpleName(), e);
        }
    }

    private void writeField(Field field, Object value) {
        Class<?> type = field.getType();

        // Booleanos primitivos: sempre serializados (o cliente precisa saber quando mudam para false)
        if (type == boolean.class) {
            json.name(field.getName()).value((boolean) value);
            return;
        }

        if (value == null)
            return;

        // CubeView: "<campo>Id": instanceId
        if (value instanceof CubeView cv) {
            json.name(field.getName() + "Id").value(cv.instanceId());
            return;
        }

        // String: omite se em branco (campo direto)
        if (value instanceof String s) {
            if (!s.isBlank()) {
                json.name(field.getName()).value(s);
            }
            return;
        }

        // Primitivos numéricos: omite se zero (campo direto)
        if (type == int.class) {
            int v = (int) value;
            if (v != 0)
                json.name(field.getName()).value(v);
            return;
        }
        if (type == long.class) {
            long v = (long) value;
            if (v != 0L)
                json.name(field.getName()).value(v);
            return;
        }
        if (type == double.class) {
            double v = (double) value;
            if (v != 0.0)
                json.name(field.getName()).value(v);
            return;
        }
        if (type == float.class) {
            float v = (float) value;
            if (v != 0.0f)
                json.name(field.getName()).value(v);
            return;
        }
        if (type == short.class) {
            short v = (short) value;
            if (v != 0)
                json.name(field.getName()).value(v);
            return;
        }
        if (type == byte.class) {
            byte v = (byte) value;
            if (v != 0)
                json.name(field.getName()).value(v);
            return;
        }
        if (type == char.class) {
            json.name(field.getName()).value(String.valueOf((char) value));
            return;
        }

        // Demais tipos: wrappers, datas, coleções, etc.
        json.name(field.getName());
        writeValue(value);
    }

    /**
     * Serializa um valor sem prefixo de nome — usado dentro de arrays e maps. Strings vazias e números zero NÃO são
     * omitidos neste contexto.
     */
    private void writeValue(Object value) {
        if (value == null) {
            json.nullValue();
            return;
        }

        if (value instanceof CubeView cv) {
            json.value(cv.instanceId());
            return;
        }

        if (value instanceof Boolean b) {
            json.value(b);
            return;
        }

        if (value instanceof String s) {
            json.value(s);
            return;
        }

        if (value instanceof Character c) {
            json.value(String.valueOf(c));
            return;
        }

        // Datas: do mais específico para o mais genérico (subtipos sql estendem java.util.Date)
        if (value instanceof java.sql.Date sqlDate) {
            json.value(sqlDate.toLocalDate().format(DATE_FMT));
            return;
        }
        if (value instanceof java.sql.Time sqlTime) {
            json.value(sqlTime.toLocalTime().format(TIME_FMT));
            return;
        }
        // java.sql.Timestamp estende java.util.Date e toInstant() funciona para ambos
        if (value instanceof java.util.Date date) {
            json.value(date.toInstant().atOffset(ZoneOffset.UTC).format(DATE_TIME_FMT));
            return;
        }

        // Wrappers numéricos: Byte, Short, Integer, Long, Float, Double
        if (value instanceof Number n) {
            json.value(n);
            return;
        }

        if (value instanceof Collection<?> coll) {
            json.beginArray();
            for (Object item : coll) {
                writeValue(item);
            }
            json.endArray();
            return;
        }

        if (value instanceof Map<?, ?> map) {
            json.beginObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                json.name(String.valueOf(entry.getKey()));
                writeValue(entry.getValue());
            }
            json.endObject();
            return;
        }

        // Records Java: serializa como objeto JSON usando os componentes do record.
        if (value.getClass().isRecord()) {
            writeRecord(value);
            return;
        }

        // POJOs com campos públicos (ex: PurchaseInfo)
        Field[] publicFields = value.getClass().getFields();
        if (publicFields.length > 0) {
            writePojo(value, publicFields);
            return;
        }

        // Fallback: toString (enums, tipos não previstos)
        json.value(value.toString());
    }

    private void writeRecord(Object record) {
        try {
            json.beginObject();
            for (var component : record.getClass().getRecordComponents()) {
                var accessor = component.getAccessor();
                accessor.setAccessible(true);
                json.name(component.getName());
                writeValue(accessor.invoke(record));
            }
            json.endObject();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar record " + record.getClass().getSimpleName(), e);
        }
    }

    private void writePojo(Object pojo, Field[] fields) {
        try {
            json.beginObject();
            for (Field field : fields) {
                if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
                    continue;
                Object value = field.get(pojo);
                if (value == null)
                    continue;
                json.name(field.getName());
                writeValue(value);
            }
            json.endObject();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Falha ao serializar POJO " + pojo.getClass().getSimpleName(), e);
        }
    }
}