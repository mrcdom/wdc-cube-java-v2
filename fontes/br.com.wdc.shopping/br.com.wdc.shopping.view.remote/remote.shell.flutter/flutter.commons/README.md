# flutter_commons

Biblioteca compartilhada para os shells Flutter (desktop, mobile, web) da aplicação **WeDoCode Shopping**.

Contém toda a infraestrutura necessária para que qualquer shell Flutter funcione como thin client do protocolo de Remote Presentation — sem lógica de negócio, apenas protocolo, renderização e interação.

## Módulos

```
lib/src/
├── bridge/              ← Protocolo WebSocket, segurança, coordenação de views
│   ├── ViewStateCoordinator   ← Gerencia views ativas, protocolo WS, dirty states
│   ├── DataSecurity           ← RSA key exchange + AES-GCM por sessão
│   ├── FlushRequestContext    ← Envio de eventos e form data
│   └── ReconnectController    ← Backoff progressivo para reconexão
├── design_tokens.dart   ← Cores, tipografia, espaçamentos do design system
├── utils/               ← Helpers compartilhados
├── views/               ← Implementação das views (Home, Login, Root, etc.)
└── widgets/             ← Widgets reutilizáveis (formulários, tabelas, etc.)
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
