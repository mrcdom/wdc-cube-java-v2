package br.com.wdc.shopping.view.remote.shell.cn1.util;

/** Formatação monetária com 2 casas (evita lixo de ponto flutuante). */
public final class Money {

    private Money() {
        // NOOP
    }

    public static String format(double v) {
        long cents = Math.round(v * 100);
        long whole = cents / 100;
        long frac = Math.abs(cents % 100);
        return "R$ " + whole + "." + (frac < 10 ? "0" + frac : "" + frac);
    }
}
