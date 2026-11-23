# WDC Framework — Cube Remote

Protocolo de comunicação remota para o motor Cube MVP, permitindo que **thin-clients** (browser, TeaVM) executem a lógica de presenters no servidor via WebSocket.

---

## Arquitetura

```mermaid
graph LR
    subgraph Servidor
        Host["remote.host<br/>(Javalin + WebSocket)"]
        App["RemoteApplication"]
        Skel["CubeSkeleton"]
    end

    subgraph Cliente
        Bridge["Bridge<br/>(React ou TeaVM)"]
        Coord["ViewStateCoordinator"]
        VS["ViewScope[]"]
    end

    Host <-->|"WebSocket<br/>JSON frames"| Bridge
    App --> Skel
    Bridge --> Coord
    Coord --> VS
```

---

## Submódulos

| Submódulo | Linguagem | Descrição |
|-----------|-----------|-----------|
| `remote.backend` | Java 21 | Servidor — `RemoteApplication`, `RemoteApplicationSupport`, `ScopeSerializer`, controllers Javalin (WebSocket + HTTP index) |
| `remote.bridge.java` | Java 21 | Bridge Java (fat-client) — `HostClient`, `HostClientSession`, `AbstractPresenterClient`, handshake RSA/AES-GCM; usado por `remote.shell.probe` e `remote.shell.swt` |
| `remote.bridge.react` | TypeScript/React | Bridge do thin-client React — `ViewStateCoordinator`, reconnection, data security, GC de views |
| `remote.bridge.teavm` | Java (TeaVM) | Bridge do thin-client TeaVM — mesma lógica que React, compilada para JavaScript via TeaVM |

---

## remote.backend

Classes principais no servidor:

| Classe | Responsabilidade |
|--------|-----------------|
| `RemoteApplication` | Contrato para apps remotas — autenticação, sessão WS, lifecycle |
| `RemoteApplicationSupport` | Infraestrutura delegada — gerencia id, WsContext, expiração |
| `RemoteApplicationRegistry` | Registra e localiza instâncias de `RemoteApplication` ativas |
| `RemoteBrowserPresenter` | Presenter do "browser" do thin-client (alerta, início, histórico, keep-alive): um `BrowserScope` com `CLASS_ID` e ações com código, como os demais; a view dele tem o id fixo `7b32e816a191:0` e o escopo guarda o do presenter raiz em `content` |
| `RemoteViewImpl` | A view remota de um escopo (`forScope`, pelo `CLASS_ID`): serializa o estado e encaminha os eventos ao skeleton do escopo |
| `ScopeSerializer` | Serializa o escopo → JSON por reflexão; um escopo dentro dele sai como `"<campo>ViewId"`, uma lista de escopos como `"<campo>ViewIds"` |
| `RemoteHostModule` | Módulo Javalin que registra as rotas WS e HTTP |
| `DispatcherController` | Controller WebSocket — recebe eventos e despacha para o `CubeSkeleton` |
| `IndexHtmlController` | Serve o HTML do thin-client |
| `RemoteAppSecurity` / `RemoteDataSecurity` | Segurança da sessão e dos dados |

---

## remote.bridge.react

Módulo TypeScript que roda no browser:

| Arquivo | Responsabilidade |
|---------|-----------------|
| `ViewStateCoordinator.ts` | Coordenador central — WebSocket, reconnect, GC, render scheduling |
| `FlushRequestContext.ts` | Serializa formulários e envia eventos ao servidor |
| `ReconnectController.ts` | Exponential backoff para reconexão |
| `ViewGarbageCollector.ts` | Remove views órfãs do DOM |
| `ViewScope.ts` | Gerencia estado de uma view individual |
| `DataSecurity.ts` | Criptografia de dados (RSA) |
| `utils/` | Base64, BigInt, RSA, UTF8 |

---

## remote.bridge.teavm

Equivalente Java/TeaVM do bridge React:

| Classe | Responsabilidade |
|--------|-----------------|
| `ViewStateCoordinator` | Mesma lógica — WebSocket, factory de views, render via `requestAnimationFrame` |
| `FlushRequestContext` | Troca de contexto servidor ↔ cliente |
| `ReconnectController` | Backoff exponencial |
| `ViewGarbageCollector` | GC de views |
| `ViewScope` | Estado local de uma view |
| `DataSecurity` | RSA no lado TeaVM |
| `JsonParser` | Parsing JSON leve para TeaVM |
| `RemoteView` | Implementação de `CubeView` no lado cliente |
| `interop/` | Bindings JS: `Console`, `Timers`, `JsRunnable`, etc. |

---

## Coordenadas Maven

```xml
<!-- Backend (servidor) -->
<dependency>
    <groupId>io.github.mrcdom.wdc.java</groupId>
    <artifactId>framework-cube-remote-backend</artifactId>
    <version>2.0.0</version>
</dependency>

<!-- Bridge TeaVM (thin-client) -->
<dependency>
    <groupId>io.github.mrcdom.wdc.java</groupId>
    <artifactId>framework-cube-remote-bridge-teavm</artifactId>
    <version>2.0.0</version>
</dependency>
```
