# 0015. The server decides the price of an item

- **Date:** 2026-11-24
- **Status:** Accepted

## Context

At checkout, `PurchaseCreate` copied each cart item's price into the new `PurchaseItem`, and the repository stored what it was given. In teavm.web the presenters run in the browser and the items reach the server through the REST API, so the price recorded was whatever the client sent: a modified client could buy at any price. Even with an honest client, the cart's price is what the screen showed when the product was added, not necessarily the product's price when the purchase is recorded.

## Decision

The price of a new item is decided on the server, from its product, in every host:

- `PurchaseItemRepositoryImpl.insertRow` — shared by the insert of a single item and the insert of a purchase with its items — writes an item that carries no price with its product's price, read by a subquery in the **same `INSERT`**; the recorded price comes back through `RETURNING` and is set on the item. It is the price at the moment the row is written.
- `PurchaseCreate` sends no price: only the product and the quantity of each item.
- `PurchaseItemDataPolicy` does not let a CUSTOMER write the price: the customer's create grant covers the amount, the purchase and the product only, so an item sent with a price over REST is a 403 ([0006](0006-access-control-at-the-rest-boundary.md)).

An item that does carry a price keeps it: only an ADMIN can send one over REST, and server-side code that sets one is trusted.

## Consequences

- A customer cannot choose what it pays, whatever client it uses.
- The receipt shows the price actually recorded, which may differ from the one the cart showed if the product's price changed in between.
- Reading the price inside the `INSERT` leaves no window between reading and writing it.

See [security-architecture.md → Rules](../security-architecture.md#rules).
