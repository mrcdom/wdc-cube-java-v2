# Remote Shell TeaVM

Thin client Java compilado para JavaScript via **TeaVM 0.14.0**, usando **Virtual DOM** (VNode API) com Spectrum Web Components. Comunica-se com o `remote.host` via WebSocket — **sem lógica de negócio local**.

## Conceito

Este módulo é uma implementação alternativa do shell (cliente leve) da arquitetura de Remote Presentation. Substitui o React como engine de renderização, mantendo o mesmo protocolo WebSocket bidirecional:

- Recebe **ViewStates** serializados do host (apenas deltas/dirty)
- Renderiza usando Virtual DOM (diff eficiente → DOM real)
- Emite **eventos de interação** do usuário de volta ao host

```mermaid
graph TD
    subgraph browser["Browser (JavaScript)"]
        main["js/app.js ← Java compilado para JS pelo TeaVM"]
        mainClass["Main.java — Entry point: registra factories das views"]
        bridge["bridge/ — Infra de comunicação WS + VDom base"]
        views["views/ — 9 views (thin renderers)"]
        main --> mainClass
        main --> bridge
        main --> views
    end
```

## Arquitetura

```mermaid
graph TD
    subgraph browser["Browser (remote.shell.teavm)"]
        coordinator["ViewStateCoordinator"]
        ws["WebSocket"]
        subgraph vdom["AbstractRemoteView (VDom)"]
            scope["ViewScope (estado reativo)"]
            render["render() → VNode tree"]
            diff["diff/patch → DOM real"]
        end
        coordinator --> ws
        coordinator --> vdom
    end

    subgraph host["remote.host"]
        presenters["Presenters + ViewStates"]
    end

    ws <--> host
```

## Estrutura do Projeto

```mermaid
graph TD
    root["remote.shell.teavm/"]
    buildsh["build.sh<br/><small>Build simplificado (TeaVM → JS)</small>"]
    watchsh["watch.sh<br/><small>Rebuild automático ao detectar mudanças</small>"]
    pom["pom.xml<br/><small>Maven + plugin TeaVM</small>"]
    docs["docs/<br/><small>GUIDE_NEW_VIEW.md</small>"]
    srcMain["src/main/"]
    java["java/.../shell/teavm/"]
    main["Main.java<br/><small>Entry point (registra view factories)</small>"]
    bridge["bridge/"]
    arv["AbstractRemoteView.java<br/><small>Base VDom (render/diff/useCallback)</small>"]
    vsc["ViewStateCoordinator.java<br/><small>Gerencia views ativas + protocolo WS</small>"]
    vs["ViewScope.java<br/><small>Estado reativo recebido do host</small>"]
    frc["FlushRequestContext.java<br/><small>Comunicação WS (request/response)</small>"]
    rc["ReconnectController.java<br/><small>Backoff progressivo para reconexão</small>"]
    ds["DataSecurity.java<br/><small>RSA + AES-GCM (criptografia de sessão)</small>"]
    jp["JsonParser.java<br/><small>Parsing de respostas JSON do host</small>"]
    vgc["ViewGarbageCollector.java<br/><small>Cleanup de views desalocadas</small>"]
    interop["interop/<br/><small>JSO bridges — Console · Timers · JsConsumer/Runnable</small>"]
    views["views/<br/><small>BrowserView · RootView · LoginView · HomeView<br/>ProductsPanelView · ProductView · CartView · ReceiptView · PurchasesPanelView</small>"]
    webapp["webapp/<br/><small>index.html + css/app.css</small>"]

    root --> buildsh
    root --> watchsh
    root --> pom
    root --> docs
    root --> srcMain
    srcMain --> java
    srcMain --> webapp
    java --> main
    java --> bridge
    java --> interop
    java --> views
    bridge --> arv
    bridge --> vsc
    bridge --> vs
    bridge --> frc
    bridge --> rc
    bridge --> ds
    bridge --> jp
    bridge --> vgc
```

## Virtual DOM

As views estendem `AbstractRemoteView` e implementam `render()` retornando uma árvore `VNode`:

```java
@Override
protected VNode render() {
    var state = scope.state();
    return div(Css.ROOT).children(
        h5(Css.TITLE).text(state.title),
        spButton("accent")
            .on("click", onConfirm)
            .children(span().text("Confirmar"))
    );
}
```

### Otimizações de Referência

O diff de event listeners compara por **identidade de referência**:

1. **Stable fields** — listeners sem parâmetros:
   ```java
   private final EventListener<Event> onBack = evt -> flush("back");
   ```

2. **`useCallback(key, listener)`** — listeners paramétricos cacheados:
   ```java
   .on("click", useCallback("remove-" + id, mkOnRemove(id)))
   ```

### Compact Css

```java
@SuppressWarnings({"java:S1214", "static-access"})
private interface Css {
    CssUtility u = CssUtility.INSTANCE;
    CssComponents c = CssComponents.INSTANCE;
    CssIcons icon = CssIcons.INSTANCE;

    String ROOT = u.PAGE_SCROLL_ROOT;
    String TITLE = clsx(c.CARD_TITLE, u.MB_12);
}
```

## Diferença vs. app.teavm (teavm.web)

| Aspecto | remote.shell.teavm | app.teavm (teavm.web) |
|---------|-------------------|----------------------|
| **Lógica de negócio** | Nenhuma (thin client) | Completa (SPA autônomo) |
| **Comunicação** | WebSocket bidirecional | REST (XMLHttpRequest) |
| **Estado** | Recebido do host (`ViewScope`) | Gerenciado localmente (Presenters) |
| **Presenters** | Server-side (remote.host) | Client-side (compilados junto) |
| **Vantagem** | Zero lógica no client | Funciona offline |

## Build

```bash
# Build simples
JAVA21_HOME=<caminho-jdk-21> ./build.sh

# Build completo (instala dependências do framework antes)
JAVA21_HOME=<caminho-jdk-21> ./build.sh --full
```

### Output

```mermaid
graph TD
    root["work/frontend/remote.shell.teavm/"]
    index["index.html"]
    css["css/app.css"]
    js["js/"]
    appJs["app.js<br/><small>Java compilado para JavaScript</small>"]

    root --> index
    root --> css
    root --> js
    js --> appJs
```

## Desenvolvimento

```bash
# Watch mode — rebuild automático ao salvar
./watch.sh
```

Requer `fswatch` instalado (`brew install fswatch` no macOS).

## Execução

O shell é servido como recurso estático pelo backend:

```
http://localhost:8080/remote.shell.teavm
```

## Guias

- [Como criar uma nova View](docs/GUIDE_NEW_VIEW.md) — passo a passo para implementar uma view neste projeto

## Screenshots

### Login

![Login](docs/screenshots/01-login.png)

### Lista de Produtos

![Produtos](docs/screenshots/02-products.png)

### Detalhe do Produto

![Detalhe do Produto](docs/screenshots/03-product-detail.png)

### Carrinho

![Carrinho](docs/screenshots/04-cart.png)

### Carrinho Vazio

![Carrinho Vazio](docs/screenshots/04-cart-empty.png)

### Recibo

![Recibo](docs/screenshots/05-receipt.png)

### Histórico de Compras

![Histórico](docs/screenshots/06-history.png)
