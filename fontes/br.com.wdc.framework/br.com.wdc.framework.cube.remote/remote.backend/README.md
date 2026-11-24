# remote.backend

Server-side infrastructure for the **Cube MVP** remote presentation protocol. Provides the generic abstractions that any application based on the Host/Shell protocol must implement — it is the server side of the protocol, independent of business domain.

## Responsibility

Manages the lifecycle of remote sessions (security handshake, event reception, ViewState serialization, delta delivery) by exposing HTTP/WebSocket endpoints via **Javalin 7.2**. The concrete application implements `RemoteApplication` and delegates to a `RemoteApplicationSupport` it holds (`getSupport()`); it hands the application a `ScopeViews` catalog with `fallback(RemoteViewImpl::forScope)`, so every scope is drawn by a remote view identified by its `CLASS_ID`.

## Main Classes

| Class | Responsibility |
|-------|----------------|
| `RemoteApplication` | SPI — interface the concrete application implements; its default methods delegate to `getSupport()` (views, dirty tracking, session, security) |
| `RemoteApplicationSupport` | Per-session infrastructure the application delegates to: view map, dirty tracking, event dispatch, history signing, client storage, JSON delta response generation |
| `RemoteApplicationRegistry` | Registry of instances per session; creates, looks up and removes `RemoteApplication` by `sessionId`; enforces a capacity limit |
| `RemoteViewImpl` | The remote view of a scope (made by `forScope`, by `CLASS_ID`): `update()` marks it dirty; `syncClientToServer()` and `submit()` go to the scope's skeleton; `writeState()` serializes the scope |
| `RemoteBrowserPresenter` | Presenter of the client's root "browser" view (fixed vsid `7b32e816a191:0`): start (`-1`) and history (`-2`) navigation, alerts (`1`), keep-alive (`2`); its `BrowserScope` holds the root presenter's scope in `content` |
| `ScopeSerializer` | Serializes a whole scope to JSON via reflection (sent for each dirty view); a scope inside it goes out as `"<field>ViewId"`, a list of scopes as `"<field>ViewIds"` |
| `RemoteAppSecurity` | Handshake RSA key pair (textbook RSA) and SHA256withRSA signing (`signAsHash`) of the `appId` and of the navigation address |
| `RemoteDataSecurity` | Per-session AES-GCM, key derived via PBKDF2 (250,000 iterations) for sensitive data |
| `RemoteHostModule` | Configures the module in the container (wiring) |
| `CapacityExceededException` | Thrown when the maximum number of concurrent sessions is reached |

### Javalin Controllers

| Class | Responsibility |
|-------|----------------|
| `DispatcherController` | WebSocket `ws://.../dispatcher/{appId}` — validates the `appId`, receives shell events, runs dispatch, returns the ViewState delta |
| `SessionInitController` | `GET /api/session/init` (and `/{ctx}/api/session/init`) — returns `{appId, appSKey}` to clients without page cookies; HTTP 503 when the server is at capacity |
| `IndexHtmlController` | `before` handler on `/{ctx}/index.html` — plants the `app_id` and `app_skey` session cookies |

## Dependencies

- `framework-cube` — Cube MVP engine (`Scope`, `CubeView`, `ScopeViews`, presenters)
- `io.javalin:javalin` 7.2.4 — HTTP/WebSocket
- `com.google.code.gson` — JSON serialization
- `org.apache.commons:commons-lang3`
- `commons-codec` — Base64/Hex encoding for crypto

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.backend -am
```
