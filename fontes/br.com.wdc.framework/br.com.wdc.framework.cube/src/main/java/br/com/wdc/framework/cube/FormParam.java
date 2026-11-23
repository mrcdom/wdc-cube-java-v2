package br.com.wdc.framework.cube;

import java.util.Map;
import java.util.function.Function;

import br.com.wdc.framework.commons.lang.CoerceUtils;

/**
 * How an action reads one of its values from the form data a remote client sends with the event: the field name and
 * the conversion. See {@code AbstractPresenter.actionOf(int, FormParam, ...)}.
 */
public final class FormParam<T> {

    public static FormParam<Long> asLong(String name) {
        return new FormParam<>(name, CoerceUtils::asLong);
    }

    public static FormParam<Integer> asInteger(String name) {
        return new FormParam<>(name, CoerceUtils::asInteger);
    }

    public static FormParam<String> asString(String name) {
        return new FormParam<>(name, CoerceUtils::asString);
    }

    /** A field with its own conversion (a value that arrives encoded, for instance). */
    public static <T> FormParam<T> of(String name, Function<Object, T> decoder) {
        return new FormParam<>(name, decoder);
    }

    private final String name;

    private final Function<Object, T> decoder;

    private FormParam(String name, Function<Object, T> decoder) {
        this.name = name;
        this.decoder = decoder;
    }

    public String name() {
        return name;
    }

    /** The value of the field in the form data, converted. */
    public T read(Map<String, Object> formData) {
        return decoder.apply(formData.get(name));
    }
}
