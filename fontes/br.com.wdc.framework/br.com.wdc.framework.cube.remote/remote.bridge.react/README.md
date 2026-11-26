# remote.bridge.react

TypeScript/React bridge for the **Remote Cube MVP** Host/Shell protocol. Implements the client (shell) side of the protocol in the browser — connects to `remote.host` over WebSocket, agrees on each connection's key with the host (remote protocol version 1: ephemeral ECDH P-256 + HKDF-SHA256 → AES-256-GCM), keeps the scopes the host publishes (each presenter's view state, serialized) and exposes a small API (`bridge`, the default export) for UI components to render scopes and emit events.

It is the React counterpart of the Java (`remote.bridge.java`) and TeaVM (`remote.bridge.teavm`) bridges. Consumed by `remote.shell.react` (WDC Store application) and any other React shell on the same protocol.

> `packaging=pom` module — contains only TypeScript/React source code; it produces no JAR.

## Structure

```
src/
  bridge/
    ScopeCoordinator.ts       # Manages the WS session, dispatches events, distributes scope updates
    ViewScope.ts              # Holds one view's scope (getValues/setValues); calls forceUpdate() on a new scope
    ChannelCrypto.ts          # Protocol version 1 primitives: ECDH P-256, HKDF-SHA256, AES-256-GCM; Web Crypto
                              # when available, @noble/curves + @noble/hashes + @noble/ciphers on plain HTTP
    DataSecurity.ts           # The current connection's handshake and key; ciphering waits for the key
    ClientStorage.ts          # ClientStorage contract + InMemoryClientStorage; secure entries use the "sec." prefix
    EncryptedLocalStorage.ts  # localStorage entries under "<syncNamespace>sec.", encrypted at rest
    FlushRequestContext.ts    # Connection lifecycle (handshake, bootstrap, refusal of the version), serialization
                              # and delivery of events to the host
    ReconnectController.ts    # Linear backoff on WS reconnection: min(2000·n, 120000) ms
    ViewGarbageCollector.ts   # Cleanup of deallocated views
    types.ts                  # Shared types (BrowserScope, ViewProps, ...)
    constants.ts              # Protocol constants
    index.tsx                 # Public entrypoint of the bridge
  utils/
    Base64.ts                 # Base64 encode/decode
    UTF8.ts                   # TextEncoder/TextDecoder helpers
    LangUtils.ts              # General utilities (throttle, etc.)
test/
  conformance.test.ts         # Runs ../conformance/client/*.json against the bridge (fake WebSocket as the host)
  channel-crypto.test.ts      # The primitives, both implementations (RFC 5869 vector, ECDH, tampering)
  bridge-session.test.ts      # Queued requests, ciphering before the handshake, reconnection with a new key
  remote-host.ts              # The host's end for the tests: fake WebSocket, Node's crypto
  browser-globals.ts          # The browser globals the bridge reads, for running it in Node
```

## Integration with a React Shell

```tsx
import bridge from "@wdc/remote-bridge-react"

// Initialize the bridge: the namespace isolates this shell's client storage keys
bridge.configure("~rr:") // optional 2nd argument: { outdatedMessage } — the alert when the host refuses the version
bridge.registerComponents(BrowserView, LoginView, CartView /* , ... the shell's view components */)

// In a view component (registered under the scope's CLASS_ID; vsid is its instance id), firing event 1 of the login:
bridge.setFormField(vsid, "p.userName", userName)
bridge.setFormField(vsid, "p.password", await bridge.cipher(password))
bridge.submit(vsid, 1)
```

## Security

The bridge speaks **remote protocol version 1** ([docs/bridge-skeleton-protocol.md](../../../../docs/bridge-skeleton-protocol.md) §6), like the Java and TeaVM bridges:

1. **Session identity**: the page reads the `app_id` cookie the host plants (and removes it), then keeps the `appId` in session storage, so an F5 reuses the same session.
2. **Handshake, on every connection** (the first and each reconnection): the first and only message is `{"protocol": 1, "clientKey": "<Base64 SPKI of an ephemeral P-256 public key>"}`. Nothing else goes out — no request, no ping — until the host answers `{"protocol": 1, "serverKey": "..."}`; requests made meanwhile wait and go out after it, in order, in the bootstrap message (with the bootstrap `storage`, ciphered with the new key).
3. **Connection key**: `HKDF-SHA256(ikm = ECDH x-coordinate (32 bytes), salt = UTF-8 appId, info = "wdc-cube-remote/aes-256-gcm/v1")`, 32 bytes. No long-lived key exists on either side.
4. **Ciphered values** (storage in both directions, sensitive event parameters such as `p.password` via `bridge.cipher()`): AES-256-GCM, 128-bit tag, a random 12-byte IV per value, `Base64(IV ‖ ciphertext ‖ tag)`. `bridge.cipher()` waits for the connection's key, so a value is never sent in plaintext. A storage value that does not authenticate is refused.
5. **Version refusal**: on `{"error": "unsupported-protocol"}` (or close `4002`) the bridge stops for good — it does not reconnect — and the browser view shows the alert "This page is out of date. Reload it to continue." (`scope.outdated` is set); its OK reloads the page.
6. **Navigation signature**: `sign` parameter on each intent, validated by the host.

Web Crypto exists only in secure contexts (HTTPS or localhost); over plain HTTP the bridge uses `@noble/curves`, `@noble/hashes` and `@noble/ciphers`, with identical parameters.

## Tests

```bash
npm install
npm test        # Vitest: the client conformance fixtures, the crypto (both implementations), the connection lifecycle
```
