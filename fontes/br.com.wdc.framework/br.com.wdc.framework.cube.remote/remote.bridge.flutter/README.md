# remote.bridge.flutter

Flutter/Dart bridge for the **Remote Cube MVP** Host/Shell protocol. Implements the client (shell) side of the protocol in Dart — connects to `remote.host` over WebSocket, performs the RSA/AES-GCM security handshake, manages ViewStates and provides base classes for Flutter widgets to render states and emit events.

It is the Flutter counterpart of the React (`remote.bridge.react`), TeaVM (`remote.bridge.teavm`) and Java (`remote.bridge.java`) bridges. Consumed by `flutter_commons` (and indirectly by all Flutter shells).

## Structure

```
lib/
  remote_bridge_flutter.dart        # Public entrypoint — exports the whole API
  src/
    bridge/
      ViewStateCoordinator          # Central orchestrator: view registry, WS lifecycle,
                                    # submit, history, security, form data
      ViewScope                     # Reactive state of a view (JSON map from the server);
                                    # notifies the widget via forceUpdate()
      DataSecurity                  # Per-session RSA + PBKDF2 + AES-GCM handshake
      FlushRequestContext           # Serializes and sends events to the host
      ReconnectController           # Progressive backoff for WebSocket reconnection
      ViewGarbageCollector          # Cleanup of ViewScopes of views removed by the host
      types.dart                    # Shared types (HostResponse, ViewStateSnapshot, ...)
      constants.dart                # Protocol constants (fixed viewIds, timeouts, ...)
    views/
      BaseView                      # Base StatefulWidget for bridge-driven views;
                                    # binds to the ViewScope via vsid, rebuilds on new state
      SlotView                      # View that renders the child the server assigned to the slot
    utils/
      RSA                           # RSA OAEP encrypt/decrypt in pure Dart
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

Initialize the bridge in `main.dart`:

```dart
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

final coordinator = ViewStateCoordinator(
  id: sessionId,
  baseWebSocketUrl: 'ws://host:8080',
);

// In a view:
class LoginView extends BaseView {
  const LoginView({super.key}) : super(vsid: 'login');

  @override
  Widget buildView(BuildContext context, ViewScope scope) {
    final error = scope.state['error'] as String?;
    // ...
  }
}
```

## Security

Same protocol as the Java/React/TeaVM bridges:

1. **RSA-OAEP handshake**: encrypts the AES password with the host's public key
2. **AES-256-GCM**: per-session key derived via PBKDF2 (250,000 iterations)
3. **Navigation signature**: `sign` parameter on each intent, validated by the host
