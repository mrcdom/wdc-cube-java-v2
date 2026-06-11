# remote.bridge.java

Bridge Java para o protocolo Host/Shell do **Cube MVP Remoto**. Implementa o lado cliente (shell) do protocolo em Java puro — conecta ao `remote.host` via WebSocket, faz o handshake de segurança RSA/AES, navega a hierarquia de presenters, submete eventos e lê os ViewStates recebidos.

É a contraparte Java dos bridges React (`remote.bridge.react`) e TeaVM (`remote.bridge.teavm`) — mesma semântica de protocolo, tecnologia diferente.

## Responsabilidade

Fornecer a infra de conectividade para qualquer cliente Java que precise comunicar com um host remoto sem ter interface gráfica — usado por `remote.shell.probe` (benchmarks/testes) e `remote.shell.swt` (shell desktop SWT).

## Classes Principais

| Classe | Responsabilidade |
|--------|------------------|
| `HostClient` | Ponto de entrada: cria sessões com o host, gerencia ciclo de vida da conexão WebSocket |
| `HostClientSession` | Sessão ativa: submete eventos (`submit()`), recebe `HostResponse` com o delta de ViewStates |
| `AbstractPresenterClient` | Base para clientes tipados por presenter — facilita scripting de fluxos (ex: login, compra) |
| `ClientCrypto` | Handshake RSA + derivação AES-GCM via PBKDF2; espelha `RemoteDataSecurity` do servidor |

`HostClient` expõe dois escopos de storage por sessão:

| Método | Backing | Ciclo de vida |
|--------|---------|---------------|
| `clientSessionStore()` | `InMemoryClientStorage` | Dura enquanto a conexão existir |
| `clientPersistentStore()` | `InMemoryClientStorage` por default (via `connect(url)`); customizável via `connect(url, storage)` | Configurável |

O storage persistente é bootstrapped para o servidor no momento do `connect()` — permite que o servidor restaure o estado da sessão anterior (ex: auto-login via refresh token).

### Modelos

| Classe | Responsabilidade |
|--------|------------------|
| `HostResponse` | Resposta do host: `requestId`, `uri`, lista de `ViewStateSnapshot` |
| `ViewStateSnapshot` | Estado de uma view: `instanceId` + mapa de campos |
| `ViewStateMap` | Mapa tipado de campos de um ViewState |
| `SecretContext` | Contexto criptográfico da sessão (chave AES derivada, IV, salt) |

## Dependências

- `br.com.wdc.framework.commons` — Base62, logging
- `com.google.code.gson` — parsing da resposta JSON do host
- `ch.qos.logback:logback-classic` — logging
- `org.apache.commons:commons-lang3`

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.framework/br.com.wdc.framework.cube.remote/remote.bridge.java -am
```
