package br.com.wdc.shopping.view.remote.shell.cn1.widgets;

import com.codename1.ui.Button;
import com.codename1.ui.FontImage;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/**
 * Botão "voltar" neutro (slate, fonte leve) com seta à esquerda e fundo cinza arredondado no clique
 * — UIID {@code BackButton}. O simulador CN1 não expõe hover de mouse, então o realce aparece no
 * <i>pressed</i>.
 */
public final class BackButton extends Button {

    private static final Sel sel = Sel.INSTANCE;

    public BackButton(String text, Runnable onClick) {
        super(text);
        setUIID(sel.BACK_BUTTON);
        FontImage.setMaterialIcon(this, FontImage.MATERIAL_ARROW_BACK, 3.5f);
        getAllStyles().setBorder(RoundRectBorder.create().cornerRadius(3f));
        getUnselectedStyle().setBgTransparency(0);
        for (Style s : new Style[] { getSelectedStyle(), getPressedStyle() }) {
            s.setBgColor(0xe5e7eb);
            s.setBgTransparency(255);
        }
        addActionListener(e -> onClick.run());
    }
}
