package br.com.wdc.store.view.teavm.commons;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Virtual node representing a DOM element or text. Used by {@link VDom} for efficient diffing and patching.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * import static br.com.wdc.store.view.teavm.commons.VNode.*;
 *
 * var tree = el("div").cls("card shadow").children(
 *     el("h5").text("Title"),
 *     el("input").attr("type", "text").attr("value", name),
 *     el("button").cls("btn btn-primary").text("OK").on("click", e -> doSomething())
 * );
 * }</pre>
 */
public class VNode {

    static final String TEXT_TAG = "#text";
    static final String SLOT_TAG = "#slot";
    
    public static String clsx(String... classNames) {
        return Stream.of(classNames).collect(Collectors.joining(" "));
    }

    String tag;
    String text;
    String key;
    Map<String, String> attrs;
    Map<String, Object> props;
    Map<String, EventListener<? extends Event>> events;
    List<VNode> children;
    Consumer<HTMLElement> ref;

    private VNode() {
    }

    // ---- Factory methods ----

    /**
     * Creates an element node.
     */
    public static VNode el(String tag) {
        var n = new VNode();
        n.tag = tag;
        return n;
    }

    /**
     * Creates a plain text node.
     */
    public static VNode textNode(String content) {
        var n = new VNode();
        n.tag = TEXT_TAG;
        n.text = content != null ? content : "";
        return n;
    }

    // ---- Fluent builder ----

    /**
     * Sets CSS classes (space-separated).
     */
    public VNode cls(String classes) {
        return attr("class", classes);
    }

    /**
     * Sets an attribute.
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
     * Sets the style attribute.
     */
    public VNode style(String css) {
        return attr("style", css);
    }

    /**
     * Sets a boolean attribute (presence/absence in the DOM). Web Components use this pattern:
     * {@code <sp-button disabled>}.
     */
    public VNode boolAttr(String name, boolean present) {
        if (present) {
            return attr(name, "");
        }
        // Not added → diffAttrs removes it if it existed before
        return this;
    }

    /**
     * Sets a JavaScript property on the element (not an HTML attribute). Used for Web Components that take complex
     * data through a property. Supported values: String, Boolean, Integer, Double, JSObject.
     */
    public VNode prop(String name, Object value) {
        if (this.props == null) {
            this.props = new LinkedHashMap<>();
        }
        this.props.put(name, value);
        return this;
    }

    /**
     * Sets a key for list reconciliation.
     */
    public VNode key(String key) {
        this.key = key;
        return this;
    }

    /**
     * Sets the text as the only child (shortcut for a child text node).
     */
    public VNode text(String content) {
        if (this.children == null) {
            this.children = new ArrayList<>(1);
        }
        this.children.add(VNode.textNode(content));
        return this;
    }

    /**
     * Registers an event listener.
     */
    public VNode on(String eventType, EventListener<? extends Event> listener) {
        if (this.events == null) {
            this.events = new LinkedHashMap<>();
        }
        this.events.put(eventType, listener);
        return this;
    }

    /**
     * Captures a reference to the real DOM element (called after creation/patch). Similar to React useRef/callback ref.
     */
    @SuppressWarnings("unchecked")
    public <T extends HTMLElement> VNode ref(Consumer<T> refCallback) {
        this.ref = (Consumer<HTMLElement>) (Consumer<?>) refCallback;
        return this;
    }

    /**
     * Sets the children.
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
     * Adds children from a list (useful for loops/dynamic lists).
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

    public static VNode div() {
        return el("div");
    }

    public static VNode div(String classes) {
        return el("div").cls(classes);
    }

    public static VNode span() {
        return el("span");
    }

    public static VNode span(String classes) {
        return el("span").cls(classes);
    }

    public static VNode button() {
        return el("button").attr("type", "button");
    }

    public static VNode button(String classes) {
        return el("button").cls(classes).attr("type", "button");
    }

    public static VNode input(String type) {
        return el("input").attr("type", type);
    }

    public static VNode img() {
        return el("img");
    }

    public static VNode nav() {
        return el("nav");
    }

    public static VNode nav(String classes) {
        return el("nav").cls(classes);
    }

    public static VNode footer() {
        return el("footer");
    }

    public static VNode footer(String classe) {
        return el("footer").cls(classe);
    }

    public static VNode h5() {
        return el("h5");
    }

    public static VNode h5(String classe) {
        return el("h5").cls(classe);
    }

    public static VNode h6() {
        return el("h6");
    }

    public static VNode h6(String classes) {
        return el("h6").cls(classes);
    }

    public static VNode p() {
        return el("p");
    }

    public static VNode p(String classes) {
        return el("p").cls(classes);
    }

    public static VNode ul() {
        return el("ul");
    }

    public static VNode ul(String classes) {
        return el("ul").cls(classes);
    }

    public static VNode li() {
        return el("li");
    }

    public static VNode li(String classes) {
        return el("li").cls(classes);
    }

    public static VNode table() {
        return el("table");
    }

    public static VNode table(String classes) {
        return el("table").cls(classes);
    }

    public static VNode thead() {
        return el("thead");
    }

    public static VNode thead(String classes) {
        return el("thead").cls(classes);
    }

    public static VNode tbody() {
        return el("tbody");
    }

    public static VNode tbody(String classes) {
        return el("tbody").cls(classes);
    }

    public static VNode tr() {
        return el("tr");
    }

    public static VNode tr(String classes) {
        return el("tr").cls(classes);
    }

    public static VNode th() {
        return el("th");
    }

    public static VNode th(String classes) {
        return el("th").cls(classes);
    }

    public static VNode td() {
        return el("td");
    }

    public static VNode td(String classes) {
        return el("td").cls(classes);
    }

    // ---- Slot (hosts external element without diffing children) ----

    /**
     * Creates a slot node that hosts an external DOM element. The VDom creates/keeps a container div and the ref takes
     * care of hosting the element — it never diffs children.
     *
     * @param hosted external element to host (may be null)
     */
    public static VNode slot(HTMLElement hosted) {
        var n = new VNode();
        n.tag = SLOT_TAG;
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

    public static VNode slot(String classes, HTMLElement hosted) {
        return slot(hosted).cls(classes);
    }

    // ---- Conditional rendering ----

    /**
     * Returns this node if condition is true, otherwise null (ignored by children()).
     */
    public VNode when(boolean condition) {
        return condition ? this : null;
    }
}
