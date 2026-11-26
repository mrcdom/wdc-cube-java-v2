# WeDoCode Cube Backend

Standalone HTTP server built with [Javalin 7.2.4](https://javalin.io). It is the composition root of the WDC Store: it wires the database, the repositories and the transactions, and serves the web frontends, the remote host (WebSocket) and the REST API from a single self-contained JAR.

## Overview

The server:
- Serves each web frontend found under `{basedir}/frontend/<name>` at its own context (`/<name>/...`), with a landing page at `/` listing them
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

The usual way is `./work/bin/start-server.sh [port]`, which runs from `work/` (where `config/application.toml` lives) and builds the JAR if it is missing.

The port is resolved in this order, each one overriding the previous:

1. `server.port` in `application.toml` (default 8080)
2. the first command-line argument
3. the `SERVER_PORT` environment variable

```bash
cd work
java -jar ../fontes/br.com.wdc.cube.backend/target/br.com.wdc.cube.backend-1.0.0.jar 9090
```

The configuration file is `config/application.toml` relative to the working directory, or the path in the system property `store.config.file`.

## URLs

After starting the server:

- **Landing page**: http://localhost:8080/ — links to each frontend context
- **Health Check**: http://localhost:8080/health
- **Auth Challenge**: http://localhost:8080/api/auth/challenge (POST `{userName}`)
- **Auth Login**: http://localhost:8080/api/auth/login (POST)
- **Repository API**: http://localhost:8080/api/repo/* (Bearer JWT required)
- **Remote transactions**: http://localhost:8080/api/tx/* (Bearer JWT required)
- **OpenAPI**: http://localhost:8080/openapi.json
- **Product Images**: http://localhost:8080/api/repo/product/{id}/image and http://localhost:8080/image/product/{id}.png (public, no auth)

## Features

### Static Asset Serving
Each subdirectory of `{basedir}/frontend/` (filled by `./work/bin/build-frontends.sh`) is registered as an external static file source at `/<subdir>`, together with the REST routes under `/<subdir>/api/...`. Images packaged in the classpath (`/META-INF/resources/images`) are served at `/images`.

### SPA Routing
A before-handler redirects a request at `/<context>/...` to `/<context>/index.html` when its path is not a static file (judged by the extension), does not contain `/api/` and does not start with `/ws/`, `/health`, `/dispatcher`, `/web-cache/` or `/__dev/`.

### Sessions
The application sessions of the remote frontends are managed by `remote.host`. Their limits come from the `[server]` section of `application.toml`: `maxSessions` (concurrent sessions) and `sessionTtlSeconds` (how long an authenticated session survives a WebSocket disconnect).

### CORS
Configured in `[server]`: `cors.allowAll` reflects any origin (development); otherwise only `cors.allowedHosts` is accepted. `cors.allowCredentials` defaults to `true`.

### Development mode
With `server.devMode = true`, the server registers the development endpoints: live reload (`/__dev/ws`, `/__dev/notify`), database reset, heap and GC.

### Logging
SLF4J + Logback. `LogBootstrap` loads `{basedir}/config/logback.xml` (`work/config/logback.xml`), creating a default one if it does not exist.

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

Versions are managed by the `framework-dependencies` BOM.

## Deployment

The fat JAR runs on any environment with Java 25+. Run it from a directory that holds `config/application.toml`, or point `store.config.file` to it:

```bash
# Copy JAR to server
scp br.com.wdc.cube.backend-1.0.0.jar user@server:/opt/app/

# Run with systemd (example)
[Service]
WorkingDirectory=/opt/app
ExecStart=/usr/bin/java -jar /opt/app/br.com.wdc.cube.backend-1.0.0.jar
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

`BackendServer.start()` calls `BusinessContext.configure()` **before** `createJavalinApp()`, so `AuthenticationService.BEAN` is already populated when `RepositoryApiRoutes` decides whether to register the auth routes and the security filter. `BusinessContext.start()` runs after the app is created, and the server starts listening last.
