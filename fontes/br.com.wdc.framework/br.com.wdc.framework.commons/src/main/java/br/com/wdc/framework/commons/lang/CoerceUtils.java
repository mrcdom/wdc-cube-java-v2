package br.com.wdc.framework.commons.lang;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;
import java.util.HexFormat;

import br.com.wdc.framework.commons.convert.DateUtil;

@SuppressWarnings({
        "java:S6201", "java:S3776"
})
public class CoerceUtils {

    private CoerceUtils() {
        super();
    }

    // String: Start

    public static String asString(Object v) {
        return CoerceUtils.asString(v, null);
    }

    public static String asString(Object v, String defaultValue) {
        if (v == null) {
            return defaultValue;
        }
        return v instanceof String str ? str : v.toString();
    }

    public static String asTrimmedString(Object v) {
        return CoerceUtils.asTrimmedString(v, null);
    }

    public static String asTrimmedString(Object v, String defaultValue) {
        if (v == null) {
            return defaultValue != null ? defaultValue.trim() : null;
        }
        return v.toString().trim();
    }

    public static String asLowerCaseString(Object v) {
        return CoerceUtils.asLowerCaseString(v, null);
    }

    public static String asLowerCaseString(Object v, String defaultValue) {
        if (v == null) {
            return defaultValue;
        }
        return v.toString().toLowerCase();
    }

    public static String asUpperCaseString(Object v) {
        return CoerceUtils.asUpperCaseString(v, null);
    }

    public static String asUpperCaseString(Object v, String defaultValue) {
        if (v == null) {
            return defaultValue != null ? defaultValue.toUpperCase() : null;
        }
        return v.toString().toUpperCase();
    }

    // String: End

    public static Boolean asBoolean(Object v) {
        return CoerceUtils.asBoolean(v, null);
    }

    public static Boolean asBoolean(Object v, Boolean defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof Boolean bool) {
            return bool;
        }

        if (v instanceof Number number) {
            return Boolean.valueOf(number.intValue() != 0);
        }

        if (v instanceof Character character) {
            char ch = character;

            if (ch == 's' || ch == 'S' || ch == 't' || ch == 'T') {
                return Boolean.TRUE;
            }

            if (ch == 'n' || ch == 'N' || ch == 'f' || ch == 'F') {
                return Boolean.FALSE;
            }

            return defaultValue;
        }

        if (v instanceof String s) {
            if ("".equals(s)) {
                return defaultValue;
            }

            if (s.length() == 1) {
                char ch = s.charAt(0);

                if (ch == 's' || ch == 'S' || ch == 't' || ch == 'T') {
                    return Boolean.TRUE;
                }

                if (ch == 'n' || ch == 'N' || ch == 'f' || ch == 'F') {
                    return Boolean.FALSE;
                }

                return defaultValue;
            }

            if ("true".equalsIgnoreCase(s) || "sim".equalsIgnoreCase(s)) {
                return Boolean.TRUE;
            }

            if ("false".equalsIgnoreCase(s) || "nao".equalsIgnoreCase(s)) {
                return Boolean.FALSE;
            }
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    public static Byte asByte(Object v) {
        return CoerceUtils.asByte(v, null);
    }

    public static Byte asByte(Object v, Byte defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof Byte byteValue) {
            return byteValue;
        }

        if (v instanceof Number number) {
            return number.byteValue();
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            return Byte.parseByte(str);
        }

        if (v instanceof Boolean bool) {
            return bool.booleanValue() ? (byte) 1 : (byte) 0;
        }

        throw new NumberFormatException(getErrorMessage(v));
    }

    public static Short asShort(Object v) {
        return CoerceUtils.asShort(v, null);
    }

    public static Short asShort(Object v, Short defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof Short shortValue) {
            return shortValue;
        }

        if (v instanceof Number number) {
            return number.shortValue();
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            return Short.parseShort(str);
        }

        if (v instanceof Boolean bool) {
            return bool.booleanValue() ? (short) 1 : (short) 0;
        }

        throw new NumberFormatException(getErrorMessage(v));
    }

    public static Integer asInteger(Object v) {
        return CoerceUtils.asInteger(v, null);
    }

    public static Integer asInteger(Object v, Integer defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof Integer integerValue) {
            return integerValue;
        }

        if (v instanceof Number number) {
            return number.intValue();
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            return Integer.parseInt(str);
        }

        if (v instanceof Boolean bool) {
            return bool.booleanValue() ? 1 : 0;
        }

        throw new NumberFormatException(getErrorMessage(v));
    }

    public static Character asCharacter(Object v) {
        return CoerceUtils.asCharacter(v, null);
    }

    public static Character asCharacter(Object v, Character defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof Character characterValue) {
            return characterValue;
        }

        if (v instanceof Number number) {
            return Character.valueOf((char) number.intValue());
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            return Character.valueOf(str.charAt(0));
        }

        if (v instanceof Boolean bool) {
            return bool.booleanValue() ? 'T' : 'F';
        }

        throw new NumberFormatException(getErrorMessage(v));
    }

    public static Long asLong(Object v) {
        return CoerceUtils.asLong(v, null);
    }

    public static Long asLong(Object v, Long defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof Long longValue) {
            return longValue;
        }

        if (v instanceof Number number) {
            return number.longValue();
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            return Long.parseLong(str);
        }

        if (v instanceof Boolean bool) {
            return bool.booleanValue() ? 1L : 0L;
        }

        throw new NumberFormatException(getErrorMessage(v));
    }

    public static BigInteger asBigInteger(Object v) {
        return CoerceUtils.asBigInteger(v, null);
    }

    public static BigInteger asBigInteger(Object v, BigInteger defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof BigInteger bigIntegerValue) {
            return bigIntegerValue;
        }

        if (v instanceof BigDecimal bigDecimalValue) {
            return bigDecimalValue.toBigInteger();
        }

        if (v instanceof Number number) {
            return BigInteger.valueOf(number.longValue());
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            return new BigInteger(str);
        }

        if (v instanceof Boolean bool) {
            return bool.booleanValue() ? BigInteger.ONE : BigInteger.ZERO;
        }

        throw new NumberFormatException(getErrorMessage(v));
    }

    public static Float asFloat(Object v) {
        return CoerceUtils.asFloat(v, null);
    }

    public static Float asFloat(Object v, Float defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof Float floatValue) {
            return floatValue;
        }

        if (v instanceof Number number) {
            return number.floatValue();
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            return Float.parseFloat(str);
        }

        if (v instanceof Boolean bool) {
            return bool.booleanValue() ? 1.0f : 0.0f;
        }

        throw new NumberFormatException(getErrorMessage(v));
    }

    public static Double asDouble(Object v) {
        return CoerceUtils.asDouble(v, null);
    }

    public static Double asDouble(Object v, Double defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof Double doubleValue) {
            return doubleValue;
        }

        if (v instanceof Number number) {
            return number.doubleValue();
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            return Double.parseDouble(str);
        }

        if (v instanceof Boolean bool) {
            return bool.booleanValue() ? 1.0 : 0.0;
        }

        throw new NumberFormatException(getErrorMessage(v));
    }

    public static BigDecimal asBigDecimal(Object v) {
        return CoerceUtils.asBigDecimal(v, null);
    }

    public static BigDecimal asBigDecimal(Object v, BigDecimal defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof BigDecimal bigDecimalValue) {
            return bigDecimalValue;
        }

        if (v instanceof BigInteger bigIntegerValue) {
            return new BigDecimal(bigIntegerValue);
        }

        if (v instanceof Number number) {
            if (number instanceof Long longNumber) {
                return BigDecimal.valueOf(longNumber);
            }

            if (number instanceof Short shortNumber) {
                return BigDecimal.valueOf(shortNumber);
            }

            if (number instanceof Byte byteNumber) {
                return BigDecimal.valueOf(byteNumber);
            }

            return BigDecimal.valueOf(number.doubleValue());
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            return new BigDecimal(str);
        }

        if (v instanceof Boolean bool) {
            return bool.booleanValue() ? BigDecimal.ONE : BigDecimal.ZERO;
        }

        throw new NumberFormatException(getErrorMessage(v));
    }

    public static Number asNumber(Object v) {
        return CoerceUtils.asNumber(v, null);
    }

    public static Number asNumber(Object v, Number defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof Number number) {
            return number;
        }

        if (defaultValue == null) {
            return CoerceUtils.asBigDecimal(v);
        }

        if (defaultValue instanceof Integer integerDefaultValue) {
            return CoerceUtils.asInteger(v, integerDefaultValue);
        }

        if (defaultValue instanceof Long longDefaultValue) {
            return CoerceUtils.asLong(v, longDefaultValue);
        }

        if (defaultValue instanceof Short shortDefaultValue) {
            return CoerceUtils.asShort(v, shortDefaultValue);
        }

        if (defaultValue instanceof Byte byteDefaultValue) {
            return CoerceUtils.asByte(v, byteDefaultValue);
        }

        if (defaultValue instanceof Double doubleDefaultValue) {
            return CoerceUtils.asDouble(v, doubleDefaultValue);
        }

        if (defaultValue instanceof Float floatDefaultValue) {
            return CoerceUtils.asFloat(v, floatDefaultValue);
        }

        if (defaultValue instanceof BigInteger bigIntegerDefaultValue) {
            return CoerceUtils.asBigInteger(v, bigIntegerDefaultValue);
        }

        if (defaultValue instanceof BigDecimal bigDecimalDefaultValue) {
            return CoerceUtils.asBigDecimal(v, bigDecimalDefaultValue);
        }

        throw new NumberFormatException(getErrorMessage(v));
    }

    public static java.util.Date asDate(Object v) {
        return CoerceUtils.asDate(v, null);
    }

    public static java.util.Date asDate(Object v, java.util.Date defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof java.util.Date dateValue) {
            if (dateValue instanceof java.sql.Date sqlDateValue) {
                return new java.util.Date(sqlDateValue.getTime());
            }

            if (dateValue instanceof java.sql.Timestamp sqlTimestampValue) {
                return new java.util.Date(sqlTimestampValue.getTime());
            }

            return dateValue;
        }

        if (v instanceof LocalDate dt) {
            return java.util.Date.from(LocalDateTime.of(dt, LocalTime.MIN).toInstant(DateUtil.getSysZoneOffset()));
        }

        if (v instanceof LocalDateTime dt) {
            return java.util.Date.from(dt.toInstant(DateUtil.getSysZoneOffset()));
        }

        if (v instanceof OffsetDateTime dt) {
            return java.util.Date.from(dt.toInstant());
        }

        if (v instanceof ZonedDateTime dt) {
            return java.util.Date.from(dt.toInstant());
        }

        if (v instanceof TemporalAccessor temporalAccessor) {
            LocalDateTime dt = LocalDateTime.from(temporalAccessor);
            return java.util.Date.from(dt.toInstant(DateUtil.getSysZoneOffset()));
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            var parsed = parseFlexibleTemporal(str);
            return java.util.Date.from(toInstant(parsed));
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    public static java.sql.Timestamp asTimestamp(Object v) {
        return CoerceUtils.asTimestamp(v, null);
    }

    public static java.sql.Timestamp asTimestamp(Object v, java.sql.Timestamp defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof java.sql.Timestamp timestampValue) {
            return timestampValue;
        }

        if (v instanceof java.util.Date dateValue) {
            return new java.sql.Timestamp(dateValue.getTime());
        }

        if (v instanceof LocalDate dt) {
            return java.sql.Timestamp.valueOf(LocalDateTime.of(dt, LocalTime.MIN));
        }

        if (v instanceof LocalDateTime dt) {
            return java.sql.Timestamp.valueOf(dt);
        }

        if (v instanceof OffsetDateTime dt) {
            return java.sql.Timestamp.valueOf(dt.toLocalDateTime());
        }

        if (v instanceof ZonedDateTime dt) {
            return java.sql.Timestamp.valueOf(dt.toLocalDateTime());
        }

        if (v instanceof TemporalAccessor temporalAccessor) {
            LocalDateTime dt = LocalDateTime.from(temporalAccessor);
            return java.sql.Timestamp.valueOf(dt);
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            var parsed = parseFlexibleTemporal(str);
            return java.sql.Timestamp.valueOf(toLocalDateTime(parsed));
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    public static java.sql.Date asSqlDate(Object v) {
        return CoerceUtils.asSqlDate(v, null);
    }

    public static java.sql.Date asSqlDate(Object v, java.sql.Date defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof java.sql.Date sqlDateValue) {
            return sqlDateValue;
        }

        if (v instanceof java.util.Date dateValue) {
            return java.sql.Date.valueOf(localDateOf(dateValue));
        }

        if (v instanceof LocalDate localDateValue) {
            return java.sql.Date.valueOf(localDateValue);
        }

        if (v instanceof LocalDateTime localDateTimeValue) {
            return java.sql.Date.valueOf(localDateTimeValue.toLocalDate());
        }

        if (v instanceof OffsetDateTime offsetDateTimeValue) {
            return java.sql.Date.valueOf(offsetDateTimeValue.toLocalDate());
        }

        if (v instanceof ZonedDateTime zonedDateTimeValue) {
            return java.sql.Date.valueOf(zonedDateTimeValue.toLocalDate());
        }

        if (v instanceof TemporalAccessor temporalAccessor) {
            return java.sql.Date.valueOf(LocalDate.from(temporalAccessor));
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            var parsed = parseFlexibleTemporal(str);
            return java.sql.Date.valueOf(toLocalDate(parsed));
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    public static LocalDate asLocalDate(Object v) {
        return asLocalDate(v, null);
    }

    public static LocalDate asLocalDate(Object v, LocalDate defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof LocalDate localDateValue) {
            return localDateValue;
        }

        if (v instanceof java.util.Date dateValue) {
            if (dateValue instanceof java.sql.Timestamp sqlTimestampValue) {
                return sqlTimestampValue.toLocalDateTime().toLocalDate();
            }

            if (dateValue instanceof java.sql.Date sqlDateValue) {
                return sqlDateValue.toLocalDate();
            }

            return localDateOf(dateValue);
        }

        if (v instanceof LocalDateTime localDateTimeValue) {
            return localDateTimeValue.toLocalDate();
        }

        if (v instanceof OffsetDateTime offsetDateTimeValue) {
            return offsetDateTimeValue.toLocalDate();
        }

        if (v instanceof ZonedDateTime zonedDateTimeValue) {
            return zonedDateTimeValue.toLocalDate();
        }

        if (v instanceof TemporalAccessor temporalAccessor) {
            return LocalDate.from(temporalAccessor);
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            var parsed = parseFlexibleTemporal(str);
            return toLocalDate(parsed);
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    public static LocalDateTime asLocalDateTime(Object v) {
        return asLocalDateTime(v, null);
    }

    public static LocalDateTime asLocalDateTime(Object v, LocalDateTime defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof LocalDateTime localDateTimeValue) {
            return localDateTimeValue;
        }

        if (v instanceof java.util.Date dateValue) {
            if (dateValue instanceof java.sql.Timestamp sqlTimestampValue) {
                return sqlTimestampValue.toLocalDateTime();
            }

            if (dateValue instanceof java.sql.Date sqlDateValue) {
                return LocalDateTime.of(sqlDateValue.toLocalDate(), LocalTime.MIN);
            }

            return localDateTimeOf(dateValue);
        }

        if (v instanceof LocalDate localDateValue) {
            return localDateValue.atStartOfDay();
        }

        if (v instanceof OffsetDateTime offsetDateTimeValue) {
            return offsetDateTimeValue.toLocalDateTime();
        }

        if (v instanceof ZonedDateTime zonedDateTimeValue) {
            return zonedDateTimeValue.toLocalDateTime();
        }

        if (v instanceof TemporalAccessor temporalAccessor) {
            return LocalDateTime.from(temporalAccessor);
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            var parsed = parseFlexibleTemporal(str);
            return toLocalDateTime(parsed);
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    public static LocalTime asLocalTime(Object v) {
        return asLocalTime(v, null);
    }

    public static LocalTime asLocalTime(Object v, LocalTime defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof LocalTime localTimeValue) {
            return localTimeValue;
        }

        if (v instanceof java.util.Date dateValue) {
            if (dateValue instanceof java.sql.Timestamp sqlTimestampValue) {
                return sqlTimestampValue.toLocalDateTime().toLocalTime();
            }

            return localTimeOf(dateValue);
        }

        if (v instanceof OffsetDateTime offsetDateTimeValue) {
            return offsetDateTimeValue.toLocalTime();
        }

        if (v instanceof ZonedDateTime zonedDateTimeValue) {
            return zonedDateTimeValue.toLocalTime();
        }

        if (v instanceof TemporalAccessor temporalAccessor) {
            return LocalTime.from(temporalAccessor);
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            return LocalTime.parse(str);
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    public static OffsetDateTime asOffsetDateTime(Object v) {
        return asOffsetDateTime(v, null);
    }

    public static OffsetDateTime asOffsetDateTime(Object v, OffsetDateTime defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof OffsetDateTime offsetDateTimeValue) {
            return offsetDateTimeValue;
        }

        if (v instanceof java.util.Date dateValue) {
            if (dateValue instanceof java.sql.Timestamp sqlTimestampValue) {
                return sqlTimestampValue.toLocalDateTime().atOffset(DateUtil.getSysZoneOffset());
            }

            if (dateValue instanceof java.sql.Date sqlDateValue) {
                return sqlDateValue.toLocalDate().atTime(LocalTime.MIN).atOffset(DateUtil.getSysZoneOffset());
            }

            return localDateTimeOf(dateValue).atOffset(DateUtil.getSysZoneOffset());
        }

        if (v instanceof LocalDateTime dt) {
            return dt.atOffset(DateUtil.getSysZoneOffset());
        }

        if (v instanceof LocalDate localDateValue) {
            return localDateValue.atTime(OffsetTime.of(LocalTime.MIN, DateUtil.getSysZoneOffset()));
        }

        if (v instanceof ZonedDateTime dt) {
            return dt.toOffsetDateTime();
        }

        if (v instanceof TemporalAccessor temporalAccessor) {
            return OffsetDateTime.from(temporalAccessor);
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            var parsed = parseFlexibleTemporal(str);
            return toOffsetDateTime(parsed);
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    public static ZonedDateTime asZonedDateTime(Object v) {
        return asZonedDateTime(v, null);
    }

    public static ZonedDateTime asZonedDateTime(Object v, ZonedDateTime defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof ZonedDateTime zonedDateTimeValue) {
            return zonedDateTimeValue;
        }

        if (v instanceof java.util.Date dateValue) {
            if (dateValue instanceof java.sql.Timestamp sqlTimestampValue) {
                return sqlTimestampValue.toLocalDateTime().atZone(DateUtil.getSysZoneOffset());
            }

            if (dateValue instanceof java.sql.Date sqlDateValue) {
                return ZonedDateTime.of(sqlDateValue.toLocalDate(), LocalTime.of(0, 0),
                        DateUtil.getSysZoneOffset());
            }

            return localDateTimeOf(dateValue).atZone(DateUtil.getSysZoneOffset());
        }

        if (v instanceof LocalDate localDateValue) {
            return localDateValue.atStartOfDay(DateUtil.getSysZoneOffset());
        }

        if (v instanceof LocalDateTime dt) {
            return dt.atZone(DateUtil.getSysZoneOffset());
        }

        if (v instanceof OffsetDateTime dt) {
            return dt.toZonedDateTime();
        }

        if (v instanceof TemporalAccessor temporalAccessor) {
            return ZonedDateTime.from(temporalAccessor);
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }
            var parsed = parseFlexibleTemporal(str);
            return toZonedDateTime(parsed);
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    public static byte[] asByteArray(Object v) {
        return asByteArray(v, null);
    }

    public static byte[] asByteArray(Object v, byte[] defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof byte[] bytes) {
            return bytes;
        }

        if (v instanceof Byte byteValue) {
            return new byte[] { byteValue };
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }

            return Base64.getDecoder().decode(str);
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    public static byte[] asByteArrayFromHex(Object v) {
        return asByteArrayFromHex(v, null);
    }

    public static byte[] asByteArrayFromHex(Object v, byte[] defaultValue) {
        if (v == null) {
            return defaultValue;
        }

        if (v instanceof byte[] bytes) {
            return bytes;
        }

        if (v instanceof Byte byteValue) {
            return new byte[] { byteValue };
        }

        if (v instanceof String str) {
            if ("".equals(str)) {
                return defaultValue;
            }

            return HexFormat.of().parseHex(str);
        }

        throw new IllegalArgumentException(getErrorMessage(v));
    }

    private static String getErrorMessage(Object v) {
        return "The value cannot be parsed to: '" + v.getClass() + "'";
    }

    private static LocalDate localDateOf(java.util.Date dt) {
        return new java.sql.Date(dt.getTime()).toLocalDate();
    }

    private static LocalDateTime localDateTimeOf(java.util.Date dt) {
        return new java.sql.Timestamp(dt.getTime()).toLocalDateTime();
    }

    private static LocalTime localTimeOf(java.util.Date dt) {
        return new java.sql.Timestamp(dt.getTime()).toLocalDateTime().toLocalTime();
    }

    /**
     * Parses a date/time string flexibly by inspecting the string structure
     * to route directly to the most appropriate parser.
     *
     * @return a TemporalAccessor (Instant, OffsetDateTime, ZonedDateTime, LocalDateTime, or LocalDate)
     */
    private static TemporalAccessor parseFlexibleTemporal(String str) {
        var len = str.length();

        // "yyyy-MM-dd" — 10 chars, no time component
        if (len == 10) {
            return LocalDate.parse(str);
        }

        var hasT = str.indexOf('T') >= 0;

        // "yyyy-MM-dd HH:mm:ss" — space separator, no T
        if (!hasT && len >= 19 && str.charAt(10) == ' ') {
            return LocalDateTime.parse(str, DATETIME_SPACE_FORMAT);
        }

        // From here on, all formats have 'T'
        if (hasT) {
            // "[" indicates ZonedDateTime (e.g. "2010-01-01T00:00:00+03:00[Europe/Moscow]")
            if (str.indexOf('[') >= 0) {
                return ZonedDateTime.parse(str);
            }

            // "Z" at end indicates Instant (e.g. "2010-01-01T00:00:00Z")
            if (str.charAt(len - 1) == 'Z') {
                return Instant.parse(str);
            }

            // "+" or "-" after the time part indicates offset (e.g. "2010-01-01T00:00:00+03:00")
            // Look for +/- after position 10 (date part), skipping the possible "-" in timezone offset
            var afterT = str.indexOf('T') + 1;
            var plusIdx = str.indexOf('+', afterT);
            var minusIdx = str.indexOf('-', afterT);

            if (plusIdx >= 0 || minusIdx >= 0) {
                // Has offset → OffsetDateTime
                return OffsetDateTime.parse(str);
            }

            // No offset, no Z → LocalDateTime (e.g. "2010-01-01T10:30:00")
            return LocalDateTime.parse(str);
        }

        // Fallback: try all parsers
        return parseFlexibleTemporalFallback(str);
    }

    private static final DateTimeFormatter DATETIME_SPACE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static TemporalAccessor parseFlexibleTemporalFallback(String str) {
        DateTimeParseException last = null;

        try {
            return Instant.parse(str);
        } catch (DateTimeParseException e) {
            last = e;
        }

        try {
            return OffsetDateTime.parse(str);
        } catch (DateTimeParseException _) {
            // continue
        }

        try {
            return LocalDateTime.parse(str);
        } catch (DateTimeParseException _) {
            // continue
        }

        try {
            return LocalDate.parse(str);
        } catch (DateTimeParseException _) {
            // continue
        }

        throw new IllegalArgumentException("Cannot parse date/time string: '" + str + "'", last);
    }

    private static LocalDate toLocalDate(TemporalAccessor parsed) {
        if (parsed instanceof Instant instant) {
            return instant.atOffset(DateUtil.getSysZoneOffset()).toLocalDate();
        }
        if (parsed instanceof OffsetDateTime odt) {
            return odt.toLocalDate();
        }
        if (parsed instanceof ZonedDateTime zdt) {
            return zdt.toLocalDate();
        }
        if (parsed instanceof LocalDateTime ldt) {
            return ldt.toLocalDate();
        }
        if (parsed instanceof LocalDate ld) {
            return ld;
        }
        return LocalDate.from(parsed);
    }

    private static LocalDateTime toLocalDateTime(TemporalAccessor parsed) {
        if (parsed instanceof Instant instant) {
            return instant.atOffset(DateUtil.getSysZoneOffset()).toLocalDateTime();
        }
        if (parsed instanceof OffsetDateTime odt) {
            return odt.toLocalDateTime();
        }
        if (parsed instanceof ZonedDateTime zdt) {
            return zdt.toLocalDateTime();
        }
        if (parsed instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (parsed instanceof LocalDate ld) {
            return ld.atStartOfDay();
        }
        return LocalDateTime.from(parsed);
    }

    private static OffsetDateTime toOffsetDateTime(TemporalAccessor parsed) {
        if (parsed instanceof Instant instant) {
            return instant.atOffset(DateUtil.getSysZoneOffset());
        }
        if (parsed instanceof OffsetDateTime odt) {
            return odt;
        }
        if (parsed instanceof ZonedDateTime zdt) {
            return zdt.toOffsetDateTime();
        }
        if (parsed instanceof LocalDateTime ldt) {
            return ldt.atOffset(DateUtil.getSysZoneOffset());
        }
        if (parsed instanceof LocalDate ld) {
            return ld.atTime(OffsetTime.of(LocalTime.MIN, DateUtil.getSysZoneOffset()));
        }
        return OffsetDateTime.from(parsed);
    }

    private static ZonedDateTime toZonedDateTime(TemporalAccessor parsed) {
        if (parsed instanceof Instant instant) {
            return instant.atZone(DateUtil.getSysZoneOffset());
        }
        if (parsed instanceof OffsetDateTime odt) {
            return odt.toZonedDateTime();
        }
        if (parsed instanceof ZonedDateTime zdt) {
            return zdt;
        }
        if (parsed instanceof LocalDateTime ldt) {
            return ldt.atZone(DateUtil.getSysZoneOffset());
        }
        if (parsed instanceof LocalDate ld) {
            return ld.atStartOfDay(DateUtil.getSysZoneOffset());
        }
        return ZonedDateTime.from(parsed);
    }

    private static Instant toInstant(TemporalAccessor parsed) {
        if (parsed instanceof Instant instant) {
            return instant;
        }
        if (parsed instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (parsed instanceof ZonedDateTime zdt) {
            return zdt.toInstant();
        }
        if (parsed instanceof LocalDateTime ldt) {
            return ldt.toInstant(DateUtil.getSysZoneOffset());
        }
        if (parsed instanceof LocalDate ld) {
            return ld.atStartOfDay().toInstant(DateUtil.getSysZoneOffset());
        }
        return Instant.from(parsed);
    }

}