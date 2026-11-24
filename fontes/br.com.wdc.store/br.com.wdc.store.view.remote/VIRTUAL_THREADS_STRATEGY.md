# Virtual Threads Strategy — Remote Host

## Overview

The backend that hosts the remote presentation (`br.com.wdc.cube.backend` + `remote.host` + `framework.cube.remote/remote.backend`) runs on **Virtual Threads**: Jetty serves HTTP requests and WebSocket messages on virtual threads, and all background work of the remote sessions runs on a single scheduled executor backed by a virtual-thread factory. No platform thread pool is created by the application.

| Component | Version |
|---|---|
| Java | 25 (`maven.compiler.release` of the backend and demos) |
| Javalin | 7.2.4 (Jetty 12) |

## Where the threads come from

### 1. HTTP and WebSocket — `BackendServer`

`BackendServer.createJavalinApp()` turns on Javalin's virtual-thread support and sets the WebSocket idle timeout:

```java
return Javalin.create(config -> {
    // Javalin 7+ built-in virtual thread support for request handling.
    config.concurrency.useVirtualThreads = true;

    // Configure WebSocket idle timeout at the WebSocket container level.
    config.jetty.modifyWebSocketServletFactory(wsFactory -> wsFactory.setIdleTimeout(Duration.ofMinutes(2)));
    // ...
});
```

Every REST request and every WebSocket message (`DispatcherController`, `/dispatcher/{appId}`) is handled on a virtual thread. A submit is processed on that thread: it dispatches the event to the scope's skeleton, runs the presenter's action (and the repository calls it makes), and writes the response with the dirty states. The connection itself is kept alive by Javalin's automatic WebSocket pings, enabled per session in `DispatcherController` (`ctx.enableAutomaticPings(15, TimeUnit.SECONDS)`).

### 2. Background work — `BusinessContext` and `ScheduledExecutor.BEAN`

`BusinessContext.configure` creates one scheduled executor whose threads are virtual, and publishes it, through `ScheduledExecutorAdapter`, as the application's `ScheduledExecutor.BEAN`:

```java
var scheduledExecutor = createScheduledExecutor();
ScheduledExecutor.BEAN.set(new ScheduledExecutorAdapter(scheduledExecutor));
cleanUp.push(() -> {
    scheduledExecutor.close();
    ScheduledExecutor.BEAN.set(null);
});
```

```java
private ScheduledExecutorService createScheduledExecutor() {
    return java.util.concurrent.Executors
            .newScheduledThreadPool(1, VirtualThreadFactory.ofVirtual("ScheduledTasks"));
}
```

`VirtualThreadFactory.ofVirtual(prefix)` is `Thread.ofVirtual().name(prefix + "-", 0).factory()`: each scheduled task runs on a new virtual thread named `ScheduledTasks-<n>`.

Everything scheduled through `ScheduledExecutor.BEAN` uses it — the remote session registry (below) and the presenters' delayed work (e.g. `CartPresenter` clearing an error message after 3 seconds).

### 3. Remote sessions — `RemoteApplicationRegistry`

The registry schedules its periodic tasks on `ScheduledExecutor.BEAN` when it starts:

| Task | Interval | What it does |
|---|---|---|
| `executeBackgroundFlush` | 50 ms (`FLUSH_INTERVAL`) | Drains the queue of sessions with dirty views and sends their states (`flushDirtyViewsFromRegistry`) |
| `removeExpireds` | 30 s (`EXPIRY_CHECK_INTERVAL`) | Releases the expired sessions that have no WebSocket; extends the life of the ones still connected |

While a submit is being processed, a view marked dirty asks for an immediate flush (`triggerImmediateFlush`), which runs through `ScheduledExecutor.BEAN.execute(...)` (rate-limited to one wake-up every 10–16 ms per session).

The session limits come from `application.toml` (`RemoteHostBootstrap`): `server.maxSessions` (`-1` = unlimited) and `server.sessionTtlSeconds` (`0` = release on disconnect; default TTL 3 min).

## Synchronization

- `RemoteApplicationSupport` guards a session's client→server state sync, navigation (`safeGo`), alerts and the `commitComputedState` pass with a `ReentrantLock`.
- `CubeApplication` guards its live views with short `synchronized` blocks.
- On Java 25, a virtual thread that blocks inside `synchronized` releases its carrier thread (JEP 491), so neither form pins a carrier thread.

## Cryptography

The handshake work runs on the same virtual threads as the request that needs it: the RSA decryption and the PBKDF2 derivation (250,000 iterations, `RemoteDataSecurity.updateSecret`) when a session receives its `secret`, and the AES-GCM operations on the storage values and access tokens. There is no separate executor for them.

## Monitoring

Use Java Flight Recorder:

```bash
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
     -jar fontes/br.com.wdc.cube.backend/target/br.com.wdc.cube.backend-1.0.0.jar 8080
```

What to watch:

- Carrier (platform) thread count stays small while virtual threads follow the number of requests in flight.
- `jdk.VirtualThreadPinned` events (none are expected).
- Heap per session — see [throughput-and-memory-analysis.md](../../../docs/throughput-and-memory-analysis.md), measured with the `remote.shell.probe` scenarios.
