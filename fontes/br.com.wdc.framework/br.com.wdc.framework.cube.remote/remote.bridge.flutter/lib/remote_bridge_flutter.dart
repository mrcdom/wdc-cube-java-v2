/// Framework bridge for Flutter remote shell clients.
///
/// Provides: WebSocket protocol, per-view scope management (ViewScope),
/// a key agreed per connection (ECDH P-256 + HKDF-SHA256 + AES-256-GCM,
/// protocol version 1), reconnection logic, and base view abstractions.
library;

export 'src/bridge/client_storage.dart';
export 'src/bridge/constants.dart';
export 'src/bridge/types.dart';
export 'src/bridge/view_scope.dart';
export 'src/bridge/channel_crypto.dart';
export 'src/bridge/data_security.dart';
export 'src/bridge/scope_coordinator.dart';
export 'src/bridge/view_garbage_collector.dart';
export 'src/bridge/reconnect_controller.dart';
export 'src/bridge/flush_request_context.dart';

export 'src/views/base_view.dart';
export 'src/views/slot_view.dart';
