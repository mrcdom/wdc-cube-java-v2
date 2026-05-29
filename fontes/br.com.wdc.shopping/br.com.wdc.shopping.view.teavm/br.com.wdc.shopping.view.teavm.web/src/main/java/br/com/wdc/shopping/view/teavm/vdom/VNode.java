package br.com.wdc.shopping.view.teavm.vdom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Nó virtual representando um elemento DOM ou texto.
 * Usado pelo {@link VDom} para diffing e patching eficiente.
 * <p>
 * Uso:
 * <pre>{@code
 * import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;
 *
 * var tree = el("div").cls("card shadow").children(
 *     el("h5").text("Título"),
 *     el("input").attr("type", "text").attr("value", name),
 *     el("button").cls("btn btn-primary").text("OK").on("click", e -> doSomething())
 * );
 * }</pre>
 */
public class VNode {

    static final String TEXT_TAG = "#text";
    static final String SLOT_TAG = "#slot";

    String tag;
    String text;
    String key;
    Map<String, String> attrs;
    Map<String, EventListener<? extends Event>> events;
    List<VNode> children;
    Consumer<HTMLElement> ref;

    private VNode() {
    }

    // ---- Factory methods ----

    /**
     * Cria um nó de elemento.
     */
    public static VNode el(String tag) {
        var n = new VNode();
        n.tag = tag;
        return n;
    }

    /**
     * Cria um nó de texto puro.
     */
    public static VNode textNode(String content) {
        var n = new VNode();
        n.tag = TEXT_TAG;
        n.text = content != null ? content : "";
        return n;
    }

    // ---- Fluent builder ----

    /**
     * Define classes CSS (separadas por espaço).
     */
    public VNode cls(String classes) {
        return attr("class", classes);
    }

    /**
     * Define um atributo.
     */
    public VNode attr(String name, String value) {
        if (this.attrs == null) {
            this.attrs = new LinkedHashMap<>();
        }
        if (value != null) {
            this.attrs.put(name, value);
        }
        return this;
    }

    /**
     * Define o atributo style.
     */
    public VNode style(String css) {
        return attr("style", css);
    }

    /**
     * Define uma key para reconciliação de listas.
     */
    public VNode key(String key) {
        this.key = key;
        return this;
    }

    /**
     * Define o texto como único filho (shortcut para um text node filho).
     */
    public VNode text(String content) {
        if (this.children == null) {
            this.children = new ArrayList<>(1);
        }
        this.children.add(VNode.textNode(content));
        return this;
    }

    /**
     * Registra um event listener.
     */
    public VNode on(String eventType, EventListener<? extends Event> listener) {
        if (this.events == null) {
            this.events = new LinkedHashMap<>();
        }
        this.events.put(eventType, listener);
        return this;
    }

    /**
     * Captura referência ao elemento DOM real (chamado após criação/patch).
     * Similar ao React useRef/callback ref.
     */
    @SuppressWarnings("unchecked")
    public <T extends HTMLElement> VNode ref(Consumer<T> refCallback) {
        this.ref = (Consumer<HTMLElement>) (Consumer<?>) refCallback;
        return this;
    }

    /**
     * Define os filhos.
     */
    public VNode children(VNode... nodes) {
        if (this.children == null) {
            this.children = new ArrayList<>(nodes.length);
        }
        for (var n : nodes) {
            if (n != null) {
                this.children.add(n);
            }
        }
        return this;
    }

    /**
     * Adiciona filhos de uma lista (útil para loops/listas dinâmicas).
     */
    public VNode children(List<VNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return this;
        }
        if (this.children == null) {
            this.children = new ArrayList<>(nodes.size());
        }
        for (var n : nodes) {
            if (n != null) {
                this.children.add(n);
            }
        }
        return this;
    }

    // ---- Convenience element factories ----

    public static VNode div(String classes) {
        return el("div").cls(classes);
    }

    public static VNode span(String classes) {
        return el("span").cls(classes);
    }

    public static VNode button(String classes) {
        return el("button").attr("type", "button").cls(classes);
    }

    public static VNode input(String type, String classes) {
        return el("input").attr("type", type).cls(classes);
    }

    public static VNode img(String classes) {
        return el("img").cls(classes);
    }

    public static VNode nav(String classes) {
        return el("nav").cls(classes);
    }

    public static VNode footer(String classes) {
        return el("footer").cls(classes);
    }

    public static VNode h5(String classes) {
        return el("h5").cls(classes);
    }

    public static VNode h6(String classes) {
        return el("h6").cls(classes);
    }

    public static VNode p(String classes) {
        return el("p").cls(classes);
    }

    public static VNode ul(String classes) {
        return el("ul").cls(classes);
    }

    public static VNode li(String classes) {
        return el("li").cls(classes);
    }

    public static VNode table(String classes) {
        return el("table").cls(classes);
    }

    public static VNode thead() {
        return el("thead");
    }

    public static VNode tbody() {
        return el("tbody");
    }

    public static VNode tr() {
        return el("tr");
    }

    public static VNode th(String classes) {
        return el("th").cls(classes);
    }

    public static VNode td(String classes) {
        return el("td").cls(classes);
    }

    // ---- Slot (hosts external element without diffing children) ----

    /**
     * Cria um nó slot que hospeda um elemento DOM externo.
     * O VDom cria/mantém um container div e o ref cuida de hospedar
     * o elemento — nunca diffa filhos.
     *
     * @param classes classes CSS do container div
     * @param hosted  elemento externo a hospedar (pode ser null)
     */
    public static VNode slot(String classes, HTMLElement hosted) {
        var n = new VNode();
        n.tag = SLOT_TAG;
        if (classes != null && !classes.isEmpty()) {
            n.attrs = new LinkedHashMap<>();
            n.attrs.put("class", classes);
        }
        n.ref = el -> {
            if (hosted != null) {
                if (el.getFirstChild() != hosted) {
                    el.clear();
                    el.appendChild(hosted);
                }
            } else if (el.getFirstChild() != null) {
                el.clear();
            }
        };
        return n;
    }

    /**
     * Cria um nó slot sem classes.
     */
    public static VNode slot(HTMLElement hosted) {
        return slot(null, hosted);
    }

    // ---- Conditional rendering ----

    /**
     * Retorna este nó se condition for true, senão null (ignorado pelo children()).
     */
    public VNode when(boolean condition) {
        return condition ? this : null;
    }
}
