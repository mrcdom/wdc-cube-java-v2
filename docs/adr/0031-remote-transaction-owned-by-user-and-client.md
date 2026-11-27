# 0031. A remote transaction belongs to the user's client context, not to the user alone

- **Date:** 2026-11-27
- **Status:** Accepted (supersedes the owner rule of [0010](0010-client-driven-remote-transactions.md))

## Context

A remote transaction of the REST API had the user as its owner (`user:<id>`, [0010](0010-client-driven-remote-transactions.md)). The owner decides two things: who may resume, commit or roll back the transaction, and which writes the atomicity guard refuses — a write without `X-Tx-Id` while its owner has a transaction open gets 409.

With the user as the owner, the guard confused the user with the workflow. While one tab of a user had a checkout open, a single write from another tab of the same user was refused. Every client already sends `X-Client-Id`, an id of its own context: a browser tab, an app instance.

## Decision

**The owner is the client context, within the user's namespace:**

| Requester | Owner |
|---|---|
| Authenticated, with `X-Client-Id` | `user:<id>:<X-Client-Id>` |
| Authenticated, without the header | `user:<id>` |
| Security disabled, with `X-Client-Id` | `anon:<X-Client-Id>` |
| Security disabled, without the header | none: each write is its own transaction |

- The guard and the owner check become per client: another tab of the same user writes on its own, and cannot finish the first tab's transaction.
- The user stays in the key, so another user's transaction stays out of reach even with its `txId` and its client id. What each request may do is still decided by the security, per request.
- **teavm.web** makes its client id with `crypto.getRandomValues`, which works outside a secure context (plain HTTP away from localhost), unlike `crypto.randomUUID`. It keeps the id in the tab's `sessionStorage`, so a reload keeps it and another tab has its own.

## Consequences

- Two tabs of one user each own their transactions; the 409 of the guard concerns only the tab with the transaction open.
- The per-owner cap on open transactions counts per client: a user with several tabs can open the cap in each, up to the global cap.
- A tab the browser duplicates, or a window the page itself opens (`window.open`), starts with a copy of the `sessionStorage`, so it shares the client id of the original; a tab opened from the address bar, or a link with `noopener`, gets its own. Two such tabs share their transactions' ownership, as two tabs of one user did before.
- A multi-threaded client that opens concurrent transactions from one transport still shares one client id across its threads.

## Alternatives considered

- **The client id alone, without the user:** the same granularity, but a client id and a `txId` from another user would reach that user's transaction; the security would still decide each write.
- **Keeping the user as the owner:** no change, and the two-tab conflict stays.
