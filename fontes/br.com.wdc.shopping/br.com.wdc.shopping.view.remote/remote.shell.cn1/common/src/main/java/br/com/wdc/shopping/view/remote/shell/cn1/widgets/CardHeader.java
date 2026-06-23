package br.com.wdc.shopping.view.remote.shell.cn1.widgets;

import com.codename1.compat.java.util.Objects;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;

/**
 * Cabeçalho de card: ícone num box azul-claro + título e subtítulo. Componente composto (à la {@code MultiButton} do
 * CN1). UIIDs {@code CardHeaderRow}, {@code CardHeaderIconBox}, {@code CardHeaderTitle}, {@code CardHeaderSub}.
 */
public final class CardHeader extends Container {

    private static final Sel sel = Sel.INSTANCE;

    private char curIcon;
    private float curIconSize;
    private Label iconBox;
    private Label titleBox;
    private Label subtitleBox;

    public CardHeader() {
        super(BoxLayout.x());
        Cn1Dom.render(this, (dom, me) -> {
            me.setUIID(sel.CARD_HEADER_ROW);

            dom.label(iconBox -> {
                iconBox.setUIID(sel.CARD_HEADER_ICON_BOX);
                this.iconBox = iconBox;
                curIcon = 0;
                curIconSize = 4.5f;
            });

            dom.container(BoxLayout.y(), null, col -> {
                dom.label(t -> {
                    t.setUIID(sel.CARD_HEADER_TITLE);
                    this.titleBox = t;
                });
                dom.label(s -> {
                    s.setUIID(sel.CARD_HEADER_SUB);
                    this.subtitleBox = s;
                });
            });
        });
    }

    public void setIcon(char icon) {
        if (icon != this.curIcon) {
            FontImage.setMaterialIcon(iconBox, icon, curIconSize);
            this.curIcon = icon;
        }
    }

    public void setIconSize(float iconSize) {
        if (iconSize != this.curIconSize) {
            FontImage.setMaterialIcon(iconBox, this.curIcon, iconSize);
            this.curIconSize = iconSize;
        }
    }

    public void setTitle(String value) {
        if (!Objects.equals(value, titleBox.getText())) {
            titleBox.setText(value);
        }
    }

    public void setSubtitle(String value) {
        if (!Objects.equals(value, subtitleBox.getText())) {
            subtitleBox.setText(value);
        }
    }
}
