package br.com.wdc.framework.commons.gson;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Base64;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.gson.stream.JsonWriter;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;

public class JsonExtensibleObjectOutput implements ExtensibleObjectOutput, Closeable, Flushable {

    protected JsonWriter impl;
    protected boolean useIdAsKey;

    public JsonExtensibleObjectOutput(JsonWriter impl) {
        this.impl = impl;
    }

    public JsonExtensibleObjectOutput(JsonWriter impl, boolean useIdAsKey) {
        this.impl = impl;
        this.useIdAsKey = useIdAsKey;
    }

    @Override
    public void close() {
        try {
            this.impl.close();
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    @Override
    public void flush() {
        try {
            this.impl.flush();
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    public JsonExtensibleObjectOutput beginArray() {
        try {
            impl.beginArray();
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput endArray() {
        try {
            impl.endArray();
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput beginObject() {
        try {
            impl.beginObject();
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput endObject() {
        try {
            impl.endObject();
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput name(String name) {
        try {
            impl.name(name);
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput name(int id, String name) {
        try {
            if (this.useIdAsKey) {
                impl.name(String.valueOf(id));
            } else {
                impl.name(name);
            }
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput value(String value) {
        try {
            impl.value(value);
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput value(byte[] value) {
        try {
            if (value == null) {
                impl.nullValue();
                return this;
            }
            impl.value(Base64.getEncoder().encodeToString(value));
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput nullValue() {
        try {
            impl.nullValue();
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput value(boolean value) {
        try {
            impl.value(value);
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput value(double value) {
        try {
            impl.value(value);
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput value(long value) {
        try {
            impl.value(value);
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

    public JsonExtensibleObjectOutput value(Number value) {
        try {
            impl.value(value);
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
        return this;
    }

}
