# remote.backend

Server-side infrastructure for the **Cube MVP** remote presentation protocol. Provides the generic abstractions that any application based on the Host/Shell protocol must implement — it is the server side of the protocol, independent of business domain.

## Responsibility

Manages the lifecycle of remote sessions (security handshake, event reception, scope serialization, delta delivery) by exposing HTTP/WebSocket endpoints via **Javalin 7.2**. The concrete application implements `RemoteApplication` and delegates to a `RemoteApplicationSupport` it holds (`getSupport()`). This module is the remote host's rendering technology: every scope is drawn by a remote view (`RemoteViewImpl`) identified by its `CLASS_ID`, made the first time a scope that holds it is written; the presenters never deal with views.

## Main Classes

| Class | Responsibility |
|-------|----------------|
| `RemoteApplication` | SPI — interface the concrete application implements; its default methods delegate to `getSupport()` (views, dirty tracking, session, security) |
| `RemoteApplicationSupport` | Per-session infrastructure the application delegates to: view map (by id, and `viewOf(scope)` by scope), dirty tracking, event dispatch, history signing, client storage, JSON delta response generation |
| `RemoteApplicationRegistry` | Registry of instances per session; creates, looks up and removes `RemoteApplication` by `sessionId`; enforces a capacity limit. For an application that measures or runs as several instances, it tells of each client message handled (`onMessage`) and of each session made here (`onCreated`), and releases a session no socket holds (`releaseIdle`) |
| `RemoteViewImpl` | The remote view of a scope (made by `forScope`, by `CLASS_ID`): installs itself as the scope's updater; `update()` marks it dirty; `syncClientToServer()` and `submit()` go to the scope's skeleton; `writeScope()` serializes the scope and releases the views it referred to the last time and no longer does (a view is released once no view refers to it, with the views it referred to in turn); `release()` takes the updater back and puts the id in `releasedViews` for the client |
| `RemoteBrowserPresenter` | Presenter of the client's root "browser" view (fixed vsid `7b32e816a191:0`): start (`-1`) and history (`-2`) navigation, alerts (`1`), keep-alive (`2`); its `BrowserScope` holds the root presenter's scope in `content`. The unexpected-error alert (`-1`, `alertUnexpectedError`) carries only the cause's message, which the client words in its user's language |
| `ScopeSerializer` | Serializes a whole scope to JSON via reflection (sent for each dirty view); a scope inside it goes out as `"<field>ViewId"`, a list of scopes as `"<field>ViewIds"` |
| `RemoteAppSecurity` | HMAC-SHA256 signing (`[remote] signingKey`) of the `appId` (`newSessionId`, `isSessionId`) and of the navigation address (`sign`, `verify`, constant-time) |
| `RemoteDataSecurity` | The key the current connection agreed on (ECDH P-256 + HKDF, in `DispatcherController`'s handshake), and the AES-GCM ciphering of sensitive values with an IV per value |
| `RemoteHostModule` | Configures the module in the container (wiring) |
| `CapacityExceededException` | Thrown when the maximum number of concurrent sessions is reached |

### Javalin Controllers

| Class | Responsibility |
|-------|----------------|
| `DispatcherController` | WebSocket `ws://.../dispatcher/{appId}` — validates the `appId`, receives shell events, runs dispatch, returns the scope delta |
| `SessionInitController` | `GET /api/session/init` (and `/{ctx}/api/session/init`) — returns `{appId}` to clients without page cookies; HTTP 503 when the server is at capacity |
| `IndexHtmlController` | `before` handler on `/{ctx}/index.html` — plants the `app_id` session cookie |

## Dependencies

- `framework-cube` — Cube MVP engine (`Scope`, presenters)
- `io.javalin:javalin` 7.2.4 — HTTP/WebSocket
- `com.google.code.gson` — JSON serialization
- `org.apache.commons:commons-lang3`
- `commons-codec` — Base64/Hex encoding for crypto

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.backend -am
```
