# 0014. Server-driven remote views over the bridge/skeleton protocol

- **Date:** 2026-04-28 (remote React view); 2026-05-26 (generic skeleton and scope serializer); 2026-07-05 (protocol specification)
- **Status:** Accepted

## Context

The presenters are Java. A browser, a phone or a desktop shell written in another language (TypeScript, Dart) cannot run them, and porting the presentation logic to each client would multiply it by the number of clients. Running the presenters in the client also puts the authorization of every query on the client side ([0006](0006-access-control-at-the-rest-boundary.md)).

## Decision

For remote clients, **the presenters run on the server** and the client is a thin shell, like a remote desktop that works on the application's data instead of pixels.

- **Host** (`framework.cube.remote`, `remote.backend`; the store's `remote.host`): holds the presenter and scope tree per session, processes events, navigates, and is the only authority over which views exist. Each scope drawn for a client has a `RemoteViewImpl`; a `Prop` change marks it dirty, and only the dirty scopes go out (`ScopeSerializer`, by reflection over the public fields; a held scope goes as `"<field>ViewId"`, a list of them as `"<field>ViewIds"`).
- **Shell**: renders scopes and sends events (`"<vsid>:<eventCode>"` plus the view's form data), which the scope's skeleton dispatches to the bound action ([0004](0004-actions-are-guarded-scope-fields.md)). It holds no business logic and no navigation rule: it asks, the host decides. The shells include React, Flutter, a TeaVM shell, an SWT shell and a Codename One shell.
- **Channel**: one WebSocket per session (`/dispatcher/{appId}`); a response carries the dirty scopes, the address and client-storage deltas; a background flush (~50 ms) pushes changes not caused by a request; a 15 s keep-alive carries `releasedViews` ([0002](0002-the-cube-knows-only-scopes.md)). The session id (`appId`) and the navigation address are signed by the server.
- **Security of the channel**, as it is today: the client draws an ephemeral password, derives an AES-256-GCM key from it with PBKDF2-HMAC-SHA256 (250,000 iterations), and sends the password encrypted with the server's RSA handshake key; sensitive values (client storage, the access token, the login password) then travel AES-GCM encrypted; signatures use the server's RSA signing key. Scopes and events rely on TLS.
- **Threads**: the backend runs Javalin/Jetty with virtual threads (`config.concurrency.useVirtualThreads = true` in `BackendServer`), and the background flush runs on a virtual-thread executor.

## Consequences

- A new client technology needs only a renderer and a bridge for the protocol, which is specified at the wire level.
- Every remote session costs server memory: about 410 KB idle and 1.1 MB with a working set, measured with 1,000 sessions — about 5,900 recommended sessions in a 10 GB heap.
- A session survives a dropped connection for a configurable TTL (3 minutes by default) and is resumed by its `appId`.

See [bridge-skeleton-protocol.md](../bridge-skeleton-protocol.md), [cube-mvp-multiplatform-architecture.md → Remote Presentation](../cube-mvp-multiplatform-architecture.md#remote-presentation--the-hostshell-protocol), [security-architecture.md → The Remote Presentation Channel](../security-architecture.md#the-remote-presentation-channel) and [throughput-and-memory-analysis.md](../throughput-and-memory-analysis.md#part-2--memory-per-session).
