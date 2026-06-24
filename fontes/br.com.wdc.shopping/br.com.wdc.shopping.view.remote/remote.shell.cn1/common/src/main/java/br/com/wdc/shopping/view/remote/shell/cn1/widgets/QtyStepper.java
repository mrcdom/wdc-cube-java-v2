package br.com.wdc.shopping.view.remote.shell.cn1.widgets;

import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;

/**
 * Stepper de quantidade: {@code − [valor] +}. Widget não-reativo (build-once + {@link #setValue(int)}
 * guardado), no mesmo padrão do {@code CardHeader}. Os cliques são injetados no construtor
 * ({@code onDecrement}/{@code onIncrement}); o número exibido é mutado por {@code setValue}. Botões com
 * lado fixo em mm (densidade-independente, ver {@code util.Px}). UIIDs {@code QtyBtn}/{@code QtyValue}.
 */
public final class QtyStepper extends Container {

    private static final Sel sel = Sel.INSTANCE;

    /** Largura fixa (mm) do valor: a fonte negrito nativa mede menos que o glifo e o cortaria. */
    private static final float VALUE_W_MM = 11f;

    private Consumer<String> value;

    public QtyStepper(float buttonMm, Runnable onDecrement, Runnable onIncrement) {
        super(BoxLayout.x());
        getAllStyles().setBgTransparency(0); // Android: Container nativo é branco opaco
        Cn1Dom.render(this, (dom, me) -> {
            dom.add(new IconButton(sel.QTY_BTN, FontImage.MATERIAL_REMOVE, 3f, Px.mm(buttonMm), onDecrement), null);
            dom.label(l -> {
                l.setUIID(sel.QTY_VALUE);
                l.setPreferredW(Px.mm(VALUE_W_MM));
                this.value = Guard.text(l);
            });
            dom.add(new IconButton(sel.QTY_BTN, FontImage.MATERIAL_ADD, 3f, Px.mm(buttonMm), onIncrement), null);
        });
    }

    /** Atualiza o número exibido (guardado: só troca quando muda). */
    public void setValue(int qty) {
        value.accept(String.valueOf(qty));
    }
}
