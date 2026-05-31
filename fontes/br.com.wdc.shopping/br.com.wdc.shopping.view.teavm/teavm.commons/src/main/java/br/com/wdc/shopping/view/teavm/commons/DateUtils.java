package br.com.wdc.shopping.view.teavm.commons;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.teavm.jso.JSBody;

/**
 * Utilitário para formatação de datas usando o timezone do navegador.
 */
public final class DateUtils {

    private static ZoneId browserZone;

    private DateUtils() {
    }

    @JSBody(params = {}, script = "try { return Intl.DateTimeFormat().resolvedOptions().timeZone; } catch(e) { return ''; }")
    private static native String getBrowserTimeZone();

    private static ZoneId getZone() {
        if (browserZone == null) {
            var tz = getBrowserTimeZone();
            if (tz != null && !tz.isEmpty()) {
                try {
                    browserZone = ZoneId.of(tz);
                } catch (Exception e) {
                    browserZone = ZoneOffset.UTC;
                }
            } else {
                browserZone = ZoneOffset.UTC;
            }
        }
        return browserZone;
    }

    /**
     * Formata epoch millis como "dd/MM/yyyy HH:mm" no timezone do navegador.
     */
    public static String formatDateTime(long millis) {
        var ldt = Instant.ofEpochMilli(millis)
                .atZone(getZone())
                .toLocalDateTime();
        return String.format("%02d/%02d/%04d %02d:%02d",
                ldt.getDayOfMonth(), ldt.getMonthValue(), ldt.getYear(),
                ldt.getHour(), ldt.getMinute());
    }

    /**
     * Formata epoch millis como "dd/MM/yyyy" no timezone do navegador.
     */
    public static String formatDate(long millis) {
        var ldt = Instant.ofEpochMilli(millis)
                .atZone(getZone())
                .toLocalDateTime();
        return String.format("%02d/%02d/%04d",
                ldt.getDayOfMonth(), ldt.getMonthValue(), ldt.getYear());
    }
}
