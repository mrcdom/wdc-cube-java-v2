package br.com.wdc.store.view.remote.shell.cn1.util;

/**
 * US dollar formatting (en-US): "$1,234.56" — "$" sign, "," thousands separator, "." decimal separator and
 * 2 decimal places (avoids floating-point noise). Done by hand because CN1 has no {@code java.text.NumberFormat}.
 */
public final class Money {

    private Money() {
        // NOOP
    }

    public static String format(double v) {
        long cents = Math.round(v * 100);
        String sign = cents < 0 ? "-" : "";
        cents = Math.abs(cents);
        long frac = cents % 100;
        return sign + "$" + group(cents / 100) + "." + (frac < 10 ? "0" + frac : "" + frac);
    }

    private static String group(long whole) {
        String digits = Long.toString(whole);
        StringBuilder sb = new StringBuilder();
        int lead = digits.length() % 3;
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (i - lead) % 3 == 0) {
                sb.append(',');
            }
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }
}
