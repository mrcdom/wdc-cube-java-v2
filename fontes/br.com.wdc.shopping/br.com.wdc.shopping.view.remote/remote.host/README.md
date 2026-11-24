# remote.host

Concrete instance of the remote presentation protocol host for the **WeDoCode Store** application. It is the WebSocket server that receives events from the shells (React, TeaVM, Flutter, SWT), dispatches them to the Presenters and returns the modified ViewStates.

## Responsibility

Wire the Shopping application into the framework's `remote.backend` infrastructure: it registers `ShoppingApplicationImpl` in the registry, configures the Javalin server and starts the host on the configured port.

## Classes

| Class | Responsibility |
|--------|------------------|
| `RemoteHostBootstrap` | Entry point: configures Javalin, registers HTTP/WS routes via `remote.backend`, starts the server |
| `ShoppingApplicationImpl` | Concrete `RemoteApplication`: instantiates the Shopping application's views/presenters, full wiring of the object graph |

## Dependencies

- `br.com.wdc.shopping.presentation` — the application's Presenters and ViewStates
- `br.com.wdc.shopping.persistence` — Repositories and data access
- `remote.backend` — Remote protocol infrastructure (framework)
- `io.javalin:javalin` 7.2.4 — HTTP/WebSocket

## Running

The host is started by `br.com.wdc.cube.backend` (which orchestrates the REST backend + remote host in the same JVM). To run only the remote host in isolation:

```bash
cd fontes && mvn clean package -pl br.com.wdc.shopping/br.com.wdc.shopping.view.remote/remote.host -am
java -cp target/... br.com.wdc.shopping.view.remote.host.RemoteHostBootstrap
```

The default port is configured in `work/config/application.toml`.

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.shopping/br.com.wdc.shopping.view.remote/remote.host -am
```
