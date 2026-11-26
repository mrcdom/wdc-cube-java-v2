# 0007. Resource calls are named by HTTP-like verbs; writes open the transaction

- **Date:** 2026-11-23 (calls replace the presentation services); 2026-11-24 (HTTP-like verbs, bases in the framework)
- **Status:** Accepted

## Context

Presenters reached the data through presentation services (`ProductService`, `PurchasesPanelService`, `ReceiptService`, `LoginService`): classes with several methods, some used only by tests, each method opening a transaction or not as its author remembered. The first replacement, one `XxxCall` class per operation over a `ShoppingCall` base, gave each access a name and a test, but the generic `Call` suffix said nothing about what a call guarantees — whether it changes data, whether it may be repeated after a failure, whether it runs in a transaction.

## Decision

Each action a presenter needs on a resource is a small class in the `calls/` package of its module, with the request gathered by fluent `withXxx(...)` setters and the answer from `run()`, a new instance per use:

```java
var product = new ProductQuery()
        .withProductId(id)
        .run();
```

The suffix is a verb in the spirit of the HTTP methods, and each verb has a base in `framework.domain.resource` that keeps its guarantees:

| Suffix | Base | Implements |
|---|---|---|
| `Query` (GET) | `ResourceQuery<R>` | `run()` — safe, no transaction of its own |
| `Create` (POST) | `ResourceCreate<R>` | `create()` |
| `Save` (PUT) | `ResourceSave<R>` | `save()` |
| `Update` (PATCH) | `ResourceUpdate<R>` | `update()` |
| `Delete` (DELETE) | `ResourceDelete<R>` | `delete()` |

The four write bases extend `ResourceWrite<R>`, whose `final run()` calls `requiredCall` on the module's `TransactionService` and hands over to the verb's method, so no write runs outside a transaction. Its constructor is package-private, which keeps the list of write verbs closed. A write call names its module's transactions as a `Supplier`: `super(StoreTransactions.BEAN::get)`.

What fits no verb gets one of its own and extends `ResourceCall<R>` directly: `CredentialsCheck` (the login). There is no generic `XxxCall`. A call reaches its repositories through their `BEAN`; its answer is an existing type, a struct in `structs/`, or an inner class of the call (`CredentialsCheck.Result`).

## Consequences

- The name says whether a call may be retried: a `Query` may; a `Create` never may on its own (the checkout is `PurchaseCreate`).
- The same call runs in every host: with the jOOQ repositories and `TransactionServiceImpl` on the server, with the HTTP repositories and `RestTransactionService` in teavm.web ([0010](0010-client-driven-remote-transactions.md)).
- A new kind of write needs a change in the framework, by design.

## Alternatives considered

- **Presentation services.** Replaced on 2026-11-23.
- **A generic `XxxCall` with a `ShoppingCall` base.** Replaced on 2026-11-24 by the verb bases.

See [data-layer.md → Resource Calls](../data-layer.md#resource-calls--actions-on-a-resource-as-objects) and the [presentation README](../../fontes/br.com.wdc.store/br.com.wdc.store.presentation/README.md#calls).
