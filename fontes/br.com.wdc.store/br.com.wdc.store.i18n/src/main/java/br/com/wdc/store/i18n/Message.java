package br.com.wdc.store.i18n;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A text to show, as a key of the catalog ({@link StoreMessages}) and the values of its {@code {placeholders}}: what
 * the presentation puts in a scope instead of the text itself, so that each view shows it in the user's language.
 * Remotely it travels as {@code {"key": ..., "args": {...}}}. Made by the factories of {@link Messages}, one per key,
 * with a parameter per placeholder.
 */
public record Message(String key, Map<String, String> args) {

    public Message {
        args = args == null || args.isEmpty() ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(args));
    }

    /** A message of {@code key}, with its arguments as name, value, name, value... */
    public static Message of(String key, Object... nameValue) {
        if (nameValue.length % 2 != 0) {
            throw new IllegalArgumentException("Arguments go in name, value pairs");
        }
        var args = new LinkedHashMap<String, String>();
        for (int i = 0; i < nameValue.length; i += 2) {
            args.put(String.valueOf(nameValue[i]), nameValue[i + 1] == null ? "" : String.valueOf(nameValue[i + 1]));
        }
        return new Message(key, args);
    }

    /** The text in {@code language} (a tag such as {@code en-US}, {@code pt-BR}, {@code pt}). */
    public String in(String language) {
        return StoreMessages.format(language, key, args);
    }
}
