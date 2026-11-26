# 0008. Read-only and read-write repository contracts

- **Date:** 2026-11-26
- **Status:** Accepted

## Context

`Repository<E, C, K>` was the only repository contract: reads and writes together. An entity an application only reads — a report, a database view, a dashboard — would have had to implement `insert`, `update` and `delete` just to refuse them, and a caller could not tell from the type that writing was not an option. The HTTP client mirrored the same single shape in `HttpRepository`.

## Decision

The reads are a contract of their own, `ReadOnlyRepository<E, C, K>` in `framework.domain.repository`: `count`, `fetch` (with its `fetch(criteria)` and `fetch(criteria, limit)` defaults), `fetchPage`, `fetchById` and `newProjection`.

`Repository<E, C, K> extends ReadOnlyRepository<E, C, K>` and adds the writes: `insert`, `update` (with its overloads), `insertOrUpdate` and `delete`.

The HTTP side mirrors the two levels in `persistence.client`: `HttpReadOnlyRepository` implements `ReadOnlyRepository`, and `HttpRepository extends HttpReadOnlyRepository` and implements `Repository`.

An entity's repository interface extends the level it needs. The four entities of the WDC Store (`ProductRepository`, `UserRepository`, `PurchaseRepository`, `PurchaseItemRepository`) are written, so they all extend `Repository`.

## Consequences

- A read-only entity has no writes to refuse, and its type says so to every caller.
- Code that only reads can depend on `ReadOnlyRepository` even when the entity is written elsewhere, since every `Repository` is one.
- Nothing in the store uses `ReadOnlyRepository` on its own yet; it is there for adopters' read-only entities.

See [data-layer.md → Repositories as Injectable Interfaces](../data-layer.md#repositories-as-injectable-interfaces).
