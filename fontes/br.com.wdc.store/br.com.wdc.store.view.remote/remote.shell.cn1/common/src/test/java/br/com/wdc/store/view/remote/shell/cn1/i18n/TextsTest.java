package br.com.wdc.store.view.remote.shell.cn1.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.wdc.store.view.remote.shell.cn1.util.Dates;

/**
 * The shell's language choice from device locales, presenter messages from the bridge wire map, and dates in
 * the catalog's patterns of each language.
 */
class TextsTest {

    @AfterEach
    void backToDefault() {
        Texts.useLanguage(null);
    }

    @Test
    void deviceLocalesMapToCatalogLanguages() {
        assertEquals("pt-BR", StoreMessages.language("pt_BR"));
        assertEquals("pt-BR", StoreMessages.language("pt_PT"));
        assertEquals("pt-BR", StoreMessages.language("pt"));
        assertEquals("en-US", StoreMessages.language("en_GB"));
        assertEquals("en-US", StoreMessages.language("fr_FR"));
        assertEquals("en-US", StoreMessages.language(""));
        assertEquals("en-US", StoreMessages.language(null));
    }

    @Test
    void wireMessageIsTranslatedWithItsArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("seconds", "30");
        Map<String, Object> wire = new HashMap<>();
        wire.put("key", StoreMessages.ERROR_SIGNIN_TOO_MANY_SECONDS);
        wire.put("args", args);

        Texts.useLanguage("pt-BR");
        assertEquals("Muitas tentativas de entrada sem sucesso. Tente de novo em 30 s.", Texts.message(wire));

        Texts.useLanguage("en-US");
        assertEquals("Too many failed sign-ins. Try again in 30 s.", Texts.message(wire));
    }

    @Test
    void wireMessageWithoutArgsOrKey() {
        Map<String, Object> wire = new HashMap<>();
        wire.put("key", StoreMessages.ERROR_SIGNIN_UNRECOGNIZED);
        assertEquals("Username or password not recognized!", Texts.message(wire));
        assertEquals("", Texts.message(null));
        assertEquals("", Texts.message(new HashMap<String, Object>()));
    }

    @Test
    void datesFollowTheLanguagePatterns() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2026, Calendar.MARCH, 7, 14, 5);
        long afternoon = c.getTimeInMillis();
        c.set(2026, Calendar.MARCH, 7, 0, 30);
        long midnight = c.getTimeInMillis();

        Texts.useLanguage("en-US");
        assertEquals("03/07/2026", Dates.formatDate(afternoon));
        assertEquals("03/07/2026 2:05 PM", Dates.formatDateTime(afternoon));
        assertEquals("03/07/2026 12:30 AM", Dates.formatDateTime(midnight));
        assertEquals("Date unavailable", Dates.formatDate(0));

        Texts.useLanguage("pt-BR");
        assertEquals("07/03/2026", Dates.formatDate(afternoon));
        assertEquals("07/03/2026 14:05", Dates.formatDateTime(afternoon));
        assertEquals("07/03/2026 00:30", Dates.formatDateTime(midnight));
        assertEquals(StoreMessages.text("pt-BR", StoreMessages.FORMAT_DATE_UNAVAILABLE), Dates.formatDateTime(0));
    }

    @Test
    void theDeviceTag_isTheLanguageAndTheCountry() {
        assertEquals("pt-BR", Texts.tagOf("pt", "BR"));
        assertEquals("pt_BR", Texts.tagOf("pt", "pt_BR"));
        assertEquals("en", Texts.tagOf("en", null));
        assertEquals("pt-BR", StoreMessages.language(Texts.tagOf("pt", "BR")));
        assertEquals("en-US", StoreMessages.language(Texts.tagOf("en", "GB")));
    }
}
