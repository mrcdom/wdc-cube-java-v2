import 'package:web_socket_channel/web_socket_channel.dart';

import 'client_storage.dart';

/// Factory that creates a widget given its vsid and optional props.
typedef ViewFactory = Object Function(String vsid, Map<String, dynamic> props);

/// Form data map: { vsid -> { fieldName -> value } } + event + requestId.
typedef FormMapType = Map<String, dynamic>;

/// Opens the WebSocket channel to [uri] with the given [protocols].
typedef ChannelConnector =
    WebSocketChannel Function(Uri uri, Iterable<String> protocols);

/// A form value that travels ciphered with the connection key (e.g.
/// `p.password`). It is ciphered when its request goes out, so it is never
/// sent in plaintext and always with the key of the connection that carries it.
class SecretValue {
  final String text;

  const SecretValue(this.text);

  @override
  String toString() => 'SecretValue(***)';
}

/// Configuration needed to bootstrap the coordinator (platform provides these).
class CoordinatorConfig {
  final String appId;
  final String baseWebSocketUrl;

  /// Session-scoped storage (in-memory; lives while the app is running).
  final ClientStorage? sessionStorage;

  /// Persistent-scoped storage (backed by SharedPreferences / Keychain).
  final ClientStorage? persistentStorage;

  /// Called to persist the request sequence counter (survives F5).
  final void Function(int requestCount)? onPersistRequestSeq;

  /// Called once on init to restore the persisted request sequence counter.
  final int Function()? onRestoreRequestSeq;

  /// Opens the WebSocket channel; [WebSocketChannel.connect] when absent.
  final ChannelConnector? connectChannel;

  const CoordinatorConfig({
    required this.appId,
    required this.baseWebSocketUrl,
    this.sessionStorage,
    this.persistentStorage,
    this.onPersistRequestSeq,
    this.onRestoreRequestSeq,
    this.connectChannel,
  });
}
