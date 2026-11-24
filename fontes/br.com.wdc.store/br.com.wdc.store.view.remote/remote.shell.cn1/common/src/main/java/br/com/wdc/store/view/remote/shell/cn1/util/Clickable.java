package br.com.wdc.store.view.remote.shell.cn1.util;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.plaf.Style;

import br.com.wdc.store.view.remote.shell.cn1.Sel;
import br.com.wdc.store.view.remote.shell.cn1.theme.Colors;

/**
 * Makes a <i>card</i> (composite Container) clickable as a whole. In Codename One, an
 * {@code addPointerReleasedListener} on the Container does <b>not</b> fire (the event goes to the leaf under
 * the pointer); the right way is a <b>lead component</b>: a {@link Button} (sizeless/transparent)
 * added to the hierarchy that takes over the events and states (pressed/selected → hover) of the whole
 * card. Unlike an overlay, the lead does not cover the content — it only captures the events.
 */
public final class Clickable {

    private static final Sel sel = Sel.INSTANCE;

    private Clickable() {
        // NOOP
    }

    /** Hover/click highlight (the design's light blue). */
    private static final int HOVER_BG = Colors.ACCENT_LIGHT;

    /** Marks {@code content} (already with its visual children) as a card with UIID {@code uiid} and clickable. */
    public static Container card(String uiid, Container content, Runnable onClick) {
        content.setUIID(uiid);
        applyHover(content);

        Button lead = new Button();
        lead.setUIID(sel.CARD_LEAD);
        lead.setPreferredH(0);
        content.add(lead);
        content.setLeadComponent(lead);
        lead.addActionListener(e -> onClick.run());

        return content;
    }

    /**
     * Sets the highlight of the <i>selected</i> (hover on desktop) and <i>pressed</i> (touch) states in Java
     * — CN1's CSS does not support pseudo-classes, so the card takes these colors when the lead component
     * is focused/pressed.
     */
    private static void applyHover(Container card) {
        for (Style s : new Style[] { card.getSelectedStyle(), card.getPressedStyle() }) {
            s.setBgColor(HOVER_BG);
            s.setBgTransparency(255);
            s.setBorder(card.getUnselectedStyle().getBorder()); // keeps the same border as the normal state

        }
    }
}
