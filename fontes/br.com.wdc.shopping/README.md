# 🛒 WeDoCode Shopping

A **complete e-commerce system** built on the **Cube MVP** architecture, showing how the same business logic can power entirely different interfaces — **React (web)**, **Vaadin (server-side web)**, **SWT (native desktop)** and **TeaVM (web/desktop/Android/iOS)** — without duplicating a single line of presentation code.

> **Five frontends. One soul.**

---

## What you will find

✅ Product catalog with images and descriptions  
✅ Shopping cart with adding, removing and total calculation  
✅ Complete purchase flow with receipt  
✅ Paginated purchase history  
✅ Secure authentication (RSA + AES-GCM in the web version)  
✅ Embedded H2 database — zero configuration to run  
✅ Virtual Threads (Java 21+) — ultra-light web server  
✅ Fat JAR (~11 MB) — trivial deployment

---

## Run it in 3 commands

```bash
# 1. Clone
git clone https://github.com/mrcdom/wdc-cube-java-v2.git
cd wdc-cube-java-v2/fontes

# 2. Build (requires Java 25 + Maven 3.9+)
export JAVA_HOME=<path-to-jdk-25>  # e.g.: /Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
mvn clean package -DskipTests

# 3. Run
java -jar \
  br.com.wdc.cube.backend/target/br.com.wdc.cube.backend-1.0.0.jar
```

Open **http://localhost:8080** and sign in with `admin` / `admin`.

---

## Or run the Server-Side Web version (Vaadin)

```bash
export JAVA_HOME=<path-to-jdk-25>
cd br.com.wdc.shopping/br.com.wdc.shopping.view.vaadin
java -cp "$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout):target/classes" \
  br.com.wdc.shopping.view.vaadin.ShoppingVaadinMain
```

Open **http://localhost:8090**. A fully server-side UI with Vaadin 25 + Lumo theme. Same presenters, same domain.

---

## Or run the Desktop version (Eclipse SWT)

```bash
export JAVA_HOME=<path-to-jdk-25>
cd br.com.wdc.shopping/br.com.wdc.shopping.view.swt
mvn exec:java \
  -Dexec.mainClass="br.com.wdc.shopping.view.swt.ShoppingSwtMain"
```

A native desktop application with Eclipse SWT. Same presenters, same domain, embedded H2 database.

---

## Or run the TeaVM version (Web / Desktop / Android / iOS)

```bash
# TeaVM build (compiles Java → JavaScript)
cd br.com.wdc.shopping/br.com.wdc.shopping.view.teavm
bash build.sh

# Native desktop (Tauri)
cd br.com.wdc.shopping.view.teavm.native
cargo tauri dev
```

Java compiled to JavaScript via TeaVM, packaged as a native app with Tauri 2. Same presenters, same domain — running on 4 platforms.

---

## Modules

| Module | Responsibility |
|--------|-----------------|
| [`domain`](br.com.wdc.shopping.domain/) | Entities, repository contracts, criteria and codecs per entity; roles, config and transaction holders |
| [`persistence`](br.com.wdc.shopping.persistence/) | Groups the repository implementations: `persistence.impl` (jOOQ), `persistence.client` (HTTP/OkHttp) and `persistence.rest` (Javalin API) |
| [`presentation`](br.com.wdc.shopping.presentation/) | Hierarchical presenters, scopes, calls, navigation |
| [`scripts`](br.com.wdc.shopping.scripts/) | DDL scripts (DBCreate, DBReset) |
| [`view.remote`](br.com.wdc.shopping.view.remote/) | Complete web frontend (React + Javalin + WebSocket) |
| [`view.vaadin`](br.com.wdc.shopping.view.vaadin/) | Server-side web frontend (Vaadin 25 + Jetty 12 + Lumo theme) |
| [`view.swt`](br.com.wdc.shopping.view.swt/) | Native desktop frontend (Eclipse SWT) |
| [`view.teavm`](br.com.wdc.shopping.view.teavm/) | Cross-platform frontend (TeaVM + Tauri — Web, Desktop, Android, iOS) |
| [`persistence.impl`](br.com.wdc.shopping.persistence/persistence.impl/) | jOOQ repositories (`*RepositoryImpl`), generated schema classes, data source |
| [`persistence.rest`](br.com.wdc.shopping.persistence/persistence.rest/) | REST controllers (Javalin) that expose the repositories over HTTP |
| [`persistence.client`](br.com.wdc.shopping.persistence/persistence.client/) | REST client (OkHttp + the domain codecs) that implements the repositories over HTTP |
| [`tests`](br.com.wdc.shopping.tests/) | Workflow and repository tests |

---

## Why explore this project?

- **No Spring, no CDI, no magic** — injection via `AtomicReference<T> BEAN`, 100% explicit
- **Real separation of concerns** — swap the whole UI without touching logic (multiple frontends prove it)
- **Solid patterns** — Repository, Presenter, Scope
- **Modern technology** — Java 25, Virtual Threads, React 19, Vaadin 25, TypeScript
- **Clean code** — ~3.5s full build, zero warnings
- **Real security** — RSA + PBKDF2 + AES-GCM (not just for show)

---

## License

[MIT License](https://github.com/mrcdom/wdc-cube-java-v2/blob/main/LICENSE) — use, modify, distribute.

Copyright (c) 2026 Marcelo Domingos / WeDoCode Consultoria LTDA
