import 'dart:async';

import 'constants.dart';
import 'view_state_coordinator.dart';

/// Manages exponential-backoff reconnection to the WebSocket server.
class ReconnectController {
  final ViewStateCoordinator app;
  late String url;
  int count = 0;
  Timer? _reconnectTimer;
  int delay = 0;
  Object? cause;

  ReconnectController(this.app) {
    url = '${app.baseWebSocketUrl}/dispatcher/${app.id}';
  }

  void close() {
    _reconnectTimer?.cancel();
    _reconnectTimer = null;
  }

  void reconnect(Object? cause) {
    count++;
    delay = (2000 * count).clamp(0, 120000);
    this.cause = cause;

    _updateBrowserError();

    _reconnectTimer ??= Timer.periodic(const Duration(seconds: 1), (_) => check());
  }

  void check() {
    if (app.viewMap[browserVsid] == null || app.isConnected) {
      reset();
      return;
    }

    if (delay > 0) {
      delay -= 1000;
      if (delay < 0) delay = 0;
    }

    _updateBrowserError();

    if (delay <= 0) {
      Future.microtask(() => app.assureContextExchangerIsConnected());
    }
  }

  void reset() {
    final browserView = app.viewMap[browserVsid];
    if (browserView != null) {
      browserView.removeField('error');
    }

    count = 0;
    delay = 0;
    cause = null;

    _reconnectTimer?.cancel();
    _reconnectTimer = null;
  }

  void checkNow() {
    delay = 0;
    check();
  }

  void _updateBrowserError() {
    final bvScope = app.viewMap[browserVsid];
    if (bvScope == null) return;
    bvScope.patchField('error', {
      'cause': cause,
      'numAttempt': count,
      'delay': delay,
    });
  }
}
