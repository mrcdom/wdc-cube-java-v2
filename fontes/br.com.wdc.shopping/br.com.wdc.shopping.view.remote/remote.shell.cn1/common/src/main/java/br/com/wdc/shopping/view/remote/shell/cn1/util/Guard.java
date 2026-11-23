package br.com.wdc.shopping.view.remote.shell.cn1.util;

import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Label;

/**
 * Value guards for widgets: the function returns a <b>lambda</b> (a guarded setter) that only
 * applies the value — and invalidates the layout — when it actually changes. In CN1, {@code Label.setText}
 * always re-marks the parent for layout + repaint, even with the same value, and the non-reactive shell
 * re-runs {@code doUpdate()} on every push. The lambda is created <b>together with the component</b>;
 * {@code doUpdate} only invokes it, without knowing about the guard.
 *
 * <pre>{@code
 * private Consumer<String> price;
 * ...
 * price = Guard.text(dom.label(l -> l.setUIID(sel.PRODUCT_PRICE_BADGE)));   // at creation
 * ...
 * price.accept(Money.format(value));                                       // in doUpdate
 * }</pre>
 */
public final class Guard {

    private Guard() {
        // NOOP
    }
    
    /**
     * Shows/hides a component, removing it from the layout when hidden (CN1 idiom). Guarded: only
     * touches the state when it actually changes — {@code setHidden}/{@code setVisible} re-mark the layout,
     * and the non-reactive shell re-runs {@code doUpdate()} on every push (same motivation as {@code Guard.text}).

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
