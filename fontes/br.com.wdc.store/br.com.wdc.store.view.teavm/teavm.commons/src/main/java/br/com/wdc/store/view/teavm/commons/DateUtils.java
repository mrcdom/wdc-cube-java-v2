package br.com.wdc.store.view.teavm.commons;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.teavm.jso.JSBody;

/**
 * Date formatting utility (en-US) using the browser's time zone.
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
     * Formats epoch millis as "MM/dd/yyyy h:mm AM|PM" in the browser's time zone.
     */
    public static String formatDateTime(long millis) {
        var ldt = Instant.ofEpochMilli(millis)
                .atZone(getZone())
                .toLocalDateTime();
        var hour = ldt.getHour() % 12 == 0 ? 12 : ldt.getHour() % 12;
        return String.format("%02d/%02d/%04d %d:%02d %s",
                ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getYear(),
                hour, ldt.getMinute(), ldt.getHour() < 12 ? "AM" : "PM");
    }

    /**
     * Formats epoch millis as "MM/dd/yyyy" in the browser's time zone.
     */
    public static String formatDate(long millis) {
        var ldt = Instant.ofEpochMilli(millis)
                .atZone(getZone())
                .toLocalDateTime();
        return String.format("%02d/%02d/%04d",
                ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getYear());
    }
}
