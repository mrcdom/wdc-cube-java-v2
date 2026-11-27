package br.com.wdc.store.view.remote.shell.cn1.util;

import java.util.Calendar;
import java.util.Date;

import br.com.wdc.store.view.remote.shell.cn1.i18n.StoreMessages;
import br.com.wdc.store.view.remote.shell.cn1.i18n.Texts;

/**
 * Date formatting in the language of the UI: the catalog's {@code format.date} / {@code format.dateTime}
 * patterns ({@code MM/dd/yyyy h:mm a}, {@code dd/MM/yyyy HH:mm}, ...). Codename One has no {@code java.time}
 * and its {@code SimpleDateFormat} depends on the platform, so {@link #format(String, long)} reads the pattern
 * by hand.
 */
public final class Dates {

    private Dates() {
        // NOOP
    }

    /** The date, with the catalog's {@code format.date}; {@code format.dateUnavailable} for no date. */
    public static String formatDate(long millis) {
        return millis > 0 ? format(Texts.text(StoreMessages.FORMAT_DATE), millis)
                : Texts.text(StoreMessages.FORMAT_DATE_UNAVAILABLE);
    }

    /** The date and time, with the catalog's {@code format.dateTime}; {@code format.dateUnavailable} for no date. */
    public static String formatDateTime(long millis) {
        return millis > 0 ? format(Texts.text(StoreMessages.FORMAT_DATE_TIME), millis)
                : Texts.text(StoreMessages.FORMAT_DATE_UNAVAILABLE);
    }

    /**
     * Formats {@code millis} (device time zone) with a pattern of the tokens {@code yyyy MM dd HH h mm a}
     * ({@code a} = AM/PM); every other character is copied as it is.
     */
    public static String format(String pattern, long millis) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(millis));
        int hour = c.get(Calendar.HOUR_OF_DAY);
        StringBuilder out = new StringBuilder(pattern.length() + 4);
        int i = 0;
        while (i < pattern.length()) {
            char ch = pattern.charAt(i);
            int run = 1;
            while (i + run < pattern.length() && pattern.charAt(i + run) == ch) {
                run++;
            }
            switch (ch) {
                case 'y':
                    out.append(c.get(Calendar.YEAR));
                    break;
                case 'M':
                    out.append(pad(c.get(Calendar.MONTH) + 1, run));
                    break;
                case 'd':
                    out.append(pad(c.get(Calendar.DAY_OF_MONTH), run));
                    break;
                case 'H':
                    out.append(pad(hour, run));
                    break;
                case 'h':
                    out.append(pad(hour % 12 == 0 ? 12 : hour % 12, run));
                    break;
                case 'm':
                    out.append(pad(c.get(Calendar.MINUTE), run));
                    break;
                case 'a':
                    out.append(hour < 12 ? "AM" : "PM");
                    break;
                default:
                    for (int k = 0; k < run; k++) {
                        out.append(ch);
                    }
                    break;
            }
            i += run;
        }
        return out.toString();
    }

    private static String pad(int v, int width) {
        String s = Integer.toString(v);
        return width >= 2 && v < 10 ? "0" + s : s;
    }
}
