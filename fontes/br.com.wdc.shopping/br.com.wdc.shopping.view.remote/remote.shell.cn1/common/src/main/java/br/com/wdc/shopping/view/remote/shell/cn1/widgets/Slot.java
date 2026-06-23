package br.com.wdc.shopping.view.remote.shell.cn1.widgets;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

/**
 * Container de <b>filho único</b>: acomoda um {@link Component} e só toca a árvore na transição.
 *
 * <p>
 * Detecta mudança por <b>comparação de referência</b>: como o app resolve um vsid sempre para a mesma
 * instância de view (e {@code view.getElement()} sempre o mesmo element), basta comparar a referência
 * do filho para saber se mudou. Por isso o widget é <b>genérico</b> — não conhece vsid nem o app; quem
 * chama resolve o {@code Component} (um {@code view.getElement()} ou um componente local) e o passa.
 * </p>
 */
public final class Slot extends Container {

    private Component current;

    public Slot() {
        super(new BorderLayout());
    }

    /** Garante que {@code child} seja o único filho ({@code null} limpa). Re-monta só quando a referência muda. */
    public void mount(Component child) {
        if (child == current) {
            return;
        }
        current = child;
        removeAll();
        if (child != null) {
            add(BorderLayout.CENTER, child);
        }
        revalidate();
    }
}
