/// Framework bridge for Flutter remote shell clients.
///
/// Provides: WebSocket protocol, ViewScope state management,
/// RSA/AES-GCM security, reconnection logic, and base view abstractions.
library;

export 'src/bridge/constants.dart';
export 'src/bridge/types.dart';
export 'src/bridge/view_scope.dart';
export 'src/bridge/data_security.dart';
export 'src/bridge/view_state_coordinator.dart';
export 'src/bridge/view_garbage_collector.dart';
export 'src/bridge/reconnect_controller.dart';
export 'src/bridge/flush_request_context.dart';

export 'src/utils/big_int_utils.dart';
export 'src/utils/rsa.dart';

export 'src/views/base_view.dart';
export 'src/views/slot_view.dart';
