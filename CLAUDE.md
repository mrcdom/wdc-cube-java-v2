# CLAUDE.md

Guide for working in this repository. For the full architectural view, see [README.md](README.md) and [docs/](docs/).

## What it is

**WDC Cube Java v2** — architectural reference for the **Cube MVP** pattern (an MVP variation with hierarchical presenters, intent-based navigation and serializable Scopes). It demonstrates an e-commerce application (WDC Store, in the `br.com.wdc.store` modules) with a Java 25 backend and **five independent frontends** (React, Flutter, Vaadin, SWT, TeaVM) that share the same presenters/Scopes — only the rendering layer changes.

Project language: **US English** — docs, READMEs, code comments, strings, identifiers, database seed content and commit messages. The UI uses US dollars and en-US formats (e.g. `$16.00`, dates as MM/DD/YYYY).

## Layout

- `fontes/` — all the Maven source code (multi-module). **The build runs from here, not from the root.**
  - `br.com.wdc.framework/` — reusable framework: `commons`, `domain` (generic abstractions: `Repository`, `Page`, `ModelCodec`, `Projection*`, resource calls `Resource*`, base exceptions, security/`PermissionModel`, `AppConfig`), `cube`, `cube.remote`, `jooq`, `dependencies` (BOM)
  - `br.com.wdc.store/` — sample app: `domain`, `persistence` (aggregator of `persistence.impl` (jOOQ), `persistence.client` (HTTP/okhttp), `persistence.rest` (Javalin API)), `presentation`, `backend`, `scripts`, `tests`, `view.*`
    - **`domain` is organized by entity, not by class type**: `domain.product`, `domain.user`, `domain.purchase`, `domain.purchaseitem` — each package gathers the four classes of that entity (`Xxx`, `XxxCriteria`, `XxxCodec`, `XxxRepository`). When creating a new entity, create its package with the complete set; there are no `model`/`criteria`/`codec`/`repositories` packages. Beyond that, `domain` holds `exception`, `security` and the root holders (`StoreConfig`, `StoreTransactions`).
- `work/` — runtime directory: `config/` (TOML), `bin/` (start scripts), `data/`, `log/`, `frontend/`
- `docs/` — detailed architectural documentation

## Build & Run

```bash
# Build (Java 25 required)
export JAVA_HOME=$JAVA25_HOME  # demos/backend on release 25; the framework stays on release 21 (published to Central)
cd fontes && mvn clean package          # fat JAR in br.com.wdc.cube.backend/target/
cd fontes && mvn test                   # tests

# Run the backend (React/Flutter via WebSocket, port 8080)
./work/bin/start-server.sh [port]       # cd to work/, builds if the JAR is missing
./work/bin/start-h2-server.sh           # H2 in TCP mode (config points to tcp://localhost by default)

# Web frontends (React, Flutter web, remote.shell.teavm, teavm.web → work/frontend/)
./work/bin/build-frontends.sh            # all; --list shows targets/prerequisites, --skip-install skips the mvn install
./work/bin/build-frontends.sh react      # a single target

# Development: each module has its own watch
cd fontes/.../remote.shell.react && npm install && npm run watch
```

The four web frontends drop their artifacts in `work/frontend/<name>`; each one has its own `build.sh` in its source module, and `build-frontends.sh` only orchestrates the four. `work/frontend/{api.docs,openapi}` are static.

**TeaVM targets pitfall**: `remote.shell.teavm` and `teavm.web` compile against the JARs in `~/.m2`, not against the reactor — without a prior `mvn install`, they silently generate an app with the previous version of the domain (it opens normally and fails only on the call that changed). That is exactly why `build-frontends.sh` reinstalls by default.

**Publishing the framework to Maven Central**: only `br.com.wdc.framework`, as `io.github.mrcdom.wdc.java:framework-*` — `cd fontes/br.com.wdc.framework && mvn -Prelease clean deploy` (details in the [framework README](fontes/br.com.wdc.framework/README.md#publishing-to-maven-central)). `framework-parent` does not inherit from the root pom, on purpose: that keeps the demos out of consumers' dependency resolution.

External config: `work/config/application.toml` (resolution: system property `store.config.file` → fallback to `config/application.toml` relative to the working directory, which is `work/` when started by `work/bin/start-server.sh`). `application.local.toml` for local overrides.

## Persistence: jOOQ

The persistence layer uses **jOOQ**; queries are built with its type-safe DSL, with no SQL strings or command classes.

- Repositories (`*RepositoryImpl`) extend `BaseRepositoryImpl` and obtain the `DSLContext` via `dsl()` — which reads `StoreDSLContext.BEAN` (an **app** holder, in `persistence.impl`, not in the framework). `framework.jooq` receives the `DSLContext` by injection (`JsonQueryBuilder.setDSLContextSupplier`).
- Generated jOOQ classes live in `br.com.wdc.store.persistence.impl.scheme.*` (`Tables`, `Sequences`, `tables.EnProduct`, etc.) — `scheme` = database schema; jOOQ is just the generation technology.
- JSON query helpers in `br.com.wdc.framework.jooq` (`JsonQuery`, `JsonQueryBuilder`, `JsonChildQueryBuilder`).

### Transactions (JTA/JDBC)

CMT-style programmatic control via `TransactionService` (`framework.domain.transaction`, contract) + `TransactionServiceImpl`/`TransactionScope` (`framework.persistence`, dual-mode impl). Details: [docs/data-layer.md → Transactions](docs/data-layer.md#transactions--atomicity-and-dual-jtajdbc-mode).

- Mode chosen in `application.toml`: `database.transaction = "jta"` (Narayana + Agroal XA) or `"non-jta"` (direct JDBC, default). Concrete technology (Agroal/Narayana) only in the `br.com.wdc.cube.backend` host (`supports/`); `framework.persistence` is neutral (`javax.sql.DataSource` + `jakarta.transaction`).
- **Per module (no global holder)**: each module exposes its holders — `StoreDSLContext` (impl) and `StoreTransactions` (domain) — populated by the backend (composition root). `TransactionServiceImpl(Supplier<DataSource>)` is bound to the module's DataSource. There are **no** global `SqlDataSource.BEAN` or `TransactionService.BEAN`. The JTA TM remains unique per JVM (coordinator).
- Repositories do **not** demarcate transactions. The `DSLContext` uses `TransactionAwareConnectionProvider` (`framework.jooq`): inside `StoreTransactions.BEAN.get().required(...)` it shares the scope's connection; outside, a standalone connection (autocommit).
- The boundary is opened in the write use cases: the write resource calls (`ResourceWrite.run()`, e.g. the checkout's `PurchaseCreate`) and REST handlers (`RepositoryApiRoutes.transactional(...)`).
- **Client-driven remote transaction (REST)**: the HTTP client (`RestTransactionService`, an impl of `TransactionService` in `persistence.client`) makes **several** REST calls atomic — `begin`/`commit`/`rollback` via `/api/tx/*` (`TxApiController`), propagating the `txId` in the `X-Tx-Id` header. Server-side: SPI `RemoteTransactionCoordinator` (`framework.persistence`, **not** in the domain — it is server-only) + holder `RemoteTransactions.COORDINATOR` (`persistence.rest`), populated by the composition roots (`BusinessContext`, `TestEnvironment`). Same `required(...)` API as the in-process mode — a symmetric Host/client boundary. Details: [docs/data-layer.md → Remote Transactions](docs/data-layer.md#client-driven-remote-transactions-over-rest).

## Conventions

- **Dependency injection without a DI framework**: static service locator via `AtomicReference<T> BEAN` (e.g. `StoreDSLContext.BEAN`). In the presentation layer, each operation on the repositories is a class in the module's `calls/` package (sibling of `structs/`), named by a verb as the suffix, in the spirit of the HTTP methods: `XxxQuery extends ResourceQuery<R>` (GET: read, no transaction), `XxxCreate`/`XxxSave`/`XxxUpdate`/`XxxDelete` extending `ResourceCreate`/`ResourceSave`/`ResourceUpdate`/`ResourceDelete` (POST/PUT/PATCH/DELETE: implement `create()`/`save()`/`update()`/`delete()`; the final `run()` of their common base `ResourceWrite` opens the transaction, and its package-private constructor keeps that list closed; a write call hands over its module's transactions with `super(StoreTransactions.BEAN::get)`). The bases live in the framework, `framework.domain.resource`. What fits none gets a verb of its own and extends `ResourceCall<R>` directly (`CredentialsCheck`, the login); there is no generic `XxxCall`. Table of verbs and guarantees: [presentation README](fontes/br.com.wdc.store/br.com.wdc.store.presentation/README.md#calls). Request in fluent `withXxx(...)` setters, response from `run()`, a new instance for each use (`new ProductQuery().withProductId(id).run()`); each call reaches its repositories through their `BEAN` (`ProductRepository.BEAN.get()`); `ResourceCall` only declares `run()`. The response is an existing type, a struct in `structs/` (if others use it) or an inner class of the call (if it only carries the response, like `CredentialsCheck.Result`). Infrastructure components (e.g. `TransactionServiceImpl`) receive their dependencies in the constructor.
- **Virtual Threads** (Java 21) for WebSocket connections.
- **RBAC security**: HMAC challenge-response + JWT; authorization lives in the REST endpoints (`SecurityEnforcer`), the boundary that teavm.web goes through — in the hosts where presenters run on the server, the presenter decides each query and the repositories are used directly (`XxxRepository.BEAN.get()`); roles ADMIN/CUSTOMER/MANAGER (allow-wins model). React transport: RSA + PBKDF2 + AES-GCM.
- **Naming**: `*Scope`, `*ViewImpl`, `*Presenter`, `*RepositoryImpl`, `*Criteria`, `ApplyConditions` (inner class of each `*RepositoryImpl`).
- **Criteria**: each field of a `*Criteria` is a `Criterion` (`framework.domain.criteria`) — `Criterion` (eq/ne/in/isNull), `ComparableCriterion` (gt/ge/lt/le/between) or `TextCriterion` (like/ilike/containing/startingWith). Requests accumulate with `AND`; `or()` makes the field disjunctive. **The field is never `null`** — `criteria.xxx() == null` compiles and is always false; use `hasXxx()`. The translation to jOOQ is single, in `CriterionTranslator`; each entity only maps field→column in its `ApplyConditions`.
- **Entities (implementing `KeyedEntity`) have a fluent API**, like the `*Criteria`: `private` field, `field()` accessor and `withField(...)` setter that returns `this`. There are no public fields — build by chaining (`new Product().withName(x).withPrice(y)`), relations included (`withPurchase(new Purchase().withId(id))`). Presentation structs (`presentation.**.structs`) follow a different pattern: they keep public fields.
- **Scope with `Prop`** (`framework.cube`): `XxxScope extends Scope`, in its own file next to the presenter (`CartScope.java`, like the `xxx.scope.ts` of the TS version); a scope that exists only inside it (segment, list row) is a public static inner class of it (`CartScope.CartItemScope`). Each observed field is `public final Prop<T> x = prop(...)`, and the presenter creates it with `public final XxxScope scope = bind(new XxxScope())`. `set` of a different value (`equals`) already requests the redraw — do **not** call `this.update()` afterwards; it is only needed when the redraw must happen without a different value (`RootPresenter.alertUnexpectedError`: the same message again is still a new alert). The view reads with `get()`; a view that consumes a one-time display message while drawing uses `setSilently`. Actions (`Action`/`ActionOf`/`ActionOf2`) are plain fields of the scope, bound in the constructor with the bridge event code and the `formData` read: `actionOf(products, 1, FormParam.asLong("p.productId"), this::openProduct)` (`HomePresenter`, on a segment). The presenter **neither creates a view nor declares a skeleton**: the scope declares `public static final String CLASS_ID` (the bridge classId) and the skeleton belongs to the scope (`scope.skeleton(CLASS_ID)`, which dispatches to the actions bound with a code; the skeleton's `syncScope` runs what the presenter binds with `scope.onSync(formData -> ...)` in the constructor). Composition: the child hands itself to the parent with `ownerSlot.setScope(this.scope)` (`ScopeSlot`, read via `keys.parentSlot()`), the parent keeps `public final Prop<Scope> content = prop()` and the parent's view draws `app.viewOf(scope.content.get())`; the host only draws the root presenter's scope.
- **Segments**: a piece of screen without its own navigation (the Home panels) gets no presenter — it is a smaller scope, a public static inner class of the owner scope (`HomeScope.ProductsPanelScope`), with a static `CLASS_ID`. The owner presenter binds the actions (`actionOf(segment, code, FormParam..., fn)`) and just puts it in a `Prop` of the owner scope — it knows nothing about views. The view belongs to the view layer: each host assembles a `ScopeViews` catalog (scope class → factory; the remote host uses `fallback(RemoteViewImpl::forScope)`, which reads the `CLASS_ID`) and hands it over with `setScopeViews`; the parent's view asks for the segment's view with `app.viewOf(scope)` (created the first time; before that, changing the segment redraws the owner). Remotely it goes out as `"<field>ViewId"`. List rows too: `Prop<List<XxxRowScope>>` goes out as `"<field>ViewIds"`; the presenter reuses the row by item identity and calls `detach` on the one that left (see `CartPresenter.syncRows`).
- **Places and keys**: each place has its `XxxKeys` (`CubeKeys`), which declares the place and is the only one that knows its names in the intent (two-letter prefix); list it in `StoreApplication.PLACES`. To navigate: `new ProductKeys(app).withProductId(id).flip()`.
- **Java formatting**: `fontes/wedocode-java-formatter.xml`. In every fluent chain, each `.withXxx(...)` goes on its own line — including in a chain nested inside an argument —, and so do the following calls of the same chain (`.run()`, `.flip()`); the continuation indents 2 levels (8 spaces or 2 tabs, depending on the file). A single setter on a variable (`criteria.withUserId(id);`) is not a chain and stays on the line. The formatter does not join these breaks (`join_wrapped_lines=false`). Frontend: Prettier (configured in `.vscode/settings.json`, format-on-save).

## Commit messages

Pattern: **emoji + `[TYPE]` in uppercase + description in English**.

```
🐛 [BUGFIX] fix what was broken
✨ [FEATURE] add the feature
🧹 [CHORE] clean up / minor refactor
```

Main branch: `main`. Current working branch: `evolucoes-estruturais`.
