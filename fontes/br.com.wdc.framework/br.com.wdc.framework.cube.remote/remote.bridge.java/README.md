# remote.bridge.java

Java bridge for the **Remote Cube MVP** Host/Shell protocol. Implements the client (shell) side of the protocol in plain Java — connects to `remote.host` over WebSocket, performs the RSA/AES security handshake, navigates the presenter hierarchy, submits events and reads the received ViewStates.

It is the Java counterpart of the React (`remote.bridge.react`) and TeaVM (`remote.bridge.teavm`) bridges — same protocol semantics, different technology.

## Responsibility

Provide the connectivity infrastructure for any Java client that needs to talk to a remote host without a graphical interface — used by `remote.shell.probe` (benchmarks/tests) and `remote.shell.swt` (SWT desktop shell).

## Main Classes

| Class | Responsibility |
|-------|----------------|
| `HostClient` | Entry point: creates sessions with the host, manages the WebSocket connection lifecycle |
| `HostClientSession` | Active session: submits events (`submit()`), receives a `HostResponse` with the ViewState delta |
| `AbstractPresenterClient` | Base for presenter-typed clients — makes it easy to script flows (e.g. login, purchase) |
| `ClientCrypto` | RSA handshake + AES-GCM derivation via PBKDF2; mirrors the server's `RemoteDataSecurity` |

`HostClient` exposes two storage scopes per session:

| Method | Backing | Lifecycle |
|--------|---------|-----------|
| `clientSessionStore()` | `InMemoryClientStorage` | Lasts as long as the connection exists |
| `clientPersistentStore()` | `InMemoryClientStorage` by default (via `connect(url)`); customizable via `connect(url, storage)` | Configurable |

The persistent storage is bootstrapped to the server at `connect()` time — it lets the server restore the previous session's state (e.g. auto-login via refresh token).

### Models

| Class | Responsibility |
|-------|----------------|
| `HostResponse` | Host response: `requestId`, `uri`, list of `ViewStateSnapshot` |
| `ViewStateSnapshot` | State of a view: `instanceId` + field map |
| `ViewStateMap` | Typed map of a ViewState's fields |
| `SecretContext` | Session cryptographic context (derived AES key, IV, salt) |

## Dependencies

- `br.com.wdc.framework.commons` — Base62, logging
- `com.google.code.gson` — parsing of the host's JSON response
- `org.slf4j:slf4j-api` — logging
- `org.apache.commons:commons-lang3`

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.bridge.java -am
```
