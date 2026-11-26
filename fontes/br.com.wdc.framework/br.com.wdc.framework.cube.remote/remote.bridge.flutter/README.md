# remote.bridge.flutter

Flutter/Dart bridge for the **Remote Cube MVP** Host/Shell protocol. Implements the client (shell) side of the protocol in Dart — connects to `remote.host` over WebSocket, agrees on a key of its own for each connection (the handshake of protocol version 1), keeps the scopes — the view state each presenter publishes for its view, serialized as a JSON map — and provides base classes for Flutter widgets to render them and emit events.

It is the Flutter counterpart of the React (`remote.bridge.react`), TeaVM (`remote.bridge.teavm`) and Java (`remote.bridge.java`) bridges. Consumed by `flutter_commons` (and indirectly by all Flutter shells).

## Structure

```
lib/
  remote_bridge_flutter.dart        # Public entrypoint — exports the whole API
  src/
    bridge/
      ScopeCoordinator              # Central orchestrator: view registry, WS lifecycle,
                                    # submit, history, security, form data
      ViewScope                     # Holder of a view's scope (JSON map from the server);
                                    # notifies the widget via forceUpdate()
      ClientStorage                 # Client storage contract (session / persistent, with a secure view)
      ChannelCrypto                 # Primitives of protocol version 1: P-256 keys (SPKI), ECDH,
                                    # HKDF-SHA256, AES-256-GCM values (Base64(IV ‖ ciphertext ‖ tag))
      DataSecurity                  # The current connection's key: begin/complete handshake, cipher
      FlushRequestContext           # Handshake on every connection, then serializes and sends
                                    # events to the host (requests made meanwhile wait)
      ReconnectController           # Linear backoff for WebSocket reconnection: min(2000·n, 120000) ms
      ViewGarbageCollector          # Cleanup of ViewScopes of views removed by the host
      types.dart                    # Shared types (CoordinatorConfig, SecretValue, ...)
      constants.dart                # Protocol constants (fixed viewIds, timeouts, close codes)
    views/
      BaseView / BaseViewState      # Base StatefulWidget for bridge-driven views; binds to the
                                    # ViewScope via vsid, rebuilds on a new scope; scope,
                                    # submit(eventId), setFormField(...), slot(vsid)
      SlotView                      # View that renders the child the server assigned to the slot
test/
  channel_crypto_test.dart          # Primitives: RFC 5869 HKDF vector, JDK interop vector, SPKI, GCM
  conformance_test.dart             # Runs ../conformance/client/*.json over a fake WebSocket
  channel_lifecycle_test.dart       # Secret fields, reconnection with a new key, close 4002
```

## Dependencies

| Package | Use |
|---------|-----|
| `web_socket_channel` ^3.0 | WebSocket communication with the host |
| `pointycastle` ^3.9 | ECDH P-256, HKDF-SHA256 and AES-256-GCM |

## Integration into a Flutter shell

Add it as a path dependency in the shell's `pubspec.yaml`:

```yaml
dependencies:
  remote_bridge_flutter:
    path: ../../../../../../br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.bridge.flutter
```

Initialize the bridge in `main.dart` (the shell gets the `appId` from the `app_id` cookie in the browser, or from `GET /api/session/init` on desktop/mobile) and register a factory per `CLASS_ID`:

```dart
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

final coordinator = ScopeCoordinator(
  CoordinatorConfig(
    appId: appId,
    baseWebSocketUrl: baseWsUrl,
  ),
);
coordinator.registerView(LoginView.viewId, (vsid, _) => LoginView(key: ValueKey(vsid), vsid: vsid));

// A view: reads its scope from the server and fires events by code
class LoginView extends BaseView {
  const LoginView({super.key, required super.vsid});

  static const viewId = 'c677cda52d14';

  @override
  State<LoginView> createState() => _LoginViewState();
}

class _LoginViewState extends BaseViewState<LoginView> {
  @override
  Widget build(BuildContext context) {
    final errorMessage = scope['errorMessage'] as String?;
    // ...
  }

  void _emitLogin() {
    setFormField('p.userName', userName);
    setSecretFormField('p.password', password); // ciphered when the request goes out
    submit(1);
  }
}
```

## Security — protocol version 1

The normative specification is [docs/bridge-skeleton-protocol.md](../../../../docs/bridge-skeleton-protocol.md), §6; the JVM ends do the same in `ChannelCrypto.java` (`framework.commons`).

1. **Handshake on every connection** (the first one and each reconnection): the first and only message is `{"protocol": 1, "clientKey": "<Base64 SPKI of an ephemeral P-256 public key>"}`. Nothing else — no request, no ping — goes out before the host answers `{"protocol": 1, "serverKey": "..."}`. Requests made meanwhile wait and go out after it, in order, behind the bootstrap message (with the client storage, ciphered with the new key).
2. **Connection key**: `HKDF-SHA256(ikm = ECDH x-coordinate (32 bytes), salt = UTF-8 appId, info = "wdc-cube-remote/aes-256-gcm/v1", 32 bytes)`. A reconnection agrees on a new one; nothing long-lived is kept.
3. **Ciphered values** (storage values both ways, `p.password`): AES-256-GCM, 128-bit tag, a random 12-byte IV per value, on the wire as `Base64(IV ‖ ciphertext ‖ tag)`. A form field set with `setSecretFormField` holds a `SecretValue` and is ciphered when its request goes out, so it never travels in plaintext and always under the key of the connection that carries it. A storage value that does not authenticate is refused.
4. **Refusal**: `{"error": "unsupported-protocol", "protocols": [...]}` and/or close `4002` → the coordinator does not reconnect, sets `isOutdated`, puts `outdated: true` on the browser view's scope and calls `onOutdated`; the shell tells the user the app is out of date. Close `4001` (forged `appId`, or a rejected key) calls `onSessionInvalid`.
5. **Navigation signature**: the `sign` parameter of each address is the host's (HMAC-SHA256); the client only sends it back.

## Tests

```bash
flutter test
```

`CoordinatorConfig.connectChannel` replaces `WebSocketChannel.connect`, which is how the tests drive the client over a fake channel.
