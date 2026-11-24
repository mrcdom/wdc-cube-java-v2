package br.com.wdc.store.view.remote.shell.cn1.widgets;

import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.store.view.remote.shell.cn1.Sel;
import br.com.wdc.store.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.store.view.remote.shell.cn1.util.Guard;
import br.com.wdc.store.view.remote.shell.cn1.util.Px;

/**
 * Quantity stepper: {@code − [value] +}. Non-reactive widget (build-once + guarded {@link #setValue(int)}),
 * in the same pattern as {@code CardHeader}. The clicks are injected in the constructor
 * ({@code onDecrement}/{@code onIncrement}); the displayed number is mutated by {@code setValue}. Buttons
 * with a fixed side in mm (density-independent, see {@code util.Px}). UIIDs {@code QtyBtn}/{@code QtyValue}.
 */
public final class QtyStepper extends Container {

    private static final Sel sel = Sel.INSTANCE;

    /** Fixed width (mm) of the value: the native bold font measures less than the glyph and would cut it. */
    private static final float VALUE_W_MM = 11f;

    private Consumer<String> value;

    public QtyStepper(float buttonMm, Runnable onDecrement, Runnable onIncrement) {
        super(BoxLayout.x());
        getAllStyles().setBgTransparency(0); // Android: native Container is opaque white
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

    /** Updates the displayed number (guarded: only swaps when it changes).
 */
    public void setValue(int qty) {
        value.accept(String.valueOf(qty));
    }
}
