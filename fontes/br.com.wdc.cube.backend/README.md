# WeDoCode Cube Backend

Standalone HTTP server built with [Javalin 7.2.4](https://javalin.io). It is the composition root of the WDC Store: it wires the database, the repositories and the transactions, and serves the web frontends, the remote host (WebSocket) and the REST API from a single self-contained JAR.

## Overview

The server:
- Serves each web frontend found under the work directory's `frontend/<name>` at its own context (`/<name>/...`), with a landing page at `/` listing them
- Redirects in-context paths that are not static files to the context's `index.html` (SPA routing)
- Hosts the application sessions of the remote frontends (React, Flutter) through `remote.host`
- Exposes the REST API (`/api/auth/*`, `/api/repo/*`, `/api/tx/*`) at the root and under each frontend context
- Wires the persistence (`BusinessContext`): DataSource (Agroal), JTA (Narayana) when configured, repositories, transactions, schema creation and the remote transaction coordinator

## Building

Build from `fontes/` (Java 25):

```bash
cd fontes && mvn clean package -DskipTests
```

The fat JAR is created at:
```
fontes/br.com.wdc.cube.backend/target/br.com.wdc.cube.backend-1.0.0.jar
```

## Running

The usual way is `./work/bin/start-server.sh [port]`, which passes the repository's `work/` as the work directory and builds the JAR if it is missing.

The server does not start without a **work directory**, given by `--workdir=<folder>` or the `STORE_WORKDIR` variable; the folder must exist. Everything comes from its fixed subfolders: `config/` (`application.toml`, `application.local.toml`, `logback.xml`), `data/`, `log/`, `temp/` and `frontend/`. A second backend with another configuration is just another folder.

The port is resolved in this order, each one overriding the previous:

1. `server.port` in `application.toml` (default 8080)
2. the first command-line argument other than `--workdir=`
3. the `SERVER_PORT` environment variable

```bash
java -jar fontes/br.com.wdc.cube.backend/target/br.com.wdc.cube.backend-1.0.0.jar --workdir=work 9090
# or: STORE_WORKDIR=work java -jar ... 9090
```

From Maven: `cd fontes/br.com.wdc.cube.backend && mvn exec:java` (the repository's `work/`; `-Dworkdir=<folder>` for another).

## URLs

After starting the server:

- **Landing page**: http://localhost:8080/ — links to each frontend context
- **Health**: http://localhost:8080/health/live (the process answers; `/health` is the same) and http://localhost:8080/health/ready (the database answers too; 503 when it does not)
- **Metrics**: http://localhost:8080/metrics — Prometheus format, only when `[management] token` is set, with `Authorization: Bearer <token>` (see [Observability](#observability))
- **Auth Challenge**: http://localhost:8080/api/auth/challenge (POST `{userName}`)
- **Auth Login**: http://localhost:8080/api/auth/login (POST)
- **Repository API**: http://localhost:8080/api/repo/* (Bearer JWT required)
- **Remote transactions**: http://localhost:8080/api/tx/* (Bearer JWT required)
- **OpenAPI**: http://localhost:8080/openapi.json
- **Product Images**: http://localhost:8080/api/repo/product/{id}/image and http://localhost:8080/image/product/{id}.png (public, no auth)

## Features

### Static Asset Serving
Each subdirectory of the work directory's `frontend/` (filled by `./work/bin/build-frontends.sh`) is registered as an external static file source at `/<subdir>`, together with the REST routes under `/<subdir>/api/...`. Images packaged in the classpath (`/META-INF/resources/images`) are served at `/images`.

### SPA Routing
A before-handler redirects a request at `/<context>/...` to `/<context>/index.html` when its path is not a static file (judged by the extension), does not contain `/api/` and does not start with `/ws/`, `/health`, `/dispatcher`, `/web-cache/` or `/__dev/`.

### Sessions
The application sessions of the remote frontends are managed by `remote.host`. Their limits come from the `[server]` section of `application.toml`: `maxSessions` (concurrent sessions) and `sessionTtlSeconds` (how long an authenticated session survives a WebSocket disconnect).

### Several instances

With `[cluster] peers`, the instances keep a WebSocket mesh between them (package `cluster`: `ClusterMesh` at `/cluster`, `StoreCluster` for what goes on it) and tell each other the user sessions renewed or ended, the product images changed, and the remote view sessions that moved. One list can serve every instance; each finds itself in it. It needs `[security] jwt.secret`, from which the key they prove to each other is derived:

```toml
[cluster]
peers = ["ws://store-a:8080", "ws://store-b:8080"]
```

What the instances share, what each keeps in memory and how to route them: [running-several-instances.md](../../docs/running-several-instances.md); a compose setup with two instances: [br.com.wdc.store.docker](../br.com.wdc.store/br.com.wdc.store.docker/README.md).

### CORS
Configured in `[server]`: `cors.allowAll` reflects any origin (development); otherwise only `cors.allowedHosts` is accepted. `cors.allowCredentials` defaults to `true`.

### Development mode
With `server.devMode = true`, the server registers the development endpoints: live reload (`/__dev/ws`, `/__dev/notify`), database reset, heap and GC.

### Logging
SLF4J + Logback. `LogBootstrap` loads the work directory's `config/logback.xml`, creating a default one, which writes to its `log/`, if it does not exist.

### Observability

`Observability` (package `observability`) keeps a Micrometer `PrometheusMeterRegistry`, added to Micrometer's global registry, and serves it at `GET /metrics` when `[management] token` is set (401 without the right `Authorization: Bearer`, no route without the setting). What it records:

| Meter | What |
|-------|------|
| `jvm_*`, `process_*`, `system_*` | memory, GC, threads, classes, CPU, uptime |
| `http_server_requests_seconds{method,route,status}` | every HTTP request, labeled by the route **as declared** (`/api/repo/product/{id}/image`); requests that match no route (static files, redirects, 404s) share `unmatched` |
| `store_signins_total{outcome}` | sign-ins: `ok`; `refused` (wrong password, unknown user, bad nonce); `throttled` (refused while the account must wait) |
| `store_sessions_live`, `store_sessions_renewed_total`, `store_sessions_changed_elsewhere_total` | user sessions held in memory; sessions renewed by refresh token; copies dropped because another instance renewed or ended them |
| `store_cluster_peers`, `store_cluster_links_ready`, `store_cluster_messages_{sent,received}_total{type}`, `store_cluster_connections_total` | with `[cluster] peers`: other instances connected here; connections from here, ready; the mesh's messages; connections made |
| `store_remote_sessions`, `store_remote_messages_seconds{outcome}` | remote view sessions alive; each client message the host handled, `ok` or `failed` |
| `store_remote_transactions_open`, `store_remote_transactions_total{event}` | the REST API's remote transactions: open now; `begun`, `committed`, `rolledback`, `reaped`, `refused` |

The framework modules take no metrics library: the backend reads what they expose (`RemoteTransactionCoordinator.stats()`, `RemoteApplicationRegistry.onMessage`), and `persistence.impl` counts the sign-ins in the global registry. The health checks are open: `/health/live` and `/health/ready`; with the mesh, `/health/ready` also says how many other instances are connected (`cluster.peers`, `cluster.links`), without failing on it.

## Technical Details

- **Framework**: Javalin 7.2.4
- **Java Version**: 25
- **Assembly Method**: maven-shade-plugin with manifest configuration
- **Entry Point**: `br.com.wdc.cube.backend.BackendServer`

## Dependencies

Core dependencies:
- `remote.host` - Remote host (WebSocket), views of the remote frontends
- `persistence.rest` - REST API (Javalin controllers)
- `br.com.wdc.store.scripts` - Schema and seed (`DBCreate`, `DBReset`)
- Javalin 7.2.4 - Web framework
- Agroal + Narayana - Connection pool and JTA transaction manager (in `supports/`)
- H2 and PostgreSQL - JDBC drivers
- Logback - Logging
- Micrometer (Prometheus registry) - Metrics at `/metrics`

Versions are managed by the `framework-dependencies` BOM.

## Deployment

The fat JAR runs on any environment with Java 25+. Give it a work directory that holds `config/application.toml`:

```bash
# Copy JAR to server
scp br.com.wdc.cube.backend-1.0.0.jar user@server:/opt/app/

# Run with systemd (example)
[Service]
ExecStart=/usr/bin/java -jar /opt/app/br.com.wdc.cube.backend-1.0.0.jar
Environment="STORE_WORKDIR=/var/lib/wdc-store"
Environment="SERVER_PORT=8080"
```

## Security

When `jwt.secret` is set in the `[security]` section of `application.toml`, the server enables authentication on the REST API:

### Authentication Flow

The sign-in is SCRAM-SHA-256 ([security-architecture.md](../../docs/security-architecture.md#sign-in-with-scram-sha-256)):

1. Client sends `POST /api/auth/challenge` with `{userName}` and gets a single-use nonce, the salt and the iterations of the user's verifier
2. Client derives the key (`Scram`: PBKDF2-HMAC-SHA256 over the password's MD5 digest) and computes its proof
3. Client sends `POST /api/auth/login` with `{userName, proof, nonce}`
4. Server checks the proof against the stored verifier and returns `{userId, accessToken, refreshToken, publicKey, expiresAt, serverSignature}`; the client checks the server's signature
5. All subsequent `/api/repo/*` and `/api/tx/*` requests must include `Authorization: Bearer <accessToken>`

Five failed sign-ins in a row make the account wait (30 s, doubling up to 15 min), counted in `EN_LOGIN_FAILURE`; `/api/auth` takes at most `[security] auth.requestsPerMinute` requests (default 60) per client address. Both answer 429 with `Retry-After`.

### SecurityFilter

`SecurityFilter` intercepts the `/api/repo/*` and `/api/tx/*` requests:
- Validates the JWT Bearer token
- Populates `SecurityContext.CURRENT` with the user's context (userId, userName, permissions)
- Public routes (ending in `/image`) bypass authentication
- Returns 401 for missing/invalid tokens; an `AccessDeniedException` raised by the endpoints becomes 403

### Startup Order

`BackendServer.start()` calls `BusinessContext.configure()` **before** `createJavalinApp()`, so `AuthenticationService.BEAN` is already populated when `RepositoryApiRoutes` decides whether to register the auth routes and the security filter. `BusinessContext.start()` runs after the app is created, and the server starts listening last; then the mesh, if any, connects to its peers, so that each instance can reach its own address in the list.
