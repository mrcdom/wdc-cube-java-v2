# Bridge/Skeleton Protocol Specification — Remote Presentation (Host/Shell)

> **Status:** normative. This is the source of truth for the protocol used by the *thin* clients (web browser, desktop applications, and mobile applications) that remotely access the presentation layer on the server.
>
> **Purpose of this document:** to capture every wire-level detail — identity, security, authority over scopes, page and site storage, connection lifecycle, refresh flow — so that the protocol can be faithfully reimplemented in **any** technology, on both client and server, on any platform.
>
> The specification is **independent of language and UI library**: it describes the wire contract and the observable behavior, not an implementation. Where it mentions platforms (browser, desktop, mobile), it is because the protocol was designed with the requirements of those platforms in mind. The reference implementations (host and client shells) live in this repository under the remote presentation modules; where this document diverges from them, **the implementations prevail** — report the divergence.

## Table of Contents

1. [Conceptual Model and Roles](#1-conceptual-model-and-roles)
2. [Terminology and Identifiers](#2-terminology-and-identifiers)
3. [Session Identity — the `appId`](#3-session-identity--the-appid)
4. [Server Cryptographic Material](#4-server-cryptographic-material)
5. [Entry Modes (Session Bootstrap)](#5-entry-modes-session-bootstrap)
6. [Security Handshake — the Connection Key (Protocol Version 1)](#6-security-handshake--the-connection-key-protocol-version-1)
7. [WebSocket Channel — Lifecycle](#7-websocket-channel--lifecycle)
8. [Message Format — Client → Server](#8-message-format--client--server)
9. [Message Format — Server → Client](#9-message-format--server--client)
10. [Event Codes and Close Codes](#10-event-codes-and-close-codes)
11. [Scope Serialization](#11-scope-serialization)
12. [Authority over Scopes and View Garbage Collection](#12-authority-over-scopes-and-view-garbage-collection)
13. [Navigation and URL Signing](#13-navigation-and-url-signing)
14. [Client Storage — Page and Site](#14-client-storage--page-and-site)
15. [Refresh (F5) Flow and Reconnection](#15-refresh-f5-flow-and-reconnection)
16. [Keep-Alive, Timeouts, Capacity, and Intervals](#16-keep-alive-timeouts-capacity-and-intervals)
17. [Cross-Platform Portability Notes](#17-cross-platform-portability-notes)
18. [Appendix — Literal Table of Constants](#18-appendix--literal-table-of-constants)

---

## 1. Conceptual Model and Roles

The protocol is analogous to a **Remote Desktop operating on the application's data layer** rather than on pixels. All logic — Presenters, scopes, business rules, navigation — lives in the **Host** (server). The **Shell** (client) is *thin*: it only renders serialized scopes and sends interaction events back.

| Role | Responsibility |
|---|---|
| **Host** | Holds the Presenter/Scope tree, processes events, performs navigation, serializes scope deltas, and is the **authority** over which views exist. |
| **Shell** | Establishes the session, performs the cryptographic handshake, renders scopes, emits user events, keeps local storage, reconnects after a drop. |
| **Bridge** | The code layer, on both sides, that speaks this protocol. On the client, a core reusable across UI platforms is desirable. |

**Fundamental invariants:**

- The Shell **never** contains business logic or navigation rules. It does not decide where to navigate; it **asks**, and the Host decides.
- Everything a view renders is a **scope** identified by an `instanceId` (vsid). The Host sends only the *dirty* scopes (delta).
- The Host is the **only** authority over the existence and identity of views. The Shell never invents vsids.
- A special, fixed view, the **Browser View** (vsid `7b32e816a191:0`), is the root of the shell and the channel through which navigation, alerts, and keep-alive travel. (The name "Browser" designates the root view that "browses" between places; it does not refer to a web browser.)

```mermaid
sequenceDiagram
    participant Shell
    participant WS as WebSocket /dispatcher/{appId}
    participant Host
    participant P as Presenters + Scopes

    Note over Shell,Host: bootstrap → connection → handshake
    Shell->>WS: 1st message { protocol: 1, clientKey }
    WS->>Shell: { protocol: 1, serverKey }  (both derive the connection key)
    Shell->>WS: { event:[] }  (+ storage bootstrap, ciphered)
    WS->>Host: onMessage (creates/retrieves app)
    Host->>P: initializes/retrieves session
    Host->>WS: { scopes:[ initial scopes ], uri }
    WS->>Shell: renders

    Shell->>WS: { requestId, event:["7eb485e5f843:3:1"], "7eb485e5f843:3":{formData} }
    WS->>Host: submit → syncs scope + event
    Host->>P: logic + navigation, marks dirty
    Host->>WS: { requestId, scopes:[ dirty only ], uri? , storage? }
    WS->>Shell: reconciles affected views
```

---

## 2. Terminology and Identifiers

| Term | Definition |
|---|---|
| **`appId`** | Session identifier. Signed format `part1.part2` (see §3). Goes in the WebSocket path: `/dispatcher/{appId}`. |
| **`vsid` (ViewScope ID / instanceId)** | Identity of a view instance. Format `"<classId>:<n>"`, e.g. `7eb485e5f843:3`, `7b32e816a191:0`. `n` is the instance number. |
| **`classId`** | Identity of the view's *class*: the `CLASS_ID` (a 12-digit hex string) declared by the scope class the view draws. It is `parts[0]` of the vsid. |
| **Connection key** | The AES-256 key each WebSocket connection agrees on in its handshake (§6), with which sensitive values travel. |
| **Browser View** | Singleton root view of the shell. `classId = 7b32e816a191`, fixed vsid `7b32e816a191:0`. |
| **`p.path`** | formData field carrying the route (the application's query path) in navigation events. |
| **`"#"`** | Reserved field inside each serialized scope object, whose value is that scope's vsid. |

**vsid parsing rule inside an event:** an `event` item has the format `"<vsid>:<eventCode>"`. Since the `vsid` itself contains `:`, parsing uses the **last** `:` as the separator: everything before it is the vsid, the integer after it is the `eventCode`. E.g.: `"7b32e816a191:0:-1"` → vsid `7b32e816a191:0`, eventCode `-1`.

---

## 3. Session Identity — the `appId`

The `appId` is self-verifying (it carries its own signature), so the server detects forgery without consulting any storage.

**Formation (server):**

```
part1 = base62( 32 random bytes from a cryptographic generator )
part2 = sign( part1 )
appId = part1 + "." + part2
```

Where `sign(text)` = `base62( first 16 bytes of HMAC-SHA256( signingKey, utf8_bytes(text) ) )` (see §4).

**Validation (server, in the WebSocket `onConnect`):** the path's `appId` must be non-empty, have exactly one `.`, and `part2` must equal `sign(part1)`, compared in constant time. Otherwise → close with code `4001 reload_required`.

Without the server's signing key, it is impossible to produce a valid `part2` for a chosen `part1`.

---

## 4. Server Cryptographic Material

The server keeps **one** secret: the **signing key**, at least 32 random bytes, used with HMAC-SHA256 to sign the `appId` (§3) and the navigation addresses (§13).

- It comes from the configuration (`[remote] signingKey`, Base64 URL-safe). When it is absent, the server generates one on its first start and writes it into the local configuration, so that a restart keeps accepting the ids and addresses signed before.
- Instances that must accept each other's sessions share the same key.
- The server keeps **no** long-lived key for ciphering: each WebSocket connection agrees on its own key (§6).

---

## 5. Entry Modes (Session Bootstrap)

There are **two modes** for obtaining the `appId`. Both converge on the same WebSocket protocol. A client must support at least one of them.

### 5.1 Browser Mode (Cookie Planted by the Page)

For clients that load an HTML page served by the Host. Before serving `/{ctx}/index.html`, the server (`before` handler) plants one cookie:

| Cookie | Value | MaxAge | Notes |
|---|---|---|---|
| `app_id` | `appId` (§3) | `10` (seconds) | Session identity, short-lived. |

Anti-cache headers: `Cache-Control: no-cache, no-store`, `Pragma: no-cache`, `Expires: 0`.

The cookie's `Secure` attribute is only set when the request is HTTPS (header `X-Forwarded-Proto == "https"` behind a proxy, or the `https` scheme). **Platform requirement:** over plain HTTP, browsers (e.g. Firefox) reject `Secure` cookies, which would throw the session into a `4001`/reload loop. That is why `Secure` is conditional on the actual transport being HTTPS.

The client reads the cookie and **removes it** (path `/`).

### 5.2 Explicit Init Mode (`GET /api/session/init`)

For clients that have no page cookies (typically desktop and mobile applications). Endpoint:

```
GET /api/session/init          (and also GET /{ctx}/api/session/init)
```

Identical anti-cache headers. Responses:

- **Capacity reached:** HTTP `503` with body `{"error":"capacity_exceeded"}`.
- **OK:** HTTP `200`, `Content-Type: application/json`, body:

```json
{ "appId": "<appId>" }
```

No cookie or authentication header is required for this call.

---

## 6. Security Handshake — the Connection Key (Protocol Version 1)

Every WebSocket connection — the first one and each reconnection — starts with a handshake that agrees on a **key of its own**, by ephemeral ECDH. No long-lived ciphering key exists on either side, so a recorded connection cannot be deciphered later (forward secrecy).

### 6.1 The Messages

1. **Client → server**, the first message on the connection, with nothing else in it:
   ```json
   { "protocol": 1, "clientKey": "<Base64 of the X.509 SubjectPublicKeyInfo of an ephemeral P-256 public key>" }
   ```
2. **Server → client**, the answer:
   ```json
   { "protocol": 1, "serverKey": "<Base64 of the X.509 SubjectPublicKeyInfo of the server's ephemeral P-256 public key>" }
   ```
   If the server already holds the session (a reconnection), it then reattaches it to the connection (§15).
3. Only after that answer does the client send anything else: the bootstrap message (§8) and any request that waited. Pings too (§16): a ping is not a handshake.

**Version refusal:** a first message without `"protocol": 1`, or without `clientKey`, is answered with

```json
{ "error": "unsupported-protocol", "protocols": [1] }
```

and the connection is closed with `4002 unsupported_protocol` (§10.3). The client must not reconnect: it tells the user that the page (or the app) is out of date. A `clientKey` that is not a P-256 public key closes the connection with `4001 reload_required`.

The version goes up only when the wire changes in a way the other side cannot read.

### 6.2 The Key

Both ends compute the same 32-byte key:

```
shared = ECDH-P256( own private key, peer public key )        -- the 32-byte x-coordinate
key    = HKDF-SHA256( ikm    = shared,
                      salt   = utf8_bytes(appId),
                      info   = utf8_bytes("wdc-cube-remote/aes-256-gcm/v1"),
                      length = 32 )
```

HKDF is that of RFC 5869 (extract, then expand). The key is an AES-256 key for the rest of the connection; a reconnection agrees on a new one.

### 6.3 Ciphered Values (AES-GCM)

Sensitive values travel ciphered: storage values (§14), in both directions, and sensitive event parameters (e.g. `p.password`, §8).

- Transform: **AES-256-GCM**, **128-bit tag**, no additional data.
- **Every value has its own random 12-byte IV**, sent in front of it.
- Wire encoding: `Base64( IV(12) ‖ ciphertext ‖ tag(16) )`, standard Base64 with padding.
- A value that does not authenticate — tampered, or ciphered with another connection's key — is refused.

### 6.4 Cryptography in the Browser (Secure Context)

**Platform requirement (browser):** the browser's native cryptography (Web Crypto) is only available in a **secure context** (HTTPS or `localhost`). Over plain HTTP, the client provides its own implementation of the primitives (P-256 ECDH, HKDF-SHA256, AES-256-GCM), with **identical** parameters. Outside the browser, the client uses the platform's cryptographic library, or its own implementation of a primitive the platform lacks.

---

## 7. WebSocket Channel — Lifecycle

### 7.1 Endpoint

```
ws(s)://<host>/dispatcher/{appId}          (and /{ctx}/dispatcher/{appId})
```

- The scheme derives from the originating transport: `http→ws`, `https→wss`.
- The `appId` travels **only in the path**. No custom header or cookie is needed.
- **Subprotocol:** the client may open with the `"wdc"` subprotocol; the server does not require it. Recommended, not mandatory.

### 7.2 Server Side — Behavior

The server keeps a map of active handlers per `appId`. Each handler holds `appId`, the current connection, that connection's internal identifier, and the key the connection agreed on (none until its handshake).

- **onConnect:**
  1. Finds or creates the session handler.
  2. Validates the `appId` (non-empty, matches the path, signature checksum — §3). Failure → close with `4001 reload_required`.
  3. Forgets any key of a previous connection: this one agrees on its own.
  4. Enables automatic pings every 15 s.
  5. Registers the connection as active.

- **onMessage:** ignores a message from a connection a newer one replaced. On a connection without a key, the message is the handshake (§6): it is answered, the key is derived, and if the session's application already exists it gets the new key, is reattached, and is **reinitialized for reconnection** (§15). Afterwards, it creates/retrieves the session's application (handing it the connection key on creation), reattaches the connection, extends the session's life, and processes the request (§8).

- **onClose:** disables pings. If the closing connection is **not** the active one (the typical F5 case, in which a new connection replaced the old one), the close is **ignored**, so as not to bring down the freshly reconnected session. Otherwise, it detaches the connection; if the session is **not** authenticated **or** the server is in immediate-release mode → releases the session. Removes the handler.

- **onError:** a closed-channel error (abrupt disconnection) is routine and only logged; other errors produce a warning and an unexpected-error alert to the session.

### 7.3 Client Side — State Machine

Phases:

1. **Bootstrap** — obtains the `appId` (§5).
2. **Connecting** — opens the WebSocket.
3. **Handshake** — sends its key, waits for the server's, derives the connection key (§6). Requests made meanwhile wait.
4. **Connected** — sends the bootstrap message, receives the initial push, operates.
5. **Reconnecting** — on a drop, applies backoff and reconnects (§15).

A reusable client core must offer, at a minimum: waiting for the next message, or waiting for the response correlated with a specific `requestId` — discarding intermediate messages (whose scopes **have already been applied** to the local scope map before enqueueing, which makes discarding safe). Recommended response and connection timeout: **10 seconds**.

---

## 8. Message Format — Client → Server

Every message is **one JSON object**. Recognized fields (all optional, combinable):

| Field | Type | Meaning |
|---|---|---|
| `ping` | `boolean` | Keep-alive. If `true`, the server extends the session's life and answers the ping (§9.2); it does **not** process events. |
| `protocol`, `clientKey` | `number`, `string` | Only in the handshake, the 1st message of a connection (§6). |
| `requestId` | 64-bit integer | Monotonically increasing id. Requests with `requestId <= lastRequestId` or null are **discarded** (idempotency/dedup). |
| `event` | `string[]` | List of events, each item `"<vsid>:<eventCode>"`. Repeated items are counted. |
| `"<vsid>"` | `object` | For each vsid present as a top-level key, the value is that view's **formData** (client→server synchronization). |
| `storage` | `object` | Client storage bootstrap (§14), with `session` / `persistent` / `persistent-secure` sub-objects. |

**formData convention:** presenter method parameter fields are prefixed with `p.` (e.g. `p.path`, `p.productId`, `p.password`); direct fields carry no prefix. Sensitive parameters (e.g. `p.password`) are ciphered with the connection key (§6.3) before sending; the presenter deciphers them (`b64Decipher`).

### 8.1 Examples

**Handshake — the first message of every connection (§6):**
```json
{ "protocol": 1, "clientKey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE..." }
```

**Bootstrap — the first message after the handshake answer, which triggers the initial push:**
```json
{ "event": [] }
```
Optionally with a bootstrap `storage`, ciphered with the connection key.

**Start / initial route (event `-1` on the Browser View):**
```json
{
  "requestId": 1,
  "event": ["7b32e816a191:0:-1"],
  "7b32e816a191:0": { "p.path": "product?pr-id=42&sign=AbC123" }
}
```

**History navigation (event `-2`):** identical to the previous one with the eventCode changed to `-2`.

**View event submit (e.g. changing the quantity of a cart row, eventCode `2` on a `CartItemScope` view, classId `c8a1f0e2b7d4`):**
```json
{
  "requestId": 7,
  "event": ["c8a1f0e2b7d4:5:2"],
  "c8a1f0e2b7d4:5": { "p.quantity": 2 }
}
```

An event with no parameter (e.g. the cart's checkout, eventCode `1` on the `CartScope` view, classId `7eb485e5f843`) carries only the `event` item: `{ "requestId": 8, "event": ["7eb485e5f843:3:1"] }`.

**Keep-alive:**
```json
{ "ping": true }
```

---

## 9. Message Format — Server → Client

### 9.1 Response / Scope Push Envelope

A JSON object. Fields (all optional). The envelope is **not sent** if it is empty (`{}`).

| Field | Type | Meaning |
|---|---|---|
| `requestId` | 64-bit integer | Echoed when the response corresponds to a request. **Absent** in background flushes (asynchronous push). |
| `storage` | `object` | Client storage delta (§14). Per scope → per key → encrypted value (`b64Cipher`) or `null` (removal). |
| `uri` | `string` | Current navigation fragment/hash (with the `sign` parameter). Sent when navigation happened or the fragment changed. |
| `scopes` | `object[]` | Array of serialized *dirty* scopes. Each object has `"#"` = vsid (§11). |

**Example:**
```json
{
  "requestId": 7,
  "uri": "cart?sign=AbC123",
  "scopes": [
    { "#": "7eb485e5f843:3",
      "rowsViewIds": [ "c8a1f0e2b7d4:5" ],
      "total": 99.8,
      "errorCode": 0 },
    { "#": "c8a1f0e2b7d4:5",
      "id": 1, "name": "Product A", "price": 49.9, "quantity": 2 }
  ],
  "storage": { "persistent": { "authToken": "k8s2...==" } }
}
```

Notes:
- Before serializing the scopes, the server consolidates the derived state of all presenters once (`commitComputedState`), under a lock.
- A view born while another's scope is being written (the scope of a child presenter, a segment, a row, requested for the first time) goes in the **same** response: the already-written scope references it.
- Error fields (`errorCode`, `errorMessage`) live **inside** the fields of each scope, not in the envelope.

### 9.2 Ping/Control Envelope

Sent in response to `{ ping: true }`. Conditional fields:

| Field | Type | Meaning |
|---|---|---|
| `releasedViews` | `string[]` | vsids of views released since the last send (*eager* GC on the client). |
| `activeViews` | `string[]` | Snapshot of **all** live views. Sent periodically (every ~5 min) for reconciliation/full GC on the client. |

---

## 10. Event Codes and Close Codes

### 10.1 Browser View Event Codes (`classId 7b32e816a191`)

| eventCode | Method | Semantics |
|---|---|---|
| `-1` | `onStart(path)` | Boot / initial route. Reads `p.path`, navigates, and resends all views. |
| `-2` | `onHistoryChanged(path)` | History/hash change (back/forward). Reads `p.path`, navigates. |
| `1` | `onAlertOk()` | Confirms/dismisses the current alert. |
| `2` | `onKeepAlive()` | Extends the session's life. (Alternative to the `{ping:true}` message.) |

Events of regular views use codes `>= 0` defined by each view. Negative eventCodes are reserved for the protocol. On the server, each code is that of the action the presenter bound on the scope (`action(1, ...)`, `actionOf(2, FormParam..., ...)`); a code the scope does not know is an error.

### 10.2 Alerts (Browser View Fields)

The Browser View scope may carry a server-commanded alert:
- `alertId`: alert code; `-1` = unexpected error; `0` = none (after OK).
- `alertArgs`: `string[]` of arguments (for the unexpected error: only the message — the stack trace stays in the server log).

With `alertId != 0`, the client shows the alert and, when it is dismissed, fires event `1` (`onAlertOk`) on the Browser View.

### 10.3 WebSocket Close Codes

They use the range reserved for applications (`4000–4999`):

| Code | Reason | Expected client action |
|---|---|---|
| `4001` | `reload_required` | Invalid/forged `appId`, or a handshake with an invalid `clientKey`. The client **must** discard the persisted `appId` and reload/restart to obtain a new `appId`. |
| `4002` | `unsupported_protocol` | The handshake did not carry a protocol version the server speaks; it was answered with `{"error": "unsupported-protocol", "protocols": [...]}` (§6.1). The client **must not** reconnect: it tells the user the page (or app) is out of date. |
| `4003` | `capacity_exceeded` | Server at its session limit. Equivalent to the init endpoint's HTTP `503 {"error":"capacity_exceeded"}`. |

---

## 11. Scope Serialization

A scope is the view state a presenter publishes for its view; it travels as a JSON object. The **observable contract** is:

- The object injects `"#"` as its **first** field, value = `instanceId` (vsid).
- Boolean fields are **always** present (even when `false`).
- `null` (non-boolean) fields are **omitted**.
- A field whose value is **another scope** (that of a child presenter, a segment) is serialized as `"<name>ViewId": <vsid of the view that draws it>`; a list of scopes (rows), as `"<name>ViewIds": [vsid, ...]`, in order. This is how references between views become pointers by id. E.g.: `contentViewId`, `productsPanelViewId`, `rowsViewIds`.
- Actions (`Action`, `ActionOf`, `ActionOf2`) do not appear on the wire: the client fires them by event code.
- Top-level strings are included **only if they are not empty/blank** (blank strings are omitted at this level).
- Numeric fields are always included.
- Dates are serialized in **ISO-8601**: date (`YYYY-MM-DD`), time (`HH:MM:SS[.sss]`), or date-time with an offset in **UTC**, depending on the type.
- Nested values (inside objects and arrays): objects → JSON object; lists → JSON array; scalars → direct values; nested strings are **not** filtered for emptiness.
- Fields marked as non-serializable or derived/transient do **not** appear on the wire.

**instanceId formation:** `"<classId>:<n>"`, where `classId` is the `CLASS_ID` declared by the scope's class. The Browser View is fixed at `7b32e816a191:0`. Other views use an incremental counter starting at `1`.

**Client-side application semantics:** each object in `scopes` **fully replaces** the previous scope of that vsid (total replace, not field-by-field merge). The client indexes by vsid; the `"#"` field is removed and the rest becomes the view's scope (`values` in the client's `ViewScope`).

---

## 12. Authority over Scopes and View Garbage Collection

The **server is the authority** over which views exist. The client never creates vsids.

**On the server:**
- A `vsid → view` map is the authoritative list. Every view is registered when created.
- **Input validation:** when processing formData/events, the server only accepts vsids that exist in that map. An unknown vsid:
  - in scope synchronization: is ignored;
  - in an event: the server resynchronizes **all** views at the end of the dispatch, to correct the out-of-sync client. This neutralizes forgery attempts.
- **Dirty tracking:** marking a view as dirty enqueues it for flushing (immediately while processing a submit; otherwise in the background flush every ~50 ms).
- **Delta collection:** the server atomically takes the dirty views to serialize into `scopes`.
- **Release:** when a view is removed, its vsid is enqueued and sent as `releasedViews` in the next ping response.

**On the client (view garbage collector):**
- Keeps the set of currently **mounted** views (rendered in the UI tree).
- `release(releasedViews)` — **eager GC (~15 s)**: removes from the local map each vsid released by the server that is **not** mounted.
- `sweep(activeViews)` — **full sweep (~5 min)**: removes from the local map every vsid that is **not** in `activeViews` **and** is not mounted.
- General rule: the client only removes a view if (1) it is not on the server's list **and** (2) it is not mounted. The Browser View is never removed.

---

## 13. Navigation and URL Signing

Navigation is **server-driven** and the URL is **signed** to prevent the client from forcing places it was not granted.

**Emission (server → client):** when updating the history, the server builds the intent (place + parameters), computes `sign = sign(intent.toString())` (§3: HMAC-SHA256 with the signing key, 16 bytes, Base62), injects the `sign=<...>` parameter into the intent, and sends the result in the `uri` field. The address is the name of the deepest place plus its query string (`CubeIntent.toString()`), e.g. `product?pr-id=42&sign=AbC123`.

**Reception (client → server):** the client navigates by sending `p.path` (the URI with `sign`) via events `-1`/`-2` on the Browser View. The server:
1. Parses the path and **removes** the `sign` parameter (the received signature).
2. Recomputes `expected = sign(intent.toString())` and compares it in constant time.
3. If the received signature ≠ expected → the URL was tampered with: the client's intent is **discarded** and the server navigates to its current state instead (the last place with the parameters the presenters publish; the root place when there is none), forcing a history update. This way, the client cannot forge navigation to arbitrary unsigned parameters/places.

**Reflection in the history (browser):** the `uri` field is applied to the browser history as a hash (`#`): *replace* on the first URI response and on navigation coming from back/forward; *push* in all other cases. The shell derives the initial route from the URL hash at boot.

---

## 14. Client Storage — Page and Site

This is the most subtle point of the specification. The model has **two scopes**, each with a **secure variant**, resulting in **three scopes on the wire**:

| Wire scope | Conceptual name | Lifetime | Backing (browser) | Backing (desktop/mobile, suggested) |
|---|---|---|---|---|
| `session` | **Page storage** | Survives F5; does **not** survive closing the tab. | browser session storage | per-session memory |
| `persistent` | **Site storage** | Survives reload and closing the tab/app (per origin/installation). | browser local storage | per-app file/preferences |
| `persistent-secure` | **Site storage (secure)** | Same as `persistent`. | browser local storage, encrypted at rest with a dedicated key | the platform's secret vault (e.g. Keychain on macOS/iOS, KeyStore on Android) |

**Invariant rules:**

1. **Every value traveling over the WebSocket is encrypted** with the session AES-GCM key (`b64Cipher`), **regardless of scope**. The `secure` variant does **not** change in-transit encryption — it is only a **hint** for the client to use a better-protected backing store **at rest**.
2. In the browser, a `persistent-secure` value ends up encrypted **twice** with different keys: at rest (dedicated local storage key) and in transit (PBKDF2-derived session key). They are orthogonal. At-rest encryption is a client detail, outside the wire contract.
3. There is no `session-secure` in the protocol — only `session`, `persistent`, `persistent-secure`.

**Isolation (synchronization namespace):** every key is prefixed by a `syncNamespace` in the format `~<shellId>:` (e.g. `~rr:`). The `~` marks the key as synchronizable; `<shellId>:` isolates one shell's data from the others in the same origin/installation. The prefix is **removed** when building the map sent in the bootstrap, and only keys with that prefix go into the bootstrap.

**Bootstrap (client → server):** in the first message, the client collects all keys of each scope, **encrypts each value** with `b64Cipher`, and builds:
```json
"storage": {
  "session":            { "key": "<encrypted value>" },
  "persistent":         { "key": "<encrypted value>" },
  "persistent-secure":  { "key": "<encrypted value>" }
}
```
Empty scopes are omitted; if everything is empty, the `storage` field is omitted. Purpose: to allow the server to **restore the user's session** (e.g. auto-login via a persistent token).

> Server note: when loading the bootstrap, the `persistent` and `persistent-secure` scopes are both read into the **same** in-memory persistent storage on the server (asymmetry: on write/delta they are kept separate). Tampered values that fail to decrypt are silently ignored.

**Delta (server → client):** in any response, the server may command a write/removal:
```json
"storage": {
  "persistent": { "authToken": "<encrypted value>", "oldKey": null }
}
```
For each entry: non-empty string value → the client **decrypts** it (`b64Decipher`) and stores it; `null`/empty value → the client **removes** the key. This way the server controls writing and removal in any of the three scopes.

**In non-browser clients:** the `session` scope is browser-specific; a desktop/mobile client typically handles only `persistent` (per-app local storage) and `persistent-secure` (the platform's secret vault) in the bootstrap/delta.

---

## 15. Refresh (F5) Flow and Reconnection

The goal is that, for the user, an F5 (or a reconnection) is imperceptible: same session, same state.

### 15.1 What the Client Preserves Locally

| Item | Where (browser) | Why |
|---|---|---|
| `appId` | browser session storage (key `app_id`) | Survives F5 → allows reusing the **same session** on the server. |
| request counter | browser session storage (key `req_seq`) | Avoids `requestId` collisions after a reload. |
| session/persistent/secure storage | browser local storages | Reread by the client itself; resent encrypted to the server in the bootstrap. |

### 15.2 Sequence (Browser Mode)

1. The page reloads; the client is rebuilt.
2. **Retrieves `appId`:** reads the `app_id` cookie (if the server planted a new one) and removes it; then **prioritizes** the value in session storage. Since that storage survives F5, the **same `appId`** is reused. If there is neither a cookie nor a persisted value, it generates a local id marked as "fake".
3. **Retrieves the request counter** → restores the `requestId` sequence.
4. **Security:** the secure storage is decrypted again using the at-rest encryption key persisted locally (which survives F5).
5. **Reconnects the WebSocket** to `/dispatcher/{appId}`; on open, does the handshake (§6), then sends the bootstrap `storage`, ciphered with the new connection key, and flushes pending requests.
6. **Restores the route:** derives the `path` from the URL hash and submits `p.path` with event `-1`. The server responds with `uri` (applied as a *replace* in the history).

### 15.3 Sequence (Server Side)

1. The server finds/creates the handler. The **application** (Presenters/Scopes) persists in the session registry as long as it has not expired.
2. If the session's application is still alive: it reattaches the new connection and **reinitializes the session for reconnection**:
   - forces resending the `uri` (clears the last sent fragment);
   - marks the history as dirty;
   - clears the `releasedViews` queue;
   - marks **all** views as dirty → the next flush resends **every scope**.
3. **Old (superseded) connection:** the previous connection's `onClose` is ignored if it is no longer the active one, avoiding bringing down the freshly reconnected session.
4. **If the session expired** (removed by TTL without a connection): the application is **recreated**, after the handshake, from the bootstrap `storage` + `event -1` (`p.path`), reconstituting the state. If the server is full → `4003 capacity_exceeded`.
5. **Dedup:** the per-session `lastRequestId` discards requests with an id `<=` the one already processed. In a recreated session, it restarts at `-1`.

### 15.4 Reconnection Backoff (Client)

When the connection drops (error/close ≠ 4001):
- `delay = min(2000 * attemptNumber, 120000)` ms (2 s per attempt, capped at 120 s).
- A 1 s timer runs a visible countdown; on reaching `0`, it tries to reconnect; if the connection comes back, it resets the counter.
- The user may force "try now" (immediate attempt).

### 15.5 Anti-Loop Guard

On receiving close `4001 reload_required`, the client **removes** the persisted `appId` and reloads — so the next boot obtains/generates a new `appId` instead of reusing the invalid one, avoiding a reload loop.

### 15.6 Reconnection in Non-Browser Clients

A minimal client core may not implement automatic reconnection — in that case, it exposes a hook to clear the scope map and leaves the reconnection policy to each shell. A desktop/mobile shell that wants parity with the browser must: persist the `appId` and the request counter, and on reconnecting redo `init → WS → handshake → bootstrap(storage) → event -1`. Alternatively, it rebuilds the session from scratch via a new init.

---

## 16. Keep-Alive, Timeouts, Capacity, and Intervals

| Parameter | Value | Side |
|---|---|---|
| Automatic WS pings | 15 s | server |
| Application keep-alive | 15 s (`{ping:true}` message; 80 ms debounce) | client |
| Background flush (dirty views) | 50 ms | server |
| Expiration check | 30 s | server |
| `activeViews` send (sweep) | 5 min | server |
| Session TTL (default) | 3 min | server |
| Eager view GC (via `releasedViews`) | ~15 s | client |
| Response timeout | 10 s | client |
| WS connection timeout | 10 s | client |
| "submitting" spinner | appears after 200 ms, disappears after ≤15 s | client |
| Reconnection backoff | `min(2000·n, 120000)` ms | client |

**Configurable TTL (server):**
- TTL `= 0` → **immediate** release on disconnection.
- TTL `> 0` → keeps the session alive for N seconds after the drop, waiting for reconnection.
- default → 3 min.

**Capacity:**
- Configurable maximum number of sessions (`-1` = unlimited). On reaching the limit: the init endpoint responds HTTP `503 {"error":"capacity_exceeded"}`; the WebSocket closes with `4003`.

**Release on disconnection:** if the session is **not** authenticated **or** the server is in immediate-release mode, it is released right away. If authenticated and with a TTL, it stays cached waiting for reconnection until it expires without a connection.

---

## 17. Cross-Platform Portability Notes

This protocol was designed for multiple client platforms: **web browser, desktop, and mobile**. To reimplement it in any technology (client or server), respect the following:

**What is immutable (the wire contract):**
- `appId` formation and validation (§3).
- The handshake of protocol version 1 (§6.1), and its refusal.
- Cryptographic primitives: ECDH P-256 with keys as Base64 X.509 SubjectPublicKeyInfo; HKDF-SHA256 with the `appId` as salt and `wdc-cube-remote/aes-256-gcm/v1` as info; AES-256-GCM, **128-bit tag**, a **random 12-byte IV per value**, written `Base64(IV ‖ ciphertext ‖ tag)`.
- WebSocket path `/dispatcher/{appId}` and (optional) `wdc` subprotocol.
- Message fields in both directions (§8, §9), including `"#"` in the scope and `"<vsid>:<eventCode>"` in `event`, parsed on the **last** `:`.
- Reserved event codes (`-1`, `-2`, `1`, `2`) and close codes (`4001`, `4002`, `4003`).
- Three storage scopes (`session`, `persistent`, `persistent-secure`), with in-transit encryption for **all** of them and `secure` as an at-rest hint.
- URL fragment signature (`sign`) — the client must **never** try to navigate to unsigned paths; it only echoes the `p.path` it received.

**What is free per platform:**
- How to obtain the `appId`: the cookie (browser mode) **or** the init endpoint (explicit mode). A desktop/mobile client typically uses the **explicit init mode**.
- Backing of the storage scopes: on desktop/mobile, `session` = memory; `persistent` = file/preferences; `persistent-secure` = the platform's secret vault (Keychain on macOS/iOS, KeyStore on Android, an equivalent secret store on desktop).
- Reconnection: implement backoff and `appId` reuse (parity with the browser) **or** recreate the session via a new init.
- Rendering: the view reads its scope (indexed by vsid) and emits events via `submit(vsid, eventCode, formData)`; the UI technology is irrelevant to the protocol.

**Platform requirements to observe:**
- **Browser:** native cryptography usually requires a secure context (HTTPS/localhost); over plain HTTP, an own implementation of the primitives is needed, with identical parameters. `Secure` cookies only work over HTTPS. Route reflection uses the URL hash (`#`).
- **Desktop/mobile:** with no page cookies, use the init endpoint; use the platform's secret vault for `persistent-secure`; persist `appId`/request counter for transparent reconnection.

**Known pitfalls:**
- Nothing but the handshake may go out before the handshake answer: not a ping, not a request. Queue them.
- The ECDH shared secret is the raw 32-byte x-coordinate (what Web Crypto's `deriveBits(…, 256)` and JCA's `KeyAgreement` give), not a hash of it.
- The HKDF salt is the full `appId`, both parts and the dot, as UTF-8.
- `event` parsing uses the **last** `:` (the vsid contains `:`).
- The client-side scope is a **total replace** per vsid, not a merge.
- The client is subordinate: it never invents vsids, never navigates without `sign`, and only garbage-collects views the server no longer lists **and** that are not mounted.

---

## 18. Appendix — Literal Table of Constants

| Constant | Value |
|---|---|
| Browser View classId | `7b32e816a191` |
| Browser View vsid | `7b32e816a191:0` |
| vsid field in the scope | `#` |
| Route field in the formData | `p.path` |
| Method parameter prefix | `p.` |
| Init endpoint | `GET /api/session/init` → `{appId}` |
| WebSocket endpoint | `ws(s)://host/dispatcher/{appId}` |
| WS subprotocol | `wdc` (optional) |
| Cookie (browser mode) | `app_id` (MaxAge 10s) |
| Keys persisted on the client (browser) | `app_id`, `req_seq` (in session storage) |
| Storage scopes on the wire | `session`, `persistent`, `persistent-secure` |
| Storage namespace prefix | `~<shellId>:` (e.g. `~rr:`) |
| Reserved eventCodes | `-1` start, `-2` historyChanged, `1` alertOk, `2` keepAlive |
| Close codes | `4001 reload_required`, `4002 unsupported_protocol`, `4003 capacity_exceeded` |
| Capacity response (init) | HTTP `503 {"error":"capacity_exceeded"}` |
| Protocol version | `1` (handshake field `protocol`) |
| Handshake | `{"protocol": 1, "clientKey"}` → `{"protocol": 1, "serverKey"}`; refusal `{"error": "unsupported-protocol", "protocols": [1]}` |
| Key agreement | ECDH P-256, keys as Base64 X.509 SubjectPublicKeyInfo, ephemeral per connection |
| Key derivation | HKDF-SHA256, salt = UTF-8 `appId`, info = `wdc-cube-remote/aes-256-gcm/v1`, 32 bytes |
| Ciphered value | AES-256-GCM, 128-bit tag, random 12-byte IV per value, `Base64(IV ‖ ciphertext ‖ tag)` |
| `sign(text)` | `base62( first 16 bytes of HMAC-SHA256(signingKey, utf8(text)) )` |
| `appId` | `base62(32 rnd bytes) + "." + sign(part1)` |
| Signing key | `[remote] signingKey`, Base64 URL-safe, ≥ 32 bytes, generated on the first start |
| Automatic WS pings | 15 s |
| Client keep-alive | 15 s (80 ms debounce) |
| Background flush | 50 ms |
| Expiration check | 30 s |
| Sweep (`activeViews`) | 5 min |
| Default TTL | 3 min |
| Reconnection backoff | `min(2000·n, 120000)` ms |
| Response/connection timeout | 10 s |

---

*The reference implementations (host and client shells) live in this repository under the remote presentation modules. See also [cube-mvp-multiplatform-architecture.md](cube-mvp-multiplatform-architecture.md).*
