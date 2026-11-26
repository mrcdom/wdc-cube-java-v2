# remote.bridge.teavm

TeaVM bridge for the **Remote Cube MVP** Host/Shell protocol. Implements the client (shell) side of the protocol in Java compiled to JavaScript via **TeaVM** — connects to `remote.host` over WebSocket using the browser's Web Crypto API, without any external JS runtime.

It is the TeaVM counterpart of the React (`remote.bridge.react`) and Java (`remote.bridge.java`) bridges. Consumed by `remote.shell.teavm` (WDC Store application) and any other TeaVM shell on the same protocol.

## Main Classes

### Protocol & Scopes

| Class | Responsibility |
|-------|----------------|
| `ScopeCoordinator` | Manages the WS connection and the request/response cycle, distributes scope updates to the active views |
| `ViewScope` | Scope of a view: receives its values from the coordinator (`setValues`) and notifies the view via `forceUpdate()` |
| `RemoteView` | Interface of the TeaVM remote views: `getVsid()`, `getElement()`, `forceUpdate()` |
| `FlushRequestContext` | Owns the WebSocket: the handshake of each connection, then serializes and sends events to the host; manages the queue of pending flushes |
| `Handshake` | The handshake messages of protocol version 1: the client's first message, and how the host's answer is read |
| `ReconnectController` | Linear backoff on WebSocket reconnection: `min(2000·n, 120000)` ms |
| `ViewGarbageCollector` | Cleanup of deallocated views when the host sends a removal signal |
| `DataSecurity` | Holds the current connection's key and ciphers sensitive values with it (AES-256-GCM); mirrors the server's `RemoteDataSecurity` |
| `ClientStorage` / `EncryptedLocalStorage` | Client storage scopes; secure entries live in `localStorage` under the `sec.` prefix, encrypted at rest |
| `JsonParser` | Lightweight JSON parser compiled to JS (no external library dependency) |

### Interop (JSO bridges)

| Class | Responsibility |
|-------|----------------|
| `Console` | `console.log/error/warn` |
| `Cookies` | Reading/writing browser cookies |
| `Timers` | `setTimeout` / `clearTimeout` |
| `WebCrypto` | The protocol's cryptography: P-256 key pair, ECDH + HKDF-SHA256 connection key, AES-256-GCM values; `crypto.subtle`, or `wdcCryptoFallback` where it does not exist |
| `JsRunnable` | JS callback with no parameters |
| `JsIntConsumer` | JS callback with an integer parameter |
| `JsStringConsumer` | JS callback with a string parameter |
| `JsObjectConsumer` | JS callback with an object parameter |

## Protocol Version 1

The bridge speaks version 1 of the remote protocol ([docs/bridge-skeleton-protocol.md](../../../../docs/bridge-skeleton-protocol.md), §6 and §7.3). The page plants only the `app_id` cookie; there is no long-lived key on either side.

1. **Handshake.** On every WebSocket open, the first and only message is `{"protocol":1,"clientKey":"<Base64 SPKI of an ephemeral P-256 public key>"}`. Nothing else goes out (no ping, no request) until the host answers `{"protocol":1,"serverKey":"<Base64 SPKI>"}`.
2. **Connection key.** `HKDF-SHA256(ikm = the 32-byte ECDH x-coordinate, salt = UTF-8 appId, info = "wdc-cube-remote/aes-256-gcm/v1", 32 bytes)`, an AES-256-GCM key held by `DataSecurity` while the connection lasts. A reconnection agrees on a new one.
3. **Bootstrap.** The first message after the answer is the bootstrap (`{"event":[]}` with the client `storage`, its values ciphered with the new key), merged with the requests made meanwhile, which wait in the queue in order. Keep-alive pings start only then. Messages the host sends while the key is being derived are held and applied after.
4. **Ciphered values.** `Base64(IV(12) ‖ ciphertext ‖ tag(16))`, standard Base64 with padding, a random IV per value: storage values in both directions and sensitive event parameters (`p.password`). A value to cipher while there is no key waits for the next one; a sensitive value never goes out in plaintext.
5. **Refusal.** `{"error":"unsupported-protocol","protocols":[1]}` (followed by close `4002`) leaves the client out of date: it does not reconnect, and sets `error.outdated` on the Browser View (`ScopeCoordinator.OUTDATED_FIELD`), whose view is created if the host never sent its scope. The shell tells the user `ScopeCoordinator.OUTDATED_MESSAGE` ("This page is out of date. Reload it to continue."), and its OK reloads the page. Close `4001` discards the stored `app_id` and reloads, as before.

### Plain HTTP (non-secure context)

`crypto.subtle` exists only over HTTPS or on `localhost`. Elsewhere, `WebCrypto` uses the global `wdcCryptoFallback` — P-256 ECDH, HKDF-SHA256 and AES-256-GCM from @noble, with identical parameters — which the page must load before the app. The WDC Store remote shell ships it as `js/crypto-compat.js` (source and build in `remote.shell.teavm`: `src/main/js/crypto-compat.mjs`, `npm run build:crypto-compat`, checked by `npm run test:crypto-compat`). `EncryptedLocalStorage` has no fallback: without `crypto.subtle` its values live in memory only.

## Dependencies

- `org.teavm:teavm-jso` — JSO API (scope: provided)
- `org.teavm:teavm-jso-apis` — browser APIs (WebSocket, etc.) (scope: provided)

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.bridge.teavm -am
```

The JVM tests (`HandshakeTest`) cover the pure-Java part of the handshake; the JavaScript side is checked in the browser, against a host.
