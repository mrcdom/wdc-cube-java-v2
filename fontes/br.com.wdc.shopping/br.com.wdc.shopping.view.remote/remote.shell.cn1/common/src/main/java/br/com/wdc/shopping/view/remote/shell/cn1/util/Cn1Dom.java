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

/**
 * DSL fluente de construção da árvore de componentes — análogo do {@code VaadinDom}/{@code GluonDom}
 * para o Codename One.
 *
 * <p>
 * Mantém um {@code currentParent}: cada método cria o componente, torna-o o parent corrente, executa
 * o {@link Consumer} (que o configura e aninha filhos via novas chamadas), adiciona-o ao parent
 * anterior e o retorna (para capturar a referência num campo da view). Os métodos têm sobrecarga com
 * {@code constraint} para layouts como {@link BorderLayout}.
 * </p>
 *
 * <p>
 * <b>Convenção:</b> dentro do lambda de configuração, {@code setUIID(...)} (ou a cor base via
 * {@code getAllStyles().setFgColor(...)}) deve ser a <b>primeira</b> ação. Helpers como
 * {@code FontImage.setMaterialIcon} "assam" o ícone a partir do estilo do componente, então só dão a
 * cor certa se rodarem <i>depois</i> do estilo definido — e assim tudo (criação, estilo, ícone) fica
 * agrupado no mesmo lambda, sem precisar capturar a referência só para decorar fora.
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

    private Container currentParent;

    private Cn1Dom(Container root) {
        this.currentParent = root;
    }

    /** Executa a DSL sobre um root já existente e o devolve (para {@code return Cn1Dom.render(...)}). */
    public static Container render(Container root, BiConsumer<Cn1Dom, Container> renderer) {
        renderer.accept(new Cn1Dom(root), root);
        return root;
    }

    /** Cria o root com o layout dado, executa a DSL e o devolve — sem {@code new Container} manual. */
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

    // :: Folhas

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

    /** Adiciona ao parent corrente um component já pronto (ex.: o element de uma sub-view). */
    public <T extends Component> T add(T component, Object constraint) {
        addChild(this.currentParent, component, constraint);
        return component;
    }

    private static void addChild(Container parent, Component child, Object constraint) {
        if (constraint != null) {
            parent.add(constraint, child);
        } else {
            parent.add(child);
        }
    }
}
