# WDC Framework — Persistence

**Programmatic CMT-style** (Container-Managed Transaction) transaction control, with a **dual JTA/JDBC** mode — a technology-neutral implementation: it depends only on `javax.sql.DataSource` (JDBC) and `jakarta.transaction` (JTA), never on a concrete pool or TransactionManager.

> Full architectural view: [docs/data-layer.md → Transactions](../../../docs/data-layer.md#transactions--atomicity-and-dual-jtajdbc-mode).

## Dependencies

| Artifact | Role |
|----------|------|
| `br.com.wdc.framework.domain` | `TransactionService` / `TransactionContext` contract and exceptions |
| `br.com.wdc.framework.commons` | `Log` (remote coordinator logging) |
| `jakarta.transaction:jakarta.transaction-api` | JTA interfaces (vendor-neutral) |

**Does not depend** on Agroal, Narayana or XA drivers — those live in the host (e.g. `br.com.wdc.cube.backend`).

## Components

- **`TransactionServiceImpl`** — implements the `TransactionService` contract (in `framework.domain.transaction`) with the propagations `required` / `requiresNew` / `mandatory` / `supports` / `notSupported` / `never` (each with a `…Call` variant that returns a value). Commits on normal return; rolls back on exception (rethrown) or `setRollbackOnly()`.
- **`TransactionScope`** — thread-bound transaction *frame* (`ThreadLocal`), dual-mode:
  - **JDBC** (default): commit/rollback directly on the `Connection`.
  - **JTA**: delegates to the `TransactionManager`; the connection, coming from a JTA-aware `DataSource`, enlists as an XA resource.
  - Reentrancy (`required`): a participant shares the *owner*'s connection; only the *owner* finishes it.
- **`JtaTransactionManager`** — static holder (`AtomicReference<jakarta.transaction.TransactionManager>`), populated by the host's JTA bootstrap.

## Usage

There is no global holder: each module exposes its own (e.g. `StoreTransactions` in `store.domain`), populated by the backend with an instance bound to the module's `DataSource`.

```java
// Registered by the module bootstrap, bound to the module's DataSource:
StoreTransactions.BEAN.set(new TransactionServiceImpl(() -> moduleDataSource));

// In the use case (service layer / handler / presenter):
StoreTransactions.BEAN.get().required(tx -> {
    repositoryA.insert(x);
    repositoryB.update(y);   // commits together, or rolls everything back
});
```

Repositories stay the same: the `DSLContext` (jOOQ) is built on top of the `TransactionAwareConnectionProvider` (in `framework.jooq`), which hands out the current `TransactionScope`'s connection when a transaction is active.

## JTA Mode (XA)

Enabled by the host via `application.toml` (`database.transaction = "jta"`). The host initializes the Narayana `TransactionManager` and configures the pool (Agroal) with XA integration; this module only consumes the standard interfaces. With a single resource (only the database), the commit is 1PC; with multiple XA resources, 2PC.
