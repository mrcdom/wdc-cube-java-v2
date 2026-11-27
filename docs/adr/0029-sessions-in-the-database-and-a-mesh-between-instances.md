# 0029. Sessions in the database, held in memory once read; the instances tell each other over a WebSocket mesh

- **Date:** 2026-11-27
- **Status:** Accepted

## Context

The backend ran as one instance. The users' sessions (`AccessContextCache`, with each session's RSA key) and the sign-in challenges in flight (`NonceStore`) lived only in its memory: with two instances behind one address, an access token made on one was unknown to the other, and a challenge from one could not be answered on the other. A restart ended every session.

Other things each instance keeps in memory can also go stale when another instance changes them:

- the sessions it has read: one signed out, or renewed with a new key, on another instance;
- the product images it has served (`ProductImageCache`): one changed on another instance;
- the remote view sessions: when a tab moves to another instance, its copy here waits to expire.

Reading the database on every request would cost a round trip per call.

## Decision

**Sessions and challenges go to the database** (`EN_USER_SESSION`, `EN_AUTH_NONCE`), as the failed sign-ins already did (`EN_LOGIN_FAILURE`, [0024](0024-scram-sign-in.md)).

- A session's private key is stored encrypted: AES-256-GCM, with a key derived from `security.jwt.secret`. A row that does not decrypt with the secret is treated as missing, and its client signs in again.
- **Each instance holds in memory the sessions it has read**, and reads the database only for a session it does not hold, or whose copy expired (another instance may have renewed it). A request with a held session does not touch the database.
- A renewal is one conditional `UPDATE` on the old refresh token: of two instances renewing the same token at once, one wins, and the token is spent.
- A nonce is used up by whichever instance deletes it first.

**The instances keep a WebSocket open to each other**, when `[cluster] peers` names them (`ClusterMesh`, in the backend, route `/cluster`). Each opens a connection to every peer and publishes on it; it receives on the connections the others open to it. A message is a type and an id (`StoreCluster`):

| Message | Sent when | The others |
|---|---|---|
| `session-changed` | a session is renewed, signed out or found past its refresh window | drop their copy, and read it again if asked |
| `image-changed` (an id, or `*`) | a product image changes, after the commit | drop it from their image cache |
| `remote-session-made` | a remote view session is made here (its tab moved here) | release their copy, if no socket holds it |

- **A static list**, which can be the same for every instance: each one recognizes its own address (the other end answers with its own node id, so it must be able to reach its own address) and leaves it alone.
- **Both ends prove a key** derived from `security.jwt.secret` (HMAC-SHA256 over a challenge each one makes) before anything is read or sent. The backend does not start with `cluster.peers` and without the secret.
- **What is missed is made up for, not resent.** A connection that drops is opened again (1 s, doubling up to 30 s), and what was published meanwhile waits for it (the last 1000 messages). Each time another instance connects, at its start or after a drop, the receiving instance drops every session and product image it holds, which are read again from the database. A remote view session whose message was missed waits to expire, as without the mesh.
- **The framework takes no part in the mesh.** `RemoteApplicationRegistry` only tells of the sessions made (`onCreated`) and releases an idle one on request (`releaseIdle`); `ProductImageCache` tells of the images changed here (`onChanged`) and drops the ones changed elsewhere without telling again.

## Consequences

- Any instance answers for any session: an instance that stops loses no user session, and neither does a restart.
- A request reads the database only the first time an instance sees a session, and after a notice or a missed connection. Sign-in, renewal and sign-out write to it.
- Every instance has to know every other one: adding an instance means changing the list everywhere.
- The mesh's messages are not encrypted by it, and session ids go as they are: between instances not on a private network, the addresses must be `wss://`.
- A message published as a connection breaks, before anyone notices, can be lost. For sessions and images that costs nothing, because the receiving instance drops what it holds when the connection comes back; a missed remote view session waits to expire. A connection that fails silently is noticed after a failed send, or after 60 to 80 s without answers to its pings.
- Starting or restarting an instance makes every instance it lists drop its held sessions and images: a burst of reads from the database.
- Without the mesh, a session ended on one instance stays valid on another that already holds it, until its access token expires (30 minutes).

## Alternatives considered

- **Sticky routing, sessions kept in memory:** no schema change, but every client pinned to one instance, and an instance that stops ends its users' sessions.
- **Reading the session from the database on every request:** nothing to invalidate, but a query on every call.
- **PostgreSQL `LISTEN`/`NOTIFY`:** no connections of its own to keep, but only on PostgreSQL, and every kind of message would go through the database.
- **Webhooks** (an HTTP POST to each peer per message): plain HTTP, but only the sender knows a delivery failed; the receiver would need sequence numbers per sender, the sender a retry queue, and both a heartbeat, to learn what a connection that drops and comes back says by itself.
- **A broker** (Redis pub/sub, NATS): the usual answer at scale, but one more service to run for a reference project.
- **Discovery through the database** (each instance writes its address, with a heartbeat): no list to keep, but a table and the cleanup of instances that died. A static list is simpler and explicit.
