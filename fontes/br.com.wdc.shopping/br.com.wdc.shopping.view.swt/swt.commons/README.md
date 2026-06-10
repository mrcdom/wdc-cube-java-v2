# swt.commons

Biblioteca compartilhada de views e componentes SWT para o **WeDoCode Shopping**. Contém as implementações concretas das telas da aplicação em SWT — reutilizadas tanto pelo app desktop local (`swt.desktop`) quanto pelo shell remoto (`remote.shell.swt`).

## Responsabilidade

Centralizar o código SWT que não depende de como o estado é obtido (local via Presenter ou remoto via ViewStateSnapshot). As views herdam de `AbstractViewSwt` e recebem dados por `update()`, independente da fonte.

## Estrutura de Pacotes

```
br.com.wdc.shopping.view.swt/
├── AbstractViewSwt.java              # Base para todas as views (performUpdate/rebuild, thread-safety)
├── SwtApp.java                       # Contrato da aplicação SWT (navegação, dirty-flush)
├── ScheduledExecutorSwtAdapter.java  # Bridge ConcurrentExecutor → SWT Display thread
├── impl/                             # Implementações das views
│   ├── RootViewSwt.java
│   ├── LoginViewSwt.java
│   ├── HomeViewSwt.java
│   ├── ProductViewSwt.java
│   ├── ProductsPanelViewSwt.java
│   ├── CartViewSwt.java
│   ├── ReceiptViewSwt.java
│   └── PurchasesPanelViewSwt.java
├── components/                       # Widgets customizados reutilizáveis
│   ├── PrimaryButton.java
│   ├── ActionButton.java
│   ├── IconButton.java
│   ├── CardHeader.java
│   ├── AccentLine.java
│   ├── Separator.java
│   ├── ShadowCard.java
│   ├── ScrolledPage.java
│   └── ErrorBanner.java
├── theme/
│   ├── Theme.java                    # Paleta de cores, fontes, espaçamentos
│   └── Surface.java                  # Enumeração de superfícies (background, card, ...)
└── util/
    ├── SwtDom.java                   # DSL fluente para criação de widgets (`.row()`, `.col()`, ...)
    ├── GridDataUtils.java            # Atalhos para GridData (fill, grab, hint)
    ├── RowDataUtils.java             # Atalhos para RowData
    ├── FormDataUtils.java            # Atalhos para FormData/FormAttachment
    ├── SlotComposite.java            # Container que troca filho ativo (navegação em slot)
    ├── StackComposite.java           # Container em pilha (CardLayout SWT)
    └── ProductImageCache.java        # Cache de imagens de produto (SWT Image)
```

## Dependências

- `br.com.wdc.shopping.presentation` — classes ViewState (apenas leitura de estado)
- `org.eclipse.platform:org.eclipse.swt.*` — via profile Maven (macos-aarch64, linux-x86_64, win32-x86_64)

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.shopping/br.com.wdc.shopping.view.swt/swt.commons -am
```
