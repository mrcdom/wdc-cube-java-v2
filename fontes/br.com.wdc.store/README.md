# 🛒 WDC Store

A **complete e-commerce system** built on the **Cube MVP** architecture, showing how the same business logic can power entirely different interfaces — **React (web)**, **Flutter (mobile/desktop/web)**, **Vaadin (server-side web)**, **SWT (native desktop)** and **TeaVM (web/desktop/Android/iOS)** — without duplicating a single line of presentation code.

> **Five frontends. One soul.**

---

## What you will find

✅ Product catalog with images and descriptions  
✅ Shopping cart with adding, removing and total calculation  
✅ Complete purchase flow with receipt  
✅ Paginated purchase history  
✅ Secure remote channel (a key per connection: ECDH + AES-GCM)  
✅ H2 database (embedded file or TCP server) or PostgreSQL, chosen in the TOML config  
✅ Virtual Threads (Java 21+) — ultra-light web server  
✅ Fat JAR (~41 MB) — trivial deployment

---

## Run it in 4 commands

```bash
# 1. Clone
git clone https://github.com/mrcdom/wdc-cube-java-v2.git
cd wdc-cube-java-v2

# 2. Build (requires Java 25 + Maven 3.9+)
export JAVA_HOME=<path-to-jdk-25>  # e.g.: /Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
(cd fontes && mvn clean package -DskipTests)

# 3. Start H2 in TCP mode (the database of the default work/config/application.toml)
./work/bin/start-h2-server.sh

# 4. Run the backend, from work/ (in another terminal)
./work/bin/start-server.sh
```

Open **http://localhost:8080** and sign in with `admin` / `admin`.

---

## Or run the Server-Side Web version (Vaadin)

```bash
export JAVA_HOME=<path-to-jdk-25>
cd fontes/br.com.wdc.store/br.com.wdc.store.view.vaadin
java -jar target/br.com.wdc.store.view.vaadin-1.0.0.jar --workdir=work
```

Open **http://localhost:8090**. The module has its own work directory (`work/`, with `config/application.toml`) and its own H2 file database. A fully server-side UI with Vaadin 25 + Lumo theme. Same presenters, same domain.

---

## Or run the Desktop version (Eclipse SWT)

```bash
export JAVA_HOME=<path-to-jdk-25>
cd fontes/br.com.wdc.store/br.com.wdc.store.view.swt/swt.desktop
# -XstartOnFirstThread is required on macOS (SWT/Cocoa runs the UI on the main thread)
java -XstartOnFirstThread -jar target/swt.desktop-1.0.0.jar --workdir=work
```

A native desktop application with Eclipse SWT. Same presenters, same domain, embedded H2 file database (its work directory is `swt.desktop/work/`).

---

## Or run the TeaVM version (Web / Desktop / Android / iOS)

```bash
# Web: compiles Java → JavaScript into work/frontend/teavm.web (served by the backend)
./work/bin/build-frontends.sh teavm.web

# Native desktop (Tauri): compiles teavm.web and packages it
cd fontes/br.com.wdc.store/br.com.wdc.store.view.teavm/teavm.native
./build.sh desktop          # or: ./build.sh desktop --dev
```

Java compiled to JavaScript via TeaVM, packaged as a native app with Tauri 2. Same presenters, same domain — running on 4 platforms.

---

## Modules

| Module | Responsibility |
|--------|-----------------|
| [`design`](br.com.wdc.store.design/) | The design tokens (`tokens.json`): palette, spacing, radii, type, generated for every view technology |
| [`i18n`](br.com.wdc.store.i18n/) | The UI texts, one catalog per language (en-US, pt-BR), generated for every view technology |
| [`domain`](br.com.wdc.store.domain/) | Entities, repository contracts, criteria and codecs per entity; roles, config and transaction holders |
| [`persistence`](br.com.wdc.store.persistence/) | Groups the repository implementations: `persistence.impl` (jOOQ), `persistence.client` (HTTP/OkHttp) and `persistence.rest` (Javalin API) |
| [`presentation`](br.com.wdc.store.presentation/) | Hierarchical presenters, scopes, calls, navigation |
| [`scripts`](br.com.wdc.store.scripts/) | The schema and the seed (DBCreate, DBReset); the code generators (jOOQ classes, design tokens, UI texts, the REST API's contract `docs/openapi.json`) |
| [`view.remote`](br.com.wdc.store.view.remote/) | Complete web frontend (React + Javalin + WebSocket) |
| [`view.vaadin`](br.com.wdc.store.view.vaadin/) | Server-side web frontend (Vaadin 25 + Jetty 12 + Lumo theme) |
| [`view.swt`](br.com.wdc.store.view.swt/) | Native desktop frontend (Eclipse SWT) |
| [`view.teavm`](br.com.wdc.store.view.teavm/) | Cross-platform frontend (TeaVM + Tauri — Web, Desktop, Android, iOS) |
| [`persistence.impl`](br.com.wdc.store.persistence/persistence.impl/) | jOOQ repositories (`*RepositoryImpl`), generated schema classes, data source |
| [`persistence.rest`](br.com.wdc.store.persistence/persistence.rest/) | REST controllers (Javalin) that expose the repositories over HTTP |
| [`persistence.client`](br.com.wdc.store.persistence/persistence.client/) | REST client (OkHttp + the domain codecs) that implements the repositories over HTTP |
| [`tests`](br.com.wdc.store.tests/) | Workflow and repository tests |
| [`docker`](br.com.wdc.store.docker/) | Compose setup with two backend instances, PostgreSQL, nginx and Prometheus (not a Maven module) |

---

## Why explore this project?

- **No Spring, no CDI, no magic** — injection via `AtomicReference<T> BEAN`, 100% explicit
- **Real separation of concerns** — swap the whole UI without touching logic (multiple frontends prove it)
- **Solid patterns** — Repository, Presenter, Scope
- **Modern technology** — Java 25, Virtual Threads, React 19, Vaadin 25, TypeScript
- **Clean code** — ~3.5s full build, zero warnings
- **Real security** — ECDH P-256 + HKDF + AES-GCM per connection, HMAC-signed sessions and addresses (not just for show)

---

## License

[MIT License](https://github.com/mrcdom/wdc-cube-java-v2/blob/main/LICENSE) — use, modify, distribute.

Copyright (c) 2026 Marcelo Domingos / WeDoCode Consultoria LTDA
