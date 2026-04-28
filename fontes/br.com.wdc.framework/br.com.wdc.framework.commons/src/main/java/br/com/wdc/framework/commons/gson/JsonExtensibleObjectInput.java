package br.com.wdc.framework.commons.gson;

import java.io.Closeable;
import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.SerializationToken;

public class JsonExtensibleObjectInput implements ExtensibleObjectInput, Closeable {

    protected JsonReader impl;

    public JsonExtensibleObjectInput(JsonReader impl) {
        this.impl = impl;
    }

    @Override
    public void close() throws IOException {
        this.impl.close();
    }

    @Override
    public void beginArray() throws IOException {
        impl.beginArray();
    }

    @Override
    public void endArray() throws IOException {
        impl.endArray();
    }

    @Override
    public void beginObject() throws IOException {
        impl.beginObject();
    }

    @Override
    public void endObject() throws IOException {
        impl.endObject();
    }

    @Override
    public boolean hasNext() throws IOException {
        return impl.hasNext();
    }

    @Override
    public SerializationToken peek() throws IOException {
        return toSerializationToken(impl.peek());
    }

    @Override
    public String nextName() throws IOException {
        return impl.nextName();
    }

    @Override
    public String nextString() throws IOException {
        return impl.nextString();
    }

    @Override
    public boolean nextBoolean() throws IOException {
        return impl.nextBoolean();
    }

    @Override
    public <T> T nextNull() throws IOException {
        impl.nextNull();
        return null;
    }

    @Override
    public double nextDouble() throws IOException {
        return impl.nextDouble();
    }

    @Override
    public Number nextNumber() throws IOException {
        var token = impl.peek();
        if (token == JsonToken.NULL) {
            impl.nextNull();
            return null;
        }
        return Double.valueOf(impl.nextDouble());
    }

    @Override
    public long nextLong() throws IOException {
        return impl.nextLong();
    }

    @Override
    public int nextInt() throws IOException {
        return impl.nextInt();
    }

    @Override
    public void skipValue() throws IOException {
        impl.skipValue();
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
