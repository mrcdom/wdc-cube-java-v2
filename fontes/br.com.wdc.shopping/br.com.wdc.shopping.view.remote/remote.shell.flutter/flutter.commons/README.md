# flutter_commons

Biblioteca compartilhada para os shells Flutter (desktop, mobile, web) da aplicação **WeDoCode Shopping**.

Contém toda a infraestrutura necessária para que qualquer shell Flutter funcione como thin client do protocolo de Remote Presentation — sem lógica de negócio, apenas protocolo, renderização e interação.

## Módulos

```mermaid
graph TD
    lib["lib/src/"]
    bridge["bridge/<br/><small>Protocolo WebSocket, segurança, coordenação de views</small>"]
    vsc["ViewStateCoordinator<br/><small>Gerencia views ativas, protocolo WS, dirty states</small>"]
    ds["DataSecurity<br/><small>RSA key exchange + AES-GCM por sessão</small>"]
    frc["FlushRequestContext<br/><small>Envio de eventos e form data</small>"]
    rc["ReconnectController<br/><small>Backoff progressivo para reconexão</small>"]
    dt["design_tokens.dart<br/><small>Cores, tipografia, espaçamentos do design system</small>"]
    utils["utils/<br/><small>Helpers compartilhados</small>"]
    views["views/<br/><small>Implementação das views (Home, Login, Root, etc.)</small>"]
    widgets["widgets/<br/><small>Widgets reutilizáveis (formulários, tabelas, etc.)</small>"]

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

## Uso

Referenciado como path dependency nos shells:

```yaml
dependencies:
  flutter_commons:
    path: ../flutter.commons
```

## Dependências externas

| Package | Uso |
|---------|-----|
| `web_socket_channel` | Comunicação WebSocket com o host |
| `pointycastle` | Criptografia RSA + AES-GCM |
