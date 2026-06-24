package br.com.wdc.shopping.view.remote.shell.cn1.util;

import com.codename1.ui.Display;

/**
 * Conversão densidade-independente: <b>milímetros → pixels</b> do device. Tamanhos em px crus não
 * escalam — num iPhone de alta densidade um valor "150" vira ~8mm (minúsculo). Sempre dimensione em
 * mm e converta no momento do uso (o {@link Display} já está pronto em {@code build()}/{@code doUpdate()}).
 */
public final class Px {

    private Px() {
        // NOOP
    }

    /** Pixels equivalentes a {@code mm} milímetros na densidade do device. */
    public static int mm(float mm) {
        return Display.getInstance().convertToPixels(mm);
    }
}
