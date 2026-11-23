package br.com.wdc.shopping.view.remote.shell.cn1.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

import br.com.wdc.shopping.view.remote.shell.cn1.widgets.CardHeader;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.Slot;

/**
 * Fluent DSL for building the component tree — the Codename One analog of
 * {@code VaadinDom}/{@code SwtDom}.
 *
 * <p>
 * Keeps a {@code currentParent}: each method creates the component, makes it the current parent, runs
 * the {@link Consumer} (which configures it and nests children via further calls), adds it to the
 * previous parent and returns it (to capture the reference in a view field). The methods have overloads
 * with a {@code constraint} for layouts such as {@link BorderLayout}.
 * </p>
 *
 * <p>
 * <b>Convention:</b> inside the configuration lambda, {@code setUIID(...)} (or the base color via
 * {@code getAllStyles().setFgColor(...)}) must be the <b>first</b> action. Helpers such as
 * {@code FontImage.setMaterialIcon} "bake" the icon from the component's style, so they only give the
 * right color if they run <i>after</i> the style is set — and so everything (creation, style, icon) stays
 * grouped in the same lambda, without having to capture the reference just to decorate outside.
 * </p>
 *
 * <pre>{@code
 * Cn1Dom.render(root, (dom, r) -> {
 *     dom.boxX(BorderLayout.NORTH, header -> {
 *         nick    = dom.label(l -> {});
 *         cartBtn = dom.button(b -> b.addActionListener(e -> submit(EVT_OPEN_CART)));
 *     });
 *     content = dom.border(BorderLayout.CENTER, c -> {});
 * });
 * }</pre>
 */
public final class Cn1Dom {
    
    /** Empty configurator: the container is a mount point, filled later in {@code doUpdate}. */
    public static final Consumer<Container> NO_CONTENT = ignored -> {};

    private Container currentParent;

    private Cn1Dom(Container root) {
        this.currentParent = root;
    }

    /** Runs the DSL over an existing root and returns it (for {@code return Cn1Dom.render(...)}). */
    public static Container render(Container root, BiConsumer<Cn1Dom, Container> renderer) {
        renderer.accept(new Cn1Dom(root), root);
        return root;
    }

    /** Creates the root with the given layout, runs the DSL and returns it — no manual {@code new Container}. */
    public static Container render(Layout layout, BiConsumer<Cn1Dom, Container> renderer) {
        return render(new Container(layout), renderer);
    }

    // :: Containers

    public Container boxY(Consumer<Container> fn) {
        return container(BoxLayout.y(), null, fn);
    }

    public Container boxY(Object constraint, Consumer<Container> fn) {
        return container(BoxLayout.y(), constraint, fn);
    }

    public Container boxX(Consumer<Container> fn) {
        return container(BoxLayout.x(), null, fn);
    }

    public Container boxX(Object constraint, Consumer<Container> fn) {
        return container(BoxLayout.x(), constraint, fn);
    }

    public Container border(Consumer<Container> fn) {
        return container(new BorderLayout(), null, fn);
    }

    public Container border(Object constraint, Consumer<Container> fn) {
        return container(new BorderLayout(), constraint, fn);
    }

    public Container container(Layout layout, Object constraint, Consumer<Container> fn) {
        Container old = this.currentParent;
        try {
            Container elm = new Container(layout);
            this.currentParent = elm;
            fn.accept(elm);
            addChild(old, elm, constraint);
            return elm;
        } finally {
            this.currentParent = old;
        }
    }

    // :: Leaves

    public Label label(Consumer<Label> fn) {
        return label(null, fn);
    }

    public Label label(Object constraint, Consumer<Label> fn) {
        Label e = new Label("");
        fn.accept(e);
        addChild(this.currentParent, e, constraint);
        return e;
    }

    public SpanLabel spanLabel(Consumer<SpanLabel> fn) {
        return spanLabel(null, fn);
    }

    public SpanLabel spanLabel(Object constraint, Consumer<SpanLabel> fn) {
        SpanLabel e = new SpanLabel("");
        fn.accept(e);
        addChild(this.currentParent, e, constraint);
        return e;
    }

    public Button button(Consumer<Button> fn) {
        return button(null, fn);
    }

    public Button button(Object constraint, Consumer<Button> fn) {
        Button e = new Button();
        fn.accept(e);
        addChild(this.currentParent, e, constraint);
        return e;
    }

    public TextField textField(Consumer<TextField> fn) {
        return textField(null, fn);
    }

    public TextField textField(Object constraint, Consumer<TextField> fn) {
        TextField e = new TextField();
        fn.accept(e);
        addChild(this.currentParent, e, constraint);
        return e;
    }
    
    public CardHeader cardHeader(Object constraint, Consumer<CardHeader> fn) {
        CardHeader e = new CardHeader();
        fn.accept(e);
        addChild(this.currentParent, e, constraint);
        return e;
    }

    public Slot slot() {
        return slot(null);
    }

    /** Creates a {@link Slot} (single child) and adds it to the current parent; the content comes later via {@code mount}. */
    public Slot slot(Object constraint) {
        Slot e = new Slot();
        addChild(this.currentParent, e, constraint);
        return e;
    }

    /** Adds a ready-made component to the current parent (e.g.: the element of a sub-view). */
    public <T extends Component> T add(T component, Object constraint) {
        addChild(this.currentParent, component, constraint);
        return component;
    }

    private static void addChild(Container parent, Component child, Object constraint) {
        // detaches from the previous parent: CN1 throws "already contained" in add() if the child already
        // has a parent (happens when reusing pieces in a layout rearrangement). For a new child it is a no-op.

        if (child.getParent() != null) {
            child.getParent().removeComponent(child);
        }
        if (constraint != null) {
            parent.add(constraint, child);
        } else {
            parent.add(child);
        }
    }
}
