package br.com.wdc.store.view.swt.util;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

import br.com.wdc.store.i18n.Message;
import br.com.wdc.store.i18n.Messages;
import br.com.wdc.store.i18n.StoreMessages;

public class I18nTest {

    private static Date sample() {
        var calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2026, Calendar.MARCH, 7, 14, 5);
        return calendar.getTime();
    }

    @Test
    public void picksTheLanguageOfTheDefaultLocale() {
        assertEquals(StoreMessages.language(Locale.getDefault().toLanguageTag()), I18n.LANGUAGE);
        assertEquals("pt-BR", StoreMessages.language(Locale.of("pt", "PT").toLanguageTag()));
        assertEquals("en-US", StoreMessages.language(Locale.FRANCE.toLanguageTag()));
    }

    @Test
    public void formatsDatesWithThePatternsOfEachLanguage() {
        assertEquals("03/07/2026", I18n.dateFormat("en-US", StoreMessages.FORMAT_DATE).format(sample()));
        assertEquals("03/07/2026 2:05 PM", I18n.dateFormat("en-US", StoreMessages.FORMAT_DATE_TIME).format(sample()));
        assertEquals("07/03/2026", I18n.dateFormat("pt-BR", StoreMessages.FORMAT_DATE).format(sample()));
        assertEquals("07/03/2026 14:05", I18n.dateFormat("pt-BR", StoreMessages.FORMAT_DATE_TIME).format(sample()));
    }

    @Test
    public void translatesAMessageWithArguments() {
        var message = Messages.errorSigninTooManySeconds(30);
        assertEquals("Too many failed sign-ins. Try again in 30 s.", message.in("en-US"));
        assertEquals("Muitas tentativas de entrada sem sucesso. Tente de novo em 30 s.", message.in("pt-BR"));
        assertEquals("", I18n.text((Message) null));
    }
}
