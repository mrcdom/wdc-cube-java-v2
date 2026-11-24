# Remote Transaction — Client Identity and Transaction Ownership

> A **design** document (part implemented, part proposed evolution). It complements the
> [Client-Driven Remote Transactions](data-layer.md#client-driven-remote-transactions-over-rest)
> section of the data layer, going deeper into **who owns a remote transaction** and how that owner is
> identified uniquely and reliably.

## Table of Contents

- [Context — the incident that started the discussion](#context--the-incident-that-started-the-discussion)
- [Current state (implemented)](#current-state-implemented)
- [Proposed evolution — ownership by context, not by user](#proposed-evolution--ownership-by-context-not-by-user)
- [Generalization — what is the unit of ownership](#generalization--what-is-the-unit-of-ownership)
- [Identifier uniqueness](#identifier-uniqueness)
- [Server-issued identifiers](#server-issued-identifiers)
- [Trust model — forged identifiers](#trust-model--forged-identifiers)
- [Summary of decisions and status](#summary-of-decisions-and-status)

---

## Context — the incident that started the discussion

The client-driven remote transaction makes **several REST calls** atomic: the client opens the
transaction (`begin` → `txId`), propagates the `txId` in the **`X-Tx-Id`** header on the following writes
(which join the same physical transaction on the server) and finishes with `commit`/`rollback`.

The **TeaVM** frontend (`FetchHttpTransport`) did not implement `setTransactionIdSupplier` and did not
send the `X-Tx-Id` header. The result, during a checkout:

```
begin             ──────────▶ tx opened on the server (txId)       ✅ (begin requires no header)
insert(purchase)  [no header] ─▶ does NOT join the tx → AUTOCOMMIT ⚠️ writes an orphan
commit            [no header] ─▶ 403 "Missing X-Tx-Id header"      ❌
rollback          [no header] ─▶ 403                               ❌
```

The purchase was **not** confirmed from the client's point of view, but the row had already been
autocommitted — an **orphan purchase** in the database. A silent atomicity break: the worst kind, with
no error visible to the user.

The fix (commit `9bca9322`) had two fronts:

1. **Propagation** — `FetchHttpTransport`/`OkHttpTransport` attach `X-Tx-Id` to every request
   (mirroring what the JVM transport does).
2. **Server-side defense** — an **atomicity guard**: if a write arrives **without** `X-Tx-Id` but the
   requester **has an open remote transaction**, the server **rejects** it (instead of autocommitting an
   orphan). It is the safety net against a future propagation failure.

The guard needs to know "**does this owner have an open transaction?**" — and that is where the central
question of this document comes in: **how the owner of a transaction is identified.**

---

## Current state (implemented)

The owner (`ownerKey`) is derived in `TxApiController.currentOwnerKey(Context)`:

| Situation | `ownerKey` |
|---|---|
| Authenticated | `user:<userId>` |
| No login (security disabled) | `anon:<X-Client-Id>` |

- **`X-Client-Id`** is a client session identifier generated **per app instance** in the transports
  (`OkHttpTransport`: `UUID.randomUUID()` per instance; `FetchHttpTransport`/TeaVM: a static
  `crypto.randomUUID()` per page load). It is sent on every request.
- The separate `user:`/`anon:` namespaces keep an anonymous client from colliding with (or impersonating)
  a real user; in authenticated mode `X-Client-Id` is **ignored** (not spoofable).
- The guard uses `RemoteTransactionCoordinator.hasOpenTransactionForOwner(ownerKey)`, implemented in
  **O(1)** with an `owner → open transaction count` index, maintained in `begin`/`finish`/`reap`.

This state closes the atomicity bug. On top of it, an adversarial review hardened the transaction
**lifecycle** (caps, absolute lifetime, idempotency, status, 409, metrics) — see
[Operational robustness](#operational-robustness-implemented), **also implemented and configurable via
`application.toml`**. The intermediate sections (ownership by context, uniqueness, server-issued
identifiers, trust model) are a **proposed evolution**.

---

## Proposed evolution — ownership by context, not by user

### The residual problem

In authenticated mode the guard's granularity is **per user**. That carries a false positive:

```
Tab A (John)  begin ─▶ tx opened (owner user:John)
Tab B (John)  insert(profile) [no X-Tx-Id] ─▶ guard sees "user:John has an open tx" → REJECTS ❌
```

Tab B's **legitimate** single-shot write is blocked because the guard confuses **user** with
**workflow**. Rare in the current model (single-threaded UI, sequential checkout), but it is a contract
trap.

### The proposal

Key the transaction **always by client context** (the tab), **not** by identity:

> The `ownerKey` stops being a security identity and becomes a **pure correlation label**.

The key to the simplification is that **authorization** and **transaction ownership** are **orthogonal**:

- **Authorization** ("may this request perform this operation?") → `SecurityFilter` + `SecurityContext`,
  per request, independent. It applies whether authenticated or not — without login, the user can only
  do what the security context allows.
- **Transaction ownership** ("which logical transaction does this request belong to?") → the context id
  (the tab).

With this, the guard automatically becomes **per tab**, and the two-tab false positive disappears.
`verifyOwner` remains valid, comparing tab ids (one tab cannot finish another tab's transaction).

### Concrete change (small)

The infrastructure for sending the id **is already committed**; what is essentially missing is the
derivation on the server:

```java
// current: user:<id> if authenticated, anon:<clientId> otherwise
// proposal: always the tab id (pure correlation), regardless of auth
var tabId = ctx.header(CLIENT_HEADER);
return (tabId != null && !tabId.isBlank()) ? tabId : null;
```

On the client side, **in the browser it is already practically per tab**: each tab loads its own JS
*realm* / TeaVM instance, so the static id is already independent per tab. Optional adjustments:

- read the id from `sessionStorage` (which is **per tab**, unlike `localStorage`) so it survives a
  *reload* within the same tab;
- rename `X-Client-Id` → `X-Tab-Id`/`X-Session-Id` (cosmetic; makes the semantics explicit).

---

## Generalization — what is the unit of ownership

"Per tab" is just the common case of a more general principle:

> The unit of ownership is **a single-threaded execution context that owns its transactions**.

| Context | How it gets the id | Notes |
|---|---|---|
| **Server-side worker** (inside the backend) | — | **Does not use** the remote tx: it uses `StoreTransactions.BEAN.required(...)` (local JTA/JDBC transaction). The remote machinery is only for clients over REST. |
| **Browser tab** | local per-tab id | Single-threaded; its own *realm*. |
| **Web Worker (browser)** | `self.crypto.randomUUID()` in memory | Single-threaded, separate *realm* → equivalent to a tab. Has **no** `sessionStorage`/`localStorage` (Web Storage is not exposed to Workers); it keeps the id in memory. XHR/`fetch` work in *dedicated workers*. |
| **Independent JVM process** (CLI, importer) | UUID per transport instance | Isolated by construction. |
| **Multi-threaded JVM worker** (concurrent txs) | id **per thread** | A per-process id is too coarse for the guard (false positive across threads). See below. |

### The multi-threaded case

A worker with a *thread pool* opening concurrent transactions has the same false positive as the "two
tabs", this time **across threads of the same process** (and often of the same user, where `userId`
namespacing disambiguates nothing). Two points:

- The **happy path works** with a per-process id: if each thread uses `required()`, every write
  carries its `X-Tx-Id` → *resume* path, the guard is not even consulted. The problem only shows up when
  **mixing** a concurrent tx with a concurrent header-less single-shot write.
- For full correctness, the id must be **per thread/tx scope**, aligned with the `ThreadLocal CURRENT`
  that `RestTransactionService` uses for the transaction state.

---

## Identifier uniqueness

### The id space is not the problem

UUIDv4 = **122** random **bits**. By the birthday bound, **~26 trillion** simultaneously active ids would
be needed for a ~1 in 1 billion chance of collision. For any realistic load, collision is irrelevant —
**as long as the RNG is strong**. The risk is at the edges:

1. **Weak RNG in the fallback.** TeaVM's `generateClientId` falls back to `Math.random()` if
   `crypto.randomUUID` does not exist — and `crypto.randomUUID()` **requires a secure context**; in a
   **plain HTTP** deployment (non-localhost) it would fall back to the weak `Math.random()`.
   **Fix:** use `crypto.getRandomValues()` (which does **not** require a secure context and works in
   Workers) to build the UUID, dropping to `Math.random()` only if `crypto` does not exist.
2. **Cloning a live process** (fork / container cloned after generating the id) → two processes with the
   same id. Rule: generate **lazily at runtime**, never bake it into the build.
3. **The server blindly trusts the client's id** — with no global uniqueness check.

### Composite key

The robust variant proposed dropping the `user:` prefix, but that prefix is a **collision isolation**.
You can have both — **compose** them:

```
ownerKey = "<userId>:<tabId>"   (authenticated)
ownerKey = "anon:<tabId>"       (no login)
```

- **Per-tab granularity:** the `tabId` distinguishes the same user's two tabs → solves the false positive.
- **Collision isolation:** even if two users drew the same `tabId`, the `userId` keeps them distinct —
  the *blast radius* of a collision stays confined to **a single user**.
- **Unguessability:** an attacker would need the `userId` (perhaps enumerable) **and** the `tabId` (the
  random secret).

> **Two distinct properties, do not confuse them:** *uniqueness* (two active contexts do not share an
> id → correctness of the correlation/guard) and *unguessability* (another client cannot predict your
> id → defense in depth of the transaction boundary). A strong UUIDv4 covers both; `Math.random` weakens
> both.

### Two-tier strategy

The collision risk **scales with the number of ids** and **worsens with weak namespacing** — so we treat
the two axes differently:

- **Tier-1 (app/tab):** few ids per backend, anchored on the `userId`. **Local generation**, no trip to
  the server. Probabilistic, sufficient.
- **Tier-2 (multi-threaded JVM worker):** many ids, often from the same user. Here the server's
  authority pays off → **server-issued id** (next section).

---

## Server-issued identifiers

For tier-2, the id comes from the server — **but it is only worth it if it is authoritative**. A
server-side `UUID.randomUUID()` **buys nothing** over local generation (same probability). The source
has to be **monotonic/authoritative**.

### Endpoint sketch

```
POST /api/client/id      (requires authentication, same filter as /api/repo/*)
Request:  {}
Response: {"clientId": "<authoritative id>"}
```

```java
public final class ClientIdApiController {

    // LOW-cardinality random axis: one per process (prefer a 'node.id' config if there is one).
    private static final String NODE_ID = resolveNodeId();
    // HIGH-cardinality deterministic axis: unique within the process.
    private static final AtomicLong SEQ = new AtomicLong();

    public static void configure(JavalinConfig config, String prefix) {
        config.routes.post(prefix + "/api/client/id", ClientIdApiController::issue);
    }

    private static void issue(Context ctx) {
        SecurityEnforcer.requireAuthenticated();
        ctx.json(Map.of("clientId", NODE_ID + "-" + SEQ.incrementAndGet()));
    }
}
```

The central idea: **move the probabilistic assumption from the high-cardinality axis (threads) to the
low-cardinality one (processes)**. `NODE_ID` is the only random piece (one per backend process — a
handful of them); the counter is deterministic.

- **Unique within the process:** `AtomicLong` → impossible to repeat.
- **Unique across processes/restarts:** a different `NODE_ID` per process → `nodeId-counter` never
  collides with another process, even if the counters restart from zero.
- **Zero bookkeeping:** there is no registry of issued ids to clean up — non-reuse is structural.

**Alternative (strict global authority):** `SEQ` becomes a **database sequence**, unique across nodes
without depending on `NODE_ID`, at the cost of one database hit per issuance.

### Backend restart

On restart, the `AtomicLong SEQ` **goes back to zero** — so uniqueness depends on the **prefix changing
on every boot**:

- **`NODE_ID` = UUID per process** (primary path): a new prefix on every boot → `nodeId-0, …` never
  collide with the previous process's ids, even with the counter reset. **Restart-safe for free.**
- **`NODE_ID` = configured `node.id`** (for a stable identity in a cluster): the **same** prefix after
  the restart + a reset counter → **it would reissue ids already handed out**. ⚠️ To keep `node.id`
  stable, an additional **per-boot component** is needed: `clientId = <nodeId>-<bootId>-<seq>`, where
  `bootId` is generated on every start (a short UUID, boot timestamp, or persisted boot counter). The
  *database sequence* also solves it on its own (the authority survives the restart).

> **Rule:** uniqueness across restarts requires a **per-boot** component in the id. The UUID-per-process
> already is that component; a stable `node.id` needs an extra `bootId`.

**Signing secret:** if the id is signed (HMAC) or becomes a JWT claim, the `SERVER_SECRET` **cannot** be
generated per process — otherwise every id/token issued before the restart fails verification (mass
rejection). It has to be **stable** (config / secret store), like any JWT key.

**In-flight transactions:** the coordinator's registry is in memory — a restart **loses all** open
transactions. The client gets "unknown/expired tx" on the next `commit`; it is the **same failure mode as
the *reaper*** (idle timeout), and the remote tx API client contract must already account for "my tx is
gone → I redo the block". A restart is the "all at once" version of that. It is not recoverable in
practice (the connection/locks are gone).

### Client integration — `ThreadLocal`

```java
private final String appClientId = UUID.randomUUID().toString();      // tier-1, per instance
private static final ThreadLocal<String> THREAD_CLIENT_ID = new ThreadLocal<>();  // tier-2

/** Called ONCE at the startup of a worker thread that owns its own transactions. */
public void bindServerIssuedClientId() {
    THREAD_CLIENT_ID.set(parse(transport.postJson("/api/client/id", "{}")));
}

private String currentClientId() {              // value sent in X-Client-Id
    var t = THREAD_CLIENT_ID.get();
    return t != null ? t : appClientId;         // default: the app id, no round-trip
}
```

The common path (UI/tab, single-threaded process) never calls `bindServerIssuedClientId` → it uses the
app id, **zero trips to the server**. Each worker makes **one** trip at its startup, amortized over all
the transactions it runs.

### Cluster

- **Uniqueness across nodes:** guaranteed by the `NODE_ID` in the prefix (or by the database sequence).
- **Routing:** a remote transaction lives (open connection) **on the node that served the `begin`** —
  `commit`/writes must land on the same node → a cluster deployment already requires **session
  affinity** (sticky) by `txId`. This is a **pre-existing** *constraint* of the remote tx, not one
  introduced by the issuance endpoint. The actual affinity is by `txId`/session, not by `clientId`.

---

## Trust model — forged identifiers

> **Question:** `X-Client-Id` is self-asserted by the client. Can a third party forge an id? Can it be
> detected and discarded?

> This analysis assumes the **proposed composite key** (`userId:clientId`). In the **current** state, the
> authenticated owner is `user:<userId>` and `X-Client-Id` is **ignored** when there is a login — so
> forging it has no effect at all in authenticated mode (the anonymous case only exists with security
> entirely disabled).

### In the stateless design, there is no structural detection — and none is needed

The server does not keep "valid ids", so it cannot tell a legitimate id from an invented one. What
protects **is not the id — it is the token**:

- The key is **composite and bound to the token**: `userId:clientId`, where the `userId` comes from the
  `SecurityContext` (from the JWT), **not** from what the client says. A forgery stays namespaced under
  the **attacker's own `userId`**; they cannot produce someone else's `userId` without that person's
  token.
- **Cross-user hijacking is blocked by the token, not by the id.** To finish the victim's tx, the
  attacker would need the `txId` (handed only to whoever opened it) **and** to match
  `victimUserId:victimClientId` — and the `userId` comes from the victim's token.
- Within the **same** user, forging an id crosses no boundary at all (it is the same principal).

### The guard is for a **cooperative** client; it is not a security control

A malicious client can **evade the guard by switching `clientId` on every request** (opens under X,
writes header-less under Y), or simply **not use a transaction**. In both cases it only corrupts its
**own** transaction; the writes are still individually authorized.

> You cannot *force* atomicity on a hostile client through headers. Atomicity guaranteed independently
> of the client has to live **on the server** — as checkout does with `transactional()` /
> `CartManager.doPurchase`, which wrap the transaction server-side.

### If you DO want real detection

Accepting **only** server-issued ids requires making the id **verifiable** — and the project's stack
already has the tool (HMAC + JWT). The id becomes a **signed token**:

```java
// issuance (server)
String payload  = userId + ":" + NODE_ID + "-" + SEQ.incrementAndGet();
String clientId = payload + "." + base64(hmacSha256(SERVER_SECRET, payload));

// per-request verification (stateless)
var i = clientId.lastIndexOf('.');
var payload = clientId.substring(0, i);
if (!constantTimeEquals(clientId.substring(i + 1), base64(hmac(SERVER_SECRET, payload))))
    throw new AccessDeniedException("Forged X-Client-Id");           // not issued by the server
if (!payload.startsWith(securityContext.userId() + ":"))
    throw new AccessDeniedException("X-Client-Id of another owner"); // signed, but not yours
```

- **Stateless:** verifies by recomputing the MAC, with no registry. It detects any fabricated id
  (without the secret there is no valid MAC) and any cross-user reuse (the embedded `userId` does not
  match the token).
- **Even cleaner delivery:** bake that claim **into the JWT at login** → **free** verification (the
  server already validates the JWT on every request) and **no extra round-trip** (tier-1); signed
  issuance via the endpoint for tier-2. Cost: the local no-trip path is lost (unless it comes in the
  JWT) + secret management.

> **Key distinction:** the **counter** gives **uniqueness**; only **signature/registry** gives
> **authenticity**. They are different requirements — choose according to the threat model.

---

## Operational robustness (implemented)

An adversarial review of the design exposed holes in the transaction's **lifecycle and operation** (not
in identity). They are resolved in the coordinator (`RemoteTransactionCoordinatorImpl`) and in the
`transactional` decorator:

| # | Hole | Resolution |
|---|---|---|
| 1 | No cap on open transactions → pool exhaustion / DoS | **Global** (`maxOpen`) and **per-owner** (`maxOpenPerOwner`) cap at `begin`; overflow → `TransactionLimitExceededException` → **429**. A *soft* bound (non-atomic check, acceptable). |
| 2 | Idle-only reaper → a client that "pings" holds a connection forever | **Absolute** lifetime (`maxLifetimeMs`, 10 min) in addition to idle. |
| 3 | No observability | Counters (`begun`/`committed`/`rolledBack`/`reaped`/`rejectedByLimit`) + gauges via `stats()` → `RemoteTransactionStats`; logs on events (reaper, rejection). No new dependency. |
| 4 | Non-idempotent `commit`/`rollback` → ambiguous outcome if the response is lost | Outcome remembered for a retention window (5 min): a retry with the same outcome is a **successful no-op**; the opposite → **409**. The reaper records `rolledback` (disambiguates). **`GET /api/tx/status`** → `open`/`committed`/`rolledback`/`unknown`. |
| 5 | Guard returned 403 (authorization semantics) | **409 Conflict** via `TransactionConflictException` — a state conflict, not an access denial. |

> **Configurable:** idle timeout, absolute lifetime, outcome retention and the caps are read from
> `application.toml` under `[database]` (`remoteTransaction.*`, durations in seconds) by
> `RemoteTransactionOptions.fromConfig`, falling back to the public defaults of
> `RemoteTransactionCoordinatorImpl` (`DEFAULT_*`) when absent.

### Client contract — serialization within a transaction (#7)

The coordinator **rejects concurrent use of the same `txId`** (`resume` does a CAS on `inUse` →
"remote transaction in concurrent use"). Therefore, **the requests of a single transaction must be
serialized** by the client — no pipelining/parallelism inside a `required` block. This is naturally
satisfied by the "one thread per action" model (see [re-entrancy](#scope-re-entrancy-in-the-asynchronous-client-6)):
the block runs sequentially on a single thread.

### Anonymous transaction policy (#8)

**Decision:** a remote transaction **requires authentication** — `TxApiController.begin` calls
`SecurityEnforcer.requireAuthenticated()`. The `anon:<clientId>` namespace exists **only** for
deployments with the **security layer entirely disabled** (tests/local), not for "an anonymous user on a
secured server". On a server with security enabled, there is no transaction without an authenticated
owner.

### Scope re-entrancy in the asynchronous client (#6)

**Investigated — not a bug.** In the browser (TeaVM, single-threaded event loop) the isolation of the
`ThreadLocal CURRENT` holds because **(1)** each action runs on its own green thread
(`safeAction` → `new Thread().start()`) and **(2)** TeaVM's `ThreadLocal` is **per thread** (`TThreadLocal`
bytecode: non-main threads use a `map` keyed by `currentThread().key`). A concurrent action during a
suspended `required` reads its own `CURRENT` (empty) and does **not** inherit the `txId`. The invariant
is documented in `RestTransactionService` (a refactor that runs actions inline or reuses pool threads
would break the isolation).

---

## Summary of decisions and status

| Item | Status |
|---|---|
| `X-Tx-Id` propagation in TeaVM + atomicity guard + O(1) index | ✅ Implemented (commit `9bca9322`) |
| `user:`/`anon:` owner namespace + sending `X-Client-Id` | ✅ Implemented |
| Cap on open txs (global + per owner) → 429 | ✅ Implemented |
| Absolute lifetime in addition to idle | ✅ Implemented |
| Idempotent `commit`/`rollback` + `GET /api/tx/status` | ✅ Implemented |
| Guard returns 409 (not 403) | ✅ Implemented |
| Observability (`stats()` + logs) | ✅ Implemented |
| Parameters (timeouts, retention, caps) configurable via `application.toml` | ✅ Implemented |
| `ThreadLocal` re-entrancy in the asynchronous client | ✅ Investigated — not a bug (documented invariant) |
| Serialization contract within the tx / anonymous begin policy | ✅ Documented |
| Per-tab ownership (pure correlation, orthogonal security) | 🔭 Proposed |
| Composite key `userId:tabId` | 🔭 Proposed (reconciles granularity + uniqueness) |
| Harden the `generateClientId` fallback (`crypto.getRandomValues`) | 🔭 Proposed (isolated improvement, worth doing now) |
| Per-thread id (multi-threaded worker) via `ThreadLocal` | 🔭 Proposed |
| `POST /api/client/id` endpoint with a monotonic source (tier-2) | 🔭 Proposed |
| Signed id (HMAC/JWT) to detect forgery | 🔭 Optional, only if "accept only issued ids" is a requirement |

**Principles guiding the design:**

- Authorization (token) and transaction ownership (context id) are **orthogonal** — do not overload one
  with the other's role.
- The unit of ownership is a **single-threaded context**; the tab is just the common case in the browser.
- Uniqueness: make **the axis that scales deterministic** (threads via a counter) and leave the
  randomness on **the axis that does not scale** (processes).
- The guard is **correctness for a cooperative client**; atomicity against a hostile client is
  server-side.
