# remote.bridge.teavm

TeaVM bridge for the **Remote Cube MVP** Host/Shell protocol. Implements the client (shell) side of the protocol in Java compiled to JavaScript via **TeaVM** — connects to `remote.host` over WebSocket using the browser's Web Crypto API, without any external JS runtime.

It is the TeaVM counterpart of the React (`remote.bridge.react`) and Java (`remote.bridge.java`) bridges. Consumed by `remote.shell.teavm` (WDC Store application) and any other TeaVM shell on the same protocol.

## Main Classes

### Protocol & State

| Class | Responsibility |
|-------|----------------|
| `ViewStateCoordinator` | Manages the WS connection and the request/response cycle, distributes ViewState updates to the active views |
| `ViewScope` | State of a view: receives snapshots from the coordinator (`setState`) and notifies the view via `forceUpdate()` |
| `RemoteView` | Interface of the TeaVM remote views: `getVsid()`, `getElement()`, `forceUpdate()` |
| `FlushRequestContext` | Serializes and sends events to the host; manages the queue of pending flushes |
| `ReconnectController` | Linear backoff on WebSocket reconnection: `min(2000·n, 120000)` ms |
| `ViewGarbageCollector` | Cleanup of deallocated views when the host sends a removal signal |
| `DataSecurity` | Handshake (textbook RSA, no padding) + AES-GCM with a PBKDF2-derived key; mirrors the server's `RemoteDataSecurity` |
| `ClientStorage` / `EncryptedLocalStorage` | Client storage scopes; secure entries live in `localStorage` under the `sec.` prefix, encrypted at rest |
| `SecurityBoot` | Initializes `DataSecurity` when the session opens |
| `JsonParser` | Lightweight JSON parser compiled to JS (no external library dependency) |

### Interop (JSO bridges)

| Class | Responsibility |
|-------|----------------|
| `Console` | `console.log/error/warn` |
| `Cookies` | Reading/writing browser cookies |
| `Timers` | `setTimeout` / `clearTimeout` |
| `WebCrypto` | Web Crypto API (SubtleCrypto: importKey, encrypt, decrypt, deriveBits) |
| `JsRunnable` | JS callback with no parameters |
| `JsIntConsumer` | JS callback with an integer parameter |
| `JsStringConsumer` | JS callback with a string parameter |
| `JsObjectConsumer` | JS callback with an object parameter |
| `JsBiObjectConsumer` | JS callback with two object parameters |

## Dependencies

- `org.teavm:teavm-jso` — JSO API (scope: provided)
- `org.teavm:teavm-jso-apis` — browser APIs (WebSocket, etc.) (scope: provided)

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.bridge.teavm -am
```
