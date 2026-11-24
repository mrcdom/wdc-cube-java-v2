# WDC Framework — Cube Remote

Remote communication protocol for the Cube MVP engine, allowing **thin clients** (browser, TeaVM) to run the presenter logic on the server over WebSocket.

---

## Architecture

```mermaid
graph LR
    subgraph Server
        Host["remote.host<br/>(Javalin + WebSocket)"]
        App["RemoteApplication"]
        Skel["CubeSkeleton"]
    end

    subgraph Client
        Bridge["Bridge<br/>(React or TeaVM)"]
        Coord["ViewStateCoordinator"]
        VS["ViewScope[]"]
    end

    Host <-->|"WebSocket<br/>JSON frames"| Bridge
    App --> Skel
    Bridge --> Coord
    Coord --> VS
```

---

## Submodules

| Submodule | Language | Description |
|-----------|----------|-------------|
| `remote.backend` | Java 21 | Server — `RemoteApplication`, `RemoteApplicationSupport`, `ScopeSerializer`, Javalin controllers (WebSocket + HTTP index) |
| `remote.bridge.java` | Java 21 | Java bridge (fat client) — `HostClient`, `HostClientSession`, `AbstractPresenterClient`, RSA/AES-GCM handshake; used by `remote.shell.probe` and `remote.shell.swt` |
| `remote.bridge.react` | TypeScript/React | React thin-client bridge — `ViewStateCoordinator`, reconnection, data security, view GC |
| `remote.bridge.teavm` | Java (TeaVM) | TeaVM thin-client bridge — same logic as React, compiled to JavaScript via TeaVM |

---

## remote.backend

Main server-side classes:

| Class | Responsibility |
|-------|----------------|
| `RemoteApplication` | Contract for remote apps — authentication, WS session, lifecycle |
| `RemoteApplicationSupport` | Delegated infrastructure — manages id, WsContext, expiration |
| `RemoteApplicationRegistry` | Registers and looks up active `RemoteApplication` instances |
| `RemoteBrowserPresenter` | Presenter of the thin client's "browser" (alert, start, history, keep-alive): a `BrowserScope` with `CLASS_ID` and coded actions, like the others; its view has the fixed id `7b32e816a191:0` and the scope holds the root presenter's scope in `content` |
| `RemoteViewImpl` | The remote view of a scope (`forScope`, by `CLASS_ID`): serializes the state and forwards events to the scope's skeleton |
| `ScopeSerializer` | Serializes the scope → JSON via reflection; a scope inside it is emitted as `"<field>ViewId"`, a list of scopes as `"<field>ViewIds"` |
| `RemoteHostModule` | Javalin module that registers the WS and HTTP routes |
| `DispatcherController` | WebSocket controller — receives events and dispatches them to the `CubeSkeleton` |
| `IndexHtmlController` | Serves the thin client's HTML |
| `RemoteAppSecurity` / `RemoteDataSecurity` | Session and data security |

---

## remote.bridge.react

TypeScript module that runs in the browser:

| File | Responsibility |
|------|----------------|
| `ViewStateCoordinator.ts` | Central coordinator — WebSocket, reconnect, GC, render scheduling |
| `FlushRequestContext.ts` | Serializes forms and sends events to the server |
| `ReconnectController.ts` | Exponential backoff for reconnection |
| `ViewGarbageCollector.ts` | Removes orphan views from the DOM |
| `ViewScope.ts` | Manages the state of an individual view |
| `DataSecurity.ts` | Data encryption (RSA) |
| `utils/` | Base64, BigInt, RSA, UTF8 |

---

## remote.bridge.teavm

Java/TeaVM equivalent of the React bridge:

| Class | Responsibility |
|-------|----------------|
| `ViewStateCoordinator` | Same logic — WebSocket, view factory, rendering via `requestAnimationFrame` |
| `FlushRequestContext` | Server ↔ client context exchange |
| `ReconnectController` | Exponential backoff |
| `ViewGarbageCollector` | View GC |
| `ViewScope` | Local state of a view |
| `DataSecurity` | RSA on the TeaVM side |
| `JsonParser` | Lightweight JSON parsing for TeaVM |
| `RemoteView` | Client-side `CubeView` implementation |
| `interop/` | JS bindings: `Console`, `Timers`, `JsRunnable`, etc. |

---

## Maven Coordinates

```xml
<!-- Backend (server) -->
<dependency>
    <groupId>io.github.mrcdom.wdc.java</groupId>
    <artifactId>framework-cube-remote-backend</artifactId>
    <version>2.0.0</version>
</dependency>

<!-- TeaVM bridge (thin client) -->
<dependency>
    <groupId>io.github.mrcdom.wdc.java</groupId>
    <artifactId>framework-cube-remote-bridge-teavm</artifactId>
    <version>2.0.0</version>
</dependency>
```
