# swt.desktop

Complete native desktop app for **WDC Store** using SWT — a standalone version with local Presenters and persistence (no remote server).

It contains only the entry point and the application wiring. All view logic lives in `swt.commons`; all business logic lives in `br.com.wdc.store.presentation` and `br.com.wdc.store.persistence`.

## Classes

| Class | Responsibility |
|--------|------------------|
| `StoreSwtMain` | Entry point: initializes the H2 database, instantiates the SWT `Display`, creates the `Shell`, starts the event loop |
| `StoreSwtApplication` | Implements `SwtApp`: instantiates and registers the views from `swt.commons`, connects local Presenters, manages the render loop (Timer 16ms → `flushDirtyViews`) and the navigation stack |

## Dependencies

- `swt.commons` — shared SWT views and components
- `br.com.wdc.store.persistence` — data access (H2 + JOOQ)
- `br.com.wdc.store.scripts` — initialization scripts (sample data)

## Prerequisites

- **Java 25**
- **Maven 3.9+**
- SWT selected automatically via a per-platform Maven profile (macOS aarch64, Linux x86_64, Windows x86_64)

## Build and Run

```bash
cd fontes && mvn clean package -pl br.com.wdc.store/br.com.wdc.store.view.swt/swt.desktop -am
cd br.com.wdc.store/br.com.wdc.store.view.swt/swt.desktop
# -XstartOnFirstThread on macOS only (SWT/Cocoa runs the UI on the main thread); the work directory is required
java -XstartOnFirstThread -jar target/swt.desktop-1.0.0.jar --workdir=work
```
