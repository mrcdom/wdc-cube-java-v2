package br.com.wdc.shopping.view.remote.shell.codenameone.util;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.LayeredLayout;

/**
 * Torna um <i>card</i> (Container composto) clicável como um todo. No Codename One, um
 * {@code addPointerReleasedListener} no Container <b>não</b> dispara (o evento vai para a folha sob o
 * ponteiro); a forma correta é um <b>lead component</b>: um {@link Button} transparente sobreposto
 * (via {@link LayeredLayout}) que assume os eventos de toda a hierarquia.
 */
public final class Clickable {

    private Clickable() {
        // NOOP
    }

    /** Embrulha {@code content} num card de UIID {@code uiid} que dispara {@code onClick} ao toque. */
    public static Container card(String uiid, Component content, Runnable onClick) {
        Container card = new Container(new LayeredLayout());
        card.setUIID(uiid);
        card.add(content);

        Button lead = new Button();
        lead.setUIID("CardLead");
        card.add(lead);
        card.setLeadComponent(lead);
        lead.addActionListener(e -> onClick.run());

        return card;
    }
}
