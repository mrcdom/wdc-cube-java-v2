package br.com.wdc.store.view.teavm.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import br.com.wdc.store.i18n.Message;
import br.com.wdc.store.i18n.StoreMessages;

public class I18nTest {

    private static final LocalDateTime AFTERNOON = LocalDateTime.of(2026, 3, 7, 14, 5);
    private static final LocalDateTime MIDNIGHT = LocalDateTime.of(2026, 12, 25, 0, 30);

    @After
    public void restoreLanguage() {
        I18n.setLanguage(StoreMessages.DEFAULT_LANGUAGE);
    }

    @Test
    public void mapsThePlatformLanguageToTheCatalogs() {
        I18n.setLanguage("pt");
        assertEquals("pt-BR", I18n.language());
        I18n.setLanguage("pt-PT");
        assertEquals("pt-BR", I18n.language());
        I18n.setLanguage("en-GB");
        assertEquals("en-US", I18n.language());
        I18n.setLanguage("fr-FR");
        assertEquals("en-US", I18n.language());
        I18n.setLanguage("");
        assertEquals("en-US", I18n.language());
    }

    @Test
    public void translatesAMessageWithArguments() {
        var message = Message.of(StoreMessages.ERROR_SIGNIN_TOO_MANY_SECONDS, "seconds", 30);
        assertEquals("Too many failed sign-ins. Try again in 30 s.", I18n.text(message));
        I18n.setLanguage("pt-BR");
        assertEquals(StoreMessages.format("pt-BR", StoreMessages.ERROR_SIGNIN_TOO_MANY_SECONDS, Map.of("seconds", "30")),
                I18n.text(message));
        assertEquals("", I18n.text((Message) null));
    }

    @Test
    public void readsTheMessageOfTheWire() {
        var message = I18n.message(Map.of("key", "error.accessDenied", "args", Map.of("reason", "role")));
        assertEquals(Message.of("error.accessDenied", "reason", "role"), message);
        assertEquals(Message.of("error.unexpected"), I18n.message(Map.of("key", "error.unexpected")));
        assertNull(I18n.message(null));
        assertNull(I18n.message("text"));
        assertNull(I18n.message(Map.of("args", Map.of())));
    }

    @Test
    public void formatsPricesInUsDollarsInEveryLanguage() {
        assertEquals("$16.00", I18n.price(16));
        assertEquals("$1,234.56", I18n.price(1234.56));
        assertEquals("$1,234,567.89", I18n.price(1234567.891));
        assertEquals("$0.05", I18n.price(0.05));
        assertEquals("$999.99", I18n.price(999.99));
        assertEquals("-$1,000.10", I18n.price(-1000.1));
        I18n.setLanguage("pt-BR");
        assertEquals("$1,234.56", I18n.price(1234.56));
    }

    @Test
    public void formatsDatesWithThePatternsOfEachLanguage() {
        assertEquals("03/07/2026", DateUtils.format(StoreMessages.text("en-US", StoreMessages.FORMAT_DATE), AFTERNOON));
        assertEquals("03/07/2026 2:05 PM",
                DateUtils.format(StoreMessages.text("en-US", StoreMessages.FORMAT_DATE_TIME), AFTERNOON));
        assertEquals("12/25/2026 12:30 AM",
                DateUtils.format(StoreMessages.text("en-US", StoreMessages.FORMAT_DATE_TIME), MIDNIGHT));
        assertEquals("07/03/2026", DateUtils.format(StoreMessages.text("pt-BR", StoreMessages.FORMAT_DATE), AFTERNOON));
        assertEquals("07/03/2026 14:05",
                DateUtils.format(StoreMessages.text("pt-BR", StoreMessages.FORMAT_DATE_TIME), AFTERNOON));
        assertEquals("25/12/2026 00:30",
                DateUtils.format(StoreMessages.text("pt-BR", StoreMessages.FORMAT_DATE_TIME), MIDNIGHT));
    }
}
