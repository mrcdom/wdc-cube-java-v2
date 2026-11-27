# Guide: How to Write a View for remote.shell.teavm

This document explains how to create a new view in the TeaVM thin client of the Remote Presentation module. As an example, we will create a fictional **"Wishlist"** view.

## Overview

A view in this shell is a thin adapter that:

1. Receives its scope from the server over WebSocket (JSON → `ViewScope`)
2. Adapts that JSON into the presentation `Scope` type and hands it to a **shared view** from `teavm.commons`, which renders the virtual tree (`VNode`)
3. Emits interaction events back to the server (`setFormField` + `submit`)

The view **has no business logic** — all the logic lives in the Presenter (server-side). The rendering itself lives in `teavm.commons` (`XxxSharedView extends SharedVDomView`), so the same tree is drawn by this shell and by `teavm.web`; each shell only supplies the scope and the action callbacks. `BrowserView` and `RootView` are the exceptions: they render directly in the shell.

## Prerequisites

Before creating the view in the shell, the Presenter and its Scope must already exist in the `presentation` layer, with:

- The classId declared in the scope: `public static final String CLASS_ID = "5f1e2d3c4b6a";` (12 hex characters, unique among the scopes)
- The observed fields as public `Prop`s of the scope (the `ScopeSerializer` writes them to JSON)
- The event codes, given when binding each action (`action(2, ...)`, `actionOf(1, FormParam.asLong("p.itemId"), ...)`)
- The view's texts in the catalog of every language ([`br.com.wdc.store.i18n`](../../../br.com.wdc.store.i18n/README.md); here `wishlist.title`, `wishlist.back`), with `StoreMessages` generated again

For the example, assume this scope (hypothetical, following `HomeScope.ProductsPanelScope`):

```java
public class WishlistScope extends Scope {

    public static final String CLASS_ID = "5f1e2d3c4b6a";

    public final Prop<List<WishlistItem>> items = prop();   // WishlistItem: public long id; public String name;
    public final Prop<Message> errorMessage = prop();        // br.com.wdc.store.i18n.Message: a catalog key and its arguments

    public ActionOf<Long> onRemoveItem = ActionOf.noop();     // bound with actionOf(1, FormParam.asLong("p.itemId"), ...)
    public Action onBack = Action.NOOP;                       // bound with action(2, ...)
}
```

---

## Step 1 — The shared view (teavm.commons)

Create `teavm.commons/src/main/java/br/com/wdc/store/view/teavm/commons/views/wishlist/WishlistSharedView.java`. It knows nothing about WebSocket or presenters: the scope comes from a `Supplier`, actions go out through plain callbacks.

```java
package br.com.wdc.store.view.teavm.commons.views.wishlist;

import static br.com.wdc.store.view.teavm.commons.Swc.spActionButton;
import static br.com.wdc.store.view.teavm.commons.VNode.clsx;
import static br.com.wdc.store.view.teavm.commons.VNode.div;
import static br.com.wdc.store.view.teavm.commons.VNode.h5;
import static br.com.wdc.store.view.teavm.commons.VNode.span;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.presentation.presenter.restricted.wishlist.WishlistScope;
import br.com.wdc.store.presentation.presenter.restricted.wishlist.structs.WishlistItem;
import br.com.wdc.store.view.teavm.commons.I18n;
import br.com.wdc.store.view.teavm.commons.SelComponents;
import br.com.wdc.store.view.teavm.commons.SelIcons;
import br.com.wdc.store.view.teavm.commons.SelUtility;
import br.com.wdc.store.view.teavm.commons.SharedVDomView;
import br.com.wdc.store.view.teavm.commons.VNode;

public class WishlistSharedView extends SharedVDomView {

    // 1. Sel interface — CSS class constants (compact pattern)
    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;
        SelComponents c = SelComponents.INSTANCE;
        SelIcons icon = SelIcons.INSTANCE;

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

    // 2. External bindings — set by each shell
    public Supplier<WishlistScope> scopeSupplier;
    public Consumer<Long> onRemoveItem;
    public Runnable onBack;

    // 3. Stable listeners — final fields for listeners without parameters
    private final EventListener<Event> backListener = evt -> onBack.run();

    // 4. Factory for parametric listeners
    private EventListener<Event> mkOnRemove(long id) {
        return evt -> onRemoveItem.accept(id);
    }

    // 5. render() — returns the virtual tree
    @Override
    public VNode render() {
        var scope = scopeSupplier.get();
        var errorMessage = I18n.text(scope.errorMessage.get());   // in the browser's language; empty for none
        var showError = !errorMessage.isEmpty();
        var items = scope.items.get();

        // @formatter:off
        return div(Sel.ROOT).children(
          div(Sel.WRAPPER).children(
            h5(Sel.TITLE).text(I18n.text(StoreMessages.WISHLIST_TITLE)),
            // Error
            div(showError ? Sel.ERROR_VISIBLE : Sel.HIDDEN).children(
              span(Sel.ERROR_ICON),
              span(Sel.ERROR_TEXT).text(errorMessage)),
            // Items
            div().children(items != null ? items.stream().map(this::renderItem).toList() : List.of()),
            // Back button
            spActionButton()
              .children(span(SelIcons.ARROW_LEFT), span().text(" " + I18n.text(StoreMessages.WISHLIST_BACK)))
              .on("click", backListener)));
        // @formatter:on
    }

    // 6. Helper render methods
    private VNode renderItem(WishlistItem item) {
        var key = String.valueOf(item.id);

        // @formatter:off
        return div(Sel.ITEM_ROW).key(key).children(
          span(Sel.ITEM_NAME).text(item.name != null ? item.name : ""),
          spActionButton("s")
            .children(span(Sel.REMOVE_ICON))
            .on("click", useCallback("remove-" + key, mkOnRemove(item.id))));
        // @formatter:on
    }
}
```

Every text comes from the catalog (`I18n.text(StoreMessages.XXX)` for the view's own, `I18n.text(message)` for the presenter's). Add its styles as a partial next to it, `views/wishlist/_wishlist.scss`, taking its colors and spacing from the design tokens (`@use "generated/tokens" as t;`, then `t.$color-primary`, `t.$space-md`, ...; [`br.com.wdc.store.design`](../../../br.com.wdc.store.design/README.md)) instead of literals.

## Step 2 — The adapter (remote.shell.teavm)

Create `remote.shell.teavm/src/main/java/br/com/wdc/store/view/remote/shell/teavm/views/wishlist/WishlistView.java`. It reads the JSON scope, fills an instance of the scope type for the shared view, and turns each callback into `setFormField` + `submit`:

```java
package br.com.wdc.store.view.remote.shell.teavm.views.wishlist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.cube.remote.bridge.teavm.ViewScope;
import br.com.wdc.store.presentation.presenter.restricted.wishlist.WishlistScope;
import br.com.wdc.store.presentation.presenter.restricted.wishlist.structs.WishlistItem;
import br.com.wdc.store.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.wishlist.WishlistSharedView;

public class WishlistView extends AbstractRemoteView {

    // 1. VIEW_ID — the scope's CLASS_ID
    public static final String VIEW_ID = WishlistScope.CLASS_ID;

    // 2. Event codes — IDENTICAL to the codes of the Presenter's actions
    private static final int ON_REMOVE_ITEM = 1;
    private static final int ON_BACK = 2;

    private final WishlistSharedView shared;
    private final WishlistScope adaptedScope = new WishlistScope();

    // 3. Constructor — receives the vsid (the view's instanceId) and binds the shared view
    public WishlistView(String vsid) {
        super(vsid);

        this.shared = new WishlistSharedView();
        this.shared.scopeSupplier = this::adaptScope;
        this.shared.onRemoveItem = id -> {
            setFormField("p.itemId", id);
            submit(ON_REMOVE_ITEM);
        };
        this.shared.onBack = () -> submit(ON_BACK);
    }

    // 4. render() — delegates to the shared view
    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    // 5. JSON → scope adaptation
    private WishlistScope adaptScope() {
        ViewScope scope = scope();
        adaptedScope.errorMessage.set(getMessage("errorMessage"));   // {"key": ..., "args": {...}} → Message
        adaptedScope.items.set(getItems(scope));
        return adaptedScope;
    }

    private List<WishlistItem> getItems(ViewScope scope) {
        var result = new ArrayList<WishlistItem>();
        if (scope.getValues().get("items") instanceof List<?> list) {
            for (var entry : list) {
                if (entry instanceof Map<?, ?> m) {
                    var item = new WishlistItem();
                    item.id = CoerceUtils.asLong(m.get("id"), 0L);
                    item.name = CoerceUtils.asString(m.get("name"), "");
                    result.add(item);
                }
            }
        }
        return result;
    }
}
```

## Step 3 — Register it in Main.java

Add to `Main.java`:

```java
app.registerView(WishlistView.VIEW_ID, WishlistView::new);
```

The `VIEW_ID` is the key the server uses to identify which factory to instantiate when it sends a scope to this view.

## Step 4 — Styles

Add `@use 'wishlist/wishlist';` to `src/main/scss/app.scss`. The Maven build compiles it (with `teavm.commons/src/main/scss` and `teavm.commons/.../commons/views` on the load path) into `src/main/webapp/css/app.css`, which is a generated file.

## Step 5 — Build and test

```bash
./work/bin/build-frontends.sh teavm.shell
```

The shell compiles against the `presentation` and `teavm.commons` JARs in `~/.m2`; `build-frontends.sh` reinstalls them first, so the new scope is picked up. The output goes to `work/frontend/remote.shell.teavm/`.

---

## Fundamental Concepts

### VIEW_ID and the Protocol

The `VIEW_ID` is the 12-character classId that binds the view in the shell to the scope on the host:

```
Host (Scope)                          Shell (View)
┌──────────────────────┐              ┌──────────────────────┐
│ XxxScope.CLASS_ID    │ ←─ same ──→  │ VIEW_ID              │
│ = "5f1e2d3c4b6a"     │              │ = XxxScope.CLASS_ID  │
└──────────────────────┘              └──────────────────────┘
```

On the host, `RemoteViewImpl::forScope` (used by `RemoteApplicationSupport.viewOf` for every scope) reads the scope's `CLASS_ID` and uses it as the classId of the remote view and of the skeleton.

When the host sends a JSON scope with that ID, the `ScopeCoordinator` looks up the registered factory, instantiates the view (if new) and delivers its values via `ViewScope`.

### ViewScope — Reading the Scope

`ViewScope` (`br.com.wdc.framework.cube.remote.bridge.teavm`) is a wrapper over the `Map<String, Object>` received from the server. Available methods:

| Method | Returns | Use |
|--------|---------|-----|
| `getString(key)` | `String` or `null` | Text fields |
| `getString(key, default)` | `String` | With fallback |
| `getInt(key)` | `int` (0 if absent) | Counters, codes |
| `getDouble(key)` | `double` (0 if absent) | Monetary values |
| `getBoolean(key)` | `boolean` | Flags |
| `getMap(key)` | `Map<String, Object>` (empty if absent) | Nested objects |
| `getValues()` | `Map<String, Object>` | Raw access, e.g. lists (`getValues().get("items") instanceof List<?> list`) |

A message of the presenter (`{"key": ..., "args": {...}}` on the wire) is read with `AbstractRemoteView.getMessage(field)`, which gives the `Message` (or `null`) that the shared view translates with `I18n.text(message)`.

### Child views

A segment or list row with a scope of its own goes out as a view id: a `Prop<XxxScope>` field as `"<field>ViewId"`, a `Prop<List<XxxRowScope>>` as `"<field>ViewIds"`. The parent gets the child's element with `getChildViewElement(vsid)` and mounts it with `VNode.slot(element)` — see `RootView` (`contentViewId`) and `CartView` (`rowsViewIds`).

### submit() and setFormField() — Emitting Events

```java
// Simple event (no data)
submit(ON_BACK);   // Sends eventCode=2 to the Presenter

// Event with data (form fields)
setFormField("p.itemId", itemId);
submit(ON_REMOVE_ITEM);  // Presenter receives formData with the fields
```

In the Presenter, the action states its code and how to read the field; the scope's skeleton (`scope.skeleton(CLASS_ID)`) dispatches:

```java
this.scope.onRemoveItem = actionOf(1, FormParam.asLong("p.itemId"), this::removeItem);
```

---

## Performance Optimizations

### Stable Listeners (`final` fields)

The VDom diff compares event listeners by **reference identity**. If the same reference is passed between renders, no DOM operation is performed.

**Rule**: Every listener that does not depend on variable parameters must be a `final` field:

```java
// ✅ Good — stable reference, zero cost in the diff
private final EventListener<Event> backListener = evt -> onBack.run();

// ❌ Bad — new lambda on every render, re-registers in the DOM
.on("click", evt -> onBack.run())
```

### useCallback(key, listener) — Parametric Listeners

When the listener depends on variable data (e.g. the ID of an item in a list), use `useCallback` (available in both `SharedVDomView` and `AbstractRemoteView`) to keep the reference stable as long as the key does not change:

```java
.on("click", useCallback("remove-" + key, mkOnRemove(item.id)))
```

**Lifecycle**:
1. First time the key appears → stores the listener in the cache
2. Subsequent renders with the same key → returns the cached instance
3. If the key disappears (item removed from the list) → automatic cleanup at the end of the render

**Tip for keys**: include every value the listener captures, so a change recreates it.

### VNode.key() — List Reconciliation

When rendering lists, use `.key(uniqueId)` so the diff identifies moves and removals without destroying/recreating nodes:

```java
return div(Sel.ITEM_ROW).key(String.valueOf(item.id)).children(...);
```

---

## Compact Sel Pattern

Each view defines a private `Sel` interface that organizes all CSS classes in a single place:

```java
@SuppressWarnings({"java:S1214", "static-access"})
private interface Sel {
    SelUtility u = SelUtility.INSTANCE;       // Utility classes (layout, spacing)
    SelComponents c = SelComponents.INSTANCE; // Reusable components (cards, alerts)
    SelIcons icon = SelIcons.INSTANCE;        // Bootstrap Icons

    String ROOT = u.PAGE_SCROLL_ROOT;
    String HIDDEN = u.HIDDEN;
    String ERROR_VISIBLE = clsx(c.ALERT_ERROR, u.MB_12);
    String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);

    // View-specific classes (defined in the view's .scss partial)
    String TITLE = "wishlist-title";
}
```

**Rules**:
- Include `@SuppressWarnings({"java:S1214", "static-access"})` when using the aliases (`"java:S1214"` alone otherwise)
- Use `clsx(...)` to combine multiple classes into one string
- Only include aliases (`u`/`c`/`icon`) for the categories actually used

---

## VNode API — Quick Reference

All in `br.com.wdc.store.view.teavm.commons` (`VNode`, `Swc`).

### HTML Elements

```java
div("class-name")             // <div class="class-name">
div()                         // <div>
span("bi bi-bag")             // <span class="bi bi-bag">
h5("title").text("Hello")     // <h5 class="title">Hello</h5>
p().text("Text")              // <p>Text</p>
img().attr("src", url)        // <img src="...">
textNode("plain text")        // text node
slot(element)                 // mounts an existing element (a child view)
```

### Spectrum Web Components (Swc)

```java
spButton("accent")            // <sp-button variant="accent">
spButton("accent", "l")       // <sp-button variant="accent" size="l">
spActionButton()              // <sp-action-button>
spActionButton("s")           // <sp-action-button size="s">
spTextField("Placeholder")    // <sp-textfield placeholder="Placeholder">
spFieldLabel("id", "Text")    // <sp-field-label for="id">Text</sp-field-label>
spDivider("s")                // <sp-divider size="s">
```

### Attributes, Events and Children

```java
.cls("extra-class")           // Adds a class to className
.attr("placeholder", "...")   // HTML attribute
.boolAttr("disabled", true)   // Boolean attribute (presence/absence)
.prop("value", obj)           // JS property (complex data)
.style("color: red")          // Inline style
.key("unique-id")             // Key for list reconciliation
.on("click", listener)        // addEventListener
.children(node1, node2)       // Fixed children (varargs)
.children(listOfNodes)        // Dynamic children (List<VNode>)
.text("content")              // Text content
.ref(el -> { ... })           // Access to the real DOM element
```

---

## New View Checklist

- [ ] Shared view `XxxSharedView extends SharedVDomView` in `teavm.commons/.../views/<x>/`, with `scopeSupplier` and action callbacks
- [ ] Adapter `XxxView extends AbstractRemoteView` with `VIEW_ID = XxxScope.CLASS_ID`
- [ ] Event codes identical to the codes of the Presenter's actions
- [ ] Registered in `Main.java` with `app.registerView(VIEW_ID, XxxView::new)`
- [ ] `Sel` interface with aliases and suppressions
- [ ] Stable listeners as `final` fields; parametric ones via `useCallback(key, factory)`
- [ ] `.key()` on items of dynamic lists
- [ ] Texts from the catalog (keys in every language), the presenter's message read with `getMessage(field)`; colors from the design tokens
- [ ] `_x.scss` partial added to `src/main/scss/app.scss`
- [ ] `./work/bin/build-frontends.sh teavm.shell` builds without errors
