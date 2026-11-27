# flutter_commons

Shared library for the Flutter shells (desktop, mobile, web) of the **WDC Store** application.

It contains all the infrastructure any Flutter shell needs to work as a thin client of the Remote Presentation protocol — no business logic, only protocol, rendering and interaction.

## Modules

```mermaid
graph TD
    lib["lib/"]
    entry["flutter_commons.dart<br/><small>Exports the package; re-exports remote_bridge_flutter</small>"]
    src["src/"]
    dt["design_tokens.dart<br/><small>Design system colors, typography, spacing (from the generated tokens)</small>"]
    i18n["i18n.dart<br/><small>StoreI18n: texts and dates in the user's language</small>"]
    gen["generated/<br/><small>messages.dart, store_tokens.dart (GenerateMessages, GenerateDesignTokens)</small>"]
    utils["utils/<br/><small>format_utils, url_utils</small>"]
    views["views/<br/><small>browser, root, login, home, products_panel, purchases_panel,<br/>product, cart, cart_item, receipt + view_registry</small>"]
    widgets["widgets/<br/><small>error_banner, hover_card, html_text, page_card, status_banner, view_header</small>"]
    bridge["remote_bridge_flutter (framework)<br/><small>ScopeCoordinator, DataSecurity, FlushRequestContext,<br/>ReconnectController, ViewScope, ClientStorage</small>"]

    lib --> entry
    lib --> src
    src --> dt
    src --> i18n
    src --> gen
    src --> utils
    src --> views
    src --> widgets
    entry --> bridge
```

The protocol client (WebSocket, protocol version 1 handshake with a key agreed per connection — ECDH P-256 + HKDF-SHA256 + AES-256-GCM —, reconnection backoff, client storage) is the framework package `remote_bridge_flutter`; this package holds the WDC Store views and widgets.

## Usage

Referenced as a path dependency in the shells:

```yaml
dependencies:
  flutter_commons:
    path: ../flutter.commons
```

## External dependencies

| Package | Purpose |
|---------|-----|
| `remote_bridge_flutter` | Remote protocol client (framework, path dependency) |
| `web_socket_channel` | WebSocket communication with the host |
| `pointycastle` | ECDH P-256, HKDF-SHA256 and AES-256-GCM (used through the bridge) |
