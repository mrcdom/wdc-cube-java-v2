# remote.backend

Server-side infrastructure for the **Cube MVP** remote presentation protocol. Provides the generic abstractions that any application based on the Host/Shell protocol must implement — it is the server side of the protocol, independent of business domain.

## Responsibility

Manages the lifecycle of remote sessions (security handshake, event reception, ViewState serialization, delta delivery) by exposing HTTP/WebSocket endpoints via **Javalin 7.2**. The concrete application extends `RemoteApplicationSupport` and registers its views/presenters.

## Main Classes

| Class | Responsibility |
|-------|----------------|
| `RemoteApplication` | SPI — interface the concrete application implements (instantiate views, handle events) |
| `RemoteApplicationSupport` | Base implementation of `RemoteApplication`: dirty tracking, event dispatch, JSON delta response generation |
| `RemoteApplicationRegistry` | Registry of instances per session; creates, looks up and removes `RemoteApplication` by `sessionId`; enforces a capacity limit |
| `RemoteViewImpl` | Abstract base for remote views: `instanceId`, `markDirty()`, `syncClientToServer()`, `writeState()` |
| `RemoteBrowserPresenter` | Coordinates navigation and intent dispatch between views |
| `ScopeSerializer` | Serializes ViewStates to JSON (dirty fields only) |
| `RemoteAppSecurity` | RSA + SHA256withRSA for signing navigation URLs |
| `RemoteDataSecurity` | Per-session AES-GCM, key derived via PBKDF2 (250,000 iterations) for sensitive data |
| `RemoteHostModule` | Configures the module in the container (wiring) |
| `CapacityExceededException` | Thrown when the maximum number of concurrent sessions is reached |

### Javalin Controllers

| Class | Responsibility |
|-------|----------------|
| `DispatcherController` | WebSocket `ws://.../dispatcher/{sessionId}` — receives shell events, runs dispatch, returns the ViewState delta |
| `SessionInitController` | `GET /session-init` — security handshake; negotiates the session AES key via RSA |
| `IndexHtmlController` | `GET /` — serves the static shell's `index.html` |

## Dependencies

- `br.com.wdc.framework.cube` — Cube MVP framework (base: View, Presenter, ViewState)
- `io.javalin:javalin` 7.2.4 — HTTP/WebSocket
- `com.google.code.gson` — JSON serialization
- `org.apache.commons:commons-lang3`
- `commons-codec` — Base64/Hex encoding for crypto

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.backend -am
```
