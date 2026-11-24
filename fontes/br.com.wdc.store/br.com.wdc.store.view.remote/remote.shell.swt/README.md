# remote.shell.swt

SWT desktop shell for the **WDC Store** remote presentation protocol. It renders the application's views using native SWT widgets, communicating with `remote.host` over WebSocket — no local business logic, no database.

It reuses the view implementations from `swt.commons`, overriding them with "remote" versions that obtain their scope via `ScopeSnapshot` (instead of local Presenters).

## Architecture

```
remote.host  <──WebSocket──>  remote.shell.swt
                              ├── RemoteViewContext       (shared context: HostClient, images)
                              ├── StoreSwtRemoteApp    (lifecycle: connects, navigates, manages views)
                              ├── StoreSwtRemoteMain   (entry point: SWT Display, event loop)
                              └── *ViewSwtRemote          (SWT views that consume ScopeSnapshot)
```

## Classes

| Class | Responsibility |
|--------|------------------|
| `StoreSwtRemoteMain` | Entry point: initializes the SWT Display, creates the Shell, starts the event loop |
| `StoreSwtRemoteApp` | Application lifecycle: connects to the host, navigates between views according to the received URI |
| `RemoteViewContext` | Context shared between views: `HostClient`, image cache, theme |
| `RemoteProductImageRepository` | Fetches product images over HTTP from the host endpoint |
| `RootViewSwtRemote` | Root view: main container, manages the active view |
| `LoginViewSwtRemote` | Login screen — submits credentials to the host via `HostClientSession.submit()` |
| `HomeViewSwtRemote` | Home: product list and purchase history |
| `ProductViewSwtRemote` | Product detail |
| `ProductsPanelViewSwtRemote` | Product listing panel |
| `CartViewSwtRemote` | Shopping cart |
| `CartItemViewSwtRemote` | Cart row (a view of its own) |
| `ReceiptViewSwtRemote` | Purchase receipt |
| `PurchasesPanelViewSwtRemote` | Purchase history |

## Dependencies

- `swt.commons` — Shared SWT widgets and components
- `remote.bridge.java` — `HostClient`, `ScopeSnapshot`, WebSocket protocol
- `br.com.wdc.store.presentation` — scope classes
- `org.eclipse.platform:org.eclipse.swt.*` 3.135.0 (platform-specific via Maven profile)

## Prerequisites

- **Java 25**
- **Maven 3.9+**
- `remote.host` running (port 8080 by default)

## Build and Run

```bash
cd fontes && mvn clean package -pl br.com.wdc.store/br.com.wdc.store.view.remote/remote.shell.swt -am
java -jar br.com.wdc.store/br.com.wdc.store.view.remote/remote.shell.swt/target/remote.shell.swt-1.0.0.jar http://localhost:8080
```

The only argument is the server URL (default: `http://localhost:8080`).
