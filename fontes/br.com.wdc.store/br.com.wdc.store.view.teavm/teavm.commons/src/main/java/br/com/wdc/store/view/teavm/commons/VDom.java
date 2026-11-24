package br.com.wdc.store.view.teavm.commons;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.xml.Node;

/**
 * Minimalist Virtual DOM engine for TeaVM.
 * <p>
 * Implements efficient diffing and patching: compares two {@link VNode} trees
 * and applies only the necessary changes to the real DOM.
 * <p>
 * Model inspired by React/Preact:
 * <ol>
 *   <li>The view describes the state as a tree of VNodes (declarative render)</li>
 *   <li>The VDom compares the new tree with the previous one</li>
 *   <li>Applies minimal patches to the real DOM</li>
 * </ol>
 *
 * Typical usage:
 * <pre>{@code
 * public class MyView {
 *     private VNode prev;
 *     private HTMLElement root;
 *
 *     public void doUpdate() {
 *         var next = render();
 *         prev = VDom.patch(root, prev, next);
 *     }
 *
 *     private VNode render() {
 *         return div("container").children(
 *             h5().text(state.title),
 *             el("input").attr("type","text").attr("value", state.name)
 *         );
 *     }
 * }
 * }</pre>
 */
public final class VDom {

    private VDom() {
    }

    /**
     * Diffs oldTree against newTree, patching the DOM under container.
     * The attributes of the root VNode are applied directly to the container (diffed).
     * The children of the root VNode are diffed as direct children of the container.
     *
     * @param container DOM element that holds the rendered tree
     * @param oldTree   previous tree (null on the first render)
     * @param newTree   new descriptive tree
     * @return newTree (use it as oldTree on the next call)
     */
    public static VNode patch(HTMLElement container, VNode oldTree, VNode newTree) {
        // Apply the root VNode's attributes to the container (with diffing)
        diffAttrs(container, oldTree != null ? oldTree.attrs : null, newTree.attrs);
        diffProps(container, oldTree != null ? oldTree.props : null, newTree.props);
        diffEvents(container, oldTree != null ? oldTree.events : null, newTree.events);

        // Diff the root VNode's children as direct children of the container
        diffChildren(container, oldTree != null ? oldTree.children : null, newTree.children);

        // Ref callback
        if (newTree.ref != null) {
            newTree.ref.accept(container);
        }

        return newTree;
    }

    // ---- DOM creation from VNode ----

    private static Node createDom(VNode vnode) {
        if (VNode.TEXT_TAG.equals(vnode.tag)) {
            return HTMLDocument.current().createTextNode(vnode.text);
        }

        // Slot: container div that hosts an external element (never diffs children)
        if (VNode.SLOT_TAG.equals(vnode.tag)) {
            var elm = HTMLDocument.current().createElement("div");
            if (vnode.attrs != null) {
                for (var entry : vnode.attrs.entrySet()) {
                    setAttr(elm, entry.getKey(), entry.getValue());
                }
            }
            if (vnode.ref != null) {
                vnode.ref.accept(elm);
            }
            return elm;
        }

        var elm = HTMLDocument.current().createElement(vnode.tag);

        // Attributes
        if (vnode.attrs != null) {
            for (var entry : vnode.attrs.entrySet()) {
                setAttr(elm, entry.getKey(), entry.getValue());
            }
        }

        // Properties (Web Components)
        if (vnode.props != null) {
            for (var entry : vnode.props.entrySet()) {
                setProp(elm, entry.getKey(), entry.getValue());
            }
        }

        // Events
        if (vnode.events != null) {
            for (var entry : vnode.events.entrySet()) {
                addEventListener(elm, entry.getKey(), entry.getValue());
            }
        }

        // Children
        if (vnode.children != null) {
            for (var child : vnode.children) {
                elm.appendChild(createDom(child));
            }
        }

        // Ref callback
        if (vnode.ref != null) {
            vnode.ref.accept(elm);
        }

        return elm;
    }

    // ---- Diffing ----

    private static void diff(HTMLElement parent, Node domNode, VNode oldNode, VNode newNode) {
        if (newNode == null) {
            // Node removed
            if (domNode != null) {
                parent.removeChild(domNode);
            }
            return;
        }

        if (oldNode == null) {
            // Node added
            parent.appendChild(createDom(newNode));
            return;
        }

        // Different tags → replace
        if (!Objects.equals(oldNode.tag, newNode.tag)) {
            var newDom = createDom(newNode);
            parent.replaceChild(newDom, domNode);
            return;
        }

        // Text node → update content
        if (VNode.TEXT_TAG.equals(newNode.tag)) {
            if (!Objects.equals(oldNode.text, newNode.text)) {
                domNode.setNodeValue(newNode.text);
            }
            return;
        }

        // Slot node — only diffs attrs and calls ref (which manages the hosted element)
        if (VNode.SLOT_TAG.equals(newNode.tag)) {
            var elm = (HTMLElement) domNode;
            diffAttrs(elm, oldNode.attrs, newNode.attrs);
            if (newNode.ref != null) {
                newNode.ref.accept(elm);
            }
            return;
        }

        // Same tag → diff attributes, events and children
        var elm = (HTMLElement) domNode;
        diffAttrs(elm, oldNode.attrs, newNode.attrs);
        diffProps(elm, oldNode.props, newNode.props);
        diffEvents(elm, oldNode.events, newNode.events);
        diffChildren(elm, oldNode.children, newNode.children);

        // Ref callback (updates the reference on every render)
        if (newNode.ref != null) {
            newNode.ref.accept(elm);
        }
    }

    // ---- Attribute diffing ----

    private static void diffAttrs(HTMLElement elm, Map<String, String> oldAttrs, Map<String, String> newAttrs) {
        var oldMap = oldAttrs != null ? oldAttrs : Collections.<String, String>emptyMap();
        var newMap = newAttrs != null ? newAttrs : Collections.<String, String>emptyMap();

        // Remove attributes that no longer exist
        for (var key : oldMap.keySet()) {
            if (!newMap.containsKey(key)) {
                removeAttr(elm, key);
            }
        }

        // Add/update attributes
        for (var entry : newMap.entrySet()) {
            var key = entry.getKey();
            var newVal = entry.getValue();
            var oldVal = oldMap.get(key);
            if (!Objects.equals(oldVal, newVal)) {
                setAttr(elm, key, newVal);
            }
        }
    }

    private static void setAttr(HTMLElement elm, String name, String value) {
        if (value == null) {
            elm.removeAttribute(name);
            return;
        }
        switch (name) {
            case "value":
                // DOM property (not attribute) — for inputs
                if (elm instanceof HTMLInputElement input) {
                    if (!Objects.equals(input.getValue(), value)) {
                        input.setValue(value);
                    }
                } else {
                    elm.setAttribute(name, value);
                }
                break;
            case "checked":
                if (elm instanceof HTMLInputElement input) {
                    input.setChecked("true".equals(value));
                } else {
                    elm.setAttribute(name, value);
                }
                break;
            case "class":
                elm.setAttribute("class", value);
                break;
            default:
                elm.setAttribute(name, value);
                break;
        }
    }

    private static void removeAttr(HTMLElement elm, String name) {
        switch (name) {
            case "value":
                if (elm instanceof HTMLInputElement input) {
                    input.setValue("");
                } else {
                    elm.removeAttribute(name);
                }
                break;
            case "checked":
                if (elm instanceof HTMLInputElement input) {
                    input.setChecked(false);
                } else {
                    elm.removeAttribute(name);
                }
                break;
            default:
                elm.removeAttribute(name);
                break;
        }
    }

    // ---- Property diffing (Web Components) ----

    private static void diffProps(HTMLElement elm, Map<String, Object> oldProps, Map<String, Object> newProps) {
        var oldMap = oldProps != null ? oldProps : Collections.<String, Object>emptyMap();
        var newMap = newProps != null ? newProps : Collections.<String, Object>emptyMap();

        // Remove properties that no longer exist
        for (var key : oldMap.keySet()) {
            if (!newMap.containsKey(key)) {
                setJsProp(elm, key, null);
            }
        }

        // Add/update properties
        for (var entry : newMap.entrySet()) {
            var key = entry.getKey();
            var newVal = entry.getValue();
            var oldVal = oldMap.get(key);
            if (!Objects.equals(oldVal, newVal)) {
                setProp(elm, key, newVal);
            }
        }
    }

    private static void setProp(HTMLElement elm, String name, Object value) {
        if (value == null) {
            setJsProp(elm, name, null);
        } else if (value instanceof String s) {
            setJsPropString(elm, name, s);
        } else if (value instanceof Boolean b) {
            setJsPropBool(elm, name, b);
        } else if (value instanceof Integer i) {
            setJsPropInt(elm, name, i);
        } else if (value instanceof Double d) {
            setJsPropDouble(elm, name, d);
        } else if (value instanceof JSObject js) {
            setJsProp(elm, name, js);
        }
    }

    @JSBody(params = { "el", "name", "val" }, script = "el[name] = val;")
    private static native void setJsProp(JSObject el, String name, JSObject val);

    @JSBody(params = { "el", "name", "val" }, script = "el[name] = val;")
    private static native void setJsPropString(JSObject el, String name, String val);

    @JSBody(params = { "el", "name", "val" }, script = "el[name] = val;")
    private static native void setJsPropBool(JSObject el, String name, boolean val);

    @JSBody(params = { "el", "name", "val" }, script = "el[name] = val;")
    private static native void setJsPropInt(JSObject el, String name, int val);

    @JSBody(params = { "el", "name", "val" }, script = "el[name] = val;")
    private static native void setJsPropDouble(JSObject el, String name, double val);

    // ---- Event diffing ----

    private static void diffEvents(HTMLElement elm,
            Map<String, EventListener<? extends Event>> oldEvents,
            Map<String, EventListener<? extends Event>> newEvents) {

        var oldMap = oldEvents != null ? oldEvents : Collections.<String, EventListener<? extends Event>>emptyMap();
        var newMap = newEvents != null ? newEvents : Collections.<String, EventListener<? extends Event>>emptyMap();

        // Remove events that no longer exist
        for (var entry : oldMap.entrySet()) {
            if (!newMap.containsKey(entry.getKey())) {
                removeEventListener(elm, entry.getKey(), entry.getValue());
            }
        }

        // Add/replace events
        for (var entry : newMap.entrySet()) {
            var type = entry.getKey();
            var newListener = entry.getValue();
            var oldListener = oldMap.get(type);
            if (oldListener != newListener) {
                if (oldListener != null) {
                    removeEventListener(elm, type, oldListener);
                }
                addEventListener(elm, type, newListener);
            }
        }
    }

    @SuppressWarnings({ "rawtypes" })
    private static void addEventListener(HTMLElement elm, String type, EventListener listener) {
        elm.addEventListener(type, listener);
    }

    @SuppressWarnings({ "rawtypes" })
    private static void removeEventListener(HTMLElement elm, String type, EventListener listener) {
        elm.removeEventListener(type, listener);
    }

    // ---- Children diffing ----

    private static void diffChildren(HTMLElement parent, List<VNode> oldChildren, List<VNode> newChildren) {
        var oldList = oldChildren != null ? oldChildren : Collections.<VNode>emptyList();
        var newList = newChildren != null ? newChildren : Collections.<VNode>emptyList();

        var oldLen = oldList.size();
        var newLen = newList.size();

        // If all new children have a key, use keyed reconciliation
        if (newLen > 0 && allKeyed(newList)) {
            diffKeyedChildren(parent, oldList, newList);
            return;
        }

        // Simple positional reconciliation
        var maxLen = Math.max(oldLen, newLen);
        var domChild = parent.getFirstChild();

        for (int i = 0; i < maxLen; i++) {
            var oldNode = i < oldLen ? oldList.get(i) : null;
            var newNode = i < newLen ? newList.get(i) : null;
            var nextDom = domChild != null ? domChild.getNextSibling() : null;

            diff(parent, domChild, oldNode, newNode);

            // Advance to the next DOM child
            domChild = nextDom;
        }
    }

    private static boolean allKeyed(List<VNode> nodes) {
        for (var n : nodes) {
            if (n.key == null) {
                return false;
            }
        }
        return true;
    }

    private static void diffKeyedChildren(HTMLElement parent, List<VNode> oldList, List<VNode> newList) {
        // Map of key → (oldVNode, domNode)
        Map<String, KeyedEntry> oldMap = new HashMap<>();
        var domChild = parent.getFirstChild();
        for (var oldNode : oldList) {
            oldMap.put(oldNode.key, new KeyedEntry(oldNode, domChild));
            domChild = domChild != null ? domChild.getNextSibling() : null;
        }

        // Process new children in order
        Node lastInserted = null;
        for (var newNode : newList) {
            var entry = oldMap.remove(newNode.key);
            if (entry != null) {
                // Exists in the old one: diff and reposition if needed
                diff(parent, entry.dom, entry.vnode, newNode);
                var expectedNext = lastInserted != null ? lastInserted.getNextSibling() : parent.getFirstChild();
                if (entry.dom != expectedNext) {
                    if (lastInserted != null) {
                        parent.insertBefore(entry.dom, lastInserted.getNextSibling());
                    } else {
                        parent.insertBefore(entry.dom, parent.getFirstChild());
                    }
                }
                lastInserted = entry.dom;
            } else {
                // New: create and insert
                var newDom = createDom(newNode);
                if (lastInserted != null) {
                    parent.insertBefore(newDom, lastInserted.getNextSibling());
                } else {
                    parent.insertBefore(newDom, parent.getFirstChild());
                }
                lastInserted = newDom;
            }
        }

        // Remove leftover old children
        for (var entry : oldMap.values()) {
            if (entry.dom != null && entry.dom.getParentNode() != null) {
                parent.removeChild(entry.dom);
            }
        }
    }

    private record KeyedEntry(VNode vnode, Node dom) {
    }
}
