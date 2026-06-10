# remote.bridge.teavm

Bridge TeaVM para o protocolo Host/Shell do **Cube MVP Remoto**. Implementa o lado cliente (shell) do protocolo em Java compilado para JavaScript via **TeaVM** — conecta ao `remote.host` via WebSocket usando a Web Crypto API do browser, sem nenhum runtime JS externo.

É a contraparte TeaVM dos bridges React (`remote.bridge.react`) e Java (`remote.bridge.java`). Consumido por `remote.shell.teavm` (aplicação shopping) e qualquer outro shell TeaVM sobre o mesmo protocolo.

## Classes Principais

### Protocolo & Estado

| Classe | Responsabilidade |
|--------|------------------|
| `ViewStateCoordinator` | Gerencia conexão WS, ciclo request/response, distribui ViewState updates para as views ativas |
| `ViewScope` | Estado reativo de uma view: recebe snapshots do coordinator e notifica a view via `update()` |
| `RemoteView` | Interface base para views remotas TeaVM: `render()` + `onViewState(ViewScope)` |
| `FlushRequestContext` | Serializa e envia eventos ao host; gerencia fila de flushes pendentes |
| `ReconnectController` | Backoff progressivo em reconexão WebSocket |
| `ViewGarbageCollector` | Cleanup de views desalocadas quando o host envia sinal de remoção |
| `DataSecurity` | Handshake RSA + AES-GCM via Web Crypto API; espelha `RemoteDataSecurity` do servidor |
| `SecurityBoot` | Inicializa `DataSecurity` na abertura da sessão |
| `JsonParser` | Parser JSON leve compilado para JS (sem dependência de biblioteca externa) |

### Interop (JSO bridges)

| Classe | Responsabilidade |
|--------|------------------|
| `Console` | `console.log/error/warn` |
| `Cookies` | Leitura/escrita de cookies do browser |
| `Timers` | `setTimeout` / `clearTimeout` |
| `WebCrypto` | Web Crypto API (SubtleCrypto: importKey, encrypt, decrypt, deriveBits) |
| `JsRunnable` | Callback JS sem parâmetros |
| `JsIntConsumer` | Callback JS com parâmetro inteiro |
| `JsStringConsumer` | Callback JS com parâmetro string |
| `JsBiObjectConsumer` | Callback JS com dois parâmetros objeto |

## Dependências

- `org.teavm:teavm-jso` — JSO API (scope: provided)
- `org.teavm:teavm-jso-apis` — APIs browser (WebSocket, etc.) (scope: provided)

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.bridge.teavm -am
```
