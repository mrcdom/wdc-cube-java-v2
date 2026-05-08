package br.com.wdc.framework.commons.serialization;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.util.Rethrow;

@SuppressWarnings({ "rawtypes" })
public class MapOrListInput implements ExtensibleObjectInput {

    private static final Double ZERO_DOUBLE = 0.0;
    private static final Integer ZERO_INTEGER = 0;
    private static final Long ZERO_LONG = 0L;

    private StackItem current;

    public MapOrListInput(Map map) {
        this.current = new StackItem();
        this.current.name = null;
        this.current.value = map;
        this.current.token = SerializationToken.BEGIN_OBJECT;
    }

    public MapOrListInput(List list) {
        this.current = new StackItem();
        this.current.name = null;
        this.current.value = list;
        this.current.token = SerializationToken.BEGIN_ARRAY;
    }

    @Override
    public void beginObject() {
        if (this.current.token != SerializationToken.BEGIN_OBJECT) {
            throw Rethrow.asRuntimeException(
                    new IOException("Expected BEGIN_OBJECT but found " + this.current.token.name()));
        }
        this.current.token = SerializationToken.END_OBJECT;

        if (this.current.value instanceof Map valueMap) {
            StackItem stackItem = new StackItem();
            stackItem.previous = this.current;
            stackItem.it = valueMap.entrySet().iterator();
            stackItem.hasValue = false;
            this.current = stackItem;
            this.fetchNext();
        } else {
            throw Rethrow.asRuntimeException(new IOException("Expected value as a Map object"));
        }
    }

    @Override
    public void endObject() {
        StackItem previousStackItem = this.current.previous;
        if (previousStackItem == null) {
            throw Rethrow.asRuntimeException(new IOException("Expected an element but no one was found"));
        }

        if (previousStackItem.token != SerializationToken.END_OBJECT) {
            throw Rethrow
                    .asRuntimeException(new IOException("Expected END_OBJECT but found " + this.current.token.name()));
        }

        this.current = previousStackItem;
        if (this.current.previous != null) {
            this.fetchNext();
        }
    }

    @Override
    public void beginArray() {
        if (this.current.token != SerializationToken.BEGIN_ARRAY) {
            throw Rethrow
                    .asRuntimeException(new IOException("Expected BEGIN_ARRAY but found " + this.current.token.name()));
        }
        this.current.token = SerializationToken.END_ARRAY;

        if (this.current.value instanceof List valueList) {
            StackItem stackItem = new StackItem();
            stackItem.previous = this.current;
            stackItem.it = valueList.iterator();
            stackItem.hasValue = false;
            this.current = stackItem;
            this.fetchNext();
        } else {
            throw Rethrow.asRuntimeException(new IOException("Expected value as a List object"));
        }
    }

    @Override
    public void endArray() {
        StackItem previousStackItem = this.current.previous;
        if (previousStackItem == null) {
            throw Rethrow.asRuntimeException(new IOException("Expected an element but no one was found"));
        }

        if (previousStackItem.token != SerializationToken.END_ARRAY) {
            throw Rethrow
                    .asRuntimeException(new IOException("Expected END_ARRAY but found " + this.current.token.name()));
        }

        this.current = previousStackItem;
        if (this.current.previous != null) {
            this.fetchNext();
        }
    }

    private void fetchNext() {
        if (this.current.it == null) {
            throw Rethrow.asRuntimeException(new IOException("Expected an iterator but found " + this.current.value));
        }

        if (!this.current.it.hasNext()) {
            this.current.name = null;
            this.current.value = null;
            this.current.hasValue = false;
            if (this.current.previous != null) {
                this.current.token = this.current.previous.token;
            }
            return;
        }

        Object item = this.current.it.next();
        if (item instanceof Map.Entry entry) {
            this.current.name = CoerceUtils.asString(entry.getKey());
            item = entry.getValue();
        }

        this.current.value = item;
        this.current.token = resolveToken(item);
        this.current.hasValue = true;
    }

    private static SerializationToken resolveToken(Object value) {
        // @formatter:off
        if (value == null) return SerializationToken.NULL;
        if (value instanceof Map) return SerializationToken.BEGIN_OBJECT;
        if (value instanceof List) return SerializationToken.BEGIN_ARRAY;
        if (value instanceof String || value instanceof Character) return SerializationToken.STRING;
        if (value instanceof Number) return SerializationToken.NUMBER;
        if (value instanceof Boolean) return SerializationToken.BOOLEAN;
        throw Rethrow.asRuntimeException(new IOException("Non supported value: " + value));
        // @formatter:on
    }

    @Override
    public boolean hasNext() {
        return this.current.hasValue;
    }

    @Override
    public SerializationToken peek() {
        return this.current.token;
    }

    @Override
    public String nextName() {
        return this.current.name;
    }

    @Override
    public <T> T nextNull() {
        if (this.current.value != null) {
            throw Rethrow.asRuntimeException((new IOException("Expected null value but found " + this.current.value)));
        }
        this.fetchNext();
        return null;
    }

    @Override
    public String nextString() {
        String result = CoerceUtils.asString(this.current.value, null);
        this.fetchNext();
        return result;
    }

    @Override
    public boolean nextBoolean() {
        boolean result = CoerceUtils.asBoolean(this.current.value, Boolean.FALSE);
        this.fetchNext();
        return result;
    }

    @Override
    public Number nextNumber() {
        Number result = CoerceUtils.asNumber(this.current.value, null);
        this.fetchNext();
        return result;
    }

    @Override
    public double nextDouble() {
        double result = CoerceUtils.asDouble(this.current.value, ZERO_DOUBLE);
        this.fetchNext();
        return result;
    }

    @Override
    public long nextLong() {
        long result = CoerceUtils.asLong(this.current.value, ZERO_LONG);
        this.fetchNext();
        return result;
    }

    @Override
    public int nextInt() {
        int result = CoerceUtils.asInteger(this.current.value, ZERO_INTEGER);
        this.fetchNext();
        return result;
    }

    @Override
    public void skipValue() {
        this.current.value = null;
        this.fetchNext();
    }

    private static class StackItem {
        StackItem previous;

        SerializationToken token;

        String name;
        Object value;

        Iterator it;

        boolean hasValue;
    }

}
