# 0010. Client-driven remote transactions over REST

- **Date:** 2026-06-19 (begin/commit/rollback over REST); 2026-06-23 (atomicity guard and lifecycle hardening)
- **Status:** Accepted; the owner rule is superseded by [0031](0031-remote-transaction-owned-by-user-and-client.md)

## Context

In teavm.web the presenters run in the browser and reach the data through the HTTP repositories. A purchase there is several REST calls — the purchase, then its items — and each call is its own request with its own connection on the server, so the per-request transaction of the REST write handlers cannot make them atomic. When the TeaVM transport did not send the transaction header, the purchase was autocommitted while the client believed it had failed: an orphan purchase, with no visible error.

## Decision

The client demarcates; the server keeps the physical transaction alive across requests.

- **Client:** `RestTransactionService` (`persistence.client`) is the `TransactionService` of teavm.web. `required(...)` calls `POST /api/tx/begin`, binds the returned `txId` to the thread, sends it in the **`X-Tx-Id`** header on every call of the block (`HttpTransport.setTransactionIdSupplier`), and finishes with `/api/tx/commit` or `/api/tx/rollback`. Presenters and calls use the same API as in-process ([0009](0009-programmatic-transactions-per-module.md)).
- **Server:** `TxApiController` delegates to the `RemoteTransactionCoordinator` SPI (`framework.persistence`, server-only), held in `RemoteTransactions.COORDINATOR` (`persistence.rest`) and filled by `BusinessContext` and `TestEnvironment`. The coordinator keeps a suspended `TransactionScope` per `txId`; `RepositoryApiRoutes.transactional(...)` resumes it for a write carrying the header and suspends it without committing.
- **Owner:** `TxApiController.currentOwnerKey` gives `user:<id>` when authenticated, `anon:<X-Client-Id>` only with security disabled; `begin` requires authentication when security is on. The owner is checked on resume, commit and rollback.
- **Atomicity guard:** a write without `X-Tx-Id` while its owner has a transaction open (`hasOpenTransactionForOwner`, O(1)) is refused with **409** (`TransactionConflictException`) instead of being autocommitted.
- **Lifecycle:** global and per-owner caps on open transactions (**429** beyond them); a reaper rolls back by idleness (60 s) or absolute lifetime (10 min); `commit`/`rollback` are idempotent, the outcome retained for 5 minutes, and `GET /api/tx/status` answers `open`, `committed`, `rolledback` or `unknown`. Concurrent use of one `txId` is refused. All of these are configurable under `[database]` `remoteTransaction.*`.

## Consequences

- The same presenter code is atomic on the server and in the browser.
- Each open remote transaction holds a pooled connection until it ends, hence the caps and the reaper.
- A client must serialize the requests of one transaction, and every transport must send `X-Tx-Id` on every call — including byte uploads and reads (fixed on 2026-11-26, commit `97d7974f`).
- Ownership is per user, so a second tab of the same user writing outside the transaction gets a 409; keying by client context is a proposal, not implemented.

See [data-layer.md → Client-Driven Remote Transactions](../data-layer.md#client-driven-remote-transactions-over-rest) and [remote-transaction-identity-and-ownership.md](../remote-transaction-identity-and-ownership.md).
