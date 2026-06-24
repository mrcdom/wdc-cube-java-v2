package br.com.wdc.shopping.view.remote.shell.cn1.util;

import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Label;

/**
 * Guardas de valor para widgets: a função devolve um <b>lambda</b> (um setter guardado) que só
 * aplica o valor — e invalida o layout — quando ele de fato muda. No CN1, {@code Label.setText}
 * sempre re-marca o pai para layout + repaint, mesmo com o mesmo valor, e o shell não-reativo
 * re-roda {@code doUpdate()} a cada push. O lambda é criado <b>junto do componente</b>; o
 * {@code doUpdate} só o invoca, sem conhecer a guarda.
 *
 * <pre>{@code
 * private Consumer<String> price;
 * ...
 * price = Guard.text(dom.label(l -> l.setUIID(sel.PRODUCT_PRICE_BADGE)));   // na criação
 * ...
 * price.accept(Money.format(valor));                                       // no doUpdate
 * }</pre>
 */
public final class Guard {

    private Guard() {
        // NOOP
    }
    
    /**
     * Mostra/esconde um component removendo-o do layout quando escondido (idioma CN1). Guardado: só
     * toca o estado quando ele de fato muda — {@code setHidden}/{@code setVisible} re-marcam o layout,
     * e o shell não-reativo re-roda {@code doUpdate()} a cada push (mesma motivação da {@code Guard.text}).
     */
    public static void visible(Component c, boolean v) {
        if (c.isVisible() == v && c.isHidden() == !v) {
            return;
        }
        c.setHidden(!v);
        c.setVisible(v);
    }

    public static Consumer<String> text(Label l) {
        return value -> {
            String cur = l.getText();
            if (cur == null ? value != null : !cur.equals(value)) {
                l.setText(value);
            }
        };
    }
}
