# Guide: How to Write a View for remote.shell.teavm

This document explains how to create a new view in the TeaVM thin client of the Remote Presentation module. As an example, we will create a fictional **"Wishlist"** view.

## Overview

A view in this project is a component that:

1. Receives state from the server over WebSocket (JSON → `ViewScope`)
2. Renders a virtual tree (`VNode`) declaratively
3. Emits interaction events back to the server (`submit`)

The view **has no business logic** — all the logic lives in the Presenter (server-side).

## Prerequisites

Before creating the view in the shell, the Presenter and its Scope must already exist in the `presentation` layer, with:

- The classId declared in the scope: `public static final String CLASS_ID = "a1b2c3d4e5f6";`
- The observed fields as public `Prop`s of the scope (the `ScopeSerializer` writes them to JSON via reflection)
- The event codes, given when binding each action (`action(1, ...)`, `actionOf(2, FormParam.asLong("p.itemId"), ...)`)

## Anatomy of a View

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

    // 1. VIEW_ID — must be IDENTICAL to the scope's CLASS_ID (WishlistScope.CLASS_ID)
    public static final String VIEW_ID = "a1b2c3d4e5f6";

    // 2. Event codes — must be IDENTICAL to the codes of the Presenter's actions (action(1, ...))
    private static final int ON_REMOVE_ITEM = 1;
    private static final int ON_BACK = 2;

    // 3. Css interface — CSS class constants (compact pattern)
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

    // 4. Stable listeners — final fields for listeners without parameters
    private final EventListener<Event> onBack = evt -> submit(ON_BACK);

    // 5. Factory methods for parametric listeners
    private EventListener<Event> mkOnRemove(Long id) {
        return evt -> {
            setFormField("p.itemId", id);
            submit(ON_REMOVE_ITEM);
        };
    }

    // 6. Constructor — receives the vsid (view-state ID)
    public WishlistView(String vsid) {
        super(vsid);
    }

    // 7. render() — returns the virtual tree
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
            h5(Css.TITLE).text("My Wishlist"),
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
              .children(span(CssIcons.ARROW_LEFT), span().text(" Back"))
              .on("click", onBack)));
        // @formatter:on
    }

    // 8. Helper render methods (extract for readability)
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

## Step by Step

### Step 1 — Create the View class

Create the file at:
```
remote.shell.teavm/src/main/java/br/com/wdc/shopping/view/remote/shell/teavm/views/WishlistView.java
```

### Step 2 — Register it in Main.java

Add to `Main.java`:

```java
app.registerView(WishlistView.VIEW_ID, WishlistView::new);
```

The `VIEW_ID` is the key the server uses to identify which factory to instantiate when it sends a state to this view.

### Step 3 — Compile and test

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/microsoft-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
cd fontes
mvn -q -DskipTests compile -pl br.com.wdc.shopping/br.com.wdc.shopping.view.remote/remote.shell.teavm -am
```

---

## Fundamental Concepts

### VIEW_ID and the Protocol

The `VIEW_ID` is a 12-character hash that binds the view in the shell to the scope on the host:

```
Host (Scope)                          Shell (View)
┌──────────────────────┐              ┌──────────────────┐
│ XxxScope.CLASS_ID    │ ←─ same ──→  │ VIEW_ID          │
│ = "a1b2c3d4e5f6"     │              │ = "a1b2c3d4e5f6" │
└──────────────────────┘              └──────────────────┘
```

On the host, `RemoteViewImpl::forScope` (the `fallback` of the `ShoppingApplicationImpl`'s `ScopeViews`) reads the scope's `CLASS_ID` and uses it as the classId of the remote view and of the skeleton.

When the host sends a JSON state with that ID, the `ViewStateCoordinator` looks up the registered factory, instantiates the view (if new) and delivers the state via `ViewScope`.

### ViewScope — Reading State

`ViewScope` is a wrapper over the `Map<String, Object>` received from the server. Available methods:

| Method | Returns | Use |
|--------|---------|-----|
| `getString(key)` | `String` or `null` | Text fields |
| `getString(key, default)` | `String` | With fallback |
| `getInt(key)` | `int` (0 if absent) | Counters, codes |
| `getDouble(key)` | `double` | Monetary values |
| `getBoolean(key)` | `boolean` | Flags |
| `getMap(key)` | `Map<String, Object>` | Nested objects |
| `getList(key)` | `List<Map<String, Object>>` | Arrays of objects |

### submit() and setFormField() — Emitting Events

To communicate user actions to the server:

```java
// Simple event (no data)
submit(ON_BACK);   // Sends eventCode=2 to the Presenter

// Event with data (form fields)
setFormField("p.itemId", itemId);
setFormField("p.quantity", 3);
submit(ON_ADD_TO_CART);  // Presenter receives formData with the fields
```

In the Presenter, the action states its code and how to read the field; the scope's skeleton (`scope.skeleton(CLASS_ID)`) dispatches:

```java
this.state.onRemoveItem = actionOf(1, FormParam.asLong("p.itemId"), this::removeItem);
```

---

## Performance Optimizations

### Stable Listeners (`final` fields)

The VDom diff compares event listeners by **reference identity**. If the same reference is passed between renders, no DOM operation is performed.

**Rule**: Every listener that does not depend on variable parameters must be a `final` field:

```java
// ✅ Good — stable reference, zero cost in the diff
private final EventListener<Event> onBack = evt -> submit(ON_BACK);

// ❌ Bad — new lambda on every render, re-registers in the DOM
.on("click", evt -> submit(ON_BACK))
```

### useCallback(key, listener) — Parametric Listeners

When the listener depends on variable data (e.g. the ID of an item in a list), use `useCallback` to keep the reference stable as long as the key does not change:

```java
// Factory creates a new listener with the captured data
private EventListener<Event> mkOnRemove(Long id) {
    return evt -> {
        setFormField("p.itemId", id);
        submit(ON_REMOVE_ITEM);
    };
}

// In render, useCallback returns the cached instance if the key already exists
.on("click", useCallback("remove-" + itemId, mkOnRemove(itemId)))
```

**Lifecycle**:
1. First time the key appears → stores the listener in the cache
2. Subsequent renders with the same key → returns the cached instance (no allocation)
3. If the key disappears (item removed from the list) → automatic cleanup at the end of the render

**Tip for keys**: Include values that invalidate the old listener:
```java
// If qty changes, the listener must be recreated (since it captures qty+1 / qty-1)
useCallback("plus-" + itemId + "-" + qty, mkOnModifyQty(itemId, qty + 1))
```

### VNode.key() — List Reconciliation

When rendering lists, use `.key(uniqueId)` so the diff identifies moves and removals without destroying/recreating nodes:

```java
return div(Css.ITEM_ROW).key(String.valueOf(item.id)).children(...);
```

---

## Compact Css Pattern

Each view defines a private `Css` interface that organizes all CSS classes in a single place:

```java
@SuppressWarnings({"java:S1214", "static-access"})
private interface Css {
    CssUtility u = CssUtility.INSTANCE;       // Utility classes (layout, spacing)
    CssComponents c = CssComponents.INSTANCE; // Reusable components (cards, alerts)
    CssIcons icon = CssIcons.INSTANCE;        // Bootstrap Icons

    // Framework classes via alias
    String ROOT = u.PAGE_SCROLL_ROOT;
    String HIDDEN = u.HIDDEN;

    // Class composition (clsx = className join)
    String ERROR_VISIBLE = clsx(c.ALERT_ERROR, u.MB_12);
    String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);

    // View-specific classes (defined in app.css)
    String TITLE = "wishlist-title";
    String ITEM_ROW = "wishlist-item-row";
}
```

**Benefits**:
- Short names in render: `Css.ROOT` vs `CssUtility.PAGE_SCROLL_ROOT`
- Compositions precomputed at compile time
- All of the view's classes centralized in a single place

**Rules**:
- Always include `@SuppressWarnings({"java:S1214", "static-access"})`
- Use `clsx(...)` to combine multiple classes into one string
- Only include aliases (`u`/`c`/`icon`) for the categories actually used

---

## VNode API — Quick Reference

### HTML Elements

```java
import static br.com.wdc.framework.vdom.VNode.*;

div("class-name")             // <div class="class-name">
div()                         // <div>
span("icon bi-bag")           // <span class="icon bi-bag">
h5("title").text("Hello")    // <h5 class="title">Hello</h5>
p().text("Text")             // <p>Text</p>
img().attr("src", url)       // <img src="...">
textNode("plain text")       // text node
```

### Spectrum Web Components (Swc)

```java
import static br.com.wdc.framework.vdom.Swc.*;

spButton("accent")           // <sp-button variant="accent">
spButton("accent", "l")      // <sp-button variant="accent" size="l">
spActionButton()             // <sp-action-button>
spActionButton("s")          // <sp-action-button size="s">
spTextField("Label")         // <sp-textfield label="Label">
spFieldLabel("Text")         // <sp-field-label>Text</sp-field-label>
spDivider("s")               // <sp-divider size="s">
```

### Attributes and Properties

```java
.cls("extra-class")          // Adds a class to className
.attr("placeholder", "...")  // HTML attribute
.boolAttr("disabled", true)  // Boolean attribute (presence/absence)
.prop("value", obj)          // JS property (complex data)
.style("color: red")         // Inline style
.key("unique-id")            // Key for list reconciliation
```

### Events

```java
.on("click", listener)       // addEventListener("click", ...)
.on("keydown", listener)     // addEventListener("keydown", ...)
.on("change", listener)      // addEventListener("change", ...)
.on("input", listener)       // addEventListener("input", ...)
```

### Children

```java
.children(node1, node2)      // Fixed children (varargs)
.children(listOfNodes)       // Dynamic children (List<VNode>)
.text("content")             // Text content (shortcut for a child textNode)
```

### Ref (access to the DOM element)

```java
.ref(el -> {
    // Called when the element is created/updated in the DOM
    // Useful for innerHTML, programmatic focus, measurements
    ((HTMLInputElement) el).focus();
})
```

---

## Error Handling

Universal pattern for displaying error messages coming from the Presenter:

```java
// In render():
var errorMessage = scope.getString("errorMessage");
var showError = errorMessage != null && !errorMessage.isEmpty();

// In the VNode tree:
div(showError ? Css.ERROR_VISIBLE : Css.HIDDEN).children(
  span(Css.ERROR_ICON),
  span(Css.ERROR_TEXT).text(errorMessage != null ? errorMessage : ""))
```

The Presenter sets `errorCode` + `errorMessage` in the ViewState (`state.errorCode.set(3)`, which already requests the redraw). The protocol sends the delta and the view re-renders automatically.

---

## State Mapping (Records)

For complex states (nested objects), use records for typed parsing:

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

## New View Checklist

- [ ] `VIEW_ID` identical to the scope's `CLASS_ID`
- [ ] Event codes identical to the codes of the Presenter's actions
- [ ] Registered in `Main.java` with `app.registerView(VIEW_ID, View::new)`
- [ ] `Css` interface with aliases and suppressions
- [ ] Stable listeners as `final` fields
- [ ] Parametric listeners via `useCallback(key, factory)`
- [ ] `.key()` on items of dynamic lists
- [ ] Standard error handling (`errorMessage` → hidden/visible alert)
- [ ] View CSS added to `src/main/webapp/css/app.css`
- [ ] Compiles without errors
