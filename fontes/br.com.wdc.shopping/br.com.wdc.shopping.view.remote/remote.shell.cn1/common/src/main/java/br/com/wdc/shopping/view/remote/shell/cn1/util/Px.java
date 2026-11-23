package br.com.wdc.shopping.view.remote.shell.cn1.util;

import com.codename1.ui.Display;

/**
 * Density-independent conversion: <b>millimeters → device pixels</b>. Raw px sizes do not
 * scale — on a high-density iPhone a value of "150" becomes ~8mm (tiny). Always size in
 * mm and convert at the time of use (the {@link Display} is already ready in {@code build()}/{@code doUpdate()}).
 */
public final class Px {

    private Px() {
        // NOOP
    }

    /** Pixels equivalent to {@code mm} millimeters at the device density.
 */
    public static int mm(float mm) {
        return Display.getInstance().convertToPixels(mm);
    }
}
