# remote.bridge.react

TypeScript/React bridge for the **Remote Cube MVP** Host/Shell protocol. Implements the client (shell) side of the protocol in the browser — connects to `remote.host` over WebSocket, performs the RSA/AES-GCM security handshake, manages ViewStates and provides React hooks for UI components to render states and emit events.

It is the React counterpart of the Java (`remote.bridge.java`) and TeaVM (`remote.bridge.teavm`) bridges. Consumed by `remote.shell.react` (WDC Store application) and any other React shell on the same protocol.

> `packaging=pom` module — contains only TypeScript/React source code; it produces no JAR.

## Structure

```
src/
  bridge/
    ViewStateCoordinator.ts   # Manages the WS session, dispatches events, distributes ViewState updates
    ViewScope.ts              # React hook/context: exposes a reactive ViewState to components
    DataSecurity.ts           # RSA + AES-GCM handshake (Web Crypto API)
    FlushRequestContext.ts    # Serialization and delivery of events to the host
    ReconnectController.ts    # Progressive backoff on WS reconnection
    ViewGarbageCollector.ts   # Cleanup of deallocated views
    types.ts                  # Shared types (HostResponse, ViewStateSnapshot, ...)
    constants.ts              # Protocol constants
    index.tsx                 # Public entrypoint of the bridge
  utils/
    RSA.ts                    # RSA OAEP encrypt/decrypt via Web Crypto API
    Base64.ts                 # Base64 encode/decode
    UTF8.ts                   # TextEncoder/TextDecoder helpers
    BigIntUtils.ts            # BigInt operations for RSA cryptography
    LangUtils.ts              # General utilities (throttle, etc.)
```

## Integration with a React Shell

```tsx
// Initialize the bridge
const coordinator = new ViewStateCoordinator(wsUrl)

// In a component:
const viewState = ViewScope.use<LoginScope>("login")
coordinator.submit({ eventCode: "LOGIN", formData: { username, password } })
```

## Security

The bridge implements the same security protocol as the Java and TeaVM bridges:

1. **Handshake**: RSA-OAEP (server public key) to encrypt the AES password
2. **Sensitive data**: per-session AES-256-GCM, key derived via PBKDF2 (250,000 iterations)
3. **Navigation signature**: `sign` parameter on each intent, validated by the host
