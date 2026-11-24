# Guide: How to Create a New Module (Feature)

This document explains, step by step, how to add a new feature to the WDC Store system using the Cube MVP pattern. As an example, we will create a fictional **"Wishlist"** module.

## Overview

Creating a new module involves changes in **3 layers**:

| Layer | Maven module | What to create |
|--------|-------------|-------------|
| **Presentation** | `br.com.wdc.store.presentation` | Presenter, Scope, Calls, DTOs |
| **Skeleton** (remote contract) | `br.com.wdc.store.presentation` | `CLASS_ID` in the Scope + an event code on each action (`remote.host` registers nothing) |
| **React Client** (frontend) | `remote.shell.react` | React component + registration |

---

## Step 1 — Presentation: Create the Presenter and Scope

### 1.1 Create the package structure

```
presenter/restricted/wishlist/
├── WishlistPresenter.java
├── WishlistScope.java
├── WishlistKeys.java
├── calls/
│   └── WishlistQuery.java     (if you need to access data)
└── structs/
    └── WishlistItem.java         (DTOs, if any)
```

### 1.2 Create the Scope

The Scope defines what the view displays and what it triggers. It lives in its own file, next to the presenter, with the bridge protocol `CLASS_ID`, one `Prop` per observed field, and one action per view gesture. A scope that only exists inside it (a panel, a list row) is a public static inner class of it:

```java
package br.com.wdc.store.presentation.presenter.restricted.wishlist;

public class WishlistScope extends Scope {

    // Bridge protocol classId: the remote host draws this scope with the remote view of this id
    public static final String CLASS_ID = "d1e2f3a4b5c6";

    // Observed fields: set() with a different value requests the redraw on its own
    public final Prop<List<WishlistItem>> items = prop(List.of());
    public final Prop<Integer> errorCode = prop(0);
    public final Prop<String> errorMessage = prop();

    // Actions the view triggers — the presenter binds them in the constructor
    public ActionOf<Long> onRemoveItem = ActionOf.noop();
    public Action onOpenProducts = Action.NOOP;

}
```

There is no serialization to write: on the remote host the `ScopeSerializer` writes the scope via reflection — `"#"` (the view id) first, then the value of each `Prop` under the field name (structs with public fields become objects, lists become arrays, blank text is omitted). Actions are left out: the client triggers them by event code. A field that holds another scope is written as `"<field>ViewId"`, and a list of scopes as `"<field>ViewIds"`.

### 1.3 Create the Presenter

The presenter contains the presentation logic. Extend `AbstractCubePresenter<StoreApplication>`:

```java
package br.com.wdc.store.presentation.presenter.restricted.wishlist;

import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;

public class WishlistPresenter extends AbstractCubePresenter<StoreApplication> {

    // Public state — read by the view (local or remote); bind wires the coded actions to the scope's skeleton
    public final WishlistScope state = bind(new WishlistScope());

    // Slot received from the parent presenter (HomePresenter → contentSlot)
    private ScopeSlot ownerSlot;

    public WishlistPresenter(StoreApplication app) {
        super(app);
        // The event code is the contract with the remote clients (Step 3.2)
        this.state.onRemoveItem = actionOf(1, FormParam.asLong("p.itemId"), this::removeItem);
        this.state.onOpenProducts = action(2, () -> new HomeKeys(this.app).flip());
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        // Read on every call: a reused presenter goes wherever the parent puts it on each navigation
        this.ownerSlot = new WishlistKeys(this.app, intent).parentSlot();
        if (initialization) {
            this.loadItems();
        }

        // The parent puts the scope in its Prop<Scope> content; the parent's view requests this scope's view
        this.ownerSlot.setScope(this.state);
        return true;
    }

    // :: User Actions — bodies of the actions bound in the constructor

    private void removeItem(Long itemId) {
        // business logic...
        this.loadItems();
    }

    // :: Data load

    private void loadItems() {
        // The new list requests the redraw (the view will be marked dirty and sent to the frontend)
        this.state.items.set(new WishlistQuery()
                .withUserId(this.app.getSubject().getId())
                .run());
    }
}
```

**Key points:**
- The presenter **does not create a view**: it hands the scope to its parent (`ownerSlot.setScope(this.state)`), and each host's view layer chooses who draws it (`ScopeViews`)
- `applyParameters()` is called by the navigation system
- The state is created with `bind(new WishlistScope())`: `set` with a different value on a `Prop` marks the view as "dirty" to be sent to the frontend, without `this.update()`. That is only needed when you change something *inside* an already-assigned value (an item of a list)
- Actions are state fields (`Action`, `ActionOf<T>`, `ActionOf2<T, U>`) bound in the constructor with `action(...)`/`actionOf(...)`/`actionOf2(...)`. The guard sends any unhandled failure to `app.alertUnexpectedError()` and, at the end, republishes the address — the body has no generic `try/catch`, it only handles the expected failures
- Validation errors use `errorCode` + `errorMessage` in the state

### 1.4 Create the calls (optional)

Each operation on the repositories is a class in `calls/`: the request accumulates in fluent `withXxx(...)` setters and `run()` answers it. The suffix is a verb, as in the HTTP methods: a read is `XxxQuery extends StoreQuery<R>`; a write extends the verb that fits it — `StoreCreate` (`create()`, a new record), `StoreSave` (`save()`, the record whole), `StoreUpdate` (`update()`, some fields) or `StoreDelete` (`delete()`) — and the base class's `run()` opens the transaction. The verbs and their guarantees are in the [presentation README](README.md#calls). The call reaches the repositories it uses through their `BEAN` (`WishlistRepository.BEAN.get()`).

```java
package br.com.wdc.store.presentation.presenter.restricted.wishlist.calls;

/** The wishlist items of a user. */
public class WishlistQuery extends StoreQuery<List<WishlistItem>> {

    private Long userId;

    public WishlistQuery withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public List<WishlistItem> run() {
        return WishlistRepository.BEAN.get()
                .fetch(new WishlistCriteria()
                        .withUserId(userId))
                .stream().map(WishlistItem::create).toList();
    }
}
```

The response is an existing type; or a struct in `structs/`, when other calls or the scope also use it; or an inner class of the call, when it only carries the response back to the caller.

---

## Step 2 — Register the Navigation Route

### 2.1 Create `WishlistKeys`

Every place has a keys class, in its presenter's package, that **declares the place** and is the only one that knows the names it reads and writes in the intent. It extends the keys of the parent place — under `HomeKeys`, the wishlist requires authentication. The place name goes into the address (`#wishlist?...`) and must be unique in the tree; the prefix (two letters) must be unique in the application.

```java
public final class WishlistKeys extends HomeKeys {

    public static final String PREFIX = "wl";

    public static final CubePlace PLACE = HomeKeys.PLACE.child("wishlist", WishlistPresenter::new);

    private static final class P {
        /** The filtered category. */
        static final String CATEGORY = PREFIX + "-category";
    }

    public WishlistKeys(StoreApplication app) {
        this(app, null);
    }

    public WishlistKeys(StoreApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    // Getter that never throws (the address comes from outside); setter that removes the name from the address at the default value
    public String category() { ... }
    public WishlistKeys withCategory(String category) { ... }
}
```

### 2.2 Register in `StoreApplication.PLACES`

```java
public static final List<CubePlace> PLACES = List.of(
        RootKeys.PLACE,
        // ...
        WishlistKeys.PLACE);
```

The list loads the keys classes — without it, an F5 on `#wishlist` would fall back to the root, because the place only enters the tree when `WishlistKeys` loads. `PlaceKeysTest` fails if any is left out.

The **hierarchical path** comes from the tree: `new WishlistKeys(app).flip()` instantiates the required presenters (Root → Restricted → Home → Wishlist) and calls `applyParameters` on each one.

---

## Step 3 — Skeleton: expose the presenter to the bridge (backend)

### 3.1 Choose the `CLASS_ID` (classId)

A 12-character hexadecimal identifier, unique among scopes (e.g. `"d1e2f3a4b5c6"`), declared in the scope itself as `public static final String CLASS_ID` (Step 1.2). It is the same one the remote shells use to find the view (`VIEW_ID` in TeaVM/SWT, `BaseViewClass.FC` in React).

### 3.2 Give each action an event code

The code goes in the action binding itself (Step 1.3), along with how to read the values from the `formData` the client sends: `actionOf(1, FormParam.asLong("p.itemId"), this::removeItem)`.

- `FormParam.asLong/asInteger/asString(name)` cover the common cases; `FormParam.of(name, conversion)` covers a value that arrives encoded (the login password arrives encrypted).
- The code is the contract with the remote clients: it must be the same in the shells. A duplicate code fails when the presenter is constructed; an unknown code coming from the client is an error.
- The local views (Vaadin, SWT, teavm.web) do not use the code: they call `state.onRemoveItem.run(id)` directly.

### 3.3 The skeleton

The presenter does not declare a skeleton: it belongs to the scope. With `bind(...)`, the scope shares the presenter's event map, and `scope.skeleton(CLASS_ID)` dispatches each event to the action with its code — the remote view is the one that requests it.

If the client also syncs fields before the events, register the `syncState` in the presenter's constructor with `onSync` (as the login does with the user name; `setSilently` because the client already shows what it sent):

```java
this.state.onSync(formData -> {
    if (formData.containsKey("userName")) {
        state.userName.setSilently(CoerceUtils.asString(formData.get("userName")));
    }
});
```

### 3.4 The remote host

Nothing to register: the view catalog of `StoreApplicationImpl` (`remote.host`) is `new ScopeViews().fallback(RemoteViewImpl::forScope)`, which creates a remote view for any scope by reading its `CLASS_ID` via reflection.

`RemoteViewImpl` serializes the scope (the value of each `Prop`; actions are left out; a `Prop<Scope>` is written as `"<field>ViewId"`) and forwards events to the scope's skeleton.

---

## Step 4 — React Client: Create the Component

### 4.1 Create the view file

Create `src/scripts/views/WishlistView.tsx`:

```tsx
import React from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import app, { type ViewProps } from '@root/App'
import { BaseViewClass } from '@root/utils/ViewUtils'

// :: Actions — must match the event codes of the presenter's actions
const ON_REMOVE_ITEM = 1
const ON_OPEN_PRODUCTS = 2

// :: View State — mirrors the fields the ScopeSerializer writes from WishlistScope
export type WishlistScope = {
  items?: { id: number; name: string; price: number }[]
  errorMessage?: string
}

class WishlistViewClass extends BaseViewClass<ViewProps, WishlistScope> {

  override render({ className }: ViewProps): React.ReactNode {
    const { state } = this

    return (
      <Box className={className}>
        <Typography variant="h5">My Wishlist</Typography>

        {state.items?.map((item) => (
          <Box key={item.id}>
            <Typography>{item.name} - ${item.price.toFixed(2)}</Typography>
            <Button onClick={() => this.emitRemoveItem(item.id)}>Remove</Button>
          </Box>
        ))}

        <Button onClick={this.emitOpenProducts}>Back</Button>
      </Box>
    )
  }

  // :: Emitters — send events to the backend

  readonly emitRemoveItem = (itemId: number) => {
    const { vsid } = this
    app.setFormField(vsid, 'itemId', itemId)  // field read in syncClientToServer/submit
    app.submit(vsid, ON_REMOVE_ITEM)          // eventCode = 1
  }

  readonly emitOpenProducts = () => {
    app.submit(this.vsid, ON_OPEN_PRODUCTS)   // eventCode = 2
  }
}

// The VID here MUST be identical to WishlistScope.CLASS_ID
export default BaseViewClass.FC(WishlistViewClass, 'd1e2f3a4b5c6')
```

**Important rules:**
- The **event codes** (1, 2, ...) must match the codes the presenter bound the actions with (`action(1, ...)`, `actionOf(2, ...)`)
- The **VID** in `BaseViewClass.FC()` must be identical to the scope's `CLASS_ID` in Java
- The **state type** must mirror the fields the `ScopeSerializer` writes from the scope (Step 1.2)
- Use `app.setFormField()` to send data before `app.submit()`

### 4.2 Register the view

In the file `src/scripts/index.tsx`, import and register:

```tsx
import WishlistView from './views/WishlistView'

app.registerComponents(
  BrowserView,
  SlotView,
  RootView,
  LoginView,
  RestrictedView,
  CartView,
  ReceiptView,
  ProductView,
  ProductsPanel,
  PurchasesPanel,
  WishlistView,        // NEW
)
```

---

## Step 5 — Invoke Navigation

You can navigate to the new module from any presenter:

```java
// In HomePresenter: a field in the state and the binding in the constructor
public Action onOpenWishlist = Action.NOOP;              // HomeScope
this.state.onOpenWishlist = action(() -> new WishlistKeys(this.app).flip());
```

And in the frontend, add a button or link that emits the corresponding event.

---

## Summary — Checklist

- [ ] **Presentation**
  - [ ] Create `WishlistScope extends Scope` (`Prop` fields, created with `prop(...)`)
  - [ ] Create `WishlistPresenter extends AbstractCubePresenter<StoreApplication>`
  - [ ] Create the calls in `calls/` (`XxxQuery extends StoreQuery<R>`, or the write verb: `XxxCreate`/`XxxSave`/`XxxUpdate`/`XxxDelete`) if you need data
  - [ ] Create DTOs in `structs/` with `projection()` + `create()`
  - [ ] Create `WishlistKeys extends HomeKeys` (unique prefix, `PLACE = HomeKeys.PLACE.child(...)`)
  - [ ] Register `WishlistKeys.PLACE` in `StoreApplication.PLACES`

- [ ] **Skeleton**
  - [ ] Declare a unique `public static final String CLASS_ID` (12 hex chars) in `WishlistScope`
  - [ ] Give each action an event code (`action(1, ...)`, `actionOf(2, FormParam..., ...)`)
  - [ ] `state.onSync(...)` in the constructor, if the client syncs fields
  - [ ] In each local host, register the view in `ScopeViews` (`register(WishlistScope.class, WishlistViewVaadin::new)`); the remote host registers nothing

- [ ] **React Client**
  - [ ] Create `WishlistView.tsx` with `BaseViewClass.FC(Class, VID)`
  - [ ] Define event codes (must match the codes of the presenter's actions)
  - [ ] Define the state type (must match the Scope's fields)
  - [ ] Register in `index.tsx` via `app.registerComponents()`

---

## Variant: Segment (Panel)

If the new module is a **panel embedded** in another view (like the products panel inside Home), it **does not get a presenter** or a file: it is a smaller scope, a public static inner class of the scope of whoever shows it. The owning presenter binds its actions and puts it in the scope; the host chooses the view:

```java
public class HomeScope extends Scope {

    public static class WishlistPanelScope extends Scope {
        public static final String CLASS_ID = "e1f2a3b4c5d6";   // the segment's remote view

        public final Prop<List<WishlistItem>> items = prop(List.of());
        public ActionOf<Long> onRemoveItem = ActionOf.noop();
    }

    public final Prop<WishlistPanelScope> wishlistPanel = prop();
}

// In HomePresenter: in the constructor, the actions (with event code, on the segment's skeleton)
wishlistPanel.onRemoveItem = actionOf(wishlistPanel, 1, FormParam.asLong("p.itemId"), this::removeItem);

// On initialization: just put it in the Home scope
this.state.wishlistPanel.set(this.wishlistPanel);

// In each local host: the view catalog (passed to the app with setScopeViews)
new ScopeViews()
        .register(HomeScope.WishlistPanelScope.class,
                (StoreVaadinApplication app, HomeScope.WishlistPanelScope s) -> new WishlistPanelViewVaadin(app, s));
```

The segment:
- Has **no** presenter, keys class, or place
- Its view is created when the parent's view requests it (`app.viewOf(state.wishlistPanel.get())`); from then on, a change in the segment redraws only that view (until then, it redraws the owner). It is released along with the owning presenter, or with `detach(segment)`
- On the remote host the catalog falls through to `fallback(RemoteViewImpl::forScope)`, which reads the `CLASS_ID`: nothing to register; the parent references it as `"wishlistPanelViewId"`
