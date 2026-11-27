# 0030. A compose setup with two instances, its image made from what Maven built

- **Date:** 2026-11-27
- **Status:** Accepted

## Context

With the sessions in the database and the mesh between instances ([0029](0029-sessions-in-the-database-and-a-mesh-between-instances.md)), the backend can run as several instances behind one address. [running-several-instances.md](../running-several-instances.md) says what they share, what each keeps in memory and how to route them, but nothing ran it all together: the database, the mesh, a proxy with the routing rules, and the metrics.

## Decision

**A compose setup in the repository** (`fontes/br.com.wdc.store/br.com.wdc.store.docker`, not a Maven module), with five services:

- PostgreSQL;
- two backend instances on their mesh;
- nginx in front: a tab's WebSocket by the session id in its path, an API client by `X-Client-Id`, and the `/api/auth` limit per address;
- Prometheus, scraping both instances with the management token.

**The image takes what the build made.**

- `prepare.sh` builds the backend with Maven and the web frontends, and stages in `build/context` the fat JAR, `work/frontend` and the web components bundle of the web cache.
- The `Dockerfile` copies them onto a Java 25 JRE.
- The configuration, secrets included, comes from the environment at start (`entrypoint.sh` writes `application.toml`), so no secret is in the image. The secrets live in a local `.env`.

**`check.py` checks it from outside:** readiness, the mesh connected, a sign-in through nginx whose token works on both instances, a sign-out on one instance refused by the other, a challenge from one instance answered on the other, an image changed on one served changed by the other, nginx's limit, and Prometheus's targets.

Running it exposed one thing the backend lacked: behind a proxy, the browsers' address was not an allowed CORS origin, and the module scripts, which send their `Origin` even to their own server, got 400. A request whose `Origin` names the host and port it was sent to is now taken as the server's own (`SameOriginFilter`), at whatever address the server is reached.

## Consequences

- The routing rules of the guide are the ones nginx runs here, not a sketch.
- The image is small, with no JDK, Maven, Node or Flutter, but `docker build` alone does not build it: `prepare.sh` comes first, and needs the project's toolchain.
- The setup is a reference to start from, not a production deployment: no TLS, the instances' ports open for the checks, secrets in a local file.
- The check runs by hand; the repository has no CI workflow to run it on every push.

## Alternatives considered

- **Everything built inside Docker** (multi-stage): self-contained, but the build needs Maven, Node, Flutter and the TeaVM toolchain in the image, which makes it heavy and slow.
- **The files as documentation, untested:** less work, but a proxy configuration that never ran is exactly what the run corrected.
