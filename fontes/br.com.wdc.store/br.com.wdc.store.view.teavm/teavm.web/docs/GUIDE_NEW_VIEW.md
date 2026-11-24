# Guide: How to Program a View for app.teavm (teavm.web)

This document explains how to create a new view in the TeaVM Web module — the **client-side** implementation (SPA) that accesses the in-memory Presenters directly. We will use a fictitious **"Wishlist"** view as the example.

## Overview

A view in this project:

1. Receives the application and the **Scope** it draws in its constructor (in-memory access) — never the Presenter
2. Reads the Scope fields to obtain data
3. Renders a virtual tree (`VNode`) declaratively
4. Fires the Scope actions (bound by the presenter) via `safeAction()` in response to interactions

Unlike `remote.shell.teavm` (which receives state via WebSocket/JSON), here the view **accesses the scope directly** — it is a pure SPA.

## Prerequisites

The corresponding Presenter must already exist in the `presentation` layer, with:

- A public `Scope` with the required fields (`Prop`s)
- The actions (`onXxx`) as Scope fields, bound in the presenter's constructor

## Anatomy of a View

```java
package br.com.wdc.store.view.teavm.views;

import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import java.util.List;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.framework.vdom.*;
import br.com.wdc.store.presentation.presenter.restricted.wishlist.WishlistScope;
import br.com.wdc.store.presentation.presenter.restricted.wishlist.structs.WishlistItem;
import br.com.wdc.store.view.teavm.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.vdom.AbstractVDomView;

public class WishlistViewVDom extends AbstractVDomView<WishlistScope> {

    // 1. Css interface — CSS class constants (compact pattern)
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

    // 2. Stable listeners (the scope the view draws is the state field, from the base) — final fields for parameterless listeners
    private final EventListener<Event> onBack = evt -> safeAction("Back", this.state.onOpenProducts::run);

    // 3. Factory methods for parameterized listeners
    private EventListener<Event> mkOnRemove(long id) {
        return evt -> safeAction("Remove", () -> this.state.onRemoveItem.run(id));
    }

    // 4. Constructor — receives the application and the scope (the factory registered in ScopeViews)
    public WishlistViewVDom(StoreTeaVMApplication app, WishlistScope state) {
        super("wishlist", app, state);
    }

    // 5. render() — returns the virtual tree based on the Scope
    @Override
    protected VNode render() {
        // Consume the one-shot error
        final boolean showError;
        final String errorMessage;
        if (this.state.errorCode.get() != 0) {
            showError = true;
            errorMessage = this.state.errorMessage.get();
            // Consumed during drawing: a redraw now would take the message away
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
            h5(Css.TITLE).text("My Wishlist"),
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
              .children(span(CssIcons.ARROW_LEFT), span().text(" Back"))
              .on("click", onBack)));
        // @formatter:on
    }

    // 6. Auxiliary render methods
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

## Step by Step

### Step 1 — Create the View class

Create the file at:
```
teavm.web/src/main/java/br/com/wdc/store/view/teavm/views/WishlistViewVDom.java
```

### Step 2 — Register the view in the catalog

In `StoreTeaVMApplication.java`, add the view to `SCOPE_VIEWS` (handed to the application with `setScopeViews` in the constructor):

```java
private static final ScopeViews SCOPE_VIEWS = new ScopeViews()
        // ... existing views ...
        .register(WishlistScope.class, WishlistViewVDom::new);
```

This connects the scope to the view: the presenter only hands the scope to its parent (`ownerSlot.setScope(state)`), and the parent's view asks for this scope's view with `app.viewOf(state.content.get())` — the application creates it the first time, using the registered factory.

### Step 3 — Compile and test

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/microsoft-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
cd fontes
mvn -q -DskipTests compile -pl br.com.wdc.store/br.com.wdc.store.view.teavm/br.com.wdc.store.view.teavm.web -am
```

---

## Differences from remote.shell.teavm

| Aspect | app.teavm (this module) | remote.shell.teavm |
|---------|------------------------|--------------------|
| **State access** | Direct via the Scope received in the constructor | Indirect via `ViewScope` (JSON) |
| **Action invocation** | `safeAction("ctx", state.onXxx::run)` | `submit(EVENT_CODE)` |
| **Data passing** | Direct method arguments | `setFormField(key, value)` |
| **Registration** | `SCOPE_VIEWS.register(XxxScope.class, View::new)` | `app.registerView(VIEW_ID, View::new)` (Main.java) |
| **VIEW_ID** | Not needed (binding by the scope class) | Required (equal to `XxxScope.CLASS_ID`) |
| **Constructor** | Receives `(app, XxxScope)` | Receives `String vsid` |
| **Thread safety** | `safeAction()` runs in a TeaVM Thread | The WS protocol is serial |

---

## Fundamental Concepts

### safeAction() — Safe Execution

Every Scope action dispatch (which reaches the presenter) must be wrapped in `safeAction()`:

```java
private final EventListener<Event> onBuy = evt -> safeAction("Buy", this.state.onBuy::run);
```

**Why?** TeaVM emulates threads to support `@Async` operations (HTTP requests via `XMLHttpRequest`). `safeAction` creates a TeaVM "thread" that allows suspension during I/O. A failure of the action itself is already handled by the presenter's guard (the action is bound with `action(...)`); `safeAction` additionally catches whatever fails in the view's own code, delegating to the `RootPresenter`.

**Exception**: Purely local actions (that do not call the presenter) do not need `safeAction`:
```java
// ✅ OK without safeAction — local state, no I/O
private final EventListener<Event> onIncrement = evt -> { this.quantity++; update(); };
```

### One-Shot Error Pattern

The Scope uses `errorCode` + `errorMessage` as display-once fields. The view consumes and clears them in render:

```java
final boolean showError;
final String errorMessage;
if (this.state.errorCode.get() != 0) {
    showError = true;
    errorMessage = this.state.errorMessage.get();
    this.state.errorCode.setSilently(0);        // ← clears, without requesting another redraw
    this.state.errorMessage.setSilently(null);   // ← clears, without requesting another redraw
} else {
    showError = false;
    errorMessage = "";
}
```

This guarantees the error is displayed only once. If the presenter calls `update()` again without a new error, the message does not reappear.

### update() vs forceUpdate()

| Method | When to use |
|--------|-------------|
| `update()` | Marks the view as dirty in the application's render loop. Uses requestAnimationFrame. Default method. |
| `forceUpdate()` | Forces an immediate re-render on the next animation frame. Useful for local state (e.g., quantity stepper). |

In practice, for local state changes (not coming from the presenter), use `update()`:
```java
private final EventListener<Event> onIncrement = evt -> { this.quantity++; update(); };
```

### Direct Access to the Scope

Unlike the remote shell (which reads JSON via `ViewScope`), here you access the Java fields directly:

```java
// Scope fields are public
var items = this.state.items.get();          // List<CartItem>
var product = this.state.product.get();      // ProductInfo
var errorMsg = this.state.errorMessage.get(); // String
```

The presentation layer DTOs (`CartItem`, `ProductInfo`, etc.) have public fields:
```java
// Use directly — no getters
item.name
item.price
item.quantity
```

---

## Performance Optimizations

### Stable Listeners (`final` fields)

The VDom diff compares event listeners by **reference identity**. Same reference = zero DOM cost.

```java
// ✅ Good — stable reference
private final EventListener<Event> onBack = evt -> safeAction("Back", this.state.onOpenProducts::run);

// ❌ Bad — new lambda on every render
.on("click", evt -> safeAction("Back", this.state.onOpenProducts::run))
```

### useCallback(key, listener) — Parameterized Listeners

For listeners that depend on variable data:

```java
private EventListener<Event> mkOnRemove(long id) {
    return evt -> safeAction("Remove", () -> this.state.onRemoveProduct.run(id));
}

// In render:
.on("click", useCallback("remove-" + itemId, mkOnRemove(itemId)))
```

**Keys with invalidation**: If the captured data changes, include it in the key:
```java
// qty changes → listener must be recreated
useCallback("minus-" + key + "-" + qty, mkOnModifyQty(id, qty - 1))
```

### VNode.key() — List Reconciliation

```java
return div(Css.ITEM_ROW).key(String.valueOf(item.id)).children(...);
```

---

## Compact Css Pattern

```java
@SuppressWarnings({"java:S1214", "static-access"})
private interface Css {
    CssUtility u = CssUtility.INSTANCE;       // Layout, spacing, visibility
    CssComponents c = CssComponents.INSTANCE; // Cards, alerts, empty states
    CssIcons icon = CssIcons.INSTANCE;        // Bootstrap Icons

    String ROOT = u.PAGE_SCROLL_ROOT;
    String ERROR_VISIBLE = clsx(c.ALERT_ERROR, u.MB_12);
    String ICON_ADD = clsx(icon.BAG_PLUS, u.MR_6);
    String MY_LOCAL_CLASS = "wishlist-custom-class";  // Defined in app.css
}
```

---

## VNode API — Quick Reference

### HTML Elements

```java
div("class-name")             // <div class="class-name">
span("icon bi-bag")           // <span class="icon bi-bag">
h5("title").text("Hello")    // <h5 class="title">Hello</h5>
p().text("Text")             // <p>Text</p>
img().attr("src", url)       // <img src="...">
textNode("plain text")       // text node
```

### Spectrum Web Components (Swc)

```java
spButton("accent")           // <sp-button variant="accent">
spButton("accent", "l")      // <sp-button variant="accent" size="l">
spActionButton()             // <sp-action-button>
spActionButton("s")          // <sp-action-button size="s">
spTextField("Label")         // <sp-textfield label="Label">
spFieldLabel("Text")         // <sp-field-label>Text</sp-field-label>
spDivider("s")               // <sp-divider size="s">
```

### Attributes, Events and Children

```java
.cls("extra-class")          // Adds a class to className
.attr("placeholder", "...")  // HTML attribute
.boolAttr("disabled", true)  // Boolean attribute (presence/absence)
.prop("value", obj)          // JS property (complex data)
.style("color: red")         // Inline style
.key("unique-id")            // Key for reconciliation
.on("click", listener)       // Event listener
.children(node1, node2)      // Fixed children (varargs)
.children(listOfNodes)       // Dynamic children (List<VNode>)
.text("content")             // Text content
.ref(el -> { ... })          // Access to the real DOM element
```

---

## New View Checklist

- [ ] Class extends `AbstractVDomView<XxxScope>`
- [ ] Constructor `(StoreTeaVMApplication app, XxxScope state)` calls `super(instanceId, app, state)` and stores `state`
- [ ] View registered in the catalog: `.register(XxxScope.class, XxxViewVDom::new)` in the `SCOPE_VIEWS` of `StoreTeaVMApplication`
- [ ] `Css` interface with aliases and `@SuppressWarnings`
- [ ] Stable listeners as `final` fields with `safeAction()`
- [ ] Parameterized listeners via `useCallback(key, factory)`
- [ ] `.key()` on dynamic list items
- [ ] One-shot errors consumed at the start of `render()`
- [ ] View CSS added to `src/main/webapp/css/app.css`
- [ ] Compiles without errors
