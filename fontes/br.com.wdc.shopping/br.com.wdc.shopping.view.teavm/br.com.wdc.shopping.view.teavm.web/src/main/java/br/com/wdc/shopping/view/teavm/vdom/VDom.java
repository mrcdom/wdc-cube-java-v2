package br.com.wdc.shopping.view.teavm.vdom;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.xml.Node;

/**
 * Motor de Virtual DOM minimalista para TeaVM.
 * <p>
 * Implementa diffing e patching eficiente: compara duas árvores {@link VNode}
 * e aplica apenas as mudanças necessárias no DOM real.
 * <p>
 * Modelo inspirado no React/Preact:
 * <ol>
 *   <li>A view descreve o estado como árvore de VNodes (render declarativo)</li>
 *   <li>O VDom compara a árvore nova com a anterior</li>
 *   <li>Aplica patches mínimos no DOM real</li>
 * </ol>
 *
 * Uso típico:
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
 *             h5("").text(state.title),
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
     * Aplica diff entre oldTree e newTree, patchando o DOM sob container.
     * Os atributos do VNode raiz são aplicados diretamente no container (diffados).
     * Os filhos do VNode raiz são diffados como filhos diretos do container.
     *
     * @param container elemento DOM que contém a árvore renderizada
     * @param oldTree   árvore anterior (null no primeiro render)
     * @param newTree   nova árvore descritiva
     * @return newTree (use como oldTree na próxima chamada)
     */
    public static VNode patch(HTMLElement container, VNode oldTree, VNode newTree) {
        // Aplicar atributos do VNode raiz no container (com diffing)
        diffAttrs(container, oldTree != null ? oldTree.attrs : null, newTree.attrs);
        diffEvents(container, oldTree != null ? oldTree.events : null, newTree.events);

        // Diffa os filhos do VNode raiz como filhos diretos do container
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

        // Slot: container div que hospeda elemento externo (nunca diffa filhos)
        if (VNode.SLOT_TAG.equals(vnode.tag)) {
            var elm = HTMLDocument.current().createElement("div");
            if (vnode.attrs != null) {
                for (var entry : vnode.attrs.entrySet()) {
                    setAttr(elm, entry.getKey(), entry.getValue());
                }
            }
            if (vnode.hostedElement != null) {
                elm.appendChild(vnode.hostedElement);
            }
            if (vnode.ref != null) {
                vnode.ref.accept(elm);
            }
            return elm;
        }

        var elm = HTMLDocument.current().createElement(vnode.tag);

        // Atributos
        if (vnode.attrs != null) {
            for (var entry : vnode.attrs.entrySet()) {
                setAttr(elm, entry.getKey(), entry.getValue());
            }
        }

        // Eventos
        if (vnode.events != null) {
            for (var entry : vnode.events.entrySet()) {
                addEventListener(elm, entry.getKey(), entry.getValue());
            }
        }

        // Filhos
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
            // Nó removido
            if (domNode != null) {
                parent.removeChild(domNode);
            }
            return;
        }

        if (oldNode == null) {
            // Nó adicionado
            parent.appendChild(createDom(newNode));
            return;
        }

        // Tags diferentes → substituir
        if (!Objects.equals(oldNode.tag, newNode.tag)) {
            var newDom = createDom(newNode);
            parent.replaceChild(newDom, domNode);
            return;
        }

        // Text node → atualizar conteúdo
        if (VNode.TEXT_TAG.equals(newNode.tag)) {
            if (!Objects.equals(oldNode.text, newNode.text)) {
                domNode.setNodeValue(newNode.text);
            }
            return;
        }

        // Slot node → apenas troca elemento hospedado (nunca diffa filhos)
        if (VNode.SLOT_TAG.equals(newNode.tag)) {
            var elm = (HTMLElement) domNode;
            diffAttrs(elm, oldNode.attrs, newNode.attrs);
            if (oldNode.hostedElement != newNode.hostedElement) {
                elm.clear();
                if (newNode.hostedElement != null) {
                    elm.appendChild(newNode.hostedElement);
                }
            }
            if (newNode.ref != null) {
                newNode.ref.accept(elm);
            }
            return;
        }

        // Mesmo tag → diff atributos, eventos e filhos
        var elm = (HTMLElement) domNode;
        diffAttrs(elm, oldNode.attrs, newNode.attrs);
        diffEvents(elm, oldNode.events, newNode.events);
        diffChildren(elm, oldNode.children, newNode.children);

        // Ref callback (atualiza referência a cada render)
        if (newNode.ref != null) {
            newNode.ref.accept(elm);
        }
    }

    // ---- Attribute diffing ----

    private static void diffAttrs(HTMLElement elm, Map<String, String> oldAttrs, Map<String, String> newAttrs) {
        var oldMap = oldAttrs != null ? oldAttrs : Collections.<String, String>emptyMap();
        var newMap = newAttrs != null ? newAttrs : Collections.<String, String>emptyMap();

        // Remover atributos que não existem mais
        for (var key : oldMap.keySet()) {
            if (!newMap.containsKey(key)) {
                removeAttr(elm, key);
            }
        }

        // Adicionar/atualizar atributos
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
                // Propriedade DOM (não atributo) — para inputs
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

    // ---- Event diffing ----

    private static void diffEvents(HTMLElement elm,
            Map<String, EventListener<? extends Event>> oldEvents,
            Map<String, EventListener<? extends Event>> newEvents) {

        var oldMap = oldEvents != null ? oldEvents : Collections.<String, EventListener<? extends Event>>emptyMap();
        var newMap = newEvents != null ? newEvents : Collections.<String, EventListener<? extends Event>>emptyMap();

        // Remover eventos que não existem mais
        for (var entry : oldMap.entrySet()) {
            if (!newMap.containsKey(entry.getKey())) {
                removeEventListener(elm, entry.getKey(), entry.getValue());
            }
        }

        // Adicionar/substituir eventos
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

        // Se todos os novos filhos têm key, usar reconciliação por key
        if (newLen > 0 && allKeyed(newList)) {
            diffKeyedChildren(parent, oldList, newList);
            return;
        }

        // Reconciliação posicional simples
        var maxLen = Math.max(oldLen, newLen);
        var domChild = parent.getFirstChild();

        for (int i = 0; i < maxLen; i++) {
            var oldNode = i < oldLen ? oldList.get(i) : null;
            var newNode = i < newLen ? newList.get(i) : null;
            var nextDom = domChild != null ? domChild.getNextSibling() : null;

            diff(parent, domChild, oldNode, newNode);

            // Avançar para o próximo filho DOM
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
        // Mapa de key → (oldVNode, domNode)
        Map<String, KeyedEntry> oldMap = new HashMap<>();
        var domChild = parent.getFirstChild();
        for (var oldNode : oldList) {
            oldMap.put(oldNode.key, new KeyedEntry(oldNode, domChild));
            domChild = domChild != null ? domChild.getNextSibling() : null;
        }

        // Processar novos filhos na ordem
        Node lastInserted = null;
        for (var newNode : newList) {
            var entry = oldMap.remove(newNode.key);
            if (entry != null) {
                // Existe no antigo: diff e reposicionar se necessário
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
                // Novo: criar e inserir
                var newDom = createDom(newNode);
                if (lastInserted != null) {
                    parent.insertBefore(newDom, lastInserted.getNextSibling());
                } else {
                    parent.insertBefore(newDom, parent.getFirstChild());
                }
                lastInserted = newDom;
            }
        }

        // Remover filhos antigos que sobraram
        for (var entry : oldMap.values()) {
            if (entry.dom != null && entry.dom.getParentNode() != null) {
                parent.removeChild(entry.dom);
            }
        }
    }

    private record KeyedEntry(VNode vnode, Node dom) {
    }
}
