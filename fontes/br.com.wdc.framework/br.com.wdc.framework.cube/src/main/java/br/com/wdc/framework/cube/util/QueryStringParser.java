package br.com.wdc.framework.cube.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.cube.CubeIntent;

/**
 * Helps to set parameters from a query string
 */
public final class QueryStringParser {

    private static final Logger logger = Logger.getLogger(QueryStringParser.class.getName());

    private static final Map<Class<?>, Function<String, Object>> NUMERIC_PARSERS = Map.of(
            BigDecimal.class, BigDecimal::new,
            Double.class, Double::valueOf,
            Float.class, Float::valueOf,
            BigInteger.class, BigInteger::new,
            Long.class, Long::valueOf,
            Integer.class, Integer::valueOf,
            Short.class, Short::valueOf,
            Byte.class, Byte::valueOf);

    private QueryStringParser() {

    }

    /**
     * Append request parameters from the specified String to the specified Map. It is presumed that the specified Map
     * is not accessed from any other thread, so no synchronization is performed.
     * <p>
     * <strong>IMPLEMENTATION NOTE</strong>: URL decoding is performed individually on the parsed name and value
     * elements, rather than on the entire query string ahead of time, to properly deal with the case where the name or
     * value includes an encoded "=" or "&" character that would otherwise be interpreted as a delimiter.
     *
     * @param url  Map that accumulates the resulting parameters
     * @param data Input string containing request parameters
     *
     * @exception IllegalArgumentException if the data is bad-formed
     */
    public static void parse(final CubeIntent url, final String data, final Charset encoding) {
        if (StringUtils.isNotBlank(data)) {

            // use the specified encoding to extract bytes out of the
            // given string so that the encoding is not lost. If an
            // encoding is not specified, let it use platform default
            byte[] bytes = null;
            try {
                if (encoding == null) {
                    bytes = data.getBytes();
                } else {
                    bytes = data.getBytes(encoding);
                }
            } catch (Exception exn) {
                logger.log(Level.WARNING, "Parsing URL", exn);
            }

            parseParameters(url, bytes, encoding);
        }

    }

    /**
     * Convert a byte character value to hex-decimal digit value.
     *
     * @param b the character value byte
     */
    private static byte convertHexDigit(final byte b) {
        if (b >= '0' && b <= '9') {
            return (byte) (b - '0');
        }
        if (b >= 'a' && b <= 'f') {
            return (byte) (b - 'a' + 10);
        }
        if (b >= 'A' && b <= 'F') {
            return (byte) (b - 'A' + 10);
        }
        return 0;
    }

    private static Object castValueTo(final String value, final Class<?> clazz) {
        if (value == null) {
            return null;
        }

        if (clazz == String.class) {
            return value;
        }

        if (clazz == Character.class) {
            return value.isEmpty() ? null : value.charAt(0);
        }

        var parser = NUMERIC_PARSERS.get(clazz);
        if (parser != null) {
            try {
                return parser.apply(value);
            } catch (NumberFormatException _) {
                return null;
            }
        }

        return null;
    }

    /**
     * Put name and value pair in map. When name already exist, add value to array of values.
     *
     * @param url   The map to populate
     * @param name  The parameter name
     * @param value The parameter value
     */
    private static void putMapEntry(final CubeIntent url, final String name, final String value) {
        var oldValue = url.getParameterValue(name);

        if (oldValue == null) {
            url.setParameter(name, value);
        } else if (oldValue.getClass().isArray()) {
            var arrayType = oldValue.getClass().getComponentType();
            int arrayLength = Array.getLength(oldValue);

            if (arrayLength == 0) {
                var array = ArrayUtils.newInstance(arrayType, 1);
                Array.set(array, 0, castValueTo(value, arrayType));
            } else {
                var array = ArrayUtils.newInstance(arrayType, arrayLength + 1);
                System.arraycopy(oldValue, 0, array, 0, arrayLength);
                Array.set(array, arrayLength, castValueTo(value, arrayType));
            }
        } else {
            var arrayType = oldValue.getClass();
            var array = ArrayUtils.newInstance(arrayType, 2);
            Array.set(array, 0, oldValue);
            Array.set(array, 1, castValueTo(value, arrayType));
        }
    }

    /**
     * Append request parameters from the specified String to the specified Map. It is presumed that the specified Map
     * is not accessed from any other thread, so no synchronization is performed.
     * <p>
     * <strong>IMPLEMENTATION NOTE</strong>: URL decoding is performed individually on the parsed name and value
     * elements, rather than on the entire query string ahead of time, to properly deal with the case where the name or
     * value includes an encoded "=" or "&" character that would otherwise be interpreted as a delimiter.
     *
     * NOTE: byte array data is modified by this method. Caller beware.
     *
     * @param url      Map that accumulates the resulting parameters
     * @param data     Input string containing request parameters
     * @param encoding Encoding to use for converting hex
     *
     * @exception UnsupportedEncodingException if the data is malformed
     */
    public static void parseParameters(final CubeIntent url, final byte[] data, final Charset encoding) {
        if (data == null || data.length == 0) {
            return;
        }

        var ix = 0;
        var ox = 0;
        String key = null;

        while (ix < data.length) {
            byte c = data[ix++];
            switch ((char) c) {
            case '&' -> {
                if (key != null) {
                    putMapEntry(url, key, new String(data, 0, ox, encoding));
                    key = null;
                }
                ox = 0;
            }
            case '=' -> {
                if (key == null) {
                    key = new String(data, 0, ox, encoding);
                    ox = 0;
                } else {
                    data[ox++] = c;
                }
            }
            case '+' -> data[ox++] = (byte) ' ';
            case '%' -> data[ox++] = (byte) (((convertHexDigit(data[ix++]) & 0xFF) << 4) | (convertHexDigit(data[ix++]) & 0xFF));
            default -> data[ox++] = c;
            }
        }

        // The last value does not end in '&'. So save it now.
        if (key != null) {
            putMapEntry(url, key, new String(data, 0, ox, encoding));
        }
    }

}