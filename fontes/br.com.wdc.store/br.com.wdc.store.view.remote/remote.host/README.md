# remote.host

Concrete instance of the remote presentation protocol host for the **WDC Store** application. It is the WebSocket server that receives events from the shells (React, TeaVM, Flutter, SWT), dispatches them to the Presenters and returns the modified scopes.

## Responsibility

Wire the WDC Store application into the framework's `remote.backend` infrastructure: it registers `StoreApplicationImpl` in the registry and adds the remote routes (HTTP + WebSocket) to the Javalin configuration of the backend that hosts it.

## Classes

| Class | Responsibility |
|--------|------------------|
| `RemoteHostBootstrap` | `configure(JavalinConfig)`: ensures the `[remote]` keys, then registers the HTTP/WS routes via `remote.backend`; `start(Defer)`: starts the application registry (flush loop, expiry checker) |
| `StoreApplicationImpl` | Concrete `StoreApplication` + `RemoteApplication`: delegates the protocol to `RemoteApplicationSupport`, which draws every scope with a `RemoteViewImpl` found by its `CLASS_ID` |

## Dependencies

- `br.com.wdc.store.presentation` — the application's Presenters and Scopes
- `persistence.impl` — Repositories (jOOQ) and data access
- `remote.backend` — Remote protocol infrastructure (framework)
- `io.javalin:javalin` 7.2.4 — HTTP/WebSocket

## Running

The host has no entry point of its own: `br.com.wdc.cube.backend` (which runs the REST backend + remote host in the same JVM) calls `RemoteHostBootstrap.configure(...)` while building its Javalin server and `RemoteHostBootstrap.start(...)` from `BusinessContext`. Run the backend:

```bash
./work/bin/start-server.sh
```

The port is configured in `work/config/application.toml` (`[server]`, 8080 by default).

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.store/br.com.wdc.store.view.remote/remote.host -am
```
