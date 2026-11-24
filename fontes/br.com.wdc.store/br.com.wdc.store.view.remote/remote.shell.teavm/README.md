# Remote Shell TeaVM

Java thin client compiled to JavaScript via **TeaVM 0.16.0**, using a **Virtual DOM** (VNode API) with Spectrum Web Components. It communicates with `remote.host` over WebSocket — **no local business logic**.

## Concept

This module is an alternative implementation of the shell (thin client) of the Remote Presentation architecture. It replaces React as the rendering engine while keeping the same bidirectional WebSocket protocol:

- Receives serialized **ViewStates** from the host (deltas/dirty only)
- Renders using a Virtual DOM (efficient diff → real DOM)
- Emits user **interaction events** back to the host

```mermaid
graph TD
    subgraph browser["Browser (JavaScript)"]
        main["js/app.js ← Java compiled to JS by TeaVM"]
        mainClass["Main.java — Entry point: registers the view factories"]
        bridge["bridge/ — WS communication infra + VDom base"]
        views["views/ — 9 views (thin renderers)"]
        main --> mainClass
        main --> bridge
        main --> views
    end
```

## Architecture

```mermaid
graph TD
    subgraph browser["Browser (remote.shell.teavm)"]
        coordinator["ViewStateCoordinator"]
        ws["WebSocket"]
        subgraph vdom["AbstractRemoteView (VDom)"]
            scope["ViewScope (reactive state)"]
            render["render() → VNode tree"]
            diff["diff/patch → real DOM"]
        end
        coordinator --> ws
        coordinator --> vdom
    end

    subgraph host["remote.host"]
        presenters["Presenters + ViewStates"]
    end

    ws <--> host
```

## Project Structure

```mermaid
graph TD
    root["remote.shell.teavm/"]
    buildsh["build.sh<br/><small>Simplified build (TeaVM → JS)</small>"]
    watchsh["watch.sh<br/><small>Automatic rebuild on detected changes</small>"]
    pom["pom.xml<br/><small>Maven + TeaVM plugin</small>"]
    docs["docs/<br/><small>GUIDE_NEW_VIEW.md</small>"]
    srcMain["src/main/"]
    java["java/.../shell/teavm/"]
    main["Main.java<br/><small>Entry point (registers view factories)</small>"]
    bridge["bridge/"]
    arv["AbstractRemoteView.java<br/><small>VDom base (render/diff/useCallback)</small>"]
    vsc["ViewStateCoordinator.java<br/><small>Manages active views + WS protocol</small>"]
    vs["ViewScope.java<br/><small>Reactive state received from the host</small>"]
    frc["FlushRequestContext.java<br/><small>WS communication (request/response)</small>"]
    rc["ReconnectController.java<br/><small>Progressive backoff for reconnection</small>"]
    ds["DataSecurity.java<br/><small>RSA + AES-GCM (session encryption)</small>"]
    jp["JsonParser.java<br/><small>Parsing of JSON responses from the host</small>"]
    vgc["ViewGarbageCollector.java<br/><small>Cleanup of deallocated views</small>"]
    interop["interop/<br/><small>JSO bridges — Console · Timers · JsConsumer/Runnable</small>"]
    views["views/<br/><small>BrowserView · RootView · LoginView · HomeView<br/>ProductsPanelView · ProductView · CartView · ReceiptView · PurchasesPanelView</small>"]
    webapp["webapp/<br/><small>index.html + css/app.css</small>"]

    root --> buildsh
    root --> watchsh
    root --> pom
    root --> docs
    root --> srcMain
    srcMain --> java
    srcMain --> webapp
    java --> main
    java --> bridge
    java --> interop
    java --> views
    bridge --> arv
    bridge --> vsc
    bridge --> vs
    bridge --> frc
    bridge --> rc
    bridge --> ds
    bridge --> jp
    bridge --> vgc
```

## Virtual DOM

Views extend `AbstractRemoteView` and implement `render()`, returning a `VNode` tree:

```java
@Override
protected VNode render() {
    var state = scope.state();
    return div(Css.ROOT).children(
        h5(Css.TITLE).text(state.title),
        spButton("accent")
            .on("click", onConfirm)
            .children(span().text("Confirm"))
    );
}
```

### Reference Optimizations

The event listener diff compares by **reference identity**:

1. **Stable fields** — listeners without parameters:
   ```java
   private final EventListener<Event> onBack = evt -> flush("back");
   ```

2. **`useCallback(key, listener)`** — cached parametric listeners:
   ```java
   .on("click", useCallback("remove-" + id, mkOnRemove(id)))
   ```

### Compact Css

```java
@SuppressWarnings({"java:S1214", "static-access"})
private interface Css {
    CssUtility u = CssUtility.INSTANCE;
    CssComponents c = CssComponents.INSTANCE;
    CssIcons icon = CssIcons.INSTANCE;

    String ROOT = u.PAGE_SCROLL_ROOT;
    String TITLE = clsx(c.CARD_TITLE, u.MB_12);
}
```

## Difference vs. app.teavm (teavm.web)

| Aspect | remote.shell.teavm | app.teavm (teavm.web) |
|---------|-------------------|----------------------|
| **Business logic** | None (thin client) | Complete (standalone SPA) |
| **Communication** | Bidirectional WebSocket | REST (XMLHttpRequest) |
| **State** | Received from the host (`ViewScope`) | Managed locally (Presenters) |
| **Presenters** | Server-side (remote.host) | Client-side (compiled together) |
| **Advantage** | Zero logic in the client | Works offline |

## Build

```bash
# Simple build
JAVA25_HOME=<path-to-jdk-25> ./build.sh

# Full build (installs the framework dependencies first)
JAVA25_HOME=<path-to-jdk-25> ./build.sh --full
```

### Output

```mermaid
graph TD
    root["work/frontend/remote.shell.teavm/"]
    index["index.html"]
    css["css/app.css"]
    js["js/"]
    appJs["app.js<br/><small>Java compiled to JavaScript</small>"]

    root --> index
    root --> css
    root --> js
    js --> appJs
```

## Development

```bash
# Watch mode — automatic rebuild on save
./watch.sh
```

Requires `fswatch` installed (`brew install fswatch` on macOS).

## Running

The shell is served as a static resource by the backend:

```
http://localhost:8080/remote.shell.teavm
```

## Guides

- [How to create a new View](docs/GUIDE_NEW_VIEW.md) — step by step to implement a view in this project

## Screenshots

### Login

![Login](docs/screenshots/01-login.png)

### Product List

![Products](docs/screenshots/02-products.png)

### Product Detail

![Product Detail](docs/screenshots/03-product-detail.png)

### Cart

![Cart](docs/screenshots/04-cart.png)

### Empty Cart

![Empty Cart](docs/screenshots/04-cart-empty.png)

### Receipt

![Receipt](docs/screenshots/05-receipt.png)

### Purchase History

![History](docs/screenshots/06-history.png)
