package br.com.wdc.framework.cube;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.cube.util.QueryStringBuilder;
import br.com.wdc.framework.cube.util.QueryStringParser;

/**
 * This class represents a composition between WebFlowPath and a bunch of parameters. It is through this class that
 * application commands the browsing from one application presentation state to another.
 */
public class CubeIntent {

    /** A logger for this class */
    private static final Logger logger = Logger.getLogger(CubeIntent.class.getName());

    public static CubeIntent parse(String placeStr) throws Exception {
        // If we have a not blank URI, then we will proceed with URI parsing
        var intent = new CubeIntent();
        if (StringUtils.isNotBlank(placeStr)) {
            // First, we are going to brake the URI into two parts
            String[] parts = placeStr.split("\\?");
            intent.setPlace(new GenericStep(-1, parts[0]));
            if (parts.length > 1) {
                QueryStringParser.parse(intent, parts[1], StandardCharsets.UTF_8);
            }
        } else {
            intent.setPlace(new GenericStep(-1, "unknown"));
        }
        return intent;
    }

    private static class GenericStep implements CubePlace {

        private Integer id;

        private String name;

        public GenericStep(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Integer getId() {
            return this.id;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public <A extends CubeApplication> Function<A, CubePresenter> presenterFactory() {
            throw new AssertionError("Must no me invoked");
        }
    }

    // :: Instance

    private CubePlace place;

    /** A parameters collection */
    private Map<String, Object> parameters;

    /** A attributes collection */
    private Map<String, Object> attributes;

    /**
     * Default constructor.
     *
     * @param path A mandatory path
     */
    public CubeIntent() {
        this.parameters = new LinkedHashMap<>();
    }

    public CubePlace getPlace() {
        return this.place;
    }

    public void setPlace(CubePlace place) {
        this.place = place;
    }

    public void setAttribute(String name, Object value) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        this.attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        if (this.attributes == null) {
            return null;
        } else {
            return this.attributes.get(name);
        }
    }

    public Object remoteAttribute(String name) {
        if (this.attributes == null) {
            return null;
        } else {
            return this.attributes.remove(name);
        }
    }

    public void setViewSlot(String name, CubeViewSlot slot) {
        this.setAttribute(name, slot);
    }

    public CubeViewSlot getViewSlot(String name) {
        var value = this.getAttribute(name);
        if (value instanceof CubeViewSlot v) {
            return v;
        }
        return null;
    }

    /**
     * Remove all parameters of this URL.
     */
    public void clearParameters() {
        this.parameters.clear();
    }

    /**
     * Remove a specific parameter.
     *
     * @param name The parameter's name
     */
    public Object removeParameter(final String name) {
        return this.parameters.remove(name);
    }

    /**
     * Sets or replace a parameter single value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name  a String specifying the name of the parameter
     * @param value a String specifying the value of the parameter
     */
    public void setParameter(final String name, final String value) {
        if (value == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, value);
        }
    }

    /**
     * Sets or replace a parameter single value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name  a String specifying the name of the parameter
     * @param value a BigDecimal specifying the value of the parameter
     */
    public final void setParameter(final String name, final BigDecimal value) {
        if (value == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, value);
        }
    }

    /**
     * Sets or replace a parameter single value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name  a String specifying the name of the parameter
     * @param value a Double specifying the value of the parameter
     */
    public final void setParameter(final String name, final Double value) {
        if (value == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, value);
        }
    }

    /**
     * Sets or replace a parameter single value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name  a String specifying the name of the parameter
     * @param value a Float specifying the value of the parameter
     */
    public final void setParameter(final String name, final Float value) {
        if (value == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, value);
        }
    }

    /**
     * Sets or replace a parameter single value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name  a String specifying the name of the parameter
     * @param value a BigInteger specifying the value of the parameter
     */
    public final void setParameter(final String name, final BigInteger value) {
        if (value == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, value);
        }
    }

    /**
     * Sets or replace a parameter single value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name  a String specifying the name of the parameter
     * @param value a Long specifying the value of the parameter
     */
    public final void setParameter(final String name, final Long value) {
        if (value == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, value);
        }
    }

    /**
     * Sets or replace a parameter single value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name  a String specifying the name of the parameter
     * @param value a Short specifying the value of the parameter
     */
    public final void setParameter(final String name, final Short value) {
        if (value == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, value);
        }
    }

    /**
     * Sets or replace a parameter single value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name  a String specifying the name of the parameter
     * @param value a Integer specifying the value of the parameter
     */
    public final void setParameter(final String name, final Integer value) {
        if (value == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, value);
        }
    }

    /**
     * Sets or replace a parameter single value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name  a String specifying the name of the parameter
     * @param value a Byte specifying the value of the parameter
     */
    public final void setParameter(final String name, final Byte value) {
        if (value == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, value);
        }
    }

    /**
     * Sets or replace a parameter single value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name  a String specifying the name of the parameter
     * @param value a Character specifying the value of the parameter
     */
    public final void setParameter(final String name, final Character value) {
        if (value == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, value);
        }
    }

    /**
     * Sets or replace a parameter multiple value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name   a String specifying the name of the parameter
     * @param values a String array specifying the multiple value of the parameter
     */
    public final void setParameter(final String name, final String[] values) {
        if (values == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, values);
        }
    }

    /**
     * Sets or replace a parameter multiple value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name   a String specifying the name of the parameter
     * @param values a BigDecimal array specifying the multiple value of the parameter
     */
    public final void setParameter(final String name, final BigDecimal[] values) {
        if (values == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, values);
        }
    }

    /**
     * Sets or replace a parameter multiple value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name   a String specifying the name of the parameter
     * @param values a Double array specifying the multiple value of the parameter
     */
    public void setParameter(final String name, final Double[] values) {
        if (values == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, values);
        }
    }

    /**
     * Sets or replace a parameter multiple value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name   a String specifying the name of the parameter
     * @param values a Float array specifying the multiple value of the parameter
     */
    public void setParameter(final String name, final Float[] values) {
        if (values == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, values);
        }
    }

    /**
     * Sets or replace a parameter multiple value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name   a String specifying the name of the parameter
     * @param values a BigInteger array specifying the multiple value of the parameter
     */
    public void setParameter(final String name, final BigInteger[] values) {
        if (values == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, values);
        }
    }

    /**
     * Sets or replace a parameter multiple value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name   a String specifying the name of the parameter
     * @param values a Long array specifying the multiple value of the parameter
     */
    public void setParameter(final String name, final Long[] values) {
        if (values == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, values);
        }
    }

    /**
     * Sets or replace a parameter multiple value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name   a String specifying the name of the parameter
     * @param values a Integer array specifying the multiple value of the parameter
     */
    public void setParameter(final String name, final Integer[] values) {
        if (values == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, values);
        }
    }

    /**
     * Sets or replace a parameter multiple value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name   a String specifying the name of the parameter
     * @param values a Short array specifying the multiple value of the parameter
     */
    public void setParameter(final String name, final Short[] values) {
        if (values == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, values);
        }
    }

    /**
     * Sets or replace a parameter multiple value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name   a String specifying the name of the parameter
     * @param values a Byte array specifying the multiple value of the parameter
     */
    public void setParameter(final String name, final Byte[] values) {
        if (values == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, values);
        }
    }

    /**
     * Sets or replace a parameter multiple value. Parameters will became part of the hash token in the history. Be
     * careful and choose short form for names and values.
     *
     * @param name   a String specifying the name of the parameter
     * @param values a Character array specifying the multiple value of the parameter
     */
    public void setParameter(final String name, final Character[] values) {
        if (values == null) {
            this.parameters.remove(name);
        } else {
            this.parameters.put(name, values);
        }
    }

    /**
     * <p>
     * Returns the value of a request parameter, or null if the parameter does not exist.
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name a String specifying the name of the parameter
     * @return a Object representing the value of the parameter
     */
    public final Object getParameterValue(final String name) {
        return this.parameters.get(name);
    }

    /**
     * <p>
     * Returns the value of a request parameter as a BigDecimal, or null if the parameter does not exist.
     * </p>
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String).
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name         a String specifying the name of the parameter
     * @param defaultValue a default value to be returned case current value is null
     * @return a String representing the single value of the parameter
     */
    public String getParameterAsString(final String name, final String defaultValue) {
        var svalue = CoerceUtils.asString(this.parameters.get(name));
        return svalue == null ? defaultValue : svalue;
    }

    /**
     * <p>
     * Returns the value of a request parameter as a BigDecimal, or null if the parameter does not exist.
     * </p>
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String).
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name         a String specifying the name of the parameter
     * @param defaultValue a default value to be returned case current value is null or a invalid conversion
     * @return a String representing the single value of the parameter
     */
    public BigDecimal getParameterAsBigDecimal(final String name, final BigDecimal defaultValue) {
        var value = this.parameters.get(name);

        if (value instanceof BigDecimal v) {
            return v;
        }

        if (value != null) {
            try {
                return new BigDecimal(CoerceUtils.asString(value));
            } catch (NumberFormatException exn) {
                logger.log(Level.WARNING, NumberFormatException.class.getSimpleName(), exn);
            }
        }

        return defaultValue;
    }

    /**
     * <p>
     * Returns the value of a request parameter as a Double, or null if the parameter does not exist.
     * </p>
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String).
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name         a String specifying the name of the parameter
     * @param defaultValue a default value to be returned case current value is null or a invalid conversion
     * @return a String representing the single value of the parameter
     */
    public Double getParameterAsDouble(final String name, final Double defaultValue) {
        var value = this.parameters.get(name);

        if (value instanceof Double v) {
            return v;
        }

        if (value != null) {
            try {
                return Double.valueOf(CoerceUtils.asString(value));
            } catch (NumberFormatException exn) {
                logger.log(Level.WARNING, NumberFormatException.class.getSimpleName(), exn);
            }
        }

        return defaultValue;
    }

    /**
     * <p>
     * Returns the value of a request parameter as a Float, or null if the parameter does not exist.
     * </p>
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String).
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name         a String specifying the name of the parameter
     * @param defaultValue a default value to be returned case current value is null or a invalid conversion
     * @return a String representing the single value of the parameter
     */
    public Float getParameterAsFloat(final String name, final Float defaultValue) {
        var value = this.parameters.get(name);

        if (value instanceof Float v) {
            return v;
        }

        if (value != null) {
            try {
                return Float.valueOf(CoerceUtils.asString(value));
            } catch (NumberFormatException exn) {
                logger.log(Level.WARNING, NumberFormatException.class.getSimpleName(), exn);
            }
        }

        return defaultValue;
    }

    /**
     * <p>
     * Returns the value of a request parameter as a BigInteger, or null if the parameter does not exist.
     * </p>
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String).
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name         a String specifying the name of the parameter
     * @param defaultValue a default value to be returned case current value is null or a invalid conversion
     * @return a String representing the single value of the parameter
     */
    public BigInteger getParameterAsBigInteger(final String name, final BigInteger defaultValue) {
        var value = this.parameters.get(name);

        if (value instanceof BigInteger v) {
            return v;
        }

        if (value != null) {
            try {
                return new BigInteger(CoerceUtils.asString(value));
            } catch (NumberFormatException exn) {
                logger.log(Level.WARNING, NumberFormatException.class.getSimpleName(), exn);
            }
        }

        return defaultValue;
    }

    /**
     * <p>
     * Returns the value of a request parameter as a Long, or null if the parameter does not exist.
     * </p>
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String).
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name         a String specifying the name of the parameter
     * @param defaultValue a default value to be returned case current value is null or a invalid conversion
     * @return a String representing the single value of the parameter
     */
    public Long getParameterAsLong(final String name, final Long defaultValue) {
        var value = this.parameters.get(name);

        if (value instanceof Long v) {
            return v;
        }

        if (value != null) {
            try {
                return Long.valueOf(CoerceUtils.asString(value));
            } catch (NumberFormatException exn) {
                logger.log(Level.WARNING, NumberFormatException.class.getSimpleName(), exn);
            }
        }

        return defaultValue;
    }

    /**
     * <p>
     * Returns the value of a request parameter as a Integer, or null if the parameter does not exist.
     * </p>
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String).
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name         a String specifying the name of the parameter
     * @param defaultValue a default value to be returned case current value is null or a invalid conversion
     * @return a String representing the single value of the parameter
     */
    public Integer getParameterAsInteger(final String name, final Integer defaultValue) {
        var value = this.parameters.get(name);

        if (value instanceof Integer v) {
            return v;
        }

        if (value != null) {
            try {
                return Integer.valueOf(CoerceUtils.asString(value));
            } catch (NumberFormatException exn) {
                logger.log(Level.WARNING, NumberFormatException.class.getSimpleName(), exn);
            }
        }

        return defaultValue;
    }

    /**
     * <p>
     * Returns the value of a request parameter as a Short, or null if the parameter does not exist.
     * </p>
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String).
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name         a String specifying the name of the parameter
     * @param defaultValue a default value to be returned case current value is null or a invalid conversion
     * @return a String representing the single value of the parameter
     */
    public Short getParameterAsShort(final String name, final Short defaultValue) {
        var value = this.parameters.get(name);

        if (value instanceof Short v) {
            return v;
        }

        if (value != null) {
            try {
                return Short.valueOf(CoerceUtils.asString(value));
            } catch (NumberFormatException exn) {
                logger.log(Level.WARNING, NumberFormatException.class.getSimpleName(), exn);
            }
        }

        return defaultValue;
    }

    /**
     * <p>
     * Returns the value of a request parameter as a Byte, or null if the parameter does not exist.
     * </p>
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String).
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name         a String specifying the name of the parameter
     * @param defaultValue a default value to be returned case current value is null or a invalid conversion
     * @return a String representing the single value of the parameter
     */
    public Byte getParameterAsByte(final String name, final Byte defaultValue) {
        var value = this.parameters.get(name);

        if (value instanceof Byte v) {
            return v;
        }

        if (value != null) {
            try {
                return Byte.valueOf(CoerceUtils.asString(value));
            } catch (NumberFormatException exn) {
                logger.log(Level.WARNING, NumberFormatException.class.getSimpleName(), exn);
            }
        }

        return defaultValue;
    }

    /**
     * <p>
     * Returns the value of a request parameter as a Character, or null if the parameter does not exist.
     * </p>
     * <p>
     * You should only use this method when you are sure the parameter has only one value. If the parameter might have
     * more than one value, use getParameterValues(java.lang.String).
     * </p>
     * <p>
     * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array.
     * </p>
     *
     * @param name         a String specifying the name of the parameter
     * @param defaultValue a default value to be returned case current value is null or a invalid conversion
     * @return a String representing the single value of the parameter
     */
    public Character getParameterAsCharacter(final String name, final Character defaultValue) {
        var value = this.parameters.get(name);

        if (value instanceof Character v) {
            return v;
        }

        if (value != null) {
            try {
                String svalue = CoerceUtils.asString(value);
                return StringUtils.isBlank(svalue) ? defaultValue : Character.valueOf(svalue.charAt(0));
            } catch (NumberFormatException exn) {
                logger.log(Level.WARNING, NumberFormatException.class.getSimpleName(), exn);
            }
        }

        return defaultValue;
    }

    /**
     * Returns the query string that is contained in the request path after the path walking.
     *
     * @return a String containing the query string.
     */
    public String getQueryString() {
        if (this.parameters.size() == 0) {
            return "";
        }

        QueryStringBuilder builder = new QueryStringBuilder();
        builder.append(this.parameters);
        return builder.toString();
    }

    @Override
    public String toString() {
        String queryString = this.getQueryString();
        if (StringUtils.isBlank(queryString)) {
            return this.place.getName();
        } else {
            return this.place.getName() + '?' + queryString;
        }
    }

}
