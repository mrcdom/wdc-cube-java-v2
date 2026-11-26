# 0009. Programmatic CMT-style transactions, dual JTA/JDBC, one TransactionService per module

- **Date:** 2026-06-17 (transaction layer, dual mode, per-module holders); 2026-11-26 (`afterCommit`)
- **Status:** Accepted

## Context

A business operation is rarely one statement: completing a purchase inserts the purchase and one row per item. With every statement in autocommit, a failure halfway left an orphan purchase without items. The first wiring also used global holders (`SqlDataSource.BEAN`, `TransactionService.BEAN`), which tied every module of the JVM to a single database.

## Decision

**The contract** is `TransactionService` in `framework.domain.transaction`, with no technology: the boundary is the work passed as a lambda, committed on normal return and rolled back on any exception. The propagation attributes mirror EJB CMT — `required`, `requiresNew`, `mandatory`, `supports`, `notSupported`, `never` — each with a value-returning `…Call` form. `TransactionContext.setRollbackOnly()` aborts without an exception.

**The engine** is `TransactionServiceImpl` and `TransactionScope` in `framework.persistence`, a thread-bound frame that works in two modes, chosen by `database.transaction` in `application.toml`: `"non-jta"` (direct JDBC, the default) or `"jta"` (Narayana, with the Agroal pool enlisted in XA). `framework.persistence` depends only on `javax.sql.DataSource` and `jakarta.transaction`; Agroal and Narayana live in the backend host.

**Repositories never demarcate.** They use the `DSLContext`, built over `TransactionAwareConnectionProvider` (`framework.jooq`): inside a transaction it hands out the scope's connection, outside it lends a standalone autocommit connection. The boundary is opened by the write use cases: `ResourceWrite.run()` ([0007](0007-resource-calls-named-by-http-verbs.md)) and the REST write handlers (`RepositoryApiRoutes.transactional(...)`).

**One `TransactionService` per module, no global holder.** Each module exposes its own holders — `StoreTransactions` (domain) and `StoreDSLContext` (`persistence.impl`) — filled by the composition root ([0013](0013-dependency-injection-without-a-framework.md)); `TransactionServiceImpl` receives the module's `Supplier<DataSource>`. The JTA `TransactionManager` stays one per JVM, as the coordinator.

**After the commit.** `TransactionService.afterCommit(task)` runs a task after the commit of the current physical transaction, outside it, and discards it on rollback; without a transaction it runs at once. It works the same in `TransactionServiceImpl`, the remote coordinator and `RestTransactionService` (shared queue: `AfterCommitTasks`). The store uses it for the product image cache: `ProductRepositoryImpl` keeps images read outside a transaction in `ProductImageCache` and drops one in an `afterCommit` of the write that changed it, so a read in between cannot put the old image back.

## Consequences

- Wrapping a use case in `required(...)` makes every query inside it atomic, in either mode, with no change to the repositories.
- Switching between JDBC and JTA is configuration.
- Two modules can use one database or two; that is the composition root's decision.
- The scope is thread-bound: work handed to another thread does not join the transaction.

## Alternatives considered

- **Global `SqlDataSource.BEAN` and `TransactionService.BEAN`.** Replaced on 2026-06-17 by the per-module holders.

See [data-layer.md → Transactions](../data-layer.md#transactions--atomicity-and-dual-jtajdbc-mode) and [After the commit](../data-layer.md#after-the-commit--aftercommit).
