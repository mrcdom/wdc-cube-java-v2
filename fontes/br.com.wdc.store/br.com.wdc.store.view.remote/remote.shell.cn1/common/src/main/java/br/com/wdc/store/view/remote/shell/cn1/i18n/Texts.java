package br.com.wdc.store.view.remote.shell.cn1.i18n;

import java.util.HashMap;
import java.util.Map;

import com.codename1.l10n.L10NManager;
import com.codename1.ui.Display;

/**
 * The shell's UI texts in the language of the device, from the store's catalog ({@link StoreMessages}).
 * The language is picked once at startup ({@link #useDeviceLanguage()}); a {@code pt-*} device shows
 * {@code pt-BR}, any other {@code en-US}.
 */
public final class Texts {

    private static String language = StoreMessages.DEFAULT_LANGUAGE;

    private Texts() {
        // NOOP
    }

    /** Picks the catalog language from the device locale ({@code pt_BR}, {@code en_US}, ...). */
    public static void useDeviceLanguage() {
        language = StoreMessages.language(deviceTag());
    }

    private static String deviceTag() {
        try {
            L10NManager l10n = Display.getInstance().getLocalizationManager();
            if (l10n == null) {
                return null;
            }
            return tagOf(l10n.getLanguage(), l10n.getLocale());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * The language tag of Codename One's localization manager: its {@code getLanguage()} is the language ({@code pt})
     * and its {@code getLocale()} the country ({@code BR}) on some ports, a whole locale ({@code pt_BR}) on others.
     */
    static String tagOf(String language, String locale) {
        if (locale != null && (locale.indexOf('_') > 0 || locale.indexOf('-') > 0)) {
            return locale;
        }
        if (language == null || language.length() == 0) {
            return null;
        }
        return locale != null && locale.length() == 2 ? language + "-" + locale : language;
    }

    /** Sets the catalog language from a tag ({@code pt-BR}, {@code pt}, {@code en}). */
    public static void useLanguage(String tag) {
        language = StoreMessages.language(tag);
    }

    /** The catalog language in use. */
    public static String language() {
        return language;
    }

    /** The text of {@code key}, its placeholders as they are. */
    public static String text(String key) {
        return StoreMessages.text(language, key);
    }

    /** The text of {@code key} with one placeholder replaced. */
    public static String format(String key, String name, String value) {
        Map<String, String> args = new HashMap<>();
        args.put(name, value);
        return StoreMessages.format(language, key, args);
    }

    /**
     * The text of a presenter message as it travels on the bridge — a map {@code {"key": ..., "args": {...}}};
     * {@code ""} when there is none.
     */
    public static String message(Object wire) {
        if (!(wire instanceof Map)) {
            return "";
        }
        Map<?, ?> map = (Map<?, ?>) wire;
        Object key = map.get("key");
        if (key == null || String.valueOf(key).isEmpty()) {
            return "";
        }
        Map<String, String> args = new HashMap<>();
        Object rawArgs = map.get("args");
        if (rawArgs instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) rawArgs).entrySet()) {
                args.put(String.valueOf(e.getKey()), e.getValue() == null ? "" : String.valueOf(e.getValue()));
            }
        }
        return StoreMessages.format(language, String.valueOf(key), args);
    }
}
