package br.com.wdc.store.view.teavm.commons;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.teavm.jso.JSBody;

import br.com.wdc.store.i18n.StoreMessages;

/**
 * Date formatting in the browser's time zone, with the patterns of the user's language ({@code format.date},
 * {@code format.dateTime} of the catalog).
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

    /** Epoch millis as date and time ({@code format.dateTime}) in the browser's time zone. */
    public static String formatDateTime(long millis) {
        return format(I18n.text(StoreMessages.FORMAT_DATE_TIME), toLocal(millis));
    }

    /** Epoch millis as a date ({@code format.date}) in the browser's time zone. */
    public static String formatDate(long millis) {
        return format(I18n.text(StoreMessages.FORMAT_DATE), toLocal(millis));
    }

    private static LocalDateTime toLocal(long millis) {
        return Instant.ofEpochMilli(millis)
                .atZone(getZone())
                .toLocalDateTime();
    }

    /**
     * Formats with a pattern of the catalog: {@code yyyy}/{@code yy} year, {@code MM}/{@code M} month, {@code dd}/
     * {@code d} day, {@code HH}/{@code H} hour of the day, {@code hh}/{@code h} hour on the 12-hour clock, {@code mm}/
     * {@code m} minute, {@code a} AM/PM; any other character as it is.
     */
    public static String format(String pattern, LocalDateTime value) {
        var out = new StringBuilder(pattern.length() + 8);
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            int run = 1;
            while (i + run < pattern.length() && pattern.charAt(i + run) == c) {
                run++;
            }
            switch (c) {
                case 'y' -> out.append(run == 2 ? pad(value.getYear() % 100, 2) : pad(value.getYear(), run));
                case 'M' -> out.append(pad(value.getMonthValue(), run));
                case 'd' -> out.append(pad(value.getDayOfMonth(), run));
                case 'H' -> out.append(pad(value.getHour(), run));
                case 'h' -> out.append(pad(value.getHour() % 12 == 0 ? 12 : value.getHour() % 12, run));
                case 'm' -> out.append(pad(value.getMinute(), run));
                case 'a' -> out.append(value.getHour() < 12 ? "AM" : "PM");
                default -> out.append(pattern, i, i + run);
            }
            i += run;
        }
        return out.toString();
    }

    private static String pad(int value, int width) {
        var text = Integer.toString(value);
        var out = new StringBuilder(width);
        for (int n = text.length(); n < width; n++) {
            out.append('0');
        }
        return out.append(text).toString();
    }
}
