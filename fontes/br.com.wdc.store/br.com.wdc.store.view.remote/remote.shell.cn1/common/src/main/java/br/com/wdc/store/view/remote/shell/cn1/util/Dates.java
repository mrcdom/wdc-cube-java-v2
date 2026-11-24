package br.com.wdc.store.view.remote.shell.cn1.util;

import java.util.Calendar;
import java.util.Date;

/** Date formatting (en-US, MM/dd/yyyy) — mirrors the React shell's {@code DateUtils.formatDate}. */
public final class Dates {

    private Dates() {
        // NOOP
    }

    public static String formatDate(long millis) {
        if (millis <= 0) {
            return "";
        }
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(millis));
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH) + 1;
        int year = c.get(Calendar.YEAR);
        return two(month) + "/" + two(day) + "/" + year;
    }

    /** MM/dd/yyyy h:mm AM|PM (12-hour clock) */
    public static String formatDateTime(long millis) {
        if (millis <= 0) {
            return "";
        }
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(millis));
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int hour12 = hour % 12 == 0 ? 12 : hour % 12;
        return formatDate(millis) + " " + hour12 + ":" + two(c.get(Calendar.MINUTE)) + (hour < 12 ? " AM" : " PM");
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : Integer.toString(v);
    }
}
