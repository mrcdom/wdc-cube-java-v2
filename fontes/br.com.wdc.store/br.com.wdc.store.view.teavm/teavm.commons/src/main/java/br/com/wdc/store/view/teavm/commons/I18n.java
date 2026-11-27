package br.com.wdc.store.view.teavm.commons;

import java.util.LinkedHashMap;
import java.util.Map;

import org.teavm.jso.JSBody;

import br.com.wdc.store.i18n.Message;
import br.com.wdc.store.i18n.StoreMessages;

/**
 * The texts of the TeaVM views, in the user's language: the catalog ({@link StoreMessages}) in the language picked once
 * at startup from the browser ({@link #init()}), and the format of prices.
 */
public final class I18n {

    private static String language = StoreMessages.DEFAULT_LANGUAGE;

    private I18n() {
    }

    /** Picks the catalog's language for the browser's ({@code navigator.language}) and marks the document with it. */
    public static void init() {
        setLanguage(browserLanguage());
        setDocumentLanguage(language);
    }

    /** The catalog's language in use, such as {@code en-US} or {@code pt-BR}. */
    public static String language() {
        return language;
    }

    /** Uses the catalog's language for {@code tag} (a platform tag such as {@code pt}, {@code pt-PT}, {@code en-GB}). */
    public static void setLanguage(String tag) {
        language = StoreMessages.language(tag);
    }

    /** The text of {@code key}. */
    public static String text(String key) {
        return StoreMessages.text(language, key);
    }

    /** The text of {@code key}, its placeholders replaced by the arguments given as name, value, name, value... */
    public static String text(String key, String... nameValue) {
        var args = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < nameValue.length; i += 2) {
            args.put(nameValue[i], nameValue[i + 1]);
        }
        return StoreMessages.format(language, key, args);
    }

    /** The text of a presenter's message; empty for none. */
    public static String text(Message message) {
        return message != null ? message.in(language) : "";
    }

    /**
     * The message that travels over the bridge as {@code {"key": ..., "args": {...}}}; {@code null} when the value is
     * not one.
     */
    public static Message message(Object wire) {
        if (!(wire instanceof Map<?, ?> map) || !(map.get("key") instanceof String key) || key.isEmpty()) {
            return null;
        }
        var args = new LinkedHashMap<String, String>();
        if (map.get("args") instanceof Map<?, ?> wireArgs) {
            for (var entry : wireArgs.entrySet()) {
                args.put(String.valueOf(entry.getKey()), entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
            }
        }
        return new Message(key, args);
    }

    /** A price in US dollars, the same in every language (the currency is a fact of the data): {@code $1,234.56}. */
    public static String price(double value) {
        long cents = Math.round(Math.abs(value) * 100);
        var whole = Long.toString(cents / 100);
        var out = new StringBuilder(whole.length() + 8);
        if (value < 0 && cents > 0) {
            out.append('-');
        }
        out.append('$');
        for (int i = 0; i < whole.length(); i++) {
            if (i > 0 && (whole.length() - i) % 3 == 0) {
                out.append(',');
            }
            out.append(whole.charAt(i));
        }
        long fraction = cents % 100;
        out.append('.').append(fraction < 10 ? "0" : "").append(fraction);
        return out.toString();
    }

    @JSBody(params = {}, script = "try { return navigator.language || ''; } catch (e) { return ''; }")
    private static native String browserLanguage();

    @JSBody(params = { "tag" }, script = "try { document.documentElement.lang = tag; } catch (e) {}")
    private static native void setDocumentLanguage(String tag);
}
