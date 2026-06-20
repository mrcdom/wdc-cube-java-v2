package br.com.wdc.shopping.view.remote.shell.codenameone.util;

import java.util.Calendar;
import java.util.Date;

/** Formatação de data (dd/MM/yyyy) — espelha o {@code DateUtils.formatDate} do shell React. */
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
        return two(day) + "/" + two(month) + "/" + year;
    }

    /** dd/MM/yyyy HH:mm */
    public static String formatDateTime(long millis) {
        if (millis <= 0) {
            return "";
        }
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(millis));
        return formatDate(millis) + " " + two(c.get(Calendar.HOUR_OF_DAY)) + ":" + two(c.get(Calendar.MINUTE));
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : Integer.toString(v);
    }
}
