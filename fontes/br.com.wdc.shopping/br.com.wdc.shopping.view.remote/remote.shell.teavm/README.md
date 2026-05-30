# Remote Shell TeaVM

Thin client Java compilado para JavaScript via **TeaVM 0.14.0**, usando **Virtual DOM** (VNode API) com Spectrum Web Components. Comunica-se com o `remote.host` via WebSocket — **sem lógica de negócio local**.

## Conceito

Este módulo é uma implementação alternativa do shell (cliente leve) da arquitetura de Remote Presentation. Substitui o React como engine de renderização, mantendo o mesmo protocolo WebSocket bidirecional:

- Recebe **ViewStates** serializados do host (apenas deltas/dirty)
- Renderiza usando Virtual DOM (diff eficiente → DOM real)
- Emite **eventos de interação** do usuário de volta ao host

```
Browser (JavaScript)
└── js/app.js  ← Java compilado para JS pelo TeaVM
    ├── Main.java                   ← Entry point: registra factories das views
    ├── bridge/                     ← Infra de comunicação WS + VDom base
    └── views/                      ← 9 views (thin renderers)
```

## Arquitetura

```
┌─────────────────────────────────────────────┐
│  Browser (remote.shell.teavm)               │
│                                             │
│  ViewStateCoordinator ──── WebSocket ───┐   │
│       │                                 │   │
│  ┌────┴────────────┐                   │   │
│  │ AbstractRemoteView (VDom)           │   │
│  │  ├─ ViewScope (estado reativo)      │   │
│  │  ├─ render() → VNode tree           │   │
│  │  └─ diff/patch → DOM real           │   │
│  └─────────────────┘                   │   │
└─────────────────────────────────────────┼───┘
                                          │
                              ┌────────────┘
                              ▼
                    ┌──────────────────┐
                    │  remote.host     │
                    │  (Presenters +   │
                    │   ViewStates)    │
                    └──────────────────┘
```

## Estrutura do Projeto

```
remote.shell.teavm/
├── build.sh                    ← Build simplificado (TeaVM → JS)
├── watch.sh                    ← Rebuild automático ao detectar mudanças
├── pom.xml                     ← Maven + plugin TeaVM
├── docs/
│   └── GUIDE_NEW_VIEW.md      ← Guia para criar novas views
└── src/main/
    ├── java/.../shell/teavm/
    │   ├── Main.java                       ← Entry point (registra view factories)
    │   ├── bridge/
    │   │   ├── AbstractRemoteView.java     ← Base VDom (render/diff/useCallback)
    │   │   ├── ViewStateCoordinator.java   ← Gerencia views ativas + protocolo WS
    │   │   ├── ViewScope.java              ← Estado reativo recebido do host
    │   │   ├── FlushRequestContext.java    ← Comunicação WS (request/response)
    │   │   ├── ReconnectController.java    ← Backoff progressivo para reconexão
    │   │   ├── DataSecurity.java           ← RSA + AES-GCM (criptografia de sessão)
    │   │   ├── JsonParser.java             ← Parsing de respostas JSON do host
    │   │   └── ViewGarbageCollector.java   ← Cleanup de views desalocadas
    │   ├── interop/                        ← JSO bridges
    │   │   ├── Console.java
    │   │   ├── Timers.java
    │   │   └── Js*Consumer/Runnable.java
    │   └── views/                          ← 9 views (thin renderers)
    │       ├── BrowserView.java            ← Controle de navegação (URI → view)
    │       ├── RootView.java               ← Layout root (header + content)
    │       ├── LoginView.java
    │       ├── HomeView.java
    │       ├── ProductsPanelView.java
    │       ├── ProductView.java
    │       ├── CartView.java
    │       ├── ReceiptView.java
    │       └── PurchasesPanelView.java
    └── webapp/
        ├── index.html
        └── css/app.css                     ← Utility classes + componentes + ícones
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

```
work/frontend/remote.shell.teavm/
├── index.html
├── css/app.css
└── js/
    └── app.js    ← Java compilado para JavaScript
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
