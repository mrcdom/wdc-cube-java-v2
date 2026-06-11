import 'client_storage.dart';

/// Factory that creates a widget given its vsid and optional props.
typedef ViewFactory = Object Function(String vsid, Map<String, dynamic> props);

/// Form data map: { vsid -> { fieldName -> value } } + event + requestId.
typedef FormMapType = Map<String, dynamic>;

/// Configuration needed to bootstrap the coordinator (platform provides these).
class CoordinatorConfig {
  final String appId;
  final String? securityKey;
  final String baseWebSocketUrl;

  /// Session-scoped storage (in-memory; lives while the app is running).
  final ClientStorage? sessionStorage;

  /// Persistent-scoped storage (backed by SharedPreferences / Keychain).
  final ClientStorage? persistentStorage;

  /// Called when the signature cookie needs to be set.
  final void Function(String name, String value)? onSetCookie;

  /// Called to persist the request sequence counter (survives F5).
  final void Function(int requestCount)? onPersistRequestSeq;

  /// Called once on init to restore the persisted request sequence counter.
  final int Function()? onRestoreRequestSeq;

  const CoordinatorConfig({
    required this.appId,
    this.securityKey,
    required this.baseWebSocketUrl,
    this.sessionStorage,
    this.persistentStorage,
    this.onSetCookie,
    this.onPersistRequestSeq,
    this.onRestoreRequestSeq,
  });
}
