# remote.bridge.flutter

Bridge Flutter/Dart para o protocolo Host/Shell do **Cube MVP Remoto**. Implementa o lado cliente (shell) do protocolo em Dart — conecta ao `remote.host` via WebSocket, faz o handshake de segurança RSA/AES-GCM, gerencia ViewStates e provê classes base para os widgets Flutter renderizarem estados e emitirem eventos.

É a contraparte Flutter dos bridges React (`remote.bridge.react`), TeaVM (`remote.bridge.teavm`) e Java (`remote.bridge.java`). Consumido por `flutter_commons` (e indiretamente por todos os shells Flutter).

## Estrutura

```
lib/
  remote_bridge_flutter.dart        # Entrypoint público — exporta toda a API
  src/
    bridge/
      ViewStateCoordinator          # Orquestrador central: registry de views, WS lifecycle,
                                    # submit, histórico, segurança, form data
      ViewScope                     # Estado reativo de uma view (mapa JSON do servidor);
                                    # notifica widget via forceUpdate()
      DataSecurity                  # Handshake RSA + PBKDF2 + AES-GCM por sessão
      FlushRequestContext           # Serializa e envia eventos ao host
      ReconnectController           # Backoff progressivo para reconexão WebSocket
      ViewGarbageCollector          # Cleanup de ViewScopes de views removidas pelo host
      types.dart                    # Tipos compartilhados (HostResponse, ViewStateSnapshot, ...)
      constants.dart                # Constantes de protocolo (viewId fixos, timeouts, ...)
    views/
      BaseView                      # StatefulWidget base para views driven pelo bridge;
                                    # vincula ao ViewScope via vsid, reconstrói ao receber estado
      SlotView                      # View que renderiza o filho atribuído pelo servidor ao slot
    utils/
      RSA                           # RSA OAEP encrypt/decrypt em Dart puro
      BigIntUtils                   # Operações BigInt auxiliares para criptografia
```

## Dependências

| Package | Uso |
|---------|-----|
| `web_socket_channel` ^3.0 | Comunicação WebSocket com o host |
| `pointycastle` ^3.9 | Criptografia RSA + AES-GCM |

## Integração num shell Flutter

Adicionar como path dependency no `pubspec.yaml` do shell:

```yaml
dependencies:
  remote_bridge_flutter:
    path: ../../../../../../br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.bridge.flutter
```

Inicializar o bridge no `main.dart`:

```dart
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

final coordinator = ViewStateCoordinator(
  id: sessionId,
  baseWebSocketUrl: 'ws://host:8080',
);

// Numa view:
class LoginView extends BaseView {
  const LoginView({super.key}) : super(vsid: 'login');

  @override
  Widget buildView(BuildContext context, ViewScope scope) {
    final error = scope.state['error'] as String?;
    // ...
  }
}
```

## Segurança

Mesmo protocolo dos bridges Java/React/TeaVM:

1. **Handshake RSA-OAEP**: cifra a senha AES com a chave pública do host
2. **AES-256-GCM**: chave derivada via PBKDF2 (250 000 iterações) por sessão
3. **Assinatura de navegação**: parâmetro `sign` em cada intent, validado pelo host
