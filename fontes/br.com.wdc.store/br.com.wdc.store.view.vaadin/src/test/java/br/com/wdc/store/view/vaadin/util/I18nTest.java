package br.com.wdc.store.view.vaadin.util;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import org.junit.Test;

import br.com.wdc.store.i18n.Message;
import br.com.wdc.store.i18n.Messages;
import br.com.wdc.store.i18n.StoreMessages;

public class I18nTest {

    private static final long SAMPLE = LocalDateTime.of(2026, 3, 7, 14, 5)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();

    @Test
    public void picksTheLanguageOfTheBrowserLocale() {
        assertEquals("pt-BR", new I18n(Locale.of("pt", "BR")).language());
        assertEquals("pt-BR", new I18n(Locale.of("pt")).language());
        assertEquals("en-US", new I18n(Locale.US).language());
        assertEquals("en-US", new I18n(Locale.GERMANY).language());
        assertEquals("en-US", new I18n(null).language());
    }

    @Test
    public void formatsDatesWithThePatternsOfEachLanguage() {
        var english = new I18n(Locale.US);
        assertEquals("03/07/2026", english.date(SAMPLE));
        assertEquals("03/07/2026 2:05 PM", english.dateTime(SAMPLE));
        var portuguese = new I18n(Locale.of("pt", "BR"));
        assertEquals("07/03/2026", portuguese.date(SAMPLE));
        assertEquals("07/03/2026 14:05", portuguese.dateTime(SAMPLE));
    }

    @Test
    public void keepsPricesInUsDollars() {
        assertEquals("$1,234.56", I18n.price(1234.56));
    }

    @Test
    public void translatesTextsAndMessages() {
        var portuguese = new I18n(Locale.of("pt", "BR"));
        assertEquals("Muitas tentativas de entrada sem sucesso. Tente de novo em 30 s.",
                portuguese.text(Messages.errorSigninTooManySeconds(30)));
        assertEquals("Quantidade", portuguese.text(StoreMessages.VAADIN_QUANTITY));
        assertEquals("", portuguese.text((Message) null));
    }
}
