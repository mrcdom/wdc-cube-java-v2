# 0017. Entities have a fluent API; presentation structs keep public fields

- **Date:** 2026-09-08
- **Status:** Accepted

## Context

The domain entities (`Product`, `User`, `Purchase`, `PurchaseItem`) were POJOs with public fields. Building one — or a projection, which in this architecture is an entity whose non-null fields mark what to load — took a sequence of assignments, one statement per field and per related object. The `XxxCriteria` classes, meanwhile, already had a fluent surface, so the two kinds of domain object were written in two styles.

## Decision

**Entities** — the classes that implement `KeyedEntity` (`framework.commons.serialization`, which exposes `key()` to the serializers' cycle detection) — have the same surface as the criteria:

- a `private` field;
- an accessor named after the field, `name()`;
- a setter `withName(...)` that returns `this`.

There are no public fields. An entity, a projection or a relation is built in one chained expression, each `.withXxx(...)` on its own line:

```java
var item = new PurchaseItem()
        .withAmount(2)
        .withPurchase(new Purchase()
                .withId(purchaseId));
```

**Presentation structs** (`presentation.**.structs`: `ProductInfo`, `CartItem`, `PurchaseInfo`, `ReceiptForm`, `ReceiptItem`, `Subject`) follow a different pattern and keep public fields. They are what scopes carry to the views, and `ScopeSerializer` writes the public fields of the POJOs it finds in a scope.

## Consequences

- Construction is one readable expression, including relations; by convention each `.withXxx(...)` goes on its own line, and the project formatter does not join those breaks (`join_wrapped_lines=false`).
- Entities and criteria read the same way, and both carry no persistence or JSON annotations.
- Two styles coexist on purpose: an entity in the domain is fluent, a struct in the presentation is a plain record of public fields. A struct is not refactored to the entity style.
- The frontends were unaffected by the change: they work with scopes and structs, not with entities.

## Alternatives considered

- **Public fields on entities.** Replaced on 2026-09-08 for the reasons above.

See [data-layer.md → Models](../data-layer.md#models).
