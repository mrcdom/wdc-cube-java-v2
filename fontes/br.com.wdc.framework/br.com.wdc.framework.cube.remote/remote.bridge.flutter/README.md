# remote.bridge.flutter

Flutter/Dart bridge for the **Remote Cube MVP** Host/Shell protocol. Implements the client (shell) side of the protocol in Dart — connects to `remote.host` over WebSocket, performs the RSA/AES-GCM security handshake, keeps the scopes — the view state each presenter publishes for its view, serialized as a JSON map — and provides base classes for Flutter widgets to render them and emit events.

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
      DataSecurity                  # Per-session RSA + PBKDF2 + AES-GCM handshake
      FlushRequestContext           # Serializes and sends events to the host
      ReconnectController           # Linear backoff for WebSocket reconnection: min(2000·n, 120000) ms
      ViewGarbageCollector          # Cleanup of ViewScopes of views removed by the host
      types.dart                    # Shared types (CoordinatorConfig, ...)
      constants.dart                # Protocol constants (fixed viewIds, timeouts, ...)
    views/
      BaseView / BaseViewState      # Base StatefulWidget for bridge-driven views; binds to the
                                    # ViewScope via vsid, rebuilds on a new scope; scope,
                                    # submit(eventId), setFormField(...), slot(vsid)
      SlotView                      # View that renders the child the server assigned to the slot
    utils/
      RSA                           # Textbook RSA encryption (modular exponentiation, no padding)
      BigIntUtils                   # Helper BigInt operations for cryptography
```

## Dependencies

| Package | Use |
|---------|-----|
| `web_socket_channel` ^3.0 | WebSocket communication with the host |
| `pointycastle` ^3.9 | RSA + AES-GCM cryptography |

## Integration into a Flutter shell

Add it as a path dependency in the shell's `pubspec.yaml`:

```yaml
dependencies:
  remote_bridge_flutter:
    path: ../../../../../../br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.bridge.flutter
```

Initialize the bridge in `main.dart` (the shell gets `appId`/`appSKey` from `GET /api/session/init`) and register a factory per `CLASS_ID`:

```dart
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

final coordinator = ScopeCoordinator(
  CoordinatorConfig(
    appId: appId,
    securityKey: appSKey,
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
    setFormField('p.password', ScopeCoordinator.instance.dataSecurity.b64Cipher(password));
    submit(1);
  }
}
```

## Security

Same protocol as the Java/React/TeaVM bridges:

1. **RSA handshake** (textbook RSA, no padding): encrypts the ephemeral AES password with the host's public key
2. **AES-256-GCM**: per-session key derived via PBKDF2 (250,000 iterations)
3. **Navigation signature**: `sign` parameter on each intent, validated by the host
