# The WDC Store in Docker

Two backend instances behind nginx, as [running-several-instances.md](../../../docs/running-several-instances.md) describes:

| Service | What | Port on the host |
|---|---|---|
| `postgres` | PostgreSQL 18, the shared database | — |
| `store-a`, `store-b` | the backend, on its mesh (`cluster.peers`) | 18081, 18082 (direct, for checks) |
| `nginx` | the one address: affinity (a tab's socket by session id, an API client by `X-Client-Id`) and the `/api/auth` limit per address | **18080** |
| `prometheus` | scrapes both instances' `/metrics` with the management token | 19090 |

It is not a Maven module: nothing in the reactor builds it.

## Running it

```bash
cp .env.example .env    # fill in the four secrets (the file says how to make each one)
./prepare.sh            # builds the backend (Maven, Java 25) and the web frontends, and stages them in build/context
docker compose up -d --build --wait
python3 check.py        # checks it end to end
```

Then open http://localhost:18080. The landing page links every web frontend. Sign in as one of the demo users: `admin`/`admin`, `jdoe`/`jdoe` or `jroe`/`jroe`.

`prepare.sh --skip-frontends` builds only the backend and takes `work/frontend` as it is. `prepare.sh --skip-build` builds nothing, and stages the JAR and `work/frontend` as they are.

`docker compose down` stops it; add `-v` to drop the database as well.

## How it is built

- **The image takes what the build made.** It contains the backend's fat JAR and the web frontends of `work/frontend`, which the [`Dockerfile`](Dockerfile) copies onto a Temurin 25 JRE. The web cache (fonts, icons) is fetched from its CDNs on first use. There is no JDK, Maven, Node or Flutter in it.
- **Configuration comes from the environment.** At start, [`entrypoint.sh`](entrypoint.sh) writes the work directory's `config/application.toml` from the environment, so no secret goes into the image. Logs go to the console ([`logback.xml`](logback.xml)), where `docker compose logs` reads them.
- **The secrets live in `.env`,** which git ignores and `.dockerignore` keeps out of the build context. Prometheus reads the token as a compose secret.
- **Every instance has the same secrets.** They share `security.jwt.secret`, so each one accepts the others' tokens and the mesh connects. They share `remote.signingKey`, so a page served by one instance and its socket on the other accept the same session id.
- **The browsers' address needs no setting.** A request from a page served by the backend names, in its `Origin`, the host and port it was sent to (nginx passes the `Host` it got), and the backend takes it as its own. `STORE_CORS_ALLOWED_HOSTS` lists other origins, whose pages call this API from elsewhere.
- **Startup order:** the first instance creates the tables and loads the demo data on an empty database, and the second waits for it to be healthy.
- **The backends' own `/api/auth` limit is off** (`auth.requestsPerMinute = 0`). Behind a proxy every client comes from the proxy's address, so nginx does the limiting, in [`nginx.conf`](nginx.conf).

## What `check.py` checks

It needs only Python's standard library, and prints PASS or FAIL for each check. The exit code is 1 when any check failed.

1. nginx answers, and both instances are ready.
2. Each instance sees the other on the mesh.
3. A sign-in through nginx works (SCRAM-SHA-256, as the store's clients do it), and its token works on both instances.
4. After a sign-out on one instance, both instances refuse the token.
5. A challenge asked of one instance is answered on the other.
6. A product image changed on one instance is served changed by the other.
7. nginx answers 429 to a burst on `/api/auth`.
8. Prometheus scrapes both instances.

The addresses can be changed with `STORE_NGINX`, `STORE_A`, `STORE_B` and `STORE_PROMETHEUS`.

The CI workflow (`.github/workflows/ci.yml`) runs the same on every push and pull request to `main`: it stages the context from the build, generates fresh secrets, brings the setup up and runs `check.py`.

## A reference, not a deployment

The setup is a reference to start from, not a production deployment:
- there is no TLS, so put the mesh's addresses on `wss://` outside a private network;
- the instances' ports are open, for the checks;
- the secrets are in a local file.

The images are pinned to exact versions: `eclipse-temurin:25-jre-noble`, `postgres:18.1`, `nginx:1.29.3` and `prom/prometheus:v3.7.3`.
