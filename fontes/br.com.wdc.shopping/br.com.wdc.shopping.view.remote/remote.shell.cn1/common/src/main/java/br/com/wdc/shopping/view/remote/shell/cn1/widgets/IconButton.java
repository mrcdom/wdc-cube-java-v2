package br.com.wdc.shopping.view.remote.shell.cn1.widgets;

import com.codename1.ui.Button;
import com.codename1.ui.FontImage;
import com.codename1.ui.geom.Dimension;

/** Fixed-size icon button (avoids the Button minimum touch height that inflates it). */
public final class IconButton extends Button {

    public IconButton(String uiid, char icon, float iconMm, int sizePx, Runnable onClick) {
        setUIID(uiid);
        FontImage.setMaterialIcon(this, icon, iconMm);
        setPreferredSize(new Dimension(sizePx, sizePx));
        addActionListener(e -> onClick.run());
    }
}
