/// Client-side key-value storage abstraction.
///
/// Mirrors the Java `ClientStorage` design: two scopes ([session] and
/// [persistent]) with an optional [secure] view for sensitive values.
///
/// **Session scope** (`ClientStorage.session`): lives while the app is running;
/// equivalent to `sessionStorage` in the browser. In standalone apps, data is
/// lost when the app exits.
///
/// **Persistent scope** (`ClientStorage.persistent`): survives app restarts;
/// equivalent to `localStorage` in the browser. Backed by `SharedPreferences`
/// (or `flutter_secure_storage` for the secure view).
///
/// **Secure view** (`scope.secure`): instructs the implementation to use a
/// secure backing store (Keychain / Android Keystore via
/// `flutter_secure_storage`). All values that travel over WebSocket are
/// ciphered regardless of this flag.
///
/// **Synchronisation protocol**: on WebSocket bootstrap the client sends all
/// keys from both scopes under the `"storage"` field; the server sends deltas
/// back in `"storage"` fields of response envelopes.
library client_storage;

import 'dart:collection';

// ---------------------------------------------------------------------------
// Public interface
// ---------------------------------------------------------------------------

abstract class ClientStorage {
  /// Returns a view of this scope that uses secure backing (Keychain /
  /// flutter_secure_storage). Implementations that don't support secure
  /// backing may return `this`.
  ClientStorage get secure;

  /// Returns the value for [key], or `null` if absent.
  String? get(String key);

  /// Stores [value] under [key]. A `null` [value] removes the entry.
  void set(String key, String value);

  /// Removes [key].
  void remove(String key);

  /// All entries in this scope as an unmodifiable map. Used when building the
  /// bootstrap payload to send to the server.
  Map<String, String> get all;
}

// ---------------------------------------------------------------------------
// In-memory implementation (session scope + fallback)
// ---------------------------------------------------------------------------

class InMemoryClientStorage implements ClientStorage {
  final _data = <String, String>{};

  @override
  ClientStorage get secure => this;

  @override
  String? get(String key) => _data[key];

  @override
  void set(String key, String value) => _data[key] = value;

  @override
  void remove(String key) => _data.remove(key);

  @override
  Map<String, String> get all => UnmodifiableMapView(_data);
}

// ---------------------------------------------------------------------------
// SharedPreferences-backed implementation (persistent scope)
// ---------------------------------------------------------------------------
//
// Platform code is responsible for constructing this with the right backing.
// The constructor takes a get/set/remove/all delegate so that the bridge
// itself has no direct dependency on `shared_preferences` or
// `flutter_secure_storage` — those are injected by the shell (main.dart).

typedef StorageGet = String? Function(String key);
typedef StorageSet = void Function(String key, String value);
typedef StorageRemove = void Function(String key);
typedef StorageAll = Map<String, String> Function();

/// Delegate-backed [ClientStorage] implementation. The shell (main.dart)
/// wires this to `SharedPreferences` for the plain view and
/// `FlutterSecureStorage` for the secure view.
class DelegateClientStorage implements ClientStorage {
  final StorageGet _get;
  final StorageSet _set;
  final StorageRemove _remove;
  final StorageAll _all;
  final ClientStorage Function() _secureFactory;

  DelegateClientStorage({
    required StorageGet get,
    required StorageSet set,
    required StorageRemove remove,
    required StorageAll all,
    required ClientStorage Function() secureFactory,
  }) : _get = get,
       _set = set,
       _remove = remove,
       _all = all,
       _secureFactory = secureFactory;

  ClientStorage? _secure;

  @override
  ClientStorage get secure => _secure ??= _secureFactory();

  @override
  String? get(String key) => _get(key);

  @override
  void set(String key, String value) => _set(key, value);

  @override
  void remove(String key) => _remove(key);

  @override
  Map<String, String> get all => _all();
}
