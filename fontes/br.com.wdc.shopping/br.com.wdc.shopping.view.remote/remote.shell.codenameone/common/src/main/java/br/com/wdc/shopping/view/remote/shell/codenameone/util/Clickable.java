package br.com.wdc.shopping.view.remote.shell.codenameone.util;

import com.codename1.ui.Button;
import com.codename1.ui.Container;

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

    /** Marca {@code content} (já com seus filhos visuais) como card de UIID {@code uiid} e clicável. */
    public static Container card(String uiid, Container content, Runnable onClick) {
        content.setUIID(uiid);

        Button lead = new Button();
        lead.setUIID("CardLead");
        lead.setPreferredH(0);
        content.add(lead);
        content.setLeadComponent(lead);
        lead.addActionListener(e -> onClick.run());

        return content;
    }
}
