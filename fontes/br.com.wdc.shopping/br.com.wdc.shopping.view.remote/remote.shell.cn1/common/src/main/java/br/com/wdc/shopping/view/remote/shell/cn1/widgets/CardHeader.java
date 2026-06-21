package br.com.wdc.shopping.view.remote.shell.cn1.widgets;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/**
 * Cabeçalho de card: ícone num box azul-claro + título e subtítulo. Componente composto (à la
 * {@code MultiButton} do CN1). UIIDs {@code CardHeaderRow}, {@code CardHeaderIconBox},
 * {@code CardHeaderTitle}, {@code CardHeaderSub}.
 */
public final class CardHeader extends Container {

    private static final Sel sel = Sel.INSTANCE;

    public CardHeader(char icon, String title, String subtitle) {
        super(BoxLayout.x());
        setUIID(sel.CARD_HEADER_ROW);

        Label iconBox = new Label();
        iconBox.setUIID(sel.CARD_HEADER_ICON_BOX);
        FontImage.setMaterialIcon(iconBox, icon, 4.5f);
        add(iconBox);

        Container col = new Container(BoxLayout.y());
        Label t = new Label(title);
        t.setUIID(sel.CARD_HEADER_TITLE);
        Label s = new Label(subtitle);
        s.setUIID(sel.CARD_HEADER_SUB);
        col.add(t);
        col.add(s);
        add(col);
    }
}
