# 0013. Dependency injection without a framework: `BEAN` holders filled by composition roots

- **Date:** 2026-04-28 (`BEAN` holders); 2026-06-17 (per-module holders, filled by the composition root)
- **Status:** Accepted

## Context

The same presenters and resource calls run in very different places: a Javalin backend, a Vaadin server, an SWT desktop process, and the browser, compiled to JavaScript by TeaVM. Each place wires different implementations behind the same contracts — the jOOQ repositories or the HTTP ones, `TransactionServiceImpl` or `RestTransactionService`. Whatever does the wiring has to work in all of them, the TeaVM-compiled browser included.

The first holders included global ones for infrastructure (`SqlDataSource.BEAN`, `TransactionService.BEAN`), which bound every module of the JVM to one database.

## Decision

No DI framework. A contract that has to be swapped exposes a static holder:

```java
AtomicReference<ProductRepository> BEAN = new AtomicReference<>();
```

The repositories (`ProductRepository.BEAN`, `UserRepository.BEAN`, ...), `AuthenticationService.BEAN`, `CryptoProvider.BEAN`, `ScheduledExecutor.BEAN`, and the module holders `StoreTransactions.BEAN` and `StoreDSLContext.BEAN` follow this shape. Code reads them where it needs them: `ProductRepository.BEAN.get().fetch(criteria)`.

The holders are filled by **composition roots**, which know every module: `BusinessContext` in the backend, `TestEnvironment` in the tests, and the entry points of the other hosts (`StoreVaadinMain`, `StoreSwtMain`, `StoreSwtRemoteApp`), mostly through the bootstraps `StoreRepositoryBootstrap` (jOOQ) and `RestRepositoryBootstrap` (HTTP); teavm.web has its own, `TeaVMRepositoryBootstrap`. Each bootstrap also undoes what it set, putting the holders back to `null` (a clean-up pushed on a `Defer`, or a `release()`).

Infrastructure is per module, not global: each module exposes its own holders, and infrastructure components get their dependencies in the constructor (`TransactionServiceImpl(Supplier<DataSource>)`, `RemoteTransactionCoordinatorImpl`).

## Consequences

- The wiring of each host is plain Java in one place, readable and debuggable, and works under TeaVM.
- Presenters, calls and repositories do not know which implementation they run on.
- A holder is global state: tests reset it through the clean-ups, and code that runs before its composition root gets `null`.
- Nothing checks at startup that every holder was filled; a missing one fails at first use.

## Alternatives considered

- **Global infrastructure holders** (`SqlDataSource.BEAN`, `TransactionService.BEAN`). Replaced on 2026-06-17 by per-module holders ([0009](0009-programmatic-transactions-per-module.md)).

See [data-layer.md → Repositories as Injectable Interfaces](../data-layer.md#repositories-as-injectable-interfaces) and [Per-module context](../data-layer.md#per-module-context-hexagonal).
