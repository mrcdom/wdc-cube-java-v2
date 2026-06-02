# WDC Shopping — SWT Desktop View

Implementação desktop nativa do WDC Shopping usando **Eclipse SWT 3.128.0** (Cocoa/macOS aarch64).
Compartilha a mesma camada de apresentação (Cube MVP) com as demais views (Swing, TeaVM, React, Gluon, Vaadin).

## Arquitetura

```
┌─────────────────────────────────────────────────────────┐
│                    ShoppingSwtMain                       │
│  (Entry point: Shell, Display, DB init, session restore)│
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│              ShoppingSwtApplication                      │
│  - View factory registration (Presenter.createView)     │
│  - Render loop (Timer 16ms → flushDirtyViews)           │
│  - Dirty-view tracking (ConcurrentHashMap)              │
│  - History management (navigation stack)                │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│              AbstractViewSwt<P>                          │
│  - Base class for all views (implements CubeView)       │
│  - performUpdate(): doUpdate() + initial layout         │
│  - rebuild(): dispose children, reset state             │
│  - Thread safety via markDirty() → EDT timer flush      │
└────────────────────────┬────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
    ┌──────────┐  ┌───────────┐  ┌──────────────┐
    │ impl/    │  │ components│  │ util/        │
    │ ViewSwt  │  │ (custom)  │  │ (helpers)    │
    └──────────┘  └───────────┘  └──────────────┘
```

### Render Loop

```
Background Thread                     SWT Display Thread (EDT)
─────────────────                     ─────────────────────────
presenter.state changes
       │
       ▼
  view.update()
       │
       ▼
  app.markDirty(view)  ───────────►  Timer(16ms) {
                                       flushHistory()
                                       flushDirtyViews() {
                                         view.performUpdate()
                                       }
                                     }
```

### Estrutura de Pacotes

```
br.com.wdc.shopping.view.swt/
├── ShoppingSwtMain.java              # Entry point, DB init, event loop
├── ShoppingSwtApplication.java       # App lifecycle, view factories, render timer
├── AbstractViewSwt.java              # Base view class (performUpdate/rebuild)
├── ScheduledExecutorSwtAdapter.java  # Bridge ConcurrentExecutor → SWT Display
├── theme/
│   ├── Theme.java                    # Colors, fonts, dimensions
│   └── Surface.java                  # Painted surfaces (gradients, shadows)
├── components/
│   ├── PrimaryButton.java            # Botão primário (computeSize override)
│   ├── ActionButton.java             # Botão de ação
│   ├── IconButton.java               # Botão com ícone (30x30)
│   ├── CardHeader.java               # Cabeçalho de card
│   ├── AccentLine.java               # Linha decorativa colorida
│   ├── Separator.java                # Separador horizontal
│   ├── ShadowCard.java               # Card com sombra
│   ├── ScrolledPage.java             # Container com scroll
│   └── ErrorBanner.java              # Banner de erro
├── util/
│   ├── SwtDom.java                   # Builder API declarativa para UI
│   ├── GridDataUtils.java            # Helpers para GridData
│   ├── RowDataUtils.java             # Helpers para RowData
│   ├── FormDataUtils.java            # Helpers para FormData
│   ├── SlotComposite.java            # Slot para troca dinâmica de views
│   ├── StackComposite.java           # Empilha composites (overlay)
│   └── ProductImageCache.java        # Cache de imagens de produtos
└── impl/
    ├── RootViewSwt.java              # View raiz (content slot)
    ├── LoginViewSwt.java             # Tela de login (gradient, form)
    ├── HomeViewSwt.java              # Home (products + purchases panels)
    ├── CartViewSwt.java              # Carrinho de compras
    ├── ProductViewSwt.java           # Detalhe de produto
    ├── ReceiptViewSwt.java           # Recibo de compra
    ├── ProductsPanelViewSwt.java     # Painel de produtos (grid dinâmico)
    └── PurchasesPanelViewSwt.java    # Painel de compras recentes
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
