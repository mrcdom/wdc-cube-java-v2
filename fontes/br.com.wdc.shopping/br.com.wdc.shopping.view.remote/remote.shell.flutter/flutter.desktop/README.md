# Shopping Native — Flutter Desktop

Shell desktop (macOS, Linux, Windows) da aplicação **WeDoCode Shopping**, usando o protocolo de **Remote Presentation**. O app é um thin client que renderiza ViewStates recebidos do servidor via WebSocket.

## Características

- **Nome do app:** Shopping Native
- **Plataformas:** macOS, Linux, Windows
- **Protocolo:** WebSocket bidirecional com criptografia RSA + AES-GCM
- **Persistência de sessão:** Access token via SharedPreferences (auto-login)
- **Código compartilhado:** Usa `flutter_commons` para protocolo, views e widgets

## Pré-requisitos

- **Flutter 3.44+** (`flutter --version`)
- **Backend rodando** na porta 8080 (ou endpoint configurado)

## Execução

```bash
# Run (detecção automática do OS)
./run.sh

# Build release
./build.sh

# Ou com endpoint customizado
WDC_ENDPOINT=http://servidor:8080 ./run.sh
```

## Estrutura

```
flutter.desktop/
├── lib/
│   └── main.dart        ← Entry point (session init, ViewStateCoordinator)
├── macos/               ← Config nativa macOS
├── linux/               ← Config nativa Linux
├── windows/             ← Config nativa Windows
├── build.sh             ← Build release (auto-detecta OS)
├── run.sh               ← Run debug (auto-detecta OS)
└── pubspec.yaml
```

## Dependências

| Package | Uso |
|---------|-----|
| `flutter_commons` | Protocolo WS, views, widgets, segurança |
| `shared_preferences` | Persistência de access token |
