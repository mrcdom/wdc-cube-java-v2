import 'dart:async';
import 'dart:convert';

import 'package:web_socket_channel/web_socket_channel.dart';

import 'client_storage.dart';
import 'constants.dart';
import 'data_security.dart';
import 'types.dart';
import 'scope_coordinator.dart';

/// Manages the WebSocket connection and request queue.
///
/// Responsible for: the handshake that opens every connection (protocol
/// version 1), flushing pending requests, keepalive pings, processing server
/// responses, and tracking the "submitting" state.
///
/// On each connection the first and only message is the handshake
/// `{"protocol": 1, "clientKey": ...}`; nothing else (no request, no ping)
/// goes out before the host answers with its `serverKey`. Requests made
/// meanwhile wait and go out, in order, after the bootstrap message.
class FlushRequestContext {
  final ScopeCoordinator app;

  WebSocketChannel? _channel;
  bool _isOpen = false;
  bool _isConnecting = false;

  /// Whether the connection's key is agreed (the host answered the handshake).
  bool _isReady = false;

  /// Whether the bootstrap message of the connection is still to go out.
  bool _bootstrapPending = false;
  final Map<int, FormMapType> _requestMap = {};
  int _lastSentRequestId = -1;
  int _requestCount = 0;
  int _lastProcessedId = -1;
  Timer? _keepAliveTimer;
  Timer? _pendingKeepAliveTimer;
  Timer? _submittingTimer;
  Timer? _submittingTimeout;
  final Set<int> _userRequestIds = {};

  FlushRequestContext(this.app) {
    // Restore request counter from platform storage (survives F5)
    final restored = app.onRestoreRequestSeq?.call() ?? 0;
    if (restored > 0) {
      _requestCount = restored;
    }
  }

  void submit(
    FormMapType formMap,
    String vsid,
    int eventId, {
    bool silent = false,
  }) {
    _cancelPendingKeepAlive();

    formMap['requestId'] = _requestCount++;
    final events = formMap['event'] as List<String>?;
    if (events != null) {
      events.add('$vsid:$eventId');
    } else {
      formMap['event'] = <String>['$vsid:$eventId'];
    }
    _requestMap[formMap['requestId'] as int] = formMap;

    if (!silent) {
      _userRequestIds.add(formMap['requestId'] as int);
    }

    app.onPersistRequestSeq?.call(_requestCount);
    _resetKeepAliveTimer();
    flush();
  }

  void flush() {
    if (!_isReady) return;

    final requestObj = <String, dynamic>{'event': <String>[]};
    var hasData = false;

    for (var i = _lastSentRequestId + 1; i < _requestCount; i++) {
      final requestItemObj = _requestMap[i];
      if (requestItemObj == null) continue;

      for (final entry in requestItemObj.entries) {
        final key = entry.key;
        final value = entry.value;
        if (value != null) {
          if (key == 'event') {
            (requestObj['event'] as List<String>).addAll(value as List<String>);
          } else if (value is Map) {
            final formData =
                requestObj.putIfAbsent(key, () => <String, dynamic>{})
                    as Map<String, dynamic>;
            for (final field in value.entries) {
              final fieldValue = field.value;
              formData[field.key as String] = fieldValue is SecretValue
                  ? app.dataSecurity.b64Cipher(fieldValue.text)
                  : fieldValue;
            }
          } else {
            requestObj[key] = value;
          }
        }
      }
      requestObj['requestId'] = i;
      _lastSentRequestId = i;
      hasData = true;
    }

    if (hasData || _bootstrapPending) {
      if (_bootstrapPending) {
        _bootstrapPending = false;
        // Every stored value, ciphered with this connection's key.
        final storage = _buildBootstrapStorage();
        if (storage.isNotEmpty) {
          requestObj['storage'] = storage;
        }
      }
      _channel?.sink.add(jsonEncode(requestObj));
      if (_userRequestIds.isNotEmpty) {
        _startSubmitting();
      }
    }
  }

  void open(String url) {
    if (_isOpen || _isConnecting || app.isOutdated) return;
    _isConnecting = true;
    _isReady = false;
    app.dataSecurity.reset();

    final channel = app.connectChannel(Uri.parse(url), const ['wdc']);
    _channel = channel;

    channel.ready
        .then((_) {
          if (!identical(_channel, channel)) return;
          _isConnecting = false;
          _isOpen = true;
          // The handshake: the first and only message until the host answers.
          channel.sink.add(
            jsonEncode({
              'protocol': DataSecurity.protocol,
              'clientKey': app.dataSecurity.beginHandshake(),
            }),
          );
        })
        .catchError((Object error) {
          if (!identical(_channel, channel)) return;
          _isConnecting = false;
          _handleDisconnect(error);
        });

    channel.stream.listen(
      (dynamic message) {
        if (!identical(_channel, channel)) return;
        if (_isReady) {
          _onMessage(message as String);
        } else {
          _onHandshakeAnswer(channel, message as String);
        }
      },
      onError: (Object error) {
        if (!identical(_channel, channel)) return;
        _handleDisconnect(error);
      },
      onDone: () {
        if (!identical(_channel, channel)) return;
        final closeCode = channel.closeCode;
        if (closeCode == closeReloadRequired) {
          // Server says session invalid — notify app to reload
          _dropChannel();
          app.onSessionInvalid();
          return;
        }
        if (closeCode == closeUnsupportedProtocol) {
          _refusedProtocol();
          return;
        }
        _handleDisconnect('WebSocket closed (code: $closeCode)');
      },
    );
  }

  /// The host's answer to the handshake: its key, or the refusal of this
  /// client's protocol version.
  void _onHandshakeAnswer(WebSocketChannel channel, String rawMessage) {
    final answer = jsonDecode(rawMessage) as Map<String, dynamic>;
    if (answer['error'] == 'unsupported-protocol') {
      _refusedProtocol();
      return;
    }
    final serverKey = answer['serverKey'];
    if (answer['protocol'] != DataSecurity.protocol || serverKey is! String) {
      _refusedProtocol();
      return;
    }
    try {
      app.dataSecurity.completeHandshake(serverKey, app.id);
    } on ArgumentError catch (caught) {
      _handleDisconnect(caught);
      channel.sink.close();
      return;
    }

    _isReady = true;
    _bootstrapPending = true;
    app.isConnected = true;
    if (app.reconnectController.count > 0) {
      app.reconnectController.reset();
    }
    _initKeepAliveChecks();
    flush();
  }

  /// The host does not speak this client's protocol version: the app is out
  /// of date and does not reconnect.
  void _refusedProtocol() {
    final channel = _channel;
    _dropChannel();
    channel?.sink.close();
    app.markOutdated();
  }

  /// Forgets the channel without reconnecting.
  void _dropChannel() {
    _isOpen = false;
    _isConnecting = false;
    _isReady = false;
    _bootstrapPending = false;
    _channel = null;
    app.isConnected = false;
    app.dataSecurity.reset();
    _stopKeepAliveChecks();
    _cancelPendingKeepAlive();
    _userRequestIds.clear();
    _stopSubmitting();
  }

  void close() {
    _isOpen = false;
    _isConnecting = false;
    _isReady = false;
    _bootstrapPending = false;
    app.dataSecurity.reset();
    _stopKeepAliveChecks();
    _cancelPendingKeepAlive();
    _stopSubmitting();
    _channel?.sink.close();
    _channel = null;
  }

  void _handleDisconnect(Object? cause) {
    if (_channel == null) return; // Intentional close — don't reconnect
    _isOpen = false;
    _isConnecting = false;
    _isReady = false;
    _bootstrapPending = false;
    _channel = null;
    app.dataSecurity.reset();
    app.isConnected = false;
    _stopKeepAliveChecks();
    _cancelPendingKeepAlive();
    // Clean up sent-but-unacknowledged requests (won't be retried)
    for (var i = _lastProcessedId + 1; i <= _lastSentRequestId; i++) {
      _requestMap.remove(i);
    }
    _userRequestIds.clear();
    _stopSubmitting();
    app.reconnectController.reconnect(cause);
  }

  void _onMessage(String rawMessage) {
    if (app.reconnectController.count > 0) {
      app.reconnectController.reset();
    }

    final response = jsonDecode(rawMessage) as Map<String, dynamic>;

    if (response['releasedViews'] != null) {
      app.viewGarbageCollector.release(
        response['releasedViews'] as List<dynamic>,
      );
    }
    if (response['activeViews'] != null) {
      app.viewGarbageCollector.sweep(response['activeViews'] as List<dynamic>);
    }

    if (response['requestId'] != null) {
      final responseRequestId = response['requestId'] as int;
      for (var i = _lastProcessedId + 1; i <= responseRequestId; i++) {
        _requestMap.remove(i);
        _userRequestIds.remove(i);
        _lastProcessedId = i;
      }
    }

    if (response['uri'] != null) {
      app.onUriChanged(response['uri'] as String);
    }

    if (response['storage'] != null) {
      app.applyStorageDelta(response['storage'] as Map<String, dynamic>);
    }

    if (response['scopes'] != null) {
      app.applyScopes(response['scopes'] as List<dynamic>);
    }

    flush();
    if (_userRequestIds.isNotEmpty) {
      _startSubmitting();
    } else {
      _stopSubmitting();
    }
  }

  // :: Keep-alive

  void _initKeepAliveChecks() {
    _stopKeepAliveChecks();
    _keepAliveTimer = Timer(keepAliveInterval, _keepAlive);
  }

  void _stopKeepAliveChecks() {
    _keepAliveTimer?.cancel();
    _keepAliveTimer = null;
  }

  void _keepAliveNow() {
    _cancelPendingKeepAlive();
    if (_isReady) {
      _pendingKeepAliveTimer = Timer(const Duration(milliseconds: 80), () {
        _pendingKeepAliveTimer = null;
        if (_isReady) {
          _channel?.sink.add(jsonEncode({'ping': true}));
        }
      });
    }
  }

  void _cancelPendingKeepAlive() {
    _pendingKeepAliveTimer?.cancel();
    _pendingKeepAliveTimer = null;
  }

  void _resetKeepAliveTimer() {
    if (_keepAliveTimer != null) {
      _stopKeepAliveChecks();
      _keepAliveTimer = Timer(keepAliveInterval, _keepAlive);
    }
  }

  void _keepAlive() {
    _stopKeepAliveChecks();
    _keepAliveNow();
    _keepAliveTimer = Timer(keepAliveInterval, _keepAlive);
  }

  // :: Submitting state

  void _startSubmitting() {
    _submittingTimer ??= Timer(const Duration(milliseconds: 200), () {
      _submittingTimer = null;
      _applySubmitting(true);
    });
    _submittingTimeout ??= Timer(const Duration(seconds: 5), () {
      _submittingTimeout = null;
      _stopSubmitting();
    });
  }

  void _stopSubmitting() {
    _submittingTimer?.cancel();
    _submittingTimer = null;
    _submittingTimeout?.cancel();
    _submittingTimeout = null;
    _applySubmitting(false);
  }

  void _applySubmitting(bool value) {
    final scope = app.viewMap[browserVsid];
    if (scope != null && scope.values['submitting'] != value) {
      scope.patchField('submitting', value);
    }
  }

  // :: Storage helpers

  /// Builds the bootstrap `storage` map sent after the handshake.
  /// All values are ciphered with the connection key. Null values are never included (nothing to remove
  /// on a fresh connection).
  Map<String, dynamic> _buildBootstrapStorage() {
    final result = <String, dynamic>{};

    void addScope(String scopeName, ClientStorage storage) {
      final all = storage.all;
      if (all.isNotEmpty) {
        result[scopeName] = {
          for (final e in all.entries)
            e.key: app.dataSecurity.b64Cipher(e.value),
        };
      }
    }

    addScope('session', app.sessionStorage);
    addScope('persistent', app.persistentStorage);
    addScope('persistent-secure', app.persistentStorage.secure);
    return result;
  }
}
