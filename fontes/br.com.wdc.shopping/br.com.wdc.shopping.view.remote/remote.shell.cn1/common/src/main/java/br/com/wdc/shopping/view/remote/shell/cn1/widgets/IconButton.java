package br.com.wdc.shopping.view.remote.shell.cn1.widgets;

import com.codename1.ui.Button;
import com.codename1.ui.FontImage;
import com.codename1.ui.geom.Dimension;

/** Botão de ícone com tamanho fixo (evita a altura mínima de toque do Button que o infla). */
public final class IconButton extends Button {

    public IconButton(String uiid, char icon, float iconMm, int sizePx, Runnable onClick) {
        setUIID(uiid);
        FontImage.setMaterialIcon(this, icon, iconMm);
        setPreferredSize(new Dimension(sizePx, sizePx));
        addActionListener(e -> onClick.run());
    }
}
