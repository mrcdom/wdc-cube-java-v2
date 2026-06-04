/// WDC Shopping - Shared logic for remote shell Flutter clients.
///
/// Contains: Shopping-specific views, widgets, design tokens, and utilities.
/// Re-exports the bridge infrastructure from remote_bridge_flutter.
library;

// Re-export all bridge infrastructure
export 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

export 'src/design_tokens.dart';
export 'src/utils/format_utils.dart';
export 'src/utils/url_utils.dart';

export 'src/widgets/error_banner.dart';
export 'src/widgets/hover_card.dart';
export 'src/widgets/html_text.dart';
export 'src/widgets/page_card.dart';
export 'src/widgets/status_banner.dart';
export 'src/widgets/view_header.dart';

export 'src/views/browser_view.dart';
export 'src/views/root_view.dart';
export 'src/views/login_view.dart';
export 'src/views/home_view.dart';
export 'src/views/products_panel.dart';
export 'src/views/purchases_panel.dart';
export 'src/views/cart_view.dart';
export 'src/views/product_view.dart';
export 'src/views/receipt_view.dart';
export 'src/views/view_registry.dart';
