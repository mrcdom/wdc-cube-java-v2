# Guia: Como Programar uma View para remote.shell.teavm

Este documento explica como criar uma nova view no thin client TeaVM do módulo Remote Presentation. Usaremos como exemplo a criação de uma view fictícia **"Wishlist"**.

## Visão Geral

Uma view neste projeto é um componente que:

1. Recebe estado do servidor via WebSocket (JSON → `ViewScope`)
2. Renderiza uma árvore virtual (`VNode`) de forma declarativa
3. Emite eventos de interação de volta ao servidor (`submit`)

A view **não possui lógica de negócio** — toda a lógica fica no Presenter (server-side).

## Pré-requisitos

Antes de criar a view no shell, o Presenter correspondente já deve existir na camada `presentation`, com:

- Um `classId()` definido no `skeleton()` (ex: `"a1b2c3d4e5f6"`)
- Um `ViewState.write()` que serializa os campos em JSON
- Os `eventCode` definidos no `skeleton().submit(eventCode, ...)`

## Anatomia de uma View

```java
package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.framework.vdom.*;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewScope;

public class WishlistView extends AbstractRemoteView {

    // 1. VIEW_ID — deve ser IDÊNTICO ao classId() do Presenter
    public static final String VIEW_ID = "a1b2c3d4e5f6";

    // 2. Event codes — devem ser IDÊNTICOS ao skeleton().submit(eventCode, ...)
    private static final int ON_REMOVE_ITEM = 1;
    private static final int ON_BACK = 2;

    // 3. Interface Css — constantes de classe CSS (compact pattern)
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

    // 4. Stable listeners — campos final para listeners sem parâmetros
    private final EventListener<Event> onBack = evt -> submit(ON_BACK);

    // 5. Factory methods para listeners paramétricos
    private EventListener<Event> mkOnRemove(Long id) {
        return evt -> {
            setFormField("p.itemId", id);
            submit(ON_REMOVE_ITEM);
        };
    }

    // 6. Construtor — recebe vsid (view-state ID)
    public WishlistView(String vsid) {
        super(vsid);
    }

    // 7. render() — retorna a árvore virtual
    @Override
    protected VNode render() {
        var scope = state();
        var errorMessage = scope.getString("errorMessage");
        var showError = errorMessage != null && !errorMessage.isEmpty();
        var items = scope.getList("items");
        var empty = items == null || items.isEmpty();

        // @formatter:off
        return div(Css.ROOT).children(
          div(Css.WRAPPER).children(
            h5(Css.TITLE).text("Minha Lista de Desejos"),
            // Error
            div(showError ? Css.ERROR_VISIBLE : Css.HIDDEN).children(
              span(Css.ERROR_ICON),
              span(Css.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")),
            // Items
            div().children(items != null
              ? items.stream().map(this::renderItem).toList()
              : List.of()),
            // Back button
            spActionButton()
              .children(span(CssIcons.ARROW_LEFT), span().text(" Voltar"))
              .on("click", onBack)));
        // @formatter:on
    }

    // 8. Métodos de render auxiliares (extrair para legibilidade)
    private VNode renderItem(Map<String, Object> item) {
        var id = CoerceUtils.asLong(item.get("id"));
        var name = CoerceUtils.asString(item.get("name"), "");
        var key = String.valueOf(id);

        // @formatter:off
        return div(Css.ITEM_ROW).key(key).children(
          span(Css.ITEM_NAME).text(name),
          spActionButton("s")
            .children(span(Css.REMOVE_ICON))
            .on("click", useCallback("remove-" + key, mkOnRemove(id))));
        // @formatter:on
    }
}
```

---

## Passo a Passo

### Passo 1 — Criar a classe da View

Crie o arquivo em:
```
remote.shell.teavm/src/main/java/br/com/wdc/shopping/view/remote/shell/teavm/views/WishlistView.java
```

### Passo 2 — Registrar no Main.java

Adicione no `Main.java`:

```java
app.registerView(WishlistView.VIEW_ID, WishlistView::new);
```

O `VIEW_ID` é a chave que o servidor usa para identificar qual factory instanciar quando envia um estado para esta view.

### Passo 3 — Compilar e testar

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/microsoft-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
cd fontes
mvn -q -DskipTests compile -pl br.com.wdc.shopping/br.com.wdc.shopping.view.remote/remote.shell.teavm -am
```

---

## Conceitos Fundamentais

### VIEW_ID e o Protocolo

O `VIEW_ID` é um hash de 12 caracteres que vincula a view no shell ao Presenter no host:

```
Host (Presenter)                      Shell (View)
┌─────────────────────┐               ┌──────────────────┐
│ skeleton().classId() │ ←─ mesmo ─→  │ VIEW_ID          │
│ = "a1b2c3d4e5f6"    │               │ = "a1b2c3d4e5f6" │
└─────────────────────┘               └──────────────────┘
```

Quando o host envia um estado JSON com esse ID, o `ViewStateCoordinator` localiza a factory registrada, instancia a view (se nova) e entrega o estado via `ViewScope`.

### ViewScope — Leitura de Estado

O `ViewScope` é um wrapper sobre o `Map<String, Object>` recebido do servidor. Métodos disponíveis:

| Método | Retorno | Uso |
|--------|---------|-----|
| `getString(key)` | `String` ou `null` | Campos texto |
| `getString(key, default)` | `String` | Com fallback |
| `getInt(key)` | `int` (0 se ausente) | Contadores, codes |
| `getDouble(key)` | `double` | Valores monetários |
| `getBoolean(key)` | `boolean` | Flags |
| `getMap(key)` | `Map<String, Object>` | Objetos aninhados |
| `getList(key)` | `List<Map<String, Object>>` | Arrays de objetos |

### submit() e setFormField() — Emissão de Eventos

Para comunicar ações do usuário ao servidor:

```java
// Evento simples (sem dados)
submit(ON_BACK);   // Envia eventCode=2 ao Presenter

// Evento com dados (form fields)
setFormField("p.itemId", itemId);
setFormField("p.quantity", 3);
submit(ON_ADD_TO_CART);  // Presenter recebe formData com os campos
```

No Presenter, os dados chegam no `skeleton().submit(eventCode, eventQtde, formData)`:

```java
case 1 -> onRemoveItem(CoerceUtils.asLong(formData.get("p.itemId")));
```

---

## Otimizações de Performance

### Stable Listeners (campos `final`)

O VDom diff compara event listeners por **identidade de referência**. Se a mesma referência é passada entre renders, nenhuma operação DOM é realizada.

**Regra**: Todo listener que não depende de parâmetros variáveis deve ser um campo `final`:

```java
// ✅ Bom — referência estável, zero custo no diff
private final EventListener<Event> onBack = evt -> submit(ON_BACK);

// ❌ Ruim — lambda nova a cada render, re-registra no DOM
.on("click", evt -> submit(ON_BACK))
```

### useCallback(key, listener) — Listeners Paramétricos

Quando o listener depende de dados variáveis (ex: ID de um item em uma lista), use `useCallback` para manter a referência estável enquanto a key não mudar:

```java
// Factory cria um novo listener com os dados capturados
private EventListener<Event> mkOnRemove(Long id) {
    return evt -> {
        setFormField("p.itemId", id);
        submit(ON_REMOVE_ITEM);
    };
}

// No render, useCallback retorna a instância cacheada se a key já existe
.on("click", useCallback("remove-" + itemId, mkOnRemove(itemId)))
```

**Ciclo de vida**:
1. Primeira vez que a key aparece → armazena o listener no cache
2. Renders subsequentes com mesma key → retorna instância cacheada (sem alocar)
3. Se a key desaparece (item removido da lista) → cleanup automático no fim do render

**Dica para keys**: Inclua valores que invalidam o listener antigo:
```java
// Se qty muda, o listener precisa ser recriado (pois captura qty+1 / qty-1)
useCallback("plus-" + itemId + "-" + qty, mkOnModifyQty(itemId, qty + 1))
```

### VNode.key() — Reconciliação de Listas

Ao renderizar listas, use `.key(uniqueId)` para que o diff identifique movimentos e remoções sem destruir/recriar nós:

```java
return div(Css.ITEM_ROW).key(String.valueOf(item.id)).children(...);
```

---

## Padrão Compact Css

Cada view define uma interface `Css` privada que organiza todas as classes CSS em um único lugar:

```java
@SuppressWarnings({"java:S1214", "static-access"})
private interface Css {
    CssUtility u = CssUtility.INSTANCE;       // Classes utilitárias (layout, spacing)
    CssComponents c = CssComponents.INSTANCE; // Componentes reutilizáveis (cards, alerts)
    CssIcons icon = CssIcons.INSTANCE;        // Bootstrap Icons

    // Framework classes via alias
    String ROOT = u.PAGE_SCROLL_ROOT;
    String HIDDEN = u.HIDDEN;

    // Composição de classes (clsx = className join)
    String ERROR_VISIBLE = clsx(c.ALERT_ERROR, u.MB_12);
    String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);

    // Classes específicas da view (definidas no app.css)
    String TITLE = "wishlist-title";
    String ITEM_ROW = "wishlist-item-row";
}
```

**Benefícios**:
- Nomes curtos no render: `Css.ROOT` vs `CssUtility.PAGE_SCROLL_ROOT`
- Composições pré-computadas em tempo de compilação
- Todas as classes da view centralizadas em um único ponto

**Regras**:
- Sempre incluir `@SuppressWarnings({"java:S1214", "static-access"})`
- Usar `clsx(...)` para combinar múltiplas classes em uma string
- Apenas incluir aliases (`u`/`c`/`icon`) para as categorias efetivamente usadas

---

## VNode API — Referência Rápida

### Elementos HTML

```java
import static br.com.wdc.framework.vdom.VNode.*;

div("class-name")             // <div class="class-name">
div()                         // <div>
span("icon bi-bag")           // <span class="icon bi-bag">
h5("title").text("Olá")      // <h5 class="title">Olá</h5>
p().text("Texto")            // <p>Texto</p>
img().attr("src", url)       // <img src="...">
textNode("texto puro")       // nó de texto
```

### Spectrum Web Components (Swc)

```java
import static br.com.wdc.framework.vdom.Swc.*;

spButton("accent")           // <sp-button variant="accent">
spButton("accent", "l")      // <sp-button variant="accent" size="l">
spActionButton()             // <sp-action-button>
spActionButton("s")          // <sp-action-button size="s">
spTextField("Label")         // <sp-textfield label="Label">
spFieldLabel("Texto")        // <sp-field-label>Texto</sp-field-label>
spDivider("s")               // <sp-divider size="s">
```

### Atributos e Propriedades

```java
.cls("extra-class")          // Adiciona classe ao className
.attr("placeholder", "...")  // Atributo HTML
.boolAttr("disabled", true)  // Atributo booleano (presença/ausência)
.prop("value", obj)          // Propriedade JS (dados complexos)
.style("color: red")         // Inline style
.key("unique-id")            // Key para reconciliação de listas
```

### Eventos

```java
.on("click", listener)       // addEventListener("click", ...)
.on("keydown", listener)     // addEventListener("keydown", ...)
.on("change", listener)      // addEventListener("change", ...)
.on("input", listener)       // addEventListener("input", ...)
```

### Filhos

```java
.children(node1, node2)      // Filhos fixos (varargs)
.children(listOfNodes)       // Filhos dinâmicos (List<VNode>)
.text("conteúdo")            // Conteúdo texto (atalho para textNode filho)
```

### Ref (acesso ao elemento DOM)

```java
.ref(el -> {
    // Chamado quando o elemento é criado/atualizado no DOM
    // Útil para innerHTML, foco programático, medições
    ((HTMLInputElement) el).focus();
})
```

---

## Tratamento de Erros

Padrão universal para exibir mensagens de erro vindas do Presenter:

```java
// No render():
var errorMessage = scope.getString("errorMessage");
var showError = errorMessage != null && !errorMessage.isEmpty();

// No VNode tree:
div(showError ? Css.ERROR_VISIBLE : Css.HIDDEN).children(
  span(Css.ERROR_ICON),
  span(Css.ERROR_TEXT).text(errorMessage != null ? errorMessage : ""))
```

O Presenter define `errorCode` + `errorMessage` no ViewState e chama `this.update()`. O protocolo envia o delta e a view re-renderiza automaticamente.

---

## Mapeamento de Estado (Records)

Para estados complexos (objetos aninhados), use records para parsing tipado:

```java
private record Product(Long id, String name, Double price) {}

private Product parseProduct(ViewScope scope) {
    Map<String, Object> map = scope.getMap("product");
    if (map.isEmpty()) return new Product(-1L, "", 0.0);
    return new Product(
        CoerceUtils.asLong(map.get("id")),
        CoerceUtils.asString(map.get("name"), ""),
        CoerceUtils.asDouble(map.get("price"), 0.0));
}
```

---

## Checklist para Nova View

- [ ] `VIEW_ID` idêntico ao `classId()` do Presenter
- [ ] Event codes idênticos ao `skeleton().submit(eventCode, ...)` do Presenter
- [ ] Registrada no `Main.java` com `app.registerView(VIEW_ID, View::new)`
- [ ] Interface `Css` com aliases e suppressions
- [ ] Listeners estáveis como campos `final`
- [ ] Listeners paramétricos via `useCallback(key, factory)`
- [ ] `.key()` em itens de listas dinâmicas
- [ ] Tratamento de erro padrão (`errorMessage` → alert hidden/visible)
- [ ] CSS da view adicionado em `src/main/webapp/css/app.css`
- [ ] Compilação sem erros
