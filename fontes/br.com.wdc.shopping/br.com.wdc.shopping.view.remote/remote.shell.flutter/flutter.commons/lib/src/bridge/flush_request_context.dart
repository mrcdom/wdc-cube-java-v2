import 'dart:async';
import 'dart:convert';

import 'package:web_socket_channel/web_socket_channel.dart';

import 'constants.dart';
import 'types.dart';
import 'view_state_coordinator.dart';

/// Manages the WebSocket connection and request queue.
///
/// Responsible for: flushing pending requests, keepalive pings,
/// processing server responses, and tracking the "submitting" state.
class FlushRequestContext {
  final ViewStateCoordinator app;

  WebSocketChannel? _channel;
  bool _isOpen = false;
  bool _isConnecting = false;
  final Map<int, FormMapType> _requestMap = {};
  int _lastSentRequestId = -1;
  int _requestCount = 0;
  int _lastProcessedId = -1;
  Timer? _keepAliveTimer;
  Timer? _pendingKeepAliveTimer;
  Timer? _submittingTimer;
  Timer? _submittingTimeout;
  final Set<int> _userRequestIds = {};
  String? _pendingSecret;
  String? _pendingAccessToken;

  FlushRequestContext(this.app) {
    // Restore request counter from platform storage (survives F5)
    final restored = app.onRestoreRequestSeq?.call() ?? 0;
    if (restored > 0) {
      _requestCount = restored;
    }
  }

  void submit(FormMapType formMap, String vsid, int eventId, {bool silent = false}) {
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
    if (!_isOpen) return;

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
            final formData = requestObj.putIfAbsent(key, () => <String, dynamic>{}) as Map<String, dynamic>;
            formData.addAll(Map<String, dynamic>.from(value));
          } else {
            requestObj[key] = value;
          }
        }
      }
      requestObj['requestId'] = i;
      _lastSentRequestId = i;
      hasData = true;
    }

    if (hasData || _pendingSecret != null) {
      if (_pendingSecret != null) {
        requestObj['secret'] = _pendingSecret;
        _pendingSecret = null;
      }
      if (_pendingAccessToken != null) {
        requestObj['accessToken'] = app.dataSecurity.b64Cipher(_pendingAccessToken!);
        _pendingAccessToken = null;
      }
      _channel?.sink.add(jsonEncode(requestObj));
      if (_userRequestIds.isNotEmpty) {
        _startSubmitting();
      }
    }
  }

  void open(String url) {
    if (_isOpen || _isConnecting) return;
    _isConnecting = true;

    final channel = WebSocketChannel.connect(
      Uri.parse(url),
      protocols: ['wdc'],
    );
    _channel = channel;

    channel.ready.then((_) {
      _isConnecting = false;
      _isOpen = true;
      app.isConnected = true;
      _pendingSecret = app.dataSecurity.getSignature();
      _pendingAccessToken = app.accessToken;
      _initKeepAliveChecks();
      flush();
    }).catchError((Object error) {
      _isConnecting = false;
      _handleDisconnect(error);
    });

    channel.stream.listen(
      (dynamic message) {
        _onMessage(message as String);
      },
      onError: (Object error) {
        _handleDisconnect(error);
      },
      onDone: () {
        final closeCode = channel.closeCode;
        if (closeCode == 4001) {
          // Server says session invalid — notify app to reload
          app.onSessionInvalid();
          return;
        }
        _handleDisconnect('WebSocket closed (code: $closeCode)');
      },
    );
  }

  void close() {
    _isOpen = false;
    _isConnecting = false;
    _stopKeepAliveChecks();
    _cancelPendingKeepAlive();
    _stopSubmitting();
    _channel?.sink.close();
    _channel = null;
  }

  void _handleDisconnect(Object? cause) {
    if (_channel == null) return; // Intentional close — don't reconnect
    _isOpen = false;
    _channel = null;
    app.isConnected = false;
    _stopKeepAliveChecks();
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
      app.viewGarbageCollector.release(response['releasedViews'] as List<dynamic>);
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

    if (response.containsKey('accessToken')) {
      final ciphered = response['accessToken'] as String?;
      if (ciphered != null && ciphered.isNotEmpty) {
        final token = app.dataSecurity.b64Decipher(ciphered);
        app.accessToken = token;
        app.onAccessTokenChanged?.call(token);
      } else {
        app.accessToken = null;
        app.onAccessTokenChanged?.call('');
      }
    }

    if (response['states'] != null) {
      app.applyViewStates(response['states'] as List<dynamic>);
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
    if (_isOpen) {
      _pendingKeepAliveTimer = Timer(const Duration(milliseconds: 80), () {
        _pendingKeepAliveTimer = null;
        if (_isOpen) {
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
    if (scope != null && scope.state['submitting'] != value) {
      scope.patchField('submitting', value);
    }
  }
}
