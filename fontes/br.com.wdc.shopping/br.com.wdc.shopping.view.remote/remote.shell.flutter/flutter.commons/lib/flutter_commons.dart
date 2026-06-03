/// WDC Shopping - Shared logic for remote shell Flutter clients.
///
/// Contains: WebSocket protocol, ViewScope state management,
/// security boot, and shared widget abstractions.
library;

export 'src/bridge/constants.dart';
export 'src/bridge/types.dart';
export 'src/bridge/view_scope.dart';
export 'src/bridge/data_security.dart';
export 'src/bridge/view_state_coordinator.dart';
export 'src/bridge/view_garbage_collector.dart';
export 'src/bridge/reconnect_controller.dart';
export 'src/bridge/flush_request_context.dart';

export 'src/design_tokens.dart';
export 'src/utils/format_utils.dart';
export 'src/utils/url_utils.dart';

export 'src/widgets/error_banner.dart';
export 'src/widgets/hover_card.dart';
export 'src/widgets/html_text.dart';
export 'src/widgets/page_card.dart';
export 'src/widgets/status_banner.dart';
export 'src/widgets/view_header.dart';

export 'src/views/base_view.dart';
export 'src/views/browser_view.dart';
export 'src/views/root_view.dart';
export 'src/views/slot_view.dart';
export 'src/views/login_view.dart';
export 'src/views/home_view.dart';
export 'src/views/products_panel.dart';
export 'src/views/purchases_panel.dart';
export 'src/views/cart_view.dart';
export 'src/views/product_view.dart';
export 'src/views/receipt_view.dart';
export 'src/views/view_registry.dart';
