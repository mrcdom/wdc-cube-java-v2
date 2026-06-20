package br.com.wdc.shopping.view.remote.shell.codenameone.util;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Rectangle;

/**
 * Decoração dos painéis azuis (hero/banner do login): pinta o gradiente azul e os <b>círculos
 * decorativos</b> translúcidos do design React (branco com alpha baixo, parcialmente fora das
 * bordas, recortados pelos limites do painel).
 *
 * <p>Feito via {@code bgPainter} (o painter substitui o fundo do estilo, então desenhamos também o
 * gradiente). Classe anônima de {@link Painter}, não lambda (o CN1 tropeça em lambdas armazenadas).</p>
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

                // gradiente azul (vertical — aproxima o 160deg do React)
                g.fillLinearGradient(0x0d66d0, 0x4da6ff, x, y, w, h, false);

                // círculos brancos translúcidos, parcialmente fora das bordas (recortados)
                int oldAlpha = g.getAlpha();
                g.setColor(0xffffff);
                int base = Math.min(w, h);

                int d1 = base * 6 / 10; // grande — canto superior direito
                g.setAlpha(16);
                g.fillArc(x + w - (d1 * 7 / 10), y - (d1 * 3 / 10), d1, d1, 0, 360);

                int d2 = base * 5 / 10; // médio — canto inferior esquerdo
                g.setAlpha(10);
                g.fillArc(x - (d2 * 3 / 10), y + h - (d2 * 7 / 10), d2, d2, 0, 360);

                int d3 = base * 25 / 100; // pequeno — meio-esquerda
                g.setAlpha(13);
                g.fillArc(x + (w / 5), y + (h * 2 / 5), d3, d3, 0, 360);

                g.setAlpha(oldAlpha);
            }
        });
    }
}
