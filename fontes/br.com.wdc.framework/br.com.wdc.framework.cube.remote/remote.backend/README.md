# remote.backend

Infraestrutura server-side do protocolo de apresentação remota do **Cube MVP**. Provê as abstrações genéricas que qualquer aplicação baseada no protocolo Host/Shell deve implementar — é o lado servidor do protocolo, independente de domínio de negócio.

## Responsabilidade

Gerencia o ciclo de vida de sessões remotas (handshake de segurança, recepção de eventos, serialização de ViewStates, envio de deltas) expondo endpoints HTTP/WebSocket via **Javalin 7.2**. A aplicação concreta herda de `RemoteApplicationSupport` e registra suas views/presenters.

## Classes Principais

| Classe | Responsabilidade |
|--------|------------------|
| `RemoteApplication` | SPI — interface que a aplicação concreta implementa (instanciar views, tratar eventos) |
| `RemoteApplicationSupport` | Implementação base de `RemoteApplication`: dirty-tracking, dispatch de eventos, geração de resposta JSON delta |
| `RemoteApplicationRegistry` | Registry de instâncias por sessão; cria, localiza e remove `RemoteApplication` por `sessionId`; aplica limite de capacidade |
| `RemoteViewImpl` | Base abstrata para views remotas: `instanceId`, `markDirty()`, `syncClientToServer()`, `writeState()` |
| `RemoteBrowserPresenter` | Coordena navegação e dispatch de intent entre views |
| `ViewStateSerializer` | Serializa ViewStates para JSON (apenas campos dirty) |
| `RemoteAppSecurity` | RSA + SHA256withRSA para assinatura de URLs de navegação |
| `RemoteDataSecurity` | AES-GCM por sessão, chave derivada via PBKDF2 (250 000 iterações) para dados sensíveis |
| `RemoteHostModule` | Configura o módulo no container (wiring) |
| `CapacityExceededException` | Lançada quando o número máximo de sessões simultâneas é atingido |

### Controllers Javalin

| Classe | Responsabilidade |
|--------|------------------|
| `DispatcherController` | WebSocket `ws://.../dispatcher/{sessionId}` — recebe eventos do shell, executa dispatch, devolve delta de ViewStates |
| `SessionInitController` | `GET /session-init` — handshake de segurança; negocia chave AES da sessão via RSA |
| `IndexHtmlController` | `GET /` — serve o `index.html` do shell estático |

## Dependências

- `br.com.wdc.framework.cube` — framework Cube MVP (base: View, Presenter, ViewState)
- `io.javalin:javalin` 7.2.0 — HTTP/WebSocket
- `com.google.code.gson` — serialização JSON
- `org.apache.commons:commons-lang3`
- `commons-codec` — codificação Base64/Hex para crypto

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.backend -am
```
