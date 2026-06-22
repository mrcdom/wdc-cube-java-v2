package br.com.wdc.shopping.view.remote.shell.cn1.util;

import java.util.function.Consumer;

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

    /** Guarda do {@code setText} de {@code l}: setter que só troca o texto quando difere do atual. */
    public static Consumer<String> text(Label l) {
        return value -> {
            String cur = l.getText();
            if (cur == null ? value != null : !cur.equals(value)) {
                l.setText(value);
            }
        };
    }
}
