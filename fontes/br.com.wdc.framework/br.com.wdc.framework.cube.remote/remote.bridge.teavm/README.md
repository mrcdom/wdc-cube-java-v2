# remote.bridge.teavm

TeaVM bridge for the **Remote Cube MVP** Host/Shell protocol. Implements the client (shell) side of the protocol in Java compiled to JavaScript via **TeaVM** — connects to `remote.host` over WebSocket using the browser's Web Crypto API, without any external JS runtime.

It is the TeaVM counterpart of the React (`remote.bridge.react`) and Java (`remote.bridge.java`) bridges. Consumed by `remote.shell.teavm` (shopping application) and any other TeaVM shell on the same protocol.

## Main Classes

### Protocol & State

| Class | Responsibility |
|-------|----------------|
| `ViewStateCoordinator` | Manages the WS connection and the request/response cycle, distributes ViewState updates to the active views |
| `ViewScope` | Reactive state of a view: receives snapshots from the coordinator and notifies the view via `update()` |
| `RemoteView` | Base interface for TeaVM remote views: `render()` + `onViewState(ViewScope)` |
| `FlushRequestContext` | Serializes and sends events to the host; manages the queue of pending flushes |
| `ReconnectController` | Progressive backoff on WebSocket reconnection |
| `ViewGarbageCollector` | Cleanup of deallocated views when the host sends a removal signal |
| `DataSecurity` | RSA + AES-GCM handshake via Web Crypto API; mirrors the server's `RemoteDataSecurity` |
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
| `JsBiObjectConsumer` | JS callback with two object parameters |

## Dependencies

- `org.teavm:teavm-jso` — JSO API (scope: provided)
- `org.teavm:teavm-jso-apis` — browser APIs (WebSocket, etc.) (scope: provided)

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.bridge.teavm -am
```
