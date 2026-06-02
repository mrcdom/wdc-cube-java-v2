package br.com.wdc.shopping.view.swt.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import br.com.wdc.shopping.view.swt.components.AccentLine;
import br.com.wdc.shopping.view.swt.components.ActionButton;
import br.com.wdc.shopping.view.swt.components.CardHeader;
import br.com.wdc.shopping.view.swt.components.ErrorBanner;
import br.com.wdc.shopping.view.swt.components.IconButton;
import br.com.wdc.shopping.view.swt.components.PrimaryButton;
import br.com.wdc.shopping.view.swt.components.ScrolledPage;
import br.com.wdc.shopping.view.swt.components.Separator;
import br.com.wdc.shopping.view.swt.components.ShadowCard;

public final class SwtDom {

    private Composite currentParent;

    private SwtDom(Composite root) {
        this.currentParent = root;
    }

    public static <T extends Composite> void render(T root, BiConsumer<SwtDom, T> renderer) {
        var dom = new SwtDom(root);
        renderer.accept(dom, root);
    }

    // ========== CONTAINER BUILDERS ==========

    public Composite col(Consumer<Composite> fn) {
        return col(SWT.NONE, fn);
    }

    public Composite col(int style, Consumer<Composite> fn) {
        var oldParent = this.currentParent;
        try {
            var elm = new Composite(oldParent, style);
            var layout = new GridLayout(1, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.verticalSpacing = 0;
            elm.setLayout(layout);
            this.currentParent = elm;
            fn.accept(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Composite row(int cols, Consumer<Composite> fn) {
        return row(cols, SWT.NONE, fn);
    }

    public Composite row(int cols, int style, Consumer<Composite> fn) {
        var oldParent = this.currentParent;
        try {
            var elm = new Composite(oldParent, style);
            var layout = new GridLayout(cols, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = 0;
            elm.setLayout(layout);
            this.currentParent = elm;
            fn.accept(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public StackComposite stack(Consumer<StackComposite> fn) {
        var oldParent = this.currentParent;
        try {
            var elm = new StackComposite(oldParent);
            this.currentParent = elm;
            fn.accept(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Composite flow(Consumer<Composite> fn) {
        var oldParent = this.currentParent;
        try {
            var elm = new Composite(oldParent, SWT.NONE);
            var layout = new RowLayout(SWT.HORIZONTAL);
            layout.wrap = true;
            layout.spacing = 0;
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            elm.setLayout(layout);
            this.currentParent = elm;
            fn.accept(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public ShadowCard card(Consumer<ShadowCard> fn) {
        var oldParent = this.currentParent;
        try {
            var elm = new ShadowCard(oldParent);
            this.currentParent = elm;
            fn.accept(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public ShadowCard card(int marginWidth, int marginHeight, int verticalSpacing, Consumer<ShadowCard> fn) {
        var oldParent = this.currentParent;
        try {
            var elm = new ShadowCard(oldParent, marginWidth, marginHeight, verticalSpacing);
            this.currentParent = elm;
            fn.accept(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public SlotComposite slot(Composite offscreen, Consumer<SlotComposite> fn) {
        var elm = new SlotComposite(this.currentParent, offscreen);
        fn.accept(elm);
        return elm;
    }

    // ========== LEAF WIDGETS ==========

    public Label label(Consumer<Label> fn) {
        var elm = new Label(this.currentParent, SWT.NONE);
        fn.accept(elm);
        return elm;
    }

    public Label label(int style, Consumer<Label> fn) {
        var elm = new Label(this.currentParent, style);
        fn.accept(elm);
        return elm;
    }

    public Text text(int style, Consumer<Text> fn) {
        var elm = new Text(this.currentParent, style);
        fn.accept(elm);
        return elm;
    }

    public Button button(Consumer<Button> fn) {
        var elm = new Button(this.currentParent, SWT.PUSH);
        fn.accept(elm);
        return elm;
    }

    public Button button(int style, Consumer<Button> fn) {
        var elm = new Button(this.currentParent, style);
        fn.accept(elm);
        return elm;
    }

    public Canvas canvas(int style, Consumer<Canvas> fn) {
        var elm = new Canvas(this.currentParent, style);
        fn.accept(elm);
        return elm;
    }

    // ========== CUSTOM COMPONENTS ==========

    public ScrolledPage scrolledPage(Consumer<SwtDom> fn) {
        return scrolledPage(20, 20, 0, fn);
    }

    public ScrolledPage scrolledPage(int marginWidth, int marginHeight, int verticalSpacing, Consumer<SwtDom> fn) {
        var oldParent = this.currentParent;
        try {
            var page = new ScrolledPage(oldParent, marginWidth, marginHeight, verticalSpacing);
            this.currentParent = page.getContent();
            fn.accept(this);
            page.complete();
            return page;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public CardHeader cardHeader(String icon, String title, String subtitle) {
        return new CardHeader(this.currentParent, icon, title, subtitle);
    }

    public ErrorBanner errorBanner(int verticalIndent, Consumer<ErrorBanner> fn) {
        var elm = new ErrorBanner(this.currentParent, verticalIndent);
        fn.accept(elm);
        return elm;
    }

    public ErrorBanner errorBanner(int verticalIndent, boolean initiallyVisible, Consumer<ErrorBanner> fn) {
        var elm = new ErrorBanner(this.currentParent, verticalIndent, initiallyVisible);
        fn.accept(elm);
        return elm;
    }

    public PrimaryButton primaryButton(String icon, String text, Consumer<PrimaryButton> fn) {
        var elm = new PrimaryButton(this.currentParent, icon, text);
        fn.accept(elm);
        return elm;
    }

    public PrimaryButton primaryButton(String icon, String text, Color background, Consumer<PrimaryButton> fn) {
        var elm = new PrimaryButton(this.currentParent, icon, text, background);
        fn.accept(elm);
        return elm;
    }

    public ActionButton actionButton(String icon, String text, Color background, Consumer<ActionButton> fn) {
        var elm = new ActionButton(this.currentParent, icon, text, background);
        fn.accept(elm);
        return elm;
    }

    public IconButton iconButton(String icon, Color background, Consumer<IconButton> fn) {
        var elm = new IconButton(this.currentParent, icon, background);
        fn.accept(elm);
        return elm;
    }

    public IconButton iconButton(String icon, Color iconColor, Font iconFont, Color background,
            Consumer<IconButton> fn) {
        var elm = new IconButton(this.currentParent, icon, iconColor, iconFont, background);
        fn.accept(elm);
        return elm;
    }

    public Separator separator(int verticalIndent) {
        return new Separator(this.currentParent, verticalIndent);
    }

    public AccentLine accentLine(int height, int verticalIndent) {
        return new AccentLine(this.currentParent, height, verticalIndent);
    }

    public AccentLine accentLine(Color color, int height, int verticalIndent) {
        return new AccentLine(this.currentParent, color, height, verticalIndent);
    }

    // ========== SPACERS ==========

    public Label spacer(int height) {
        var elm = new Label(this.currentParent, SWT.NONE);
        var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.heightHint = height;
        elm.setLayoutData(gd);
        return elm;
    }

    public Label spacer(GridData gd) {
        var elm = new Label(this.currentParent, SWT.NONE);
        elm.setLayoutData(gd);
        return elm;
    }
}
