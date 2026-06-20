package br.com.wdc.shopping.view.remote.shell.cn1.util;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Widgets reutilizáveis do shell. */
public final class Widgets {

    private static final Sel sel = Sel.INSTANCE;

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
        b.setUIID(sel.BACK_BUTTON);
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

    /**
     * Cabeçalho de card: ícone num box azul-claro + título e subtítulo. UIIDs {@code CardHeaderRow},
     * {@code CardHeaderIconBox}, {@code CardHeaderTitle}, {@code CardHeaderSub}.
     */
    public static Container cardHeader(char icon, String title, String subtitle) {
        Container row = new Container(BoxLayout.x());
        row.setUIID(sel.CARD_HEADER_ROW);

        Label iconBox = new Label();
        iconBox.setUIID(sel.CARD_HEADER_ICON_BOX);
        FontImage.setMaterialIcon(iconBox, icon, 4.5f);
        row.add(iconBox);

        Container col = new Container(BoxLayout.y());
        Label t = new Label(title);
        t.setUIID(sel.CARD_HEADER_TITLE);
        Label s = new Label(subtitle);
        s.setUIID(sel.CARD_HEADER_SUB);
        col.add(t);
        col.add(s);
        row.add(col);
        return row;
    }

    /** Botão de ícone com tamanho fixo (evita a altura mínima de toque do Button que o infla). */
    public static Button iconButton(String uiid, char icon, float iconMm, int sizePx, Runnable onClick) {
        Button b = new Button();
        b.setUIID(uiid);
        FontImage.setMaterialIcon(b, icon, iconMm);
        b.setPreferredSize(new Dimension(sizePx, sizePx));
        b.addActionListener(e -> onClick.run());
        return b;
    }
}
