package br.com.wdc.shopping.view.remote.shell.codenameone.util;

import com.codename1.ui.Button;
import com.codename1.ui.FontImage;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;

/** Widgets reutilizáveis do shell. */
public final class Widgets {

    private Widgets() {
        // NOOP
    }

    /**
     * Botão "voltar" neutro (slate, fonte leve) com seta à esquerda e fundo cinza arredondado no
     * clique — UIID {@code BackButton}. O simulador CN1 não expõe hover de mouse, então o realce
     * aparece no <i>pressed</i>.
     */
    public static Button backButton(String text, Runnable onClick) {
        Button b = new Button(text);
        b.setUIID("BackButton");
        FontImage.setMaterialIcon(b, FontImage.MATERIAL_ARROW_BACK, 3.5f);
        b.getAllStyles().setBorder(RoundRectBorder.create().cornerRadius(3f));
        b.getUnselectedStyle().setBgTransparency(0);
        for (Style s : new Style[] { b.getSelectedStyle(), b.getPressedStyle() }) {
            s.setBgColor(0xe5e7eb);
            s.setBgTransparency(255);
        }
        b.addActionListener(e -> onClick.run());
        return b;
    }
}
