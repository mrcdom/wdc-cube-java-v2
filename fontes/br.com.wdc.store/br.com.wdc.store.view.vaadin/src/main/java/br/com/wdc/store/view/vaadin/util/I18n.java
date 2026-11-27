package br.com.wdc.store.view.vaadin.util;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import br.com.wdc.store.i18n.Message;
import br.com.wdc.store.i18n.StoreMessages;

/**
 * The texts and formats of one user's views: the catalog language closest to the browser's locale, with the date
 * patterns of that language ({@code format.date}, {@code format.dateTime}). Prices are in US dollars in every language.
 */
public final class I18n {

    private final String language;
    private final Locale locale;
    private final DateTimeFormatter dateFormat;
    private final DateTimeFormatter dateTimeFormat;

    /** The texts of the catalog language closest to {@code locale} ({@code null} is the default language). */
    public I18n(Locale locale) {
        this.language = StoreMessages.language(locale != null ? locale.toLanguageTag() : null);
        this.locale = Locale.forLanguageTag(this.language);
        this.dateFormat = DateTimeFormatter.ofPattern(StoreMessages.text(this.language, StoreMessages.FORMAT_DATE),
                this.locale);
        this.dateTimeFormat = DateTimeFormatter
                .ofPattern(StoreMessages.text(this.language, StoreMessages.FORMAT_DATE_TIME), this.locale);
    }

    /** The catalog language ({@code en-US}, {@code pt-BR}). */
    public String language() {
        return this.language;
    }

    /** The locale of {@link #language()}. */
    public Locale locale() {
        return this.locale;
    }

    /** The text of {@code key}. */
    public String text(String key) {
        return StoreMessages.text(this.language, key);
    }

    /** The text of {@code message}; an empty text when there is no message. */
    public String text(Message message) {
        return message != null ? message.in(this.language) : "";
    }

    /** The date of {@code millis} (epoch milliseconds, in the server's zone) with the {@code format.date} pattern. */
    public String date(long millis) {
        return this.dateFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    /** The date and time of {@code millis} with the {@code format.dateTime} pattern. */
    public String dateTime(long millis) {
        return this.dateTimeFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    /** A price, in US dollars ({@code $1,234.56}) in every language. */
    public static String price(double value) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(value);
    }
}
