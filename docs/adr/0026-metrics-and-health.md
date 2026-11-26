# 0026. Metrics with Micrometer in Prometheus format, behind a token; live and ready health checks

- **Date:** 2026-11-26
- **Status:** Accepted

## Context

The backend gave no picture of itself. `/health` always answered `UP`, whether or not the database was reachable, and nothing reported the sessions, the sign-ins, the remote views or the remote transactions — the framework counted the transactions (`RemoteTransactionCoordinator.stats()`), but no one read them.

## Decision

**Micrometer, read by Prometheus.** The backend keeps a `PrometheusMeterRegistry` (`Observability`) and adds it to Micrometer's global registry. It records the JVM; every HTTP request by method, status and the route **as declared** (tag `route`; requests that match no route share `unmatched`, so the series stay bounded); and the WDC Store's own meters — sign-ins by outcome (`ok`, `refused`, `throttled`), sessions live and renewed, remote view sessions and each client message they handle, and the REST API's remote transactions.

**The framework modules take no metrics library.** They expose what they know and the backend reads it: `RemoteTransactionCoordinator.stats()`, and the new `RemoteApplicationRegistry.onMessage`, told by the transport after each client message. What the store's lower layer counts (`persistence.impl`, the sign-ins) goes to Micrometer's global registry, a no-op until a host adds a registry to it.

**`/metrics` on the public port, behind a token** (`[management] token`, `Authorization: Bearer`, compared in constant time). Without the token in the configuration, the route does not exist.

**Two health checks, open:** `/health/live` (the process answers; `/health` stays, as its alias) and `/health/ready` (the database answers a connection check within 2 s; 503 when it does not).

## Consequences

- Any Prometheus-compatible system can read the backend; another metrics backend (OTLP, Datadog…) means swapping the registry, not the meters.
- Each instance reports its own numbers; gauges such as `store_sessions_live` are per instance by nature.
- The token travels on every scrape: over a network that is not private, the backend has to be behind TLS, as for the API's tokens.
- A load balancer can take out an instance whose database is gone.

## Alternatives considered

- **OpenTelemetry (OTLP push, metrics and traces):** the most complete, but nothing is visible without a collector, and it is heavier for a reference project; Micrometer can export to OTLP later without touching the meters.
- **A separate management port** bound to the local interface: no token needed, but a second listener to configure and route.
- **Micrometer in the framework modules:** a dependency for every consumer of the published framework, for meters it may not want.
