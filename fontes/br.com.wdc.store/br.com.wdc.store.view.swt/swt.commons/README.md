# swt.commons

Shared library of SWT views and components for **WDC Store**. It contains the concrete SWT implementations of the application's screens — reused both by the local desktop app (`swt.desktop`) and by the remote shell (`remote.shell.swt`).

## Responsibility

Centralize the SWT code that does not depend on how the scope is obtained (locally via Presenter or remotely via ScopeSnapshot). The views extend `AbstractViewSwt` and receive data through `update()`, regardless of the source.

## Package Structure

```
br.com.wdc.store.view.swt/
├── AbstractViewSwt.java              # Base for all views (performUpdate/rebuild, thread safety)
├── SwtApp.java                       # SWT application contract (navigation, dirty flush)
├── ScheduledExecutorSwtAdapter.java  # Bridge ConcurrentExecutor → SWT Display thread
├── impl/                             # View implementations
│   ├── RootViewSwt.java
│   ├── LoginViewSwt.java
│   ├── HomeViewSwt.java
│   ├── ProductViewSwt.java
│   ├── ProductsPanelViewSwt.java
│   ├── CartViewSwt.java
│   ├── CartItemViewSwt.java
│   ├── ReceiptViewSwt.java
│   └── PurchasesPanelViewSwt.java
├── components/                       # Reusable custom widgets
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
│   ├── Theme.java                    # Color palette, fonts, spacing (from the design tokens, StoreTokens)
│   └── Surface.java                  # Enumeration of surfaces (background, card, ...)
└── util/
    ├── SwtDom.java                   # Fluent DSL for creating widgets (`.row()`, `.col()`, ...)
    ├── GridDataUtils.java            # Shortcuts for GridData (fill, grab, hint)
    ├── RowDataUtils.java             # Shortcuts for RowData
    ├── FormDataUtils.java            # Shortcuts for FormData/FormAttachment
    ├── SlotComposite.java            # Container that swaps the active child (slot navigation)
    ├── StackComposite.java           # Stacked container (SWT CardLayout)
    └── ProductImageCache.java        # Product image cache (SWT Image)
```

## Dependencies

- `br.com.wdc.store.presentation` — Scope classes (read only)
- `org.eclipse.platform:org.eclipse.swt.*` — via Maven profile (macos-aarch64, macos-x86_64, linux-x86_64, win32-x86_64, win32-aarch64)

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.store/br.com.wdc.store.view.swt/swt.commons -am
```
