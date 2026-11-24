# remote.shell.probe

Java probe for running scenarios against the **remote.host** of the **WDC Store** application.

## Purpose

Runs programmatic scenarios against the host through the remote presentation protocol. It is used for:

- **Throughput benchmarks** — measures how many requests per second the host handles with multiple parallel sessions
- **Memory measurement** — quantifies memory consumption per active session
- **Behavioral validation** — verifies that complete flows (e.g. buying a product) work correctly through the protocol

It is not a rendering thin client — it shows no UI. It is an observability and testing tool for the protocol layer.

## Scenarios

| Class | Type | Description |
|--------|------|-----------|
| `ConcurrentThroughputScenario` | Benchmark | Fires N parallel sessions and measures aggregate throughput |
| `MemoryPerSessionScenario` | Benchmark | Opens K sessions and measures heap per session |
| `BuyProductScenario` | Behavioral | Complete flow: login → home → product → cart → receipt |
| `ScenarioAssert` | Utility | Assertions for validating view states |

## Prerequisites

- **Java 25**
- **Maven 3.9+**
- Backend running (port 8080 by default)

## Running

```bash
# Concurrent throughput
mvn exec:java -Dexec.mainClass="br.com.wdc.store.view.remote.shell.probe.ConcurrentThroughputScenario"

# Memory per session
mvn exec:java -Dexec.mainClass="br.com.wdc.store.view.remote.shell.probe.MemoryPerSessionScenario"

# Basic example
mvn exec:java -Dexec.mainClass="br.com.wdc.store.view.remote.shell.probe.ExampleMain"
```

## Benchmark results

Historical results are in `src/docs/benchmarks/`:

- `concurrent-throughput-laptop.txt` — results on a development laptop
- `concurrent-throughput-server.txt` — results on a production server

## Dependencies

- `remote.bridge.java` (framework) — base for the WebSocket connection to the host
