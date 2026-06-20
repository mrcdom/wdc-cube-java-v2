package br.com.wdc.shopping.view.remote.shell.cn1.util;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.plaf.Style;

/**
 * Torna um <i>card</i> (Container composto) clicável como um todo. No Codename One, um
 * {@code addPointerReleasedListener} no Container <b>não</b> dispara (o evento vai para a folha sob o
 * ponteiro); a forma correta é um <b>lead component</b>: um {@link Button} (sem tamanho/transparente)
 * adicionado à hierarquia que assume os eventos e os estados (pressed/selected → hover) de todo o
 * card. Diferente de uma sobreposição, o lead não cobre o conteúdo — apenas captura os eventos.
 */
public final class Clickable {

    private Clickable() {
        // NOOP
    }

    /** Realce de hover/clique (azul-claro do design). */
    private static final int HOVER_BG = 0xe8f1fc;

    /** Marca {@code content} (já com seus filhos visuais) como card de UIID {@code uiid} e clicável. */
    public static Container card(String uiid, Container content, Runnable onClick) {
        content.setUIID(uiid);
        applyHover(content);

        Button lead = new Button();
        lead.setUIID("CardLead");
        lead.setPreferredH(0);
        content.add(lead);
        content.setLeadComponent(lead);
        lead.addActionListener(e -> onClick.run());

        return content;
    }

    /**
     * Define o realce dos estados <i>selected</i> (hover no desktop) e <i>pressed</i> (toque) em Java
     * — o CSS do CN1 não suporta pseudo-classes, então o card adota essas cores quando o lead component
     * está em foco/pressionado.
     */
    private static void applyHover(Container card) {
        for (Style s : new Style[] { card.getSelectedStyle(), card.getPressedStyle() }) {
            s.setBgColor(HOVER_BG);
            s.setBgTransparency(255);
            s.setBorder(card.getUnselectedStyle().getBorder()); // mantém a mesma borda do estado normal
        }
    }
}
