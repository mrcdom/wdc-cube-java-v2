package br.com.wdc.framework.commons.gson;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import br.com.wdc.framework.commons.lang.CoerceUtils;

public class JsonCoerceUtils {

    private JsonCoerceUtils() {
        super();
    }

    // String: Start

    public static String asString(JsonReader jr) {
        return JsonCoerceUtils.asString(jr, null);
    }

    public static String asString(JsonReader jr, String defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.STRING) {
                return jr.nextString();
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asString(jr.nextBoolean(), defaultValue);
            }

            if (jr.peek() == JsonToken.NUMBER) {
                if (Holder.jrIsPeekLong(jr)) {
                    return CoerceUtils.asString(jr.nextLong(), defaultValue);
                }
                return CoerceUtils.asString(jr.nextDouble(), defaultValue);
            }

            throw new IOException("No valid value found. JsonReader.peek() = " + jr.peek());
        } catch (IOException | IllegalAccessException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static String asTrimmedString(JsonReader jr) {
        return JsonCoerceUtils.asTrimmedString(jr, null);
    }

    public static String asTrimmedString(JsonReader jr, String defaultValue) {
        if (defaultValue == null) {
            return defaultValue;
        }
        return JsonCoerceUtils.asString(jr, defaultValue).trim();
    }

    public static String asLowerCaseString(JsonReader jr) {
        return JsonCoerceUtils.asLowerCaseString(jr, null);
    }

    public static String asLowerCaseString(JsonReader jr, String defaultValue) {
        if (defaultValue == null) {
            return defaultValue;
        }
        return JsonCoerceUtils.asString(jr, defaultValue).toLowerCase();
    }

    public static String asUpperCaseString(JsonReader jr) {
        return JsonCoerceUtils.asUpperCaseString(jr, null);
    }

    public static String asUpperCaseString(JsonReader jr, String defaultValue) {
        if (defaultValue == null) {
            return defaultValue;
        }
        return JsonCoerceUtils.asString(jr, defaultValue).toUpperCase();
    }

    // String: End

    public static Boolean asBoolean(JsonReader jr) {
        return JsonCoerceUtils.asBoolean(jr, null);
    }

    public static Boolean asBoolean(JsonReader jr, Boolean defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return jr.nextBoolean();
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asBoolean(jr.nextString());
            }

            if (jr.peek() == JsonToken.NUMBER) {
                if (Holder.jrIsPeekLong(jr)) {
                    return CoerceUtils.asBoolean(jr.nextLong(), defaultValue);
                }
                return CoerceUtils.asBoolean(jr.nextDouble(), defaultValue);
            }

            throw newInvalidValueFound(jr);
        } catch (IOException | IllegalAccessException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static Byte asByte(JsonReader jr) {
        return JsonCoerceUtils.asByte(jr, null);
    }

    public static Byte asByte(JsonReader jr, Byte defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return (byte) jr.nextInt();
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asByte(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asByte(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static Short asShort(JsonReader jr) {
        return JsonCoerceUtils.asShort(jr, null);
    }

    public static Short asShort(JsonReader jr, Short defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return (short) jr.nextInt();
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asShort(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asShort(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static Integer asInteger(JsonReader jr) {
        return JsonCoerceUtils.asInteger(jr, null);
    }

    public static Integer asInteger(JsonReader jr, Integer defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return jr.nextInt();
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asInteger(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asInteger(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static Character asCharacter(JsonReader jr) {
        return JsonCoerceUtils.asCharacter(jr, null);
    }

    public static Character asCharacter(JsonReader jr, Character defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asCharacter(jr.nextString());
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return CoerceUtils.asCharacter(jr.nextInt());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asCharacter(jr.nextBoolean());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static Long asLong(JsonReader jr) {
        return JsonCoerceUtils.asLong(jr, null);
    }

    public static Long asLong(JsonReader jr, Long defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return jr.nextLong();
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asLong(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asLong(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static BigInteger asBigInteger(JsonReader jr) {
        return JsonCoerceUtils.asBigInteger(jr, null);
    }

    public static BigInteger asBigInteger(JsonReader jr, BigInteger defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return CoerceUtils.asBigInteger(jr.nextLong());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asBigInteger(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asBigInteger(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static Float asFloat(JsonReader jr) {
        return JsonCoerceUtils.asFloat(jr, null);
    }

    public static Float asFloat(JsonReader jr, Float defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return (float) jr.nextDouble();
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asFloat(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asFloat(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static Double asDouble(JsonReader jr) {
        return JsonCoerceUtils.asDouble(jr, null);
    }

    public static Double asDouble(JsonReader jr, Double defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return jr.nextDouble();
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asDouble(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asDouble(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static BigDecimal asBigDecimal(JsonReader jr) {
        return JsonCoerceUtils.asBigDecimal(jr, null);
    }

    public static BigDecimal asBigDecimal(JsonReader jr, BigDecimal defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return CoerceUtils.asBigDecimal(jr.nextDouble());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asBigDecimal(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asBigDecimal(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static java.util.Date asDate(JsonReader jr) {
        return JsonCoerceUtils.asDate(jr, null);
    }

    public static java.util.Date asDate(JsonReader jr, java.util.Date defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return CoerceUtils.asDate(jr.nextDouble());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asDate(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asDate(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static java.sql.Timestamp asTimestamp(JsonReader jr) {
        return JsonCoerceUtils.asTimestamp(jr, null);
    }

    public static java.sql.Timestamp asTimestamp(JsonReader jr, java.sql.Timestamp defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return CoerceUtils.asTimestamp(jr.nextDouble());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asTimestamp(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asTimestamp(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static java.sql.Date asSqlDate(JsonReader jr) {
        return JsonCoerceUtils.asSqlDate(jr, null);
    }

    public static java.sql.Date asSqlDate(JsonReader jr, java.sql.Date defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return CoerceUtils.asSqlDate(jr.nextDouble());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asSqlDate(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asSqlDate(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static LocalDate asLocalDate(JsonReader jr) {
        return JsonCoerceUtils.asLocalDate(jr, null);
    }

    public static LocalDate asLocalDate(JsonReader jr, LocalDate defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return CoerceUtils.asLocalDate(jr.nextDouble());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asLocalDate(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asLocalDate(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static LocalDateTime asLocalDateTime(JsonReader jr) {
        return JsonCoerceUtils.asLocalDateTime(jr, null);
    }

    public static LocalDateTime asLocalDateTime(JsonReader jr, LocalDateTime defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return CoerceUtils.asLocalDateTime(jr.nextDouble());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asLocalDateTime(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asLocalDateTime(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static OffsetDateTime asOffsetDateTime(JsonReader jr) {
        return JsonCoerceUtils.asOffsetDateTime(jr, null);
    }

    public static OffsetDateTime asOffsetDateTime(JsonReader jr, OffsetDateTime defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return CoerceUtils.asOffsetDateTime(jr.nextDouble());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asOffsetDateTime(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asOffsetDateTime(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static ZonedDateTime asZonedDateTime(JsonReader jr) {
        return JsonCoerceUtils.asZonedDateTime(jr, null);
    }

    public static ZonedDateTime asZonedDateTime(JsonReader jr, ZonedDateTime defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.NUMBER) {
                return CoerceUtils.asZonedDateTime(jr.nextDouble());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asZonedDateTime(jr.nextBoolean());
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asZonedDateTime(jr.nextString());
            }

            throw newInvalidValueFound(jr);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static byte[] asByteArray(JsonReader jr) {
        return JsonCoerceUtils.asByteArray(jr, null);
    }

    public static byte[] asByteArray(JsonReader jr, byte[] defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asByteArray(jr.nextString());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asByteArray(jr.nextBoolean(), defaultValue);
            }

            if (jr.peek() == JsonToken.NUMBER) {
                if (Holder.jrIsPeekLong(jr)) {
                    return CoerceUtils.asByteArray(jr.nextLong(), defaultValue);
                }
                return CoerceUtils.asByteArray(jr.nextDouble(), defaultValue);
            }

            throw newInvalidValueFound(jr);
        } catch (IOException | IllegalAccessException e) {
            return ExceptionUtils.rethrow(e);
        }
    }
    
    public static byte[] asByteArrayFromHex(JsonReader jr) {
        return JsonCoerceUtils.asByteArrayFromHex(jr, null);
    }

    public static byte[] asByteArrayFromHex(JsonReader jr, byte[] defaultValue) {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull();
                return defaultValue;
            }

            if (jr.peek() == JsonToken.STRING) {
                return CoerceUtils.asByteArrayFromHex(jr.nextString());
            }

            if (jr.peek() == JsonToken.BOOLEAN) {
                return CoerceUtils.asByteArrayFromHex(jr.nextBoolean(), defaultValue);
            }

            if (jr.peek() == JsonToken.NUMBER) {
                if (Holder.jrIsPeekLong(jr)) {
                    return CoerceUtils.asByteArrayFromHex(jr.nextLong(), defaultValue);
                }
                return CoerceUtils.asByteArrayFromHex(jr.nextDouble(), defaultValue);
            }

            throw newInvalidValueFound(jr);
        } catch (IOException | IllegalAccessException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    // :: Internals

    private static IOException newInvalidValueFound(JsonReader jr) {
        try {
            return new IOException("No valid value found. JsonReader.peek() = " + jr.peek());
        } catch (IOException e) {
            return new IOException("No valid value found. JsonReader.peek() not available", e);
        }

    }

    private static class Holder {

        static final int PEEKED_LONG = 15;

        static Field peekedField = findPeekedField();

        private static Field findPeekedField() {
            try {
                return JsonReader.class.getDeclaredField("peeked");
            } catch (NoSuchFieldException | SecurityException e) {
                return ExceptionUtils.rethrow(e);
            }
        }

        @SuppressWarnings({
                // Infelizmente é necessario quebrar o acesso para podermos saber o tipo de número
                // parseado. Importante para perforamnce dessa classe
                "java:S3011"
        })
        static int jrPeekValue(JsonReader jr) throws IllegalAccessException {
            peekedField.setAccessible(true);
            return (Integer) peekedField.get(jr);
        }

        static boolean jrIsPeekLong(JsonReader jr) throws IllegalAccessException {
            return jrPeekValue(jr) == PEEKED_LONG;
        }

    }
}
