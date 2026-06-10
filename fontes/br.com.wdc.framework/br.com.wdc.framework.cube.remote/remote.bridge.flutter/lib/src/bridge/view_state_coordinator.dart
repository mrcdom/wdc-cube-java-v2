import 'dart:math';

import 'package:flutter/widgets.dart';

import 'client_storage.dart';
import 'constants.dart';
import 'data_security.dart';
import 'flush_request_context.dart';
import 'reconnect_controller.dart';
import 'types.dart';
import 'view_garbage_collector.dart';
import 'view_scope.dart';

/// Central orchestrator: view registry, form data, history, security init,
/// submit routing, and WebSocket lifecycle.
class ViewStateCoordinator {
  /// The active coordinator instance (set after construction).
  static late ViewStateCoordinator instance;

  final String id;
  final String baseWebSocketUrl;

  final Map<String, ViewFactory> viewFactoryMap = {};
  final Map<String, ViewScope> viewMap = {};

  FormMapType formMap = {};

  bool isConnected = false;
  String path = '/';

  final DataSecurity dataSecurity = DataSecurity();
  late final FlushRequestContext contextExchanger;
  late final ReconnectController reconnectController;
  late final ViewGarbageCollector viewGarbageCollector;

  /// Callback for when the server signals session invalid (code 4001).
  void Function() onSessionInvalid = () {};

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

  /// Sync initialization (security key derivation) — called once on start.
  void Function() readyToStart = _noopSync;

  ViewStateCoordinator(CoordinatorConfig config)
    : id = config.appId,
      baseWebSocketUrl = config.baseWebSocketUrl {
    instance = this;
    viewMap[browserVsid] = ViewScope(browserVsid);

    onPersistRequestSeq = config.onPersistRequestSeq;
    onRestoreRequestSeq = config.onRestoreRequestSeq;
    sessionStorage = config.sessionStorage ?? InMemoryClientStorage();
    persistentStorage = config.persistentStorage ?? InMemoryClientStorage();

    if (config.securityKey != null && config.securityKey!.isNotEmpty) {
      dataSecurity.updateSecurityKey(config.securityKey!);

      readyToStart = () {
        dataSecurity.updateSecretWithRandomPassword();
        config.onSetCookie?.call('app_signature', dataSecurity.getSignature());
        readyToStart = _noopSync;
      };
    }

    contextExchanger = FlushRequestContext(this);
    reconnectController = ReconnectController(this);
    viewGarbageCollector = ViewGarbageCollector(this);
  }

  /// Convenience factory that generates a random appId.
  factory ViewStateCoordinator.withRandomId({
    required String baseWebSocketUrl,
    String? securityKey,
    void Function(String name, String value)? onSetCookie,
  }) {
    final appId = _makeUniqueId();
    return ViewStateCoordinator(
      CoordinatorConfig(
        appId: appId,
        securityKey: securityKey,
        baseWebSocketUrl: baseWebSocketUrl,
        onSetCookie: onSetCookie,
      ),
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
    reconnectController.checkNow();
  }

  void assureContextExchangerIsConnected() {
    contextExchanger.open(reconnectController.url);
  }

  // :: Lifecycle

  void onStart() {
    readyToStart();
    assureContextExchangerIsConnected();
    setFormField(browserVsid, 'p.path', path);
    submit(browserVsid, -1);
  }

  void onStop() {
    contextExchanger.close();
    reconnectController.close();
  }

  // :: State Management

  void applyViewStates(List<dynamic> stateList) {
    for (final viewState in stateList) {
      if (viewState is! Map<String, dynamic>) continue;
      final vsid = viewState['#'] as String?;
      if (vsid == null) continue;

      var viewScope = viewMap[vsid];
      if (viewScope == null) {
        viewScope = ViewScope(vsid);
        viewMap[vsid] = viewScope;
      }
      viewScope.setState(Map<String, dynamic>.from(viewState));
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
        target.set(entry.key, dataSecurity.b64Decipher(ciphered));
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

  /// Encrypts a value using the AES-GCM cipher.
  String cipher(String value) {
    return dataSecurity.b64Cipher(value);
  }

  // :: Private helpers

  static void _noopSync() {}

  static void _defaultOnUriChanged(String uri) {}

  static String _makeUniqueId() {
    final random = Random.secure();
    final bytes = List.generate(16, (_) => random.nextInt(256));
    return bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  }
}
