# WDC Shopping — SWT Desktop View

Implementação desktop nativa do WDC Shopping usando **Eclipse SWT 3.128.0** (Cocoa/macOS aarch64).
Compartilha a mesma camada de apresentação (Cube MVP) com as demais views (TeaVM, React, Gluon, Vaadin).

## Arquitetura

```mermaid
graph TD
    Main["ShoppingSwtMain<br/><small>Entry point: Shell, Display, DB init, session restore</small>"]
    Main --> App["ShoppingSwtApplication<br/><small>View factory registration<br/>Render loop (Timer 16ms → flushDirtyViews)<br/>Dirty-view tracking (ConcurrentHashMap)<br/>History management (navigation stack)</small>"]
    App --> Base["AbstractViewSwt&lt;P&gt;<br/><small>Base class for all views (implements CubeView)<br/>performUpdate(): doUpdate() + initial layout<br/>rebuild(): dispose children, reset state<br/>Thread safety via markDirty() → EDT timer flush</small>"]
    Base --> Impl["impl/<br/><small>ViewSwt implementations</small>"]
    Base --> Components["components/<br/><small>Custom widgets</small>"]
    Base --> Util["util/<br/><small>Helpers</small>"]
```

### Render Loop

```mermaid
sequenceDiagram
    participant BG as Background Thread
    participant App as ShoppingSwtApplication
    participant EDT as SWT Display Thread (EDT)

    BG->>BG: presenter.state changes
    BG->>App: view.update()
    App->>App: markDirty(view)
    Note over EDT: Timer(16ms)
    EDT->>EDT: flushHistory()
    EDT->>EDT: flushDirtyViews()
    EDT->>EDT: view.performUpdate()
```

### Estrutura de Pacotes

```mermaid
graph TD
    root["br.com.wdc.shopping.view.swt/"]
    main["ShoppingSwtMain.java<br/><small>Entry point, DB init, event loop</small>"]
    app["ShoppingSwtApplication.java<br/><small>App lifecycle, view factories, render timer</small>"]
    base["AbstractViewSwt.java<br/><small>Base view class (performUpdate/rebuild)</small>"]
    sched["ScheduledExecutorSwtAdapter.java<br/><small>Bridge ConcurrentExecutor → SWT Display</small>"]
    theme["theme/<br/><small>Theme.java · Surface.java</small>"]
    components["components/<br/><small>PrimaryButton · ActionButton · IconButton<br/>CardHeader · AccentLine · Separator<br/>ShadowCard · ScrolledPage · ErrorBanner</small>"]
    util["util/<br/><small>SwtDom · GridDataUtils · RowDataUtils<br/>FormDataUtils · SlotComposite · StackComposite<br/>ProductImageCache</small>"]
    impl["impl/<br/><small>RootViewSwt · LoginViewSwt · HomeViewSwt<br/>CartViewSwt · ProductViewSwt · ReceiptViewSwt<br/>ProductsPanelViewSwt · PurchasesPanelViewSwt</small>"]

    root --> main
    root --> app
    root --> base
    root --> sched
    root --> theme
    root --> components
    root --> util
    root --> impl
```

## Dependências Principais

| Dependência | Versão | Propósito |
|-------------|--------|-----------|
| Eclipse SWT (Cocoa macOS aarch64) | 3.128.0 | Toolkit gráfico nativo |
| br.com.wdc.shopping.presentation | 1.0.0 | Presenters e state (Cube MVP) |
| br.com.wdc.shopping.persistence | 1.0.0 | Repositórios (JOOQ + H2) |
| H2 Database | — | Banco de dados embarcado |
| Logback | — | Logging |

## Build

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.swt
JAVA_HOME=$JAVA21_HOME mvn clean compile -DskipTests
```

## Execução

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.swt
JAVA_HOME=$JAVA21_HOME mvn exec:java \
  -Dexec.mainClass="br.com.wdc.shopping.view.swt.ShoppingSwtMain"
```

> A aplicação inclui backend embarcado (H2 + repositórios), não requer servidor externo.

## Screenshots

### Login

![Login](docs/screenshots/swt-login.png)

### Home

![Home](docs/screenshots/swt-home.png)

### Carrinho (vazio)

![Carrinho vazio](docs/screenshots/swt-cart-empty.png)

### Carrinho (com produtos)

![Carrinho com produtos](docs/screenshots/swt-cart.png)

### Recibo (histórico)

![Recibo](docs/screenshots/swt-receipt.png)

### Recibo (pós-compra)

![Recibo pós-compra](docs/screenshots/swt-receipt-purchased.png)
