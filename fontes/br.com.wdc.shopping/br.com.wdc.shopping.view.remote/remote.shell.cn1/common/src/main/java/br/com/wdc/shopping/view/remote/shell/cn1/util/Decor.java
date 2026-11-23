package br.com.wdc.shopping.view.remote.shell.cn1.util;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Rectangle;

import br.com.wdc.shopping.view.remote.shell.cn1.theme.Colors;

/**
 * Decoration of the blue panels (login hero/banner): paints the blue gradient and the translucent
 * <b>decorative circles</b> of the React design (white with low alpha, partly outside the
 * edges, clipped by the panel bounds).
 *
 * <p>Done via {@code bgPainter} (the painter replaces the style's background, so we also draw the
 * gradient). An anonymous {@link Painter} class, not a lambda (CN1 trips over stored lambdas).</p>
 */
public final class Decor {

    private Decor() {
        // NOOP
    }

    public static void blueWithCircles(Component panel) {
        panel.getAllStyles().setBgPainter(new Painter() {
            @Override
            public void paint(Graphics g, Rectangle rect) {
                int x = rect.getX();
                int y = rect.getY();
                int w = rect.getWidth();
                int h = rect.getHeight();

                // blue gradient (vertical — approximates React's 160deg)
                g.fillLinearGradient(Colors.ACCENT, Colors.ACCENT_3, x, y, w, h, false);

                // translucent white circles, partly outside the edges (clipped)
                int oldAlpha = g.getAlpha();
                g.setColor(Colors.SURFACE);
                int base = Math.min(w, h);

                int d1 = base * 6 / 10; // large — top-right corner
                g.setAlpha(16);
                g.fillArc(x + w - (d1 * 7 / 10), y - (d1 * 3 / 10), d1, d1, 0, 360);

                int d2 = base * 5 / 10; // medium — bottom-left corner
                g.setAlpha(10);
                g.fillArc(x - (d2 * 3 / 10), y + h - (d2 * 7 / 10), d2, d2, 0, 360);

                int d3 = base * 25 / 100; // small — middle-left

                g.setAlpha(13);
                g.fillArc(x + (w / 5), y + (h * 2 / 5), d3, d3, 0, 360);

                g.setAlpha(oldAlpha);
            }
        });
    }
}
