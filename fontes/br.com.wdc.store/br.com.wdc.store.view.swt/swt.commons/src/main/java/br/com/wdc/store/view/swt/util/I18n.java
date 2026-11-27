package br.com.wdc.store.view.swt.util;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import br.com.wdc.store.i18n.Message;
import br.com.wdc.store.i18n.StoreMessages;

/**
 * The texts of the SWT views in the user's language: the catalog language closest to the JVM's default locale
 * ({@link Locale#getDefault()}), picked once at startup.
 */
public final class I18n {

    /** The catalog language of this run ({@code en-US}, {@code pt-BR}). */
    public static final String LANGUAGE = StoreMessages.language(Locale.getDefault().toLanguageTag());

    private I18n() {
    }

    /** The text of {@code key} in {@link #LANGUAGE}. */
    public static String text(String key) {
        return StoreMessages.text(LANGUAGE, key);
    }

    /** The text of {@code key} in {@link #LANGUAGE}, with its {@code {placeholders}} filled from {@code args}. */
    public static String format(String key, Map<String, String> args) {
        return StoreMessages.format(LANGUAGE, key, args);
    }

    /** The text of {@code message} in {@link #LANGUAGE}; an empty text when there is no message. */
    public static String text(Message message) {
        return message != null ? message.in(LANGUAGE) : "";
    }

    /** A date formatter with the {@code format.date} pattern of {@link #LANGUAGE}. */
    public static SimpleDateFormat dateFormat() {
        return dateFormat(LANGUAGE, StoreMessages.FORMAT_DATE);
    }

    /** A date and time formatter with the {@code format.dateTime} pattern of {@link #LANGUAGE}. */
    public static SimpleDateFormat dateTimeFormat() {
        return dateFormat(LANGUAGE, StoreMessages.FORMAT_DATE_TIME);
    }

    /** A formatter with the pattern of {@code patternKey} in {@code language}, whose AM/PM markers follow it too. */
    public static SimpleDateFormat dateFormat(String language, String patternKey) {
        return new SimpleDateFormat(StoreMessages.text(language, patternKey), Locale.forLanguageTag(language));
    }
}
