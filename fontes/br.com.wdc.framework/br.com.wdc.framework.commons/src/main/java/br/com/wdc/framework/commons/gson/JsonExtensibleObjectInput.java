package br.com.wdc.framework.commons.gson;

import java.io.Closeable;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.SerializationToken;
import br.com.wdc.framework.commons.util.Rethrow;

public class JsonExtensibleObjectInput implements ExtensibleObjectInput, Closeable {

    protected JsonReader impl;

    public JsonExtensibleObjectInput(JsonReader impl) {
        this.impl = impl;
    }

    @Override
    public void close() {
        try {
            this.impl.close();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public void beginArray() {
        try {
            impl.beginArray();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public void endArray() {
        try {
            impl.endArray();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public void beginObject() {
        try {
            impl.beginObject();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public void endObject() {
        try {
            impl.endObject();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return impl.hasNext();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public SerializationToken peek() {
        try {
            return toSerializationToken(impl.peek());
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public String nextName() {
        try {
            return impl.nextName();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public String nextString() {
        try {
            return impl.nextString();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public boolean nextBoolean() {
        try {
            return impl.nextBoolean();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public <T> T nextNull() {
        try {
            impl.nextNull();
            return null;
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public double nextDouble() {
        try {
            return impl.nextDouble();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public Number nextNumber() {
        try {
            var token = impl.peek();
            if (token == JsonToken.NULL) {
                impl.nextNull();
                return null;
            }
            return Double.valueOf(impl.nextDouble());
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public long nextLong() {
        try {
            return impl.nextLong();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public int nextInt() {
        try {
            return impl.nextInt();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    @Override
    public void skipValue() {
        try {
            impl.skipValue();
        } catch (Exception e) {
            throw Rethrow.asRuntimeException(e);
        }
    }

    // :: Internal

    private static SerializationToken toSerializationToken(JsonToken token) {
        return switch (token) {
        case BEGIN_ARRAY -> SerializationToken.BEGIN_ARRAY;
        case END_ARRAY -> SerializationToken.END_ARRAY;
        case BEGIN_OBJECT -> SerializationToken.BEGIN_OBJECT;
        case END_OBJECT -> SerializationToken.END_OBJECT;
        case NAME -> SerializationToken.NAME;
        case STRING -> SerializationToken.STRING;
        case NUMBER -> SerializationToken.NUMBER;
        case BOOLEAN -> SerializationToken.BOOLEAN;
        case NULL -> SerializationToken.NULL;
        case END_DOCUMENT -> SerializationToken.END_DOCUMENT;
        };
    }

}
