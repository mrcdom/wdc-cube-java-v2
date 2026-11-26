# 0006. Access control lives at the REST boundary, as a data policy per entity

- **Date:** 2026-05-31 (no secured repositories); 2026-11-24 (data policies)
- **Status:** Accepted

## Context

A repository is reached from two kinds of places. **Over REST**, from teavm.web (whose presenters run in the browser) or any HTTP client: the user controls that code and can send any criteria, any projection, any entity. **Inside the server**, from the presenters of the remote host, Vaadin and local SWT: there the user only fires actions; the presenter builds every query.

Every repository used to be wrapped in a `SecuredXxxRepository` that checked permissions and the user's scope on every call, wherever it came from, through a `SecurityContext` proxy installed in the application. Inside the server that check guarded nothing the user could reach. After it moved to the REST controllers, the controllers checked `entity:operation` permissions and hand-written scope restrictions, which said who may call an endpoint, but not which rows or fields of the answer a role may see — and missed cases (an item insert without an ownership check, a password readable through `purchase.user`).

## Decision

**Authorization lives only at the REST boundary**, and there it is a **data policy per entity**: `DataPolicy`, `DataGrant`, `RowScope`, `Mask`, `EntityDescriptor` and `DataAccess` in `framework.domain.security.policy`; the store's `UserDataPolicy`, `ProductDataPolicy`, `PurchaseDataPolicy` and `PurchaseItemDataPolicy` in `persistence.rest.security`. Per role, a grant states the rows reached, the fields read as they are or masked, the fields written, and whether it deletes. Roles combine allow-wins, decided row by row; a relation goes through the related entity's policy. The controllers call one `DataAccess` method per operation and nothing else; `Role` is only the list of roles.

Rules that follow:

- a forbidden field in a projection, filter, ordering or write is a **403 naming the fields**, never trimmed silently;
- no grant reads a password;
- alternative groups in criteria do not travel ([0012](0012-criteria-are-fields-of-criterion.md)).

There are **no secured repositories**. Where presenters run on the server, they use the repositories directly (`XxxRepository.BEAN.get()`) and must restrict every query to the user in session; an id that comes from a client event is untrusted (`ReceiptQuery` looks a purchase up by id **and** buyer).

## Consequences

- One place per entity answers "who sees what", and a new entity needs a policy before it is exposed.
- Server-side code can read more than any user may see; each query that takes an id from an event must be reviewed. This is the accepted price of removing the secured layer.
- Without an `AuthenticationService` (local and test modes) `accessFor(null)` lets everything through.

## Alternatives considered

- **Secured repository decorators** (`SecuredXxxRepository`). Replaced on 2026-05-31: they guarded nothing reachable inside the server.
- **Permission checks per controller** (`entity:operation`). Replaced on 2026-11-24: they cannot express rows and fields.

See [security-architecture.md](../security-architecture.md#authorization-at-the-rest-api--data-access-policies) and [its section on server-side presenters](../security-architecture.md#authorization-where-the-presenters-run-on-the-server).
