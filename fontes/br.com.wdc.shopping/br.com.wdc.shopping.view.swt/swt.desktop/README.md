# swt.desktop

Complete native desktop app for **WeDoCode Store** using SWT — a standalone version with local Presenters and persistence (no remote server).

It contains only the entry point and the application wiring. All view logic lives in `swt.commons`; all business logic lives in `br.com.wdc.shopping.presentation` and `br.com.wdc.shopping.persistence`.

## Classes

| Class | Responsibility |
|--------|------------------|
| `ShoppingSwtMain` | Entry point: initializes the H2 database, instantiates the SWT `Display`, creates the `Shell`, starts the event loop |
| `ShoppingSwtApplication` | Implements `SwtApp`: instantiates and registers the views from `swt.commons`, connects local Presenters, manages the render loop (Timer 16ms → `flushDirtyViews`) and the navigation stack |

## Dependencies

- `swt.commons` — shared SWT views and components
- `br.com.wdc.shopping.persistence` — data access (H2 + JOOQ)
- `br.com.wdc.shopping.scripts` — initialization scripts (sample data)

## Prerequisites

- **Java 25**
- **Maven 3.9+**
- SWT selected automatically via a per-platform Maven profile (macOS aarch64, Linux x86_64, Windows x86_64)

## Build and Run

```bash
cd fontes && mvn clean package -pl br.com.wdc.shopping/br.com.wdc.shopping.view.swt/swt.desktop -am
java -jar target/swt.desktop-1.0.0.jar
```
