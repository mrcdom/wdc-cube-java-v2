# flutter_commons

Shared library for the Flutter shells (desktop, mobile, web) of the **WeDoCode Store** application.

It contains all the infrastructure any Flutter shell needs to work as a thin client of the Remote Presentation protocol — no business logic, only protocol, rendering and interaction.

## Modules

```mermaid
graph TD
    lib["lib/src/"]
    bridge["bridge/<br/><small>WebSocket protocol, security, view coordination</small>"]
    vsc["ViewStateCoordinator<br/><small>Manages active views, WS protocol, dirty states</small>"]
    ds["DataSecurity<br/><small>RSA key exchange + per-session AES-GCM</small>"]
    frc["FlushRequestContext<br/><small>Sends events and form data</small>"]
    rc["ReconnectController<br/><small>Progressive backoff for reconnection</small>"]
    dt["design_tokens.dart<br/><small>Design system colors, typography, spacing</small>"]
    utils["utils/<br/><small>Shared helpers</small>"]
    views["views/<br/><small>View implementations (Home, Login, Root, etc.)</small>"]
    widgets["widgets/<br/><small>Reusable widgets (forms, tables, etc.)</small>"]

    lib --> bridge
    lib --> dt
    lib --> utils
    lib --> views
    lib --> widgets
    bridge --> vsc
    bridge --> ds
    bridge --> frc
    bridge --> rc
```

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
| `web_socket_channel` | WebSocket communication with the host |
| `pointycastle` | RSA + AES-GCM cryptography |
