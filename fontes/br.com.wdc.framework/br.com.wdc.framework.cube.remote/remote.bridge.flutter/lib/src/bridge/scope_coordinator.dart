import 'dart:math';

import 'package:flutter/widgets.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

import 'client_storage.dart';
import 'constants.dart';
import 'data_security.dart';
import 'flush_request_context.dart';
import 'reconnect_controller.dart';
import 'types.dart';
import 'view_garbage_collector.dart';
import 'view_scope.dart';

/// Central orchestrator: view registry, form data, history, submit routing,
/// and WebSocket lifecycle (each connection agrees on its own key, see
/// [DataSecurity]).
class ScopeCoordinator {
  /// The active coordinator instance (set after construction).
  static late ScopeCoordinator instance;

  final String id;
  final String baseWebSocketUrl;

  final Map<String, ViewFactory> viewFactoryMap = {};
  final Map<String, ViewScope> viewMap = {};

  FormMapType formMap = {};

  bool isConnected = false;

  /// Whether the host refused this client's protocol version: the app is out
  /// of date and does not reconnect.
  bool isOutdated = false;

  String path = '/';

  final DataSecurity dataSecurity = DataSecurity();
  late final FlushRequestContext contextExchanger;
  late final ReconnectController reconnectController;
  late final ViewGarbageCollector viewGarbageCollector;

  /// Callback for when the server signals session invalid (code 4001).
  void Function() onSessionInvalid = () {};

  /// Callback for when the host refuses this client's protocol version
  /// (`unsupported-protocol`, close 4002). The browser view also gets the
  /// field `outdated: true`.
  void Function() onOutdated = _noopSync;

  /// Callback for when the server changes the URI.
  void Function(String uri) onUriChanged = _defaultOnUriChanged;

  /// Called to persist the request sequence counter (survives F5).
  void Function(int requestCount)? onPersistRequestSeq;

  /// Called once on init to restore the persisted request sequence counter.
  int Function()? onRestoreRequestSeq;

  /// Session-scoped storage: lives while the app is running.
  late final ClientStorage sessionStorage;

  /// Persistent-scoped storage: survives app restarts, synced on bootstrap.
  late final ClientStorage persistentStorage;

  /// Opens the WebSocket channel.
  final ChannelConnector connectChannel;

  ScopeCoordinator(CoordinatorConfig config)
    : id = config.appId,
      baseWebSocketUrl = config.baseWebSocketUrl,
      connectChannel = config.connectChannel ?? _connectWebSocket {
    instance = this;
    viewMap[browserVsid] = ViewScope(browserVsid);

    onPersistRequestSeq = config.onPersistRequestSeq;
    onRestoreRequestSeq = config.onRestoreRequestSeq;
    sessionStorage = config.sessionStorage ?? InMemoryClientStorage();
    persistentStorage = config.persistentStorage ?? InMemoryClientStorage();

    contextExchanger = FlushRequestContext(this);
    reconnectController = ReconnectController(this);
    viewGarbageCollector = ViewGarbageCollector(this);
  }

  /// Convenience factory that generates a random appId.
  factory ScopeCoordinator.withRandomId({required String baseWebSocketUrl}) {
    final appId = _makeUniqueId();
    return ScopeCoordinator(
      CoordinatorConfig(appId: appId, baseWebSocketUrl: baseWebSocketUrl),
    );
  }

  // :: View Registry

  void registerView(String viewId, ViewFactory factory) {
    viewFactoryMap[viewId] = factory;
  }

  Object? createView(String vsid, [Map<String, dynamic>? props]) {
    final parts = vsid.split(':');
    final factory = viewFactoryMap[parts[0]];
    if (factory == null) {
      throw StateError('No view registered for viewId: "${parts[0]}"');
    }

    viewMap.putIfAbsent(vsid, () => ViewScope(vsid));
    return factory(vsid, props ?? {});
  }

  /// Creates a view widget, returning SizedBox.shrink() if the factory doesn't produce a Widget.
  Widget createViewWidget(String vsid, [Map<String, dynamic>? props]) {
    final obj = createView(vsid, props);
    return obj is Widget ? obj : const SizedBox.shrink();
  }

  // :: Connection

  void connect() {
    if (isOutdated) return;
    reconnectController.checkNow();
  }

  void assureContextExchangerIsConnected() {
    if (isOutdated) return;
    contextExchanger.open(reconnectController.url);
  }

  // :: Lifecycle

  void onStart() {
    assureContextExchangerIsConnected();
    setFormField(browserVsid, 'p.path', path);
    submit(browserVsid, -1);
  }

  void onStop() {
    contextExchanger.close();
    reconnectController.close();
  }

  // :: State Management

  void applyScopes(List<dynamic> scopeList) {
    for (final scope in scopeList) {
      if (scope is! Map<String, dynamic>) continue;
      final vsid = scope['#'] as String?;
      if (vsid == null) continue;

      var viewScope = viewMap[vsid];
      if (viewScope == null) {
        viewScope = ViewScope(vsid);
        viewMap[vsid] = viewScope;
      }
      viewScope.setValues(Map<String, dynamic>.from(scope));
    }
  }

  /// Applies a storage delta received from the server.
  void applyStorageDelta(Map<String, dynamic> storageDelta) {
    _applyStorageScope(
      storageDelta['session'] as Map<String, dynamic>?,
      sessionStorage,
    );
    _applyStorageScope(
      storageDelta['persistent'] as Map<String, dynamic>?,
      persistentStorage,
    );
    _applyStorageScope(
      storageDelta['persistent-secure'] as Map<String, dynamic>?,
      persistentStorage.secure,
    );
  }

  void _applyStorageScope(Map<String, dynamic>? scope, ClientStorage target) {
    if (scope == null) return;
    for (final entry in scope.entries) {
      final ciphered = entry.value as String?;
      if (ciphered == null) {
        target.remove(entry.key);
      } else if (ciphered.isNotEmpty) {
        final String value;
        try {
          value = dataSecurity.b64Decipher(ciphered);
        } on ArgumentError {
          continue; // A value that does not authenticate is refused.
        }
        target.set(entry.key, value);
      }
    }
  }

  // :: Submit

  void submit(String vsid, int eventId) {
    final oldFormMap = formMap;
    formMap = {};
    final silent = vsid == browserVsid;
    contextExchanger.submit(oldFormMap, vsid, eventId, silent: silent);
  }

  void submitSilent(String vsid, int eventId) {
    final oldFormMap = formMap;
    formMap = {};
    contextExchanger.submit(oldFormMap, vsid, eventId, silent: true);
  }

  void setFormField(String vsid, String fieldName, dynamic fieldValue) {
    final formData =
        formMap.putIfAbsent(vsid, () => <String, dynamic>{})
            as Map<String, dynamic>;
    formData[fieldName] = fieldValue;
  }

  /// Sets a form field that travels ciphered with the connection key (e.g.
  /// `p.password`); it is ciphered when its request goes out.
  void setSecretFormField(String vsid, String fieldName, String fieldValue) {
    setFormField(vsid, fieldName, SecretValue(fieldValue));
  }

  /// Called by the channel when the host refused this client's protocol
  /// version: no reconnection, and the browser view learns it is out of date.
  void markOutdated() {
    if (isOutdated) return;
    isOutdated = true;
    isConnected = false;
    reconnectController.close();
    final browserView = viewMap[browserVsid];
    if (browserView != null) {
      browserView.removeField('error');
      browserView.patchField('outdated', true);
    }
    onOutdated();
  }

  // :: Private helpers

  static void _noopSync() {}

  static void _defaultOnUriChanged(String uri) {}

  static WebSocketChannel _connectWebSocket(
    Uri uri,
    Iterable<String> protocols,
  ) => WebSocketChannel.connect(uri, protocols: protocols);

  static String _makeUniqueId() {
    final random = Random.secure();
    final bytes = List.generate(16, (_) => random.nextInt(256));
    return bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  }
}
