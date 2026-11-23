# Guia: Como Programar uma View para app.teavm (teavm.web)

Este documento explica como criar uma nova view no módulo TeaVM Web — a implementação **client-side** (SPA) que acessa diretamente os Presenters em memória. Usaremos como exemplo uma view fictícia **"Wishlist"**.

## Visão Geral

Uma view neste projeto:

1. Recebe a aplicação e o **Scope** que desenha no construtor (acesso em memória) — nunca o Presenter
2. Lê os campos do Scope para obter dados
3. Renderiza uma árvore virtual (`VNode`) de forma declarativa
4. Dispara as ações do Scope (ligadas pelo presenter) via `safeAction()` em resposta a interações

Diferente do `remote.shell.teavm` (que recebe estado via WebSocket/JSON), aqui a view **acessa o escopo diretamente** — é um SPA puro.

## Pré-requisitos

O Presenter correspondente já deve existir na camada `presentation`, com:

- Um `Scope` público com os campos necessários (`Prop`s)
- As ações (`onXxx`) como campos do Scope, ligadas no construtor do presenter

## Anatomia de uma View

```java
package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import java.util.List;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.framework.vdom.*;
import br.com.wdc.shopping.presentation.presenter.restricted.wishlist.WishlistPresenter.WishlistScope;
import br.com.wdc.shopping.presentation.presenter.restricted.wishlist.structs.WishlistItem;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;

public class WishlistViewVDom extends AbstractVDomView<WishlistScope> {

    // 1. Interface Css — constantes de classe CSS (compact pattern)
    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Css {
        CssUtility u = CssUtility.INSTANCE;
        CssComponents c = CssComponents.INSTANCE;
        CssIcons icon = CssIcons.INSTANCE;

        String ROOT = u.PAGE_SCROLL_ROOT;
        String WRAPPER = u.PAGE_WRAPPER;
        String HIDDEN = u.HIDDEN;
        String TITLE = "wishlist-title";
        String ITEM_ROW = "wishlist-item-row";
        String ITEM_NAME = "wishlist-item-name";
        String REMOVE_ICON = clsx(icon.X_LG, "wishlist-remove-icon");
        String ERROR_VISIBLE = clsx(c.ALERT_ERROR, u.MB_12);
        String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);
        String ERROR_TEXT = c.ALERT_ERROR_TEXT;
    }

    // 2. Stable listeners (o escopo que a view desenha é o campo state, da base) — campos final para listeners sem parâmetros
    private final EventListener<Event> onBack = evt -> safeAction("Back", this.state.onOpenProducts::run);

    // 3. Factory methods para listeners paramétricos
    private EventListener<Event> mkOnRemove(long id) {
        return evt -> safeAction("Remove", () -> this.state.onRemoveItem.run(id));
    }

    // 4. Construtor — recebe a aplicação e o escopo (a fábrica registrada no ScopeViews)
    public WishlistViewVDom(ShoppingTeaVMApplication app, WishlistScope state) {
        super("wishlist", app, state);
    }

    // 5. render() — retorna a árvore virtual baseada no Scope
    @Override
    protected VNode render() {
        // Consumir erro one-shot
        final boolean showError;
        final String errorMessage;
        if (this.state.errorCode.get() != 0) {
            showError = true;
            errorMessage = this.state.errorMessage.get();
            // Consumido durante o desenho: um redesenho agora levaria a mensagem embora
            this.state.errorCode.setSilently(0);
            this.state.errorMessage.setSilently(null);
        } else {
            showError = false;
            errorMessage = "";
        }

        var items = this.state.items.get();
        var empty = items == null || items.isEmpty();

        // @formatter:off
        return div(Css.ROOT).children(
          div(Css.WRAPPER).children(
            h5(Css.TITLE).text("Minha Lista de Desejos"),
            // Error
            div(showError ? Css.ERROR_VISIBLE : Css.HIDDEN).children(
              span(Css.ERROR_ICON),
              span(Css.ERROR_TEXT).text(errorMessage)),
            // Items
            div(empty ? Css.HIDDEN : "").children(
              items != null
                ? items.stream().map(this::renderItem).toList()
                : List.of()),
            // Back button
            spActionButton()
              .children(span(CssIcons.ARROW_LEFT), span().text(" Voltar"))
              .on("click", onBack)));
        // @formatter:on
    }

    // 6. Métodos de render auxiliares
    private VNode renderItem(WishlistItem item) {
        var key = String.valueOf(item.id);

        // @formatter:off
        return div(Css.ITEM_ROW).key(key).children(
          span(Css.ITEM_NAME).text(item.name),
          spActionButton("s")
            .children(span(Css.REMOVE_ICON))
            .on("click", useCallback("remove-" + key, mkOnRemove(item.id))));
        // @formatter:on
    }
}
```

---

## Passo a Passo

### Passo 1 — Criar a classe da View

Crie o arquivo em:
```
teavm.web/src/main/java/br/com/wdc/shopping/view/teavm/views/WishlistViewVDom.java
```

### Passo 2 — Registrar a view no catálogo

No `ShoppingTeaVMApplication.java`, acrescente a view ao `SCOPE_VIEWS` (entregue à aplicação com `setScopeViews` no construtor):

```java
private static final ScopeViews SCOPE_VIEWS = new ScopeViews()
        // ... views existentes ...
        .register(WishlistScope.class, WishlistViewVDom::new);
```

Isso conecta o escopo à view: o presenter só entrega o escopo ao pai (`ownerSlot.setScope(state)`), e a view do pai pede a deste escopo com `app.viewOf(state.content.get())` — a aplicação a cria na primeira vez, com a fábrica registrada.

### Passo 3 — Compilar e testar

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/microsoft-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
cd fontes
mvn -q -DskipTests compile -pl br.com.wdc.shopping/br.com.wdc.shopping.view.teavm/br.com.wdc.shopping.view.teavm.web -am
```

---

## Diferenças em relação ao remote.shell.teavm

| Aspecto | app.teavm (este módulo) | remote.shell.teavm |
|---------|------------------------|--------------------|
| **Acesso ao estado** | Direto via o Scope recebido no construtor | Indireto via `ViewScope` (JSON) |
| **Invocação de ações** | `safeAction("ctx", state.onXxx::run)` | `submit(EVENT_CODE)` |
| **Passagem de dados** | Argumentos diretos nos métodos | `setFormField(key, value)` |
| **Registro** | `SCOPE_VIEWS.register(XxxScope.class, View::new)` | `app.registerView(VIEW_ID, View::new)` (Main.java) |
| **VIEW_ID** | Não necessário (binding pela classe do escopo) | Obrigatório (igual ao `XxxScope.CLASS_ID`) |
| **Construtor** | Recebe `(app, XxxScope)` | Recebe `String vsid` |
| **Thread safety** | `safeAction()` roda em Thread TeaVM | Protocolo WS é serial |

---

## Conceitos Fundamentais

### safeAction() — Execução Segura

Todo disparo de ação do Scope (que chega ao presenter) deve ser envelopado em `safeAction()`:

```java
private final EventListener<Event> onBuy = evt -> safeAction("Buy", this.state.onBuy::run);
```

**Por quê?** O TeaVM emula threads para suportar operações `@Async` (HTTP requests via `XMLHttpRequest`). O `safeAction` cria uma "thread" TeaVM que permite suspensão durante I/O. A falha da ação em si já é tratada pela guarda do presenter (a ação é ligada com `action(...)`); o `safeAction` ainda captura o que falhar no próprio código da view, delegando ao `RootPresenter`.

**Exceção**: Ações puramente locais (sem chamar o presenter) não precisam de `safeAction`:
```java
// ✅ OK sem safeAction — estado local, sem I/O
private final EventListener<Event> onIncrement = evt -> { this.quantity++; update(); };
```

### Padrão One-Shot Error

O Scope usa `errorCode` + `errorMessage` como campos de exibição única. A view consome e limpa no render:

```java
final boolean showError;
final String errorMessage;
if (this.state.errorCode.get() != 0) {
    showError = true;
    errorMessage = this.state.errorMessage.get();
    this.state.errorCode.setSilently(0);        // ← limpa, sem pedir outro redesenho
    this.state.errorMessage.setSilently(null);   // ← limpa, sem pedir outro redesenho
} else {
    showError = false;
    errorMessage = "";
}
```

Isso garante que o erro é exibido apenas uma vez. Se o presenter chamar `update()` novamente sem novo erro, a mensagem não reaparece.

### update() vs forceUpdate()

| Método | Quando usar |
|--------|-------------|
| `update()` | Marca a view como dirty no render loop da aplicação. Usa requestAnimationFrame. Método padrão. |
| `forceUpdate()` | Força re-render imediato no próximo animation frame. Útil para estado local (ex: quantity stepper). |

Na prática, para mudanças de estado local (não vindas do presenter), use `update()`:
```java
private final EventListener<Event> onIncrement = evt -> { this.quantity++; update(); };
```

### Acesso Direto ao Scope

Diferente do remote shell (que lê JSON via `ViewScope`), aqui você acessa os campos Java diretamente:

```java
// Campos do Scope são públicos
var items = this.state.items.get();          // List<CartItem>
var product = this.state.product.get();      // ProductInfo
var errorMsg = this.state.errorMessage.get(); // String
```

Os DTOs da camada presentation (`CartItem`, `ProductInfo`, etc.) têm campos públicos:
```java
// Usar diretamente — sem getters
item.name
item.price
item.quantity
```

---

## Otimizações de Performance

### Stable Listeners (campos `final`)

O VDom diff compara event listeners por **identidade de referência**. Mesma referência = zero custo DOM.

```java
// ✅ Bom — referência estável
private final EventListener<Event> onBack = evt -> safeAction("Back", this.state.onOpenProducts::run);

// ❌ Ruim — lambda nova a cada render
.on("click", evt -> safeAction("Back", this.state.onOpenProducts::run))
```

### useCallback(key, listener) — Listeners Paramétricos

Para listeners que dependem de dados variáveis:

```java
private EventListener<Event> mkOnRemove(long id) {
    return evt -> safeAction("Remove", () -> this.state.onRemoveProduct.run(id));
}

// No render:
.on("click", useCallback("remove-" + itemId, mkOnRemove(itemId)))
```

**Keys com invalidação**: Se os dados capturados mudam, inclua-os na key:
```java
// qty muda → listener precisa ser recriado
useCallback("minus-" + key + "-" + qty, mkOnModifyQty(id, qty - 1))
```

### VNode.key() — Reconciliação de Listas

```java
return div(Css.ITEM_ROW).key(String.valueOf(item.id)).children(...);
```

---

## Padrão Compact Css

```java
@SuppressWarnings({"java:S1214", "static-access"})
private interface Css {
    CssUtility u = CssUtility.INSTANCE;       // Layout, spacing, visibility
    CssComponents c = CssComponents.INSTANCE; // Cards, alerts, empty states
    CssIcons icon = CssIcons.INSTANCE;        // Bootstrap Icons

    String ROOT = u.PAGE_SCROLL_ROOT;
    String ERROR_VISIBLE = clsx(c.ALERT_ERROR, u.MB_12);
    String ICON_ADD = clsx(icon.BAG_PLUS, u.MR_6);
    String MY_LOCAL_CLASS = "wishlist-custom-class";  // Definida no app.css
}
```

---

## VNode API — Referência Rápida

### Elementos HTML

```java
div("class-name")             // <div class="class-name">
span("icon bi-bag")           // <span class="icon bi-bag">
h5("title").text("Olá")      // <h5 class="title">Olá</h5>
p().text("Texto")            // <p>Texto</p>
img().attr("src", url)       // <img src="...">
textNode("texto puro")       // nó de texto
```

### Spectrum Web Components (Swc)

```java
spButton("accent")           // <sp-button variant="accent">
spButton("accent", "l")      // <sp-button variant="accent" size="l">
spActionButton()             // <sp-action-button>
spActionButton("s")          // <sp-action-button size="s">
spTextField("Label")         // <sp-textfield label="Label">
spFieldLabel("Texto")        // <sp-field-label>Texto</sp-field-label>
spDivider("s")               // <sp-divider size="s">
```

### Atributos, Eventos e Filhos

```java
.cls("extra-class")          // Adiciona classe ao className
.attr("placeholder", "...")  // Atributo HTML
.boolAttr("disabled", true)  // Atributo booleano (presença/ausência)
.prop("value", obj)          // Propriedade JS (dados complexos)
.style("color: red")         // Inline style
.key("unique-id")            // Key para reconciliação
.on("click", listener)       // Event listener
.children(node1, node2)      // Filhos fixos (varargs)
.children(listOfNodes)       // Filhos dinâmicos (List<VNode>)
.text("conteúdo")            // Conteúdo texto
.ref(el -> { ... })          // Acesso ao elemento DOM real
```

---

## Checklist para Nova View

- [ ] Classe estende `AbstractVDomView<XxxScope>`
- [ ] Construtor `(ShoppingTeaVMApplication app, XxxScope state)` chama `super(instanceId, app, state)` e salva `state`
- [ ] View registrada no catálogo: `.register(XxxScope.class, XxxViewVDom::new)` no `SCOPE_VIEWS` de `ShoppingTeaVMApplication`
- [ ] Interface `Css` com aliases e `@SuppressWarnings`
- [ ] Listeners estáveis como campos `final` com `safeAction()`
- [ ] Listeners paramétricos via `useCallback(key, factory)`
- [ ] `.key()` em itens de listas dinâmicas
- [ ] Erros one-shot consumidos no início do `render()`
- [ ] CSS da view adicionado em `src/main/webapp/css/app.css`
- [ ] Compilação sem erros
