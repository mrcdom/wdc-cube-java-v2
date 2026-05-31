package br.com.wdc.shopping.view.teavm.util;

/**
 * @deprecated Use {@link br.com.wdc.shopping.view.teavm.commons.DateUtils} instead.
 */
@Deprecated
public final class DateUtils {

    private DateUtils() {
    }

    public static String formatDateTime(long millis) {
        return br.com.wdc.shopping.view.teavm.commons.DateUtils.formatDateTime(millis);
    }

    public static String formatDate(long millis) {
        return br.com.wdc.shopping.view.teavm.commons.DateUtils.formatDate(millis);
    }
}

