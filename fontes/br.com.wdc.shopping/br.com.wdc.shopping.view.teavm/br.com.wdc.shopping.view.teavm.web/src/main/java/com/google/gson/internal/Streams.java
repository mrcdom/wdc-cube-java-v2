/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.internal;

import java.io.EOFException;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;

/**
 * TeaVM-compatible replacement for Gson's Streams class.
 * Avoids TypeAdapters which triggers AtomicIntegerArray (unsupported in TeaVM).
 */
public final class Streams {
    private Streams() {
        throw new UnsupportedOperationException();
    }

    public static JsonElement parse(JsonReader reader) throws JsonParseException {
        boolean isEmpty = true;
        try {
            reader.peek();
            isEmpty = false;
            return readElement(reader);
        } catch (EOFException e) {
            if (isEmpty) {
                return JsonNull.INSTANCE;
            }
            throw new JsonSyntaxException(e);
        } catch (MalformedJsonException | NumberFormatException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    private static JsonElement readElement(JsonReader reader) throws IOException {
        JsonToken token = reader.peek();
        switch (token) {
            case BEGIN_OBJECT: {
                var obj = new JsonObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    obj.add(reader.nextName(), readElement(reader));
                }
                reader.endObject();
                return obj;
            }
            case BEGIN_ARRAY: {
                var arr = new JsonArray();
                reader.beginArray();
                while (reader.hasNext()) {
                    arr.add(readElement(reader));
                }
                reader.endArray();
                return arr;
            }
            case STRING:
                return new JsonPrimitive(reader.nextString());
            case NUMBER:
                return new JsonPrimitive(new LazilyParsedNumber(reader.nextString()));
            case BOOLEAN:
                return new JsonPrimitive(reader.nextBoolean());
            case NULL:
                reader.nextNull();
                return JsonNull.INSTANCE;
            default:
                throw new IllegalStateException("Unexpected token: " + token);
        }
    }

    public static void write(JsonElement element, JsonWriter writer) throws IOException {
        if (element == null || element.isJsonNull()) {
            writer.nullValue();
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive p = element.getAsJsonPrimitive();
            if (p.isBoolean()) {
                writer.value(p.getAsBoolean());
            } else if (p.isNumber()) {
                writer.value(p.getAsNumber());
            } else {
                writer.value(p.getAsString());
            }
        } else if (element.isJsonObject()) {
            writer.beginObject();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                writer.name(entry.getKey());
                write(entry.getValue(), writer);
            }
            writer.endObject();
        } else if (element.isJsonArray()) {
            writer.beginArray();
            for (JsonElement item : element.getAsJsonArray()) {
                write(item, writer);
            }
            writer.endArray();
        } else {
            throw new IllegalArgumentException("Unknown JsonElement type: " + element.getClass());
        }
    }

    public static Writer writerForAppendable(Appendable appendable) {
        return appendable instanceof Writer writer ? writer : new AppendableWriter(appendable);
    }

    private static final class AppendableWriter extends Writer {
        private final Appendable appendable;
        private final CurrentWrite currentWrite = new CurrentWrite();

        AppendableWriter(Appendable appendable) {
            this.appendable = appendable;
        }

        @Override
        public void write(char[] chars, int offset, int length) throws IOException {
            currentWrite.setChars(chars);
            appendable.append(currentWrite, offset, offset + length);
        }

        @Override
        public void flush() {
            // NOOP
        }

        @Override
        public void close() {
            // NOOP
        }

        @Override
        public void write(int i) throws IOException {
            appendable.append((char) i);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            Objects.requireNonNull(str);
            appendable.append(str, off, off + len);
        }

        @Override
        public Writer append(CharSequence csq) throws IOException {
            appendable.append(csq);
            return this;
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) throws IOException {
            appendable.append(csq, start, end);
            return this;
        }

        private static class CurrentWrite implements CharSequence {
            private char[] chars;
            private String cachedString;

            void setChars(char[] chars) {
                this.chars = chars;
                this.cachedString = null;
            }

            @Override
            public int length() {
                return chars.length;
            }

            @Override
            public char charAt(int i) {
                return chars[i];
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return new String(chars, start, end - start);
            }

            @Override
            public String toString() {
                if (cachedString == null) {
                    cachedString = new String(chars);
                }
                return cachedString;
            }
        }
    }
}
