# teavm.commons

Shared library for the TeaVM frontends of the **WeDoCode Store** application. It contains the Virtual DOM engine, the Spectrum Web Components DSL, the CSS class catalogs, browser interop and the **shared views** — one per Scope — that both TeaVM frontends render.

## Responsibility

Hold everything about *drawing* a Scope, independently of where the Scope comes from. Each shared view (`XxxSharedView extends SharedVDomView`) receives the presentation Scope through a `Supplier` and reports interactions through plain callbacks; it knows neither presenters nor the remote protocol. Two modules use it:

- **`teavm.web`** — the client-side SPA: its views (`AbstractVDomView<XxxScope>`) hand the in-memory Scope to the shared view and wrap each callback in `safeAction(...)`.
- **`remote.shell.teavm`** — the thin client: its views (`AbstractRemoteView`) adapt the JSON scope received from the host (`ViewScope`) into a Scope instance and turn each callback into `setFormField` + `submit`.

## Package Structure

```
br.com.wdc.store.view.teavm.commons/
├── VNode.java                         # Virtual tree nodes + element factories (div, span, h5, img, slot, ...) and clsx()
├── VDom.java                          # Virtual DOM engine: diffs two VNode trees and patches the real DOM
├── Swc.java                           # Spectrum Web Components DSL (spButton, spActionButton, spTextField, spTheme, ...)
├── SharedVDomView.java                # Base of the shared views: render/renderTree, useCallback, requestAnimationFrame scheduling
├── SelUtility.java                    # Catalog of utility classes (layout, spacing, typography)
├── SelComponents.java                 # Catalog of component classes (cards, alerts, empty states)
├── SelIcons.java                      # Catalog of Bootstrap Icons classes
├── StyleBuilder.java                  # Fluent builder for inline CSS style strings
├── DateUtils.java                     # Date formatting (en-US) in the browser's time zone
├── devtools/
│   └── DevWatcher.java                # File watcher used by the watch.sh scripts (-Pwatch)
├── interop/                           # JSO bridges for browser APIs
│   ├── Console.java                   # console.log/error/warn
│   ├── Timers.java                    # Browser timer APIs
│   ├── JsRunnable.java                # JS callback with no parameters
│   ├── JsIntConsumer.java             # JS callback with an int parameter
│   ├── JsObjectConsumer.java          # JS callback with an object parameter
│   └── JsStringConsumer.java          # JS callback with a string parameter
└── views/                             # One shared view per Scope, with its SCSS partial
    ├── cart/                          # CartSharedView, CartItemSharedView, _cart.scss
    ├── home/                          # HomeSharedView, ProductsPanelSharedView, PurchasesPanelSharedView,
    │                                  # _products-panel.scss, _purchases.scss
    ├── login/                         # LoginSharedView, _login.scss
    ├── product/                       # ProductSharedView, _product.scss
    └── receipt/                       # ReceiptSharedView, _receipt.scss
```

Global styles live in `src/main/scss/` (`_utilities.scss`, `_components.scss`, `_swc.scss`). Each frontend's `app.scss` `@use`s them and the views' partials (both folders are on its Sass load path) and compiles everything into its own `css/app.css`.

The remote protocol (`ScopeCoordinator`, `ViewScope`, WebSocket, security) lives in the framework module `remote.bridge.teavm`, used only by `remote.shell.teavm`.

## Shared view shape

```java
public class CartItemSharedView extends SharedVDomView {

    public Supplier<CartItemScope> scopeSupplier;   // the Scope to draw
    public IntConsumer onModifyQuantity;           // callbacks bound by each frontend
    public Runnable onRemove;

    @Override
    public VNode render() {
        var row = scopeSupplier.get();
        // ... VNode tree
    }
}
```

The frontend's view calls `shared.renderTree()` from its own `render()`.

## Dependencies

- `br.com.wdc.store.presentation` — Scope classes and structs (scope: provided; TeaVM does tree shaking)
- `org.teavm:teavm-jso` — JSO API (scope: provided)
- `org.teavm:teavm-jso-apis` — browser APIs (scope: provided)

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.store/br.com.wdc.store.view.teavm/teavm.commons -am
```

Both TeaVM frontends compile against the installed JAR in `~/.m2`; `./work/bin/build-frontends.sh` reinstalls it (and the modules it depends on) before building them.
