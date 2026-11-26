# 0020. The price of an item is always its product's, for every role

- **Date:** 2026-11-26
- **Status:** Accepted; supersedes [0015](0015-the-server-prices-items.md)

## Context

[0015](0015-the-server-prices-items.md) made the server decide the price of a new item, but only for an item that carried none: an item sent with a price kept it. Only the ADMIN could send one over REST, and an update wrote whatever price it was given. The catalog was the source of truth for customers and a suggestion for everyone else, so two sources of truth for the same value remained, and a check that depended on the role.

## Decision

`PurchaseItemRepositoryImpl` decides the price of every item from its product, on every path:

- **Insert** (a loose item, or the items of a new purchase, through `insertRow`): the price is the product's, read by a subquery in the same `INSERT`; a price the item carries is not used. The recorded price comes back through `RETURNING`.
- **Update**: one that touches the price or the product recomputes the price from the product the row ends up with; the value sent is not written.

`PurchaseItemDataPolicy` still refuses a price from a CUSTOMER with 403, naming the field ([0006](0006-access-control-at-the-rest-boundary.md)); the ADMIN's grant allows the field, and its value is replaced.

The seed's purchases are history, paid at prices the catalog no longer has: the database reset writes those prices directly, as data, after inserting the purchases through the repository.

## Consequences

- What anyone pays is what the catalog says when the row is written, whatever client or role sent it.
- A price changed between showing the cart and buying is charged at the new value; the receipt shows what was charged.
- Code that needs to record another price (an import of past orders, like the seed) writes it below the repository, knowingly.

## Alternatives considered

- **Keep the ADMIN's price** ([0015](0015-the-server-prices-items.md)): it lets an administrator correct a price, but at the cost of a second source of truth, decided by role.
