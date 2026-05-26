package br.com.wdc.shopping.view.teavm.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.dom.html.HTMLInputElement;

/**
 * DSL builder for constructing HTML element trees declaratively.
 * Equivalent to GluonDom but targeting the browser DOM via TeaVM.
 */
public class HtmlDom {

    private HTMLElement currentParent;

    private HtmlDom(HTMLElement root) {
        this.currentParent = root;
    }

    public static <T extends HTMLElement> void render(T root, BiConsumer<HtmlDom, T> renderer) {
        var dom = new HtmlDom(root);
        renderer.accept(dom, root);
    }

    private HTMLElement createElement(String tag) {
        return HTMLDocument.current().createElement(tag);
    }

    private void applyCss(HTMLElement elm, String cssClasses) {
        if (cssClasses != null && !cssClasses.isEmpty()) {
            var classList = elm.getClassList();
            for (String cls : cssClasses.split(" ")) {
                if (!cls.isEmpty()) {
                    classList.add(cls);
                }
            }
        }
    }

    private void addChild(HTMLElement parent, HTMLElement child) {
        parent.appendChild(child);
    }

    // ---- Container methods (div-based layouts) ----

    public HTMLElement div(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = createElement("div");
            applyCss(elm, cssClasses);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public HTMLElement div(Consumer<HTMLElement> fnUpdate) {
        return div(null, fnUpdate);
    }

    public HTMLElement nav(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = createElement("nav");
            applyCss(elm, cssClasses);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public HTMLElement footer(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = createElement("footer");
            applyCss(elm, cssClasses);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    // ---- Leaf / control methods ----

    public HTMLElement span(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = createElement("span");
            applyCss(elm, cssClasses);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public HTMLElement label(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var elm = createElement("label");
        applyCss(elm, cssClasses);
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public HTMLElement p(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var elm = createElement("p");
        applyCss(elm, cssClasses);
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public HTMLElement h1(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var elm = createElement("h1");
        applyCss(elm, cssClasses);
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public HTMLElement h5(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var elm = createElement("h5");
        applyCss(elm, cssClasses);
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public HTMLElement h6(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var elm = createElement("h6");
        applyCss(elm, cssClasses);
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public HTMLButtonElement button(String cssClasses, Consumer<HTMLButtonElement> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = (HTMLButtonElement) createElement("button");
            elm.setType(HTMLButtonElement.TYPE_BUTTON);
            applyCss(elm, cssClasses);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public HTMLInputElement input(String type, String cssClasses, Consumer<HTMLInputElement> fnUpdate) {
        var elm = (HTMLInputElement) createElement("input");
        elm.setAttribute("type", type);
        applyCss(elm, cssClasses);
        fnUpdate.accept(elm);
        addChild(this.currentParent, (HTMLElement) elm);
        return elm;
    }

    public HTMLImageElement img(String cssClasses, Consumer<HTMLImageElement> fnUpdate) {
        var elm = (HTMLImageElement) createElement("img");
        applyCss(elm, cssClasses);
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public HTMLElement icon(String iconClass) {
        var elm = createElement("i");
        applyCss(elm, iconClass);
        addChild(this.currentParent, elm);
        return elm;
    }

    public HTMLElement table(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = createElement("table");
            applyCss(elm, cssClasses);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public HTMLElement thead(Consumer<HTMLElement> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = createElement("thead");
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public HTMLElement tbody(Consumer<HTMLElement> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = createElement("tbody");
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public HTMLElement tr(Consumer<HTMLElement> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = createElement("tr");
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public HTMLElement th(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var elm = createElement("th");
        applyCss(elm, cssClasses);
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public HTMLElement td(String cssClasses, Consumer<HTMLElement> fnUpdate) {
        var elm = createElement("td");
        applyCss(elm, cssClasses);
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    // ---- Utility: add an existing node ----

    public <T extends HTMLElement> T node(T node) {
        addChild(this.currentParent, node);
        return node;
    }

    // ---- Text node ----

    public void text(String content) {
        var textNode = HTMLDocument.current().createTextNode(content);
        this.currentParent.appendChild(textNode);
    }

    // ---- Passive event listener ----

    @JSBody(params = { "element", "type", "listener" }, script = ""
            + "element.addEventListener(type, listener, {passive: true});")
    public static native void addPassiveEventListener(HTMLElement element, String type,
            EventListener<?> listener);
}
