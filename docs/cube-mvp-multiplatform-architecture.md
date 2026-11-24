# Cube MVP — Multiplatform Presentation Architecture in Java

[Background](#background)  
[Core Requirements](#core-requirements)  
[The Technology Stack](#the-technology-stack)  
[Java and the Type System](#java-and-the-type-system)  
[Javalin — The HTTP Server](#javalin--the-http-server)  
[Architecture of the Reference Application](#architecture-of-the-reference-application)  
[Layered Perspective](#layered-perspective)  
[The Cube MVP Pattern](#the-cube-mvp-pattern)  
[State View](#state-view)  
[Control Structures](#control-structures)  
[Route Control — Places and Intents](#route-control--places-and-intents)  
[Access Control — Navigation Guards](#access-control--navigation-guards)  
[Class Diagram](#class-diagram)  
[View Compositions — Slots](#view-compositions--slots)  
[The View Layer — Five Independent Implementations](#the-view-layer--five-independent-implementations)  
[Remote Presentation — The Host/Shell Protocol](#remote-presentation--the-hostshell-protocol)  
[The Scope as a Serialization Contract](#the-scope-as-a-serialization-contract)  
[Conclusions](#conclusions)  
[Future Work](#future-work)

---

## Background

This project is the Java realization of the Hierarchical Passive View architecture defined in 2015 ([reference](https://docs.google.com/document/d/1Rcbt3gIjdX50j2gr2zqs9VqTQVg8527h9lOUF1xrhag/edit?tab=t.0#heading=h.3wkx7ie3r9yu)). WDC Cube Java v2 keeps the same presentation logic — the same Presenters, the same ViewStates, the same business and navigation rules — and runs it on five completely independent frontend implementations:

| Implementation | Technology | Platforms |
|---|---|---|
| `view.remote / remote.shell.react` | React 19 + TypeScript + MUI 9 | Web (browser) |
| `view.remote / remote.shell.flutter` | Flutter + Dart | Web, Desktop, Mobile |
| `view.vaadin` | Vaadin 25 + Java | Web (server-side push) |
| `view.swt` | Eclipse SWT + Java | Native desktop (Windows, Linux, macOS) |
| `view.teavm` | TeaVM + Tauri 2 + Java | Web, Desktop, Android, iOS |

The central principle of the architecture is that the presentation layer knows nothing about which technology renders the interface. The view is a pluggable artifact — an implementation of a Java contract that receives state and reports actions.

This article describes how this architecture was consolidated, which problems were solved, which innovations emerged (especially the Remote Presentation protocol), and what remains as future work.

---

## Core Requirements

The following requirements guided the project's architectural decisions and make it possible to assess what has been achieved and what remains challenging.

**Navigation logic independent of the View library** ✅  
The navigation system based on `CubeNavigation`, `CubePlace` and `CubeIntent` is pure Java and references no UI technology.

**Maximize the portion of code committed only to long-lived technologies** ✅  
Java has existed for more than 30 years. The presentation layer — Presenters and ViewStates — lives in a Maven module with no UI dependencies.

**Software construction model independent of the runtime environment** ✅  
The same presentation logic compiles and runs in the browser (React, TeaVM, Vaadin), on the desktop (SWT, Flutter, TeaVM/Tauri) and on mobile (Flutter, TeaVM/Tauri).

**Consistent representation of state in the browser URL** ✅  
The navigation system serializes the active state into the query string. The Remote Presentation protocol synchronizes the URL hash between shell and host.

**Accurate state restoration after F5** ✅  
Each implementation rebuilds the state from the URL in the `applyParameters` cycle. Remote mode uses the `sessionId` to reconnect to the server-side session.

**Test all navigation flows through mock views** ✅  
Views are created by the `ScopeViews` catalog that the host hands to the application (`setScopeViews`). In tests, the mock application registers mock views for each scope class and navigation runs without any UI.

**More than one implementation for the same view** ✅  
This is the main validation point of the architecture: five implementations coexisting on top of the same Presenters.

**Contracts and type systems for traceability** ✅  
Java offers static compile-time checking. The registration `ScopeViews.register(CartScope.class, CartViewVaadin::new)` ensures that each view implementation receives the correct scope type (`(app, CartScope) -> CubeView`).

**Managing the scope of state propagation in compositions** ✅  
Slots (`ScopeSlot`) are lambdas that encapsulate assigning a child's scope to a specific composition point of the parent, without exposing the parent Presenter.

**Technology-independent View composition** ✅  
The slot concept is technology-agnostic. Each view implementation handles the slot in its own way.

**Keeping complexity under control** ✅  
The Presenter hierarchy divides responsibilities. Each Presenter handles a fragment of the application's total state.

---

## The Technology Stack

The server-side technology stack — where the logic lives — is deliberately minimalist:

| Category | Technology | Version |
|---|---|---|
| Language | Java (Temurin/Microsoft) | 21 |
| Build | Maven | 3.9+ |
| HTTP server | Javalin | 7.2.4 |
| Database | H2 | 2.5.252 |
| Data access | jOOQ | 3.21.9 |
| Serialization | Gson | 2.13.2 |
| Logging | SLF4J + Logback | 2.0.20 / 1.6.7 |
| Tests | JUnit | 4.13.2 |

Notably absent from this list is any dependency injection framework (Spring, CDI, Guice). Injection is done through a static `AtomicReference<T>` — a simple, transparent service locator. This choice removes a layer of magic that, in Spring-ecosystem applications, usually obscures the execution flow and makes instrumenting view factories harder.

### Java and the Type System

Java offers a robust static type system with native tooling support (global refactoring, symbol traceability, code navigation). Compile-time checking eliminates an entire class of integration errors between Presenter and View — if the Presenter type expected by the view implementation does not match the one declared by the system, the compiler rejects the build.

### Javalin — The HTTP Server

Javalin is a lightweight HTTP framework for Java/Kotlin. In this project it plays two distinct roles:

1. **REST API** (`persistence.rest`) — exposes the domain repositories for consumption by the `persistence.client` module, used by the client-side view implementations (TeaVM).
2. **WebSocket** (`view.remote / remote.host`) — the bidirectional channel of the Remote Presentation protocol. Each WebSocket connection is handled by a Virtual Thread (Java 21), allowing it to scale to thousands of simultaneous sessions without the overhead of traditional threading.

---

## Architecture of the Reference Application

The reference application is a simplified e-commerce system: **WeDoCode Store**. It is deliberately functional — it supports login, browsing a product catalog, a cart and receipt generation — but kept small enough that the architecture is the focus, not business complexity.

The modules are organized into two groups:

**Framework** — a generic, reusable library:

| Module | Responsibility |
|---|---|
| `framework.commons` | Functional interfaces with checked exceptions, extensible serialization, SQL abstraction, cryptography (RSA, Base62), multiplatform logging |
| `framework.cube` | Cube MVP engine: presenters, views, navigation, intents, slots |
| `framework.cube.remote` | Remote Presentation protocol: ViewState serialization, WebSocket dispatcher, bridges for React/Flutter/TeaVM |
| `framework.jooq` | jOOQ support: JSON query helpers (`JsonQuery`, `JsonQueryBuilder`, `JsonChildQueryBuilder`); the `DSLContext` is injected by the application (no global holder) |
| `framework.dependencies` | Maven BOM for centralized versions |

**WDC Store** (`br.com.wdc.shopping`) — the application itself:

| Module | Responsibility |
|---|---|
| `shopping.domain` | Domain models, repositories, query criteria |
| `shopping.persistence` | H2 + jOOQ (generated classes) |
| `shopping.persistence.rest` | REST controllers (Javalin) |
| `shopping.persistence.client` | REST client (OkHttp + Gson) |
| `shopping.presentation` | Presenters, ViewStates, navigation — **technology-agnostic** |
| `shopping.view.*` | Five frontend implementations |

---

## Layered Perspective

```mermaid
graph TD
    subgraph Client["Clients (5 implementations)"]
        react["React 19 (browser)"]
        flutter["Flutter (mobile/desktop/web)"]
        vaadin["Vaadin (browser, server-side)"]
        swt["SWT (native desktop)"]
        teavm["TeaVM + Tauri (web/desktop/Android/iOS)"]
    end

    subgraph Presentation["Presentation Layer (Java — technology-agnostic)"]
        presenters["Presenters + ViewStates"]
        navigation["Navigation System (Places, Intents, CubeNavigation)"]
    end

    subgraph Backend["Backend (Javalin + H2)"]
        rest["REST API (persistence.rest)"]
        db[("H2")]
    end

    react -->|"WebSocket (JSON delta)"| presenters
    flutter -->|"WebSocket (JSON delta)"| presenters
    vaadin -->|"Direct in-memory access + Server Push"| presenters
    swt -->|"Direct in-memory access"| presenters
    teavm -->|"REST (Fetch API)"| rest
    presenters --> navigation
    presenters --> rest
    rest --> db
```

The separation is structural, not just conceptual: the `shopping.presentation` module has no dependency on any UI technology. It depends only on `framework.cube`, `shopping.domain` and `slf4j-api`. Any technology capable of implementing the `CubeView` interface can be plugged in.

---

## The Cube MVP Pattern

Cube MVP is a variation of Passive View with two core principles:

1. **The View has access to the Scope, but not to the Presenter.** The Scope is a plain Java object with public fields (`Prop`s and actions) — the public state of the screen. The Presenter is responsible for feeding the Scope before signaling `update()`.

2. **No cross-queries between Presenter and View.** The Presenter notifies the View through the Scope: a change in a `Prop` calls `update()` on the view that draws that scope. The View notifies the Presenter by firing the Scope's actions (the `onXxx` fields). These communications return no values.

The name "Cube" reflects the third dimension of the architecture: besides the Presenter/View axis, there is the **hierarchy** axis — Presenters are composed into a tree, each node responsible for a portion of the application's total state.

### The Presenter

Every navigable Presenter extends `AbstractCubePresenter<ShoppingApplication>`. Its Scope lives in its own file, next to it (like the `xxx.scope.ts` of the TypeScript version):

```java
// CartScope.java — public state, read by the view for rendering. Each observed field is a Prop:
// set() compares with equals and, if it changed, requests a redraw — the presenter does not call update() afterwards
public class CartScope extends Scope {
    // classId of the bridge protocol: identifies the remote view that draws this scope
    public static final String CLASS_ID = "7eb485e5f843";

    public final Prop<List<CartItem>> items = prop(Collections.emptyList());
    public final Prop<Integer> errorCode = prop(0);
    public final Prop<String> errorMessage = prop();

    // Actions the view fires (the remote serializer ignores them)
    public Action onConfirm = Action.NOOP;
}
```

```java
public class CartPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // bind: the presenter is notified when a state prop changes
    public final CartScope state = bind(new CartScope());

    private ScopeSlot ownerSlot;

    public CartPresenter(ShoppingApplication app) {
        super(app);
        // Bound once; the guard sends unhandled failures to the app and republishes the address at the end
        // With an event code: the scope's skeleton dispatches event 1 from a remote client to it
        this.state.onConfirm = action(1, this::confirm);
    }

    // Called by the navigation engine when activating this Presenter
    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        this.state.items.set(this.app.getCart().getCartItems());

        // The parent puts the scope into one of its Prop<Scope>; the parent's view is the one that requests this scope's view
        this.ownerSlot = new CartKeys(this.app, intent).parentSlot();
        this.ownerSlot.setScope(this.state);
        return true;
    }

    // Action body: handles only the expected failures, no generic try/catch
    private void confirm() {
        // business logic...
        this.state.errorCode.set(0);   // the change already requests the redraw
    }
}
```

The Presenter never creates a view: it only feeds the scope and hands it to the parent through the slot. The view belongs to the view layer — each host builds a `ScopeViews` catalog (scope class → factory `(app, scope) -> CubeView`) and hands it to the application with `setScopeViews`; `app.viewOf(scope)` creates the view the first time it is requested, without the Presenter knowing which UI technology is in use. The scope's static `CLASS_ID` is the bridge protocol classId, and the remote skeleton belongs to the scope (`scope.skeleton(CLASS_ID)`), which dispatches each event to the action bound with that code.

Every Presenter is navigable (`AbstractCubePresenter<A>`, managed by the navigation engine). A piece of the screen that does not need its own navigation — such as the products and purchases panels of the Home — **does not get a Presenter**: it is a **smaller scope**, declared as a public static inner class of the scope of whoever shows it:

```java
public class HomeScope extends Scope {

    public static class ProductsPanelScope extends Scope {
        public static final String CLASS_ID = "a1b2c3d4e5f6";

        public final Prop<List<ProductInfo>> products = prop();
        public ActionOf<Long> onOpenProduct = ActionOf.noop();
    }

    public final Prop<ProductsPanelScope> productsPanel = prop();
    // ...
}
```

The `HomePresenter` binds the segment's actions (`actionOf(productsPanel, 1, FormParam.asLong("p.productId"), this::openProduct)`) and only puts it into the scope (`state.productsPanel.set(productsPanel)`). The view layer chooses the view, as in the TypeScript version: each host builds a `ScopeViews` catalog (scope class → factory) and hands it to the application with `setScopeViews`; the Home view requests the panel's view with `app.viewOf(scope)`, which creates it the first time. From then on, a change in the segment redraws only its own view (until then, it redraws the owner), and the view is released together with the Presenter. The remote host registers nothing: `fallback(RemoteViewImpl::forScope)` makes a remote view for any scope, using its `CLASS_ID`. The view never knows the Presenter — it only reads the scope and fires its actions —, so one Presenter can orchestrate several segments without a class per segment.

In remote mode the segment is a view like any other: it has its own `classId`, its own skeleton (the code-based dispatch of the actions bound on it), and the parent references it by the view id (`"productsPanelViewId"`).

**List rows are segments too.** Each cart row is a `CartScope.CartItemScope`, with its own view and actions bound to the item itself (`onRemove`, `onModifyQuantity(quantity)` — the row does not send the product id back: it *is* the item). The `CartScope` holds `Prop<List<CartItemScope>> rows`, which the serializer writes as `"rowsViewIds": [...]`, in order. The `CartPresenter` reuses the row by product id while the item stays, creates the row for a new item (the view is born when the cart requests it), and releases (`detach`) the row of an item that left; changing a quantity redraws only that row, and the total (`CartScope.total`) comes from the server.

---

## State View

The reference application has 8 *places* organized in 4 hierarchical levels:

```mermaid
graph TD
    ROOT["ROOT"]
    ROOT --> OPEN["OPEN<br/><small>guard: if authenticated → RESTRICTED</small>"]
    ROOT --> RESTRICTED["RESTRICTED<br/><small>guard: if not authenticated → OPEN</small>"]
    OPEN --> LOGIN["LOGIN<br/><small>/login</small>"]
    RESTRICTED --> HOME["HOME<br/><small>/home</small>"]
    HOME --> PRODUCT["PRODUCT<br/><small>/home/product?pr-id=N</small>"]
    HOME --> CART["CART<br/><small>/home/cart</small>"]
    HOME --> RECEIPT["RECEIPT<br/><small>/home/receipt?rc-id=N</small>"]
```

States are additive. The state `/home/product?pr-id=42` implies the chained activation of `RootPresenter → RestrictedPresenter → HomePresenter → ProductPresenter`. Each Presenter in the chain receives the URL and extracts the parameters that concern it.

The dynamic part of the URL — the query-string parameters — is encapsulated in `CubeIntent`, and each place reads and writes it through a typed keys class (`XxxKeys`, built on the framework's `CubeKeys`):

```java
// Navigate to the detail of product 42, starting from the current state
new ProductKeys(app)
        .withProductId(42L)
        .flip();
```

**Each place has a keys class, which declares the place** from the place of the keys it extends — the class tree is the place tree, and the path to a place is given by it:

```java
public class HomeKeys extends RestrictedKeys {
    public static final String PREFIX = "hm";
    public static final CubePlace PLACE = RestrictedKeys.PLACE.child("home", HomePresenter::new);
    // ...
}

public final class ProductKeys extends HomeKeys {
    public static final String PREFIX = "pr";
    public static final CubePlace PLACE = HomeKeys.PLACE.child("product", ProductPresenter::new);

    private static final class P {
        static final String PRODUCT_ID = PREFIX + "-id";   // only this class knows the name
    }

    public Long productId() { /* 0 or more, or null: the address comes from outside, the getter never throws */ }
    public ProductKeys withProductId(Long id) { /* null (or the default) removes the name from the address */ }
}
```

- **Two-letter prefix per class**, unique within the application; every name it declares starts with it (`pr-id`). `PlaceKeysTest` checks this.
- **A child place inherits the parent's getters** to read what the upper levels passed down; the only name deliberately reused is the composition slot (`parentSlot()`).
- **Without an intent, the keys start from the current state** (`app.newIntent()`); inside `applyParameters`, a redirect uses the in-flight intent: `new HomeKeys(app, intent).flip()`.
- A place declared in a static field only enters the tree when its class loads; that is why `ShoppingApplication.PLACES` lists them all.

---

## Control Structures

### Route Control — Places and Intents

The `CubeNavigation` navigation engine performs transitions between states as follows:

1. For each `CubePlace` on the path to the destination (`place.getPath()`, from the root down), it checks whether an instance of the corresponding Presenter already exists.  
2. If not, it instantiates one through the place's factory.  
3. It invokes `applyParameters(intent, initialization, deepest)` on each Presenter in the chain, in hierarchy order.  
4. It invokes `release()` on the Presenters that are no longer part of the active state, in reverse order (from deepest to shallowest).  
5. It invokes `commitComputedState()` on all of them — an opportunity to synchronize derived state.  
6. It propagates `update()` to the affected views.

When navigating from `/home/product?pr-id=5` to `/home/cart`, the sequence is:

- `applyParameters` invoked for `Root`, `Restricted`, `Home`, `Cart`  
- `release()` invoked for `Product`  

Presenters shared between the previous state and the new one are not recreated — they are only notified with `applyParameters(intent, false, ...)`.

### Access Control — Navigation Guards

`OpenPresenter` and `RestrictedPresenter` act as guards. Each one checks a condition at the start of `applyParameters` and, if it is not met, redirects to another state:

```java
@Override
public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
    if (this.app.getSubject() != null) {
        // User already authenticated — redirect to the restricted area
        new RestrictedKeys(this.app, intent).flip();
        return false;
    }
    // Continue normal navigation...
    return true;
}
```

Returning `false` interrupts the navigation chain — the deeper Presenters are not activated.

---

## Class Diagram

```mermaid
classDiagram
    class CubeApplication {
        +navigate() CubeNavigation
        +newIntent() CubeIntent
        +go(String path)
        +setScopeViews(scopeViews)
        +viewOf(scope) CubeView
        +releaseView(scope)
        +liveViews() List
    }

    class CubePresenter {
        <<interface>>
        +applyParameters(intent, init, deepest) boolean
        +publishParameters(intent)
    }

    class AbstractCubePresenter~A~ {
        +app: A
        #bind(scope) Scope
        +scope() Scope
        +view() CubeView
        +update()
        +release()
    }

    class Scope {
        <<abstract>>
        #prop(initial) Prop
        +setUpdater(updater)
        +view() CubeView
        +skeleton(classId) CubeSkeleton
        +onSync(syncState)
        +drawnBy(view)$ Scope
    }

    class ScopeViews {
        +register(scopeClass, factory) ScopeViews
        +fallback(factory) ScopeViews
    }

    class Prop {
        +get() T
        +set(value)
    }

    class CubeView {
        <<interface>>
        +instanceId() String
        +update()
        +release()
    }

    class ScopeSlot {
        <<interface>>
        +setScope(scope)
    }

    class CubePlace {
        +id: int
        +name: String
        +parent: CubePlace
        +child(name, factory) CubePlace
        +getPath() List
        +find(name) CubePlace
    }

    class CubeIntent {
        +setParameter(key, value)
        +getParameter(key) Object
        +getSlot(name) ScopeSlot
        +setSlot(name, slot)
    }

    CubePresenter <|.. AbstractCubePresenter
    AbstractCubePresenter --> Scope : state
    Scope --> CubeView : drawn by
    CubePlace --> CubePresenter : instantiates
    CubeIntent --> ScopeSlot
    ScopeSlot --> Scope : receives
    CubeApplication --> AbstractCubePresenter : holds
    CubeApplication --> ScopeViews : viewOf(scope)
    ScopeViews --> CubeView : creates
```

The application is a graph of `AbstractCubePresenter` instances. Each instance knows only its own Scope (the `state` field, bound with `bind`). The view of each Scope is created by the view layer: the host's `ScopeViews` catalog says which factory draws each scope class, and `CubeApplication.viewOf(scope)` creates it the first time it is requested. The Presenter never references a view — at most, `view()` returns the `CubeView` that draws its scope, if there already is one.

---

## View Compositions — Slots

A slot is a composition point declared by the parent Presenter that lets a child Presenter fit into the scope tree without direct coupling between them.

```java
// HomeScope holds the child Presenter's scope (product, cart, receipt)
public final Prop<Scope> content = prop();

// HomePresenter declares a content slot
private final ScopeSlot contentSlot = this::setContent;

private void setContent(Scope scope) {
    this.state.content.set(scope);   // redraws only if it changed
}
```

The `contentSlot` slot is registered in the `CubeIntent` before moving on to the child Presenter:

```java
@Override
public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
    // ...
    if (!deepest) {
        // Propagate the slot to the child Presenter
        new HomeKeys(this.app, intent)
                .withParentSlot(this.contentSlot);
    }
    return true;
}
```

The child Presenter receives the slot and uses it to fit in:

```java
// ProductPresenter receives the slot and hands over its scope
this.ownerSlot = new ProductKeys(this.app, intent).parentSlot();
this.ownerSlot.setScope(this.state);
```

The parent view requests the view of whatever scope is there — `app.viewOf(state.content.get())` — regardless of which child Presenter put it there. The host only draws the root Presenter's scope (`viewOf(getRootPresenter().state)`); every other view is requested by the view that holds the scope. Until a scope gets a view, a change in it redraws the owner (the scope that holds it in a `Prop` adopts it); after that, only its own view. In remote mode, a `Prop<Scope>` field goes out in the JSON as `"<field>ViewId"` (`content` → `"contentViewId"`). This mechanism works identically in all five view implementations.

---

## The View Layer — Five Independent Implementations

### The CubeView Interface

Every view implementation implements `CubeView`:

```java
public interface CubeView {
    String instanceId();  // unique identifier — used by the remote protocol
    void update();        // Presenter signals that the state changed
    void release();       // lifecycle ended
}
```

### The Lifecycle of a View

1. Someone requests a scope's view with `app.viewOf(scope)` — the host, for the root scope, or the view that holds the scope in a `Prop`. The first time, the factory registered in `ScopeViews` instantiates the view with `(app, scope)` and it is drawn once.  
2. The view stores a reference to the application and to the Scope — never to the Presenter.  
3. When `update()` is called (a `Prop` of the scope changed), the view reads the Scope's fields and reconciles the interface.  
4. When the user interacts, the view fires the Scope's `onXxx` action, which the Presenter bound.  
5. When the Presenter is released (`release()`), `app.releaseView(scope)` releases the scope's view and those of the segments and rows inside it.

### The Implementations

**view.swt** — Native desktop implementation with Eclipse SWT (Windows, Linux, macOS). Each view extends `AbstractViewSwt` and uses the declarative `SwtDom` DSL to build compositions. The update cycle uses `Display.asyncExec()` to guarantee execution on the event thread:

```java
public class CartViewSwt extends AbstractViewSwt {
    public Supplier<CartScope> stateSupplier;   // wired by the factory registered in ScopeViews

    @Override
    public void doUpdate() {
        var state = this.stateSupplier.get();
        this.itemList.setItems(state.items.get().stream()
            .map(item -> item.name + " × " + item.quantity)
            .toArray(String[]::new));
    }
}
```

**view.vaadin** — Browser implementation via Vaadin Flow (server-side push). Vaadin manages the Atmosphere transport (WebSocket/Long Polling) and the reconciliation of components in the DOM. The view uses `ui.access()` to guarantee thread safety on updates:

```java
public class CartViewVaadin extends AbstractViewVaadin<CartScope> {

    // The drawn scope lives in the base class's state field
    public CartViewVaadin(ShoppingVaadinApplication app, CartScope state) {
        super("cart", app, state, new VerticalLayout());
    }

    @Override
    public void doUpdate() {
        this.grid.setItems(state.items.get());
    }
}
```

**view.teavm** — The Java views are compiled to JavaScript by the TeaVM plugin. In the browser, the result is a complete SPA. For desktop, Android and iOS, the SPA is packaged in a WebView via Tauri 2 (Rust). The views extend `AbstractVDomView` and implement `render()` returning a `VNode` tree:

```java
public class CartViewVDom extends AbstractVDomView<CartScope> {

    public CartViewVDom(ShoppingTeaVMApplication app, CartScope state) {
        super("cart", app, state);
    }

    @Override
    protected VNode render() {
        return div(Css.CART).children(
            ul().children(state.items.get().stream()
                .map(item -> li().text(item.name + " × " + item.quantity))
                .toArray(VNode[]::new)),
            spButton("accent").on("click", onConfirm).children(
                span().text("Confirm"))
        );
    }
    private final JSFunction onConfirm = e -> this.state.onConfirm.run();
}
```

**view.remote / remote.shell.react** — React shell in TypeScript. The view is a functional component that renders the state received over WebSocket and emits events back:

```tsx
const CartView: React.FC<{ scope: ViewScope<CartScope> }> = ({ scope }) => {
  const state = scope.useSnapshot();
  return (
    <Stack>
      {state.items.map(item => (
        <Typography key={item.id}>{item.name} × {item.quantity}</Typography>
      ))}
      <Button onClick={() => scope.submit(EventCodes.CONFIRM)}>Confirm</Button>
    </Stack>
  );
};
```

**view.remote / remote.shell.flutter** — Flutter shell in Dart. The same WebSocket protocol, but with Flutter widgets for rendering:

```dart
class CartViewWidget extends StatelessWidget {
  final ViewScope<CartScope> scope;
  const CartViewWidget({required this.scope});

  @override
  Widget build(BuildContext context) {
    final state = scope.watch(context);
    return Column(children: [
      ...state.items.map((item) => Text('${item.name} × ${item.quantity}')),
      ElevatedButton(
        onPressed: () => scope.submit(EventCodes.confirm),
        child: const Text('Confirm'),
      ),
    ]);
  }
}
```

---

## Remote Presentation — The Host/Shell Protocol

The Remote Presentation protocol transfers state and receives events, making the client a thin shell with no business logic.

### Analogy with Remote Desktop

The protocol is analogous to a Remote Desktop, but it operates on the application's data layer instead of pixels. The server holds all the logic — Presenters, ViewStates, business rules, navigation. The client renders serialized states and sends interaction events back.

```mermaid
sequenceDiagram
    participant Shell as Shell (React/Flutter)
    participant WS as WebSocket
    participant Host as Host (Java/Javalin)
    participant Presenters as Presenters + ViewStates

    Shell->>WS: CONNECT /dispatcher/{sessionId}
    WS->>Host: onConnect
    Host->>Presenters: initialize session
    Host->>WS: { initial dirty views }
    WS->>Shell: render states

    Shell->>WS: { requestId, eventCode, formData }
    WS->>Host: onMessage
    Host->>Presenters: syncClientToServer() → submit()
    Presenters->>Presenters: logic + navigation
    Host->>WS: { dirty ViewStates (only the modified ones) }
    WS->>Shell: reconcile affected views
```

### Channel Security

The protocol implements session encryption in two layers:

1. **RSA handshake**: at the start of the session, the shell generates an ephemeral AES-256 key, encrypts it with the server's RSA public key and sends it via the `app_signature` cookie. The server decrypts it and derives the session key via PBKDF2 (salt + 250,000 iterations).  
2. **AES-GCM traffic**: after the handshake, all payloads are encrypted with the session AES-256 key.

Additionally, the session URL is signed with HMAC-SHA256, preventing a client from forging navigations to states it was not granted.

### Delta Serialization of ViewStates

A crucial property of the protocol is that only the ViewStates that changed (*dirty*) are sent in each cycle. This is possible because the Presenter explicitly marks its view as dirty by calling `app.markDirty(view)`. The host collects only the marked views, serializes their ViewStates to JSON and transmits the delta.

The Scope is serialized by the framework's `ScopeSerializer`, which inspects the `public` fields via reflection:

```json
{
  "#": "cart:0",
  "items": [
    { "id": 1, "name": "Product A", "price": 49.90, "quantity": 2 }
  ],
  "errorCode": 0,
  "errorMessage": null
}
```

The `"#"` field is the `instanceId` that binds the JSON state to the corresponding view instance in the shell.

### Virtual Threads for Scalability

Each WebSocket session is managed by a Virtual Thread (Java 21). Unlike platform threads, Virtual Threads are lightweight enough to allocate one per active session — the server can hold thousands of simultaneous sessions without the overhead of traditional thread pools.

---

## The Scope as a Serialization Contract

Besides being the public state read by the view for rendering, the `Scope` serves as the serialization contract for remote mode.

The `Scope` does not implement serialization: its fields are `Prop`s (and the actions, plain fields). Serialization is done exclusively by the framework's `ScopeSerializer`:

- Iterates over all `public` fields of the Scope via reflection
- Skips `static` and `transient` fields
- Injects `"#": instanceId` as the first field of the JSON object
- Serializes `CubeView`/`Scope` fields as `"<name>Id": vsid` (reference by ID)
- Supports primitives, `String`, collections, `Map`, `Record` and POJOs with public fields

The `Scope` itself never implements serialization logic — that is the framework's responsibility, not the application code's.

---

## Conclusions

The architectural model has proven robust over a decade of significant technological evolution — and, more importantly, it has expanded into domains that were initially future work.

The principles that proved most durable:

**The Presenter hierarchy as the unit of modularity.** Dividing the application state into hierarchical Presenters keeps complexity localized. Adding a new module means creating a new Presenter and its keys class (which declares the place), listing it in `ShoppingApplication.PLACES` and implementing a view in each technology of interest — without touching the rest of the application.

**The Scope as the boundary between logic and presentation.** The Scope is at once simple (public Java fields) and powerful (automatic serialization, incremental diff in remote mode). It solves the data transfer problem without creating separate value transfer objects — the same object serves the Presenter to feed the view and the remote protocol to transmit the state.

**The slot as a coupling-free composition mechanism.** The slot isolates the parent Presenter from the child. The parent does not know which type of child Presenter will be in the slot — only that it will receive a `Scope`. The child does not know which part of the parent screen it will be in.

**The view catalog as an extension point.** The `ScopeViews` that each host hands to the application (`setScopeViews`) is the only point of variability between view implementations. Registering other factories per scope class is enough to swap the entire UI layer — and the remote host does not even register: `fallback(RemoteViewImpl::forScope)` draws any scope by its `CLASS_ID`.

The **Remote Presentation mode** is the central innovation: it demonstrates that the separation between presentation logic and rendering is not just a good architectural practice — it is a physical property of the architecture. The Presenter genuinely does not know where the view runs: it may be a local process in the same JVM, a browser thousands of miles away, or a mobile device connected over WebSocket.

The choice of Java was decisive. The static type system eliminates an entire class of integration errors and makes the code more traceable. The JVM platform, with Virtual Threads, allows remote mode to scale at a viable computational cost. The Maven build produces deterministic results. And the longevity of the language ensures that code written today will still be understandable in 2036.

---

## Future Work

- **Dynamic module loading**: the architecture supports the concept, but there is no reference implementation for lazy loading of Java modules in remote mode.  
- **Persistent drafts**: the application instance in the backend may be discarded for several reasons — server restart, closing and reopening the page, session expiration. On resuming, the Presenter could use an identifier present in the URL to fetch previously saved draft data from external storage (database, Redis, etc.). To the user, it would be imperceptible that this is a new application instance in the backend — the form state would be preserved exactly as it was left.  
- **Porting remote mode to other environments**: the host/shell protocol is documented well enough to be implemented in Swift (native iOS) or Kotlin (native Android) without Flutter.  
- **End-to-end integration tests of the remote protocol**: navigation flows are tested through mock views. It would be valuable to add tests that exercise the full cycle — WebSocket → Presenter → serialized Scope → deserialization in the shell.
