# Guide: How to Program a View for teavm.web

This document explains how to create a new view in the TeaVM Web module — the **client-side** implementation (SPA) that runs the Presenters in the browser and reads their Scopes in memory. We will use a fictitious **"Wishlist"** view as the example.

## Overview

A view in this module:

1. Receives the application and the **Scope** it draws in its constructor (in-memory access) — never the Presenter
2. Hands that Scope to a **shared view** from `teavm.commons`, which renders the virtual tree (`VNode`)
3. Fires the Scope actions (bound by the presenter) via `safeAction()` in response to interactions

The rendering lives in `teavm.commons` (`XxxSharedView extends SharedVDomView`) and is the same one `remote.shell.teavm` uses; this module only supplies the state and the action callbacks. Unlike `remote.shell.teavm` (which receives state via WebSocket/JSON), here the view **passes the scope itself** — it is a pure SPA. `RootView` is the exception: it renders directly in this module.

## Prerequisites

The corresponding Presenter must already exist in the `presentation` layer, with:

- A public `Scope` with the required fields (`Prop`s)
- The actions (`onXxx`) as Scope fields, bound in the presenter's constructor

For the example, assume this scope (hypothetical, following `HomeScope.ProductsPanelScope`):

```java
public class WishlistScope extends Scope {

    public static final String CLASS_ID = "5f1e2d3c4b6a";

    public final Prop<List<WishlistItem>> items = prop();   // WishlistItem: public long id; public String name;
    public final Prop<String> errorMessage = prop();

    public ActionOf<Long> onRemoveItem = ActionOf.noop();
    public Action onBack = Action.NOOP;
}
```

---

## Step by Step

### Step 1 — The shared view (teavm.commons)

Create `WishlistSharedView` in `teavm.commons/src/main/java/br/com/wdc/store/view/teavm/commons/views/wishlist/`, with its `_wishlist.scss` partial next to it. The full example is in [remote.shell.teavm's guide](../../../br.com.wdc.store.view.remote/remote.shell.teavm/docs/GUIDE_NEW_VIEW.md#step-1--the-shared-view-teavmcommons); it exposes:

```java
public Supplier<WishlistScope> stateSupplier;
public Consumer<Long> onRemoveItem;
public Runnable onBack;
```

### Step 2 — The view (teavm.web)

Create `teavm.web/src/main/java/br/com/wdc/store/view/teavm/views/wishlist/WishlistView.java`:

```java
package br.com.wdc.store.view.teavm.views.wishlist;

import br.com.wdc.store.presentation.presenter.restricted.wishlist.WishlistScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.wishlist.WishlistSharedView;
import br.com.wdc.store.view.teavm.views.AbstractVDomView;

public class WishlistView extends AbstractVDomView<WishlistScope> {

    private final WishlistSharedView shared;

    // Constructor — receives the application and the scope (the factory registered in ScopeViews)
    public WishlistView(StoreTeaVMApplication app, WishlistScope state) {
        super("wishlist", app, state);

        this.shared = new WishlistSharedView();
        this.shared.stateSupplier = () -> state;
        this.shared.onRemoveItem = id -> safeAction("Remove", () -> state.onRemoveItem.run(id));
        this.shared.onBack = () -> safeAction("Back", state.onBack::run);
    }

    // render() — delegates to the shared view
    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
```

### Step 3 — Register the view in the catalog

In `StoreTeaVMApplication.java`, add the view to `SCOPE_VIEWS` (handed to the application with `setScopeViews` in the constructor):

```java
private static final ScopeViews SCOPE_VIEWS = new ScopeViews()
        // ... existing views ...
        .register(WishlistScope.class, WishlistView::new);
```

This connects the scope to the view: the presenter only hands the scope to its parent (`ownerSlot.setScope(state)`), and the parent's view asks for this scope's view with `app.viewOf(state.content.get())` — the application creates it the first time, using the registered factory.

### Step 4 — Styles

Add `@use 'wishlist/wishlist';` to `teavm.web/src/main/scss/app.scss`. The Maven build compiles it (with `teavm.commons/src/main/scss` and `teavm.commons/.../commons/views` on the load path) into `src/main/webapp/css/app.css`, which is a generated file.

### Step 5 — Build and test

```bash
./work/bin/build-frontends.sh teavm.web
```

`teavm.web` compiles against the `presentation`, `persistence.client` and `teavm.commons` JARs in `~/.m2`; `build-frontends.sh` reinstalls them first, so the new scope is picked up. The output goes to `work/frontend/teavm.web/`.

---

## Differences from remote.shell.teavm

| Aspect | teavm.web (this module) | remote.shell.teavm |
|---------|------------------------|--------------------|
| **State access** | The Scope received in the constructor, passed to the shared view | `ViewScope` (JSON), adapted into a Scope instance |
| **Action invocation** | `safeAction("ctx", state.onXxx::run)` | `submit(EVENT_CODE)` |
| **Data passing** | Direct method arguments | `setFormField(key, value)` |
| **Registration** | `SCOPE_VIEWS.register(XxxScope.class, View::new)` | `app.registerView(VIEW_ID, View::new)` (Main.java) |
| **VIEW_ID** | Not needed (binding by the scope class) | Required (`XxxScope.CLASS_ID`) |
| **Constructor** | Receives `(app, XxxScope)` | Receives `String vsid` |
| **Thread safety** | `safeAction()` runs in a TeaVM Thread | The WS protocol is serial |

---

## Fundamental Concepts

### safeAction() — Safe Execution

Every Scope action dispatch (which reaches the presenter) must be wrapped in `safeAction()`:

```java
this.shared.onBuy = () -> safeAction("Buy", state.onBuy::run);
```

**Why?** TeaVM emulates threads to support `@Async` operations (HTTP requests via `XMLHttpRequest`). `safeAction` runs the action in a TeaVM `Thread`, which allows suspension during I/O. A failure of the action itself is handled by the presenter's guard (the action is bound with `action(...)`); `safeAction` additionally catches whatever fails on the view side and reports it through `app.alertUnexpectedError`.

### update()

`update()` marks the view as dirty in the application's render loop (`app.markDirty`), which redraws it on the next animation frame. A `set` of a different value on a `Prop` already requests the redraw; `update()` is for changes the scope does not see.

### Child views

A segment or list row with a scope of its own has its own view. The parent gets its element with `app.viewOf(scope)` and mounts it with `VNode.slot(element)` — see `HomeView` (panels) and `CartView` (rows).

### Direct Access to the Scope

Here the Java fields are read directly:

```java
var items = this.state.items.get();            // List<WishlistItem>
var errorMsg = this.state.errorMessage.get();  // String
```

The presentation layer structs (`ProductInfo`, `PurchaseInfo`, etc.) have public fields — no getters:

```java
product.name
product.price
```

---

## Performance Optimizations

The shared view follows the same rules described in [remote.shell.teavm's guide](../../../br.com.wdc.store.view.remote/remote.shell.teavm/docs/GUIDE_NEW_VIEW.md#performance-optimizations):

- **Stable listeners** — listeners without parameters are `final` fields (the VDom diff compares listeners by reference identity)
- **`useCallback(key, listener)`** — cached parametric listeners, cleaned up when the key disappears
- **`.key(uniqueId)`** — on items of dynamic lists

The same guide has the `Sel` pattern (`SelUtility`/`SelComponents`/`SelIcons`) and the VNode/Swc quick reference.

---

## New View Checklist

- [ ] Shared view `XxxSharedView extends SharedVDomView` in `teavm.commons/.../views/<x>/`
- [ ] View extends `AbstractVDomView<XxxScope>`, constructor `(StoreTeaVMApplication app, XxxScope state)` calls `super(instanceId, app, state)`
- [ ] Shared view bound: `stateSupplier = () -> state`, each callback wrapped in `safeAction()`
- [ ] View registered in the catalog: `.register(XxxScope.class, XxxView::new)` in the `SCOPE_VIEWS` of `StoreTeaVMApplication`
- [ ] `_x.scss` partial added to `src/main/scss/app.scss`
- [ ] `./work/bin/build-frontends.sh teavm.web` builds without errors
