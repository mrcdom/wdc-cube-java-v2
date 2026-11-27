# Running several instances

One backend instance needs nothing in this document. It is for putting several behind one address — for capacity, or so that one can stop while the others serve — and says what they must share, what each keeps in its own memory, and how requests have to be routed so that each reaches the instance that holds what it needs.

The demo runs two instances behind nginx in the compose setup of [`br.com.wdc.store.docker`](../fontes/br.com.wdc.store/br.com.wdc.store.docker/README.md), whose `check.py` checks it from outside. What that setup does not exercise comes from reading what each part keeps and where.

## What every instance shares

- **The database.** All the data, and also what makes a user's session outlive one instance:
  - the sessions (`EN_USER_SESSION`), with their key pairs, the private key encrypted under `security.jwt.secret`;
  - the sign-in challenges in flight (`EN_AUTH_NONCE`): a challenge answered by one instance and the login sent to another find the same nonce, used up once;
  - the failed sign-ins per account (`EN_LOGIN_FAILURE`): attempts spread over the instances count as one.
- **The configuration**, in particular:
  - `[security] jwt.secret`: it signs the access tokens, encrypts the sessions' private keys in the database, makes the salts answered for unknown users, and gives the key the instances of the mesh prove to each other. Instances with different secrets reject each other's tokens, sessions and mesh connections.
  - `[remote] signingKey`: it signs the remote view's session ids and addresses. When it is missing, each instance generates its own on the first start (into its `application.local.toml`), and they reject each other's session ids.
- **The deployed web clients** (`frontend/` in the work directory), the same version everywhere.

## What each instance keeps in its own memory

| What | Where | If a request reaches another instance | What to route by |
|---|---|---|---|
| A **remote view session**: the presenters and views of one browser tab | `RemoteApplicationRegistry` | The registry there makes a new, empty session under the same id: what was on the screen is gone. With the mesh, the instance the tab left releases its copy (unless a socket still holds it there); without it, the copy waits to expire | The session id, the last segment of the WebSocket's path (`/dispatcher/<appId>`) |
| The **key that signs remote session ids** | `RemoteAppSecurity`, from `[remote] signingKey` | With the same key, nothing: a page served by one instance and its socket on another accept the same id. With different keys, the signature does not check there, the host closes the socket with `4001`, and the page reloads | With a shared key, nothing. Otherwise the page (`/index.html`, which plants the `app_id` cookie) and its socket must reach the same instance |
| A **remote transaction** of the REST API, which holds a database connection | `RemoteTransactionCoordinatorImpl` | The calls that carry its `X-Tx-Id` get 410: the transaction is unknown there | The client, `X-Client-Id` (sent on every call, from the `begin` on) |
| A **copy of a user session**, read once | `AccessContextCache` | It works: the instance reads the session from the database once, and holds it. A session renewed or ended on one instance is dropped by the others as soon as the **mesh** tells them; without the mesh, a copy already held stays valid until its access token expires (30 minutes) | — |
| **Limits per address**: `/api/auth` requests per address and minute | `AuthRateLimit` | Each instance counts its own: the effective limit is the configured one times the number of instances. That is on purpose: it guards against load, and counting every request in the database would cost a write each; the proxy is the place for it (below). Guessing one account's password is slowed down per account, in the database, however many instances there are | — |
| The **product images** already served | `ProductImageCache` (filled by `ProductRepositoryImpl`) | With the mesh, every instance drops an image when it changes. Without it, only the instance that saved it does; another can keep serving the old one | — |

## The mesh between instances

With `[cluster] peers` in `application.toml`, the instances keep a WebSocket open to each other (`ClusterMesh`, route `/cluster`) and tell each other what the table above says the others must know: a session renewed or ended, a product image changed, a remote view session that moved. One list can serve every instance — each one finds itself in it (it must be able to reach its own address to recognize itself) and leaves that address alone:

```toml
[cluster]
peers = ["ws://store-a:8080", "ws://store-b:8080", "ws://store-c:8080"]
```

- **It needs `security.jwt.secret`**: the two ends of each connection prove to each other a key derived from it (HMAC-SHA256 over a challenge each one makes), and nothing is read or sent before that. The messages travel as they are, and carry session ids: between instances not on a private network, use `wss://` addresses.
- **What is missed is made up for.** A connection that drops is opened again (1 s, doubling up to 30 s), and what was published meanwhile waits for it (the last 1000 messages). Each time another instance connects — at its start, or after a drop — the instance it reaches drops every user session and product image it holds, which are read again from the database; remote view sessions are left to expire. A connection is taken as dropped when a send fails, or after about a minute without an answer to its pings (60 to 80 s).
- **Every instance lists every other one.** An instance only hears from the ones that list it, and one without `cluster.peers` has no `/cluster` route: the others keep trying to reach it.
- `/health/ready` says how many other instances are connected (`cluster.peers`) and how many connections from this one are ready (`cluster.links`), without failing on it: an instance serves on its own while its peers are away. The same numbers are in `/metrics`.

The decision, and what else was weighed, is in [ADR 0029](adr/0029-sessions-in-the-database-and-a-mesh-between-instances.md).

## Routing

Two rules cover the table:

1. **A browser tab's socket stays on one instance**, reconnections included: hashing on the session id in the socket's path does it. The page itself can come from any instance, when every instance signs session ids with the same `[remote] signingKey`. (With a key per instance, the page must land with its socket — a load balancer's own sticky cookie does that; hashing on the id does not, because the page is requested before the id exists.)
2. **An API client stays on one instance** while it has a remote transaction open: route by `X-Client-Id`. (Its sign-in does not need it: the challenges are in the database.)

Each instance answers `GET /health/ready` with 503 when its database does not answer: point the load balancer's health check there. nginx open source has no active checks and never calls it: it takes an instance out only after connection errors, or the statuses listed in `proxy_next_upstream`.

The compose setup in [`br.com.wdc.store.docker`](../fontes/br.com.wdc.store/br.com.wdc.store.docker/README.md) runs this: two instances on their mesh, PostgreSQL, Prometheus, and nginx in front with [`nginx.conf`](../fontes/br.com.wdc.store/br.com.wdc.store.docker/nginx.conf) — rule 1 by the id in the socket's path, rule 2 by `X-Client-Id`, and the `/api/auth` limit per address.

When an instance stops, what it held in memory is gone: its remote view sessions (a tab starts over on another instance, and its user signs in again) and its open remote transactions (rolled back; their clients get 410 on the next call). The users' sessions of the REST API are in the database and survive.

## What to change for a real deployment

- **TLS** in front, and `wss://` addresses for the mesh outside a private network.
- **Limiting requests per address at the proxy**, where every instance's traffic passes, with nginx's `limit_req` as the compose setup's `nginx.conf` does. Behind a proxy, every client also reaches the backend from the proxy's address, so the backend's own per-address limit counts them all together: raise `[security] auth.requestsPerMinute`, or set it to `0`, and let the proxy limit.

See also: [security-architecture.md](security-architecture.md) (sessions, limits), [bridge-skeleton-protocol.md](bridge-skeleton-protocol.md) (the remote view's sessions), [data-layer.md](data-layer.md) (remote transactions).
