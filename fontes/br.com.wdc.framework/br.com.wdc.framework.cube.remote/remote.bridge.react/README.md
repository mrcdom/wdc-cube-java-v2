# remote.bridge.react

TypeScript/React bridge for the **Remote Cube MVP** Host/Shell protocol. Implements the client (shell) side of the protocol in the browser — connects to `remote.host` over WebSocket, performs the RSA/AES-GCM security handshake, manages ViewStates and exposes a small API (`bridge`, the default export) for UI components to render states and emit events.

It is the React counterpart of the Java (`remote.bridge.java`) and TeaVM (`remote.bridge.teavm`) bridges. Consumed by `remote.shell.react` (WDC Store application) and any other React shell on the same protocol.

> `packaging=pom` module — contains only TypeScript/React source code; it produces no JAR.

## Structure

```
src/
  bridge/
    ViewStateCoordinator.ts   # Manages the WS session, dispatches events, distributes ViewState updates
    ViewScope.ts              # State of one view (getState/setState); calls forceUpdate() on a new state
    DataSecurity.ts           # Handshake (textbook RSA + PBKDF2) and AES-GCM; Web Crypto when available,
                              # @noble/hashes + @noble/ciphers on plain HTTP
    ClientStorage.ts          # ClientStorage contract + InMemoryClientStorage; secure entries use the "sec." prefix
    EncryptedLocalStorage.ts  # localStorage entries under "<syncNamespace>sec.", encrypted at rest
    FlushRequestContext.ts    # Serialization and delivery of events to the host
    ReconnectController.ts    # Linear backoff on WS reconnection: min(2000·n, 120000) ms
    ViewGarbageCollector.ts   # Cleanup of deallocated views
    types.ts                  # Shared types (HostResponse, ViewStateSnapshot, ...)
    constants.ts              # Protocol constants
    index.tsx                 # Public entrypoint of the bridge
  utils/
    RSA.ts                    # Textbook RSA (modular exponentiation, no padding) over BigInt
    Base64.ts                 # Base64 encode/decode
    UTF8.ts                   # TextEncoder/TextDecoder helpers
    BigIntUtils.ts            # BigInt operations for RSA cryptography
    LangUtils.ts              # General utilities (throttle, etc.)
```

## Integration with a React Shell

```tsx
import bridge from "@wdc/remote-bridge-react"

// Initialize the bridge: the namespace isolates this shell's client storage keys
bridge.configure("~rr:")
bridge.registerComponents(BrowserView, LoginView, CartView /* , ... the shell's view components */)

// In a view component (registered under the scope's CLASS_ID; vsid is its instance id), firing event 1 of the login:
bridge.setFormField(vsid, "p.userName", userName)
bridge.setFormField(vsid, "p.password", await bridge.cipher(password))
bridge.submit(vsid, 1)
```

## Security

The bridge implements the same security protocol as the Java and TeaVM bridges:

1. **Handshake**: textbook RSA (server public key, no padding) to encrypt the ephemeral AES password
2. **Sensitive data**: per-session AES-256-GCM, key derived via PBKDF2 (250,000 iterations)
3. **Navigation signature**: `sign` parameter on each intent, validated by the host
