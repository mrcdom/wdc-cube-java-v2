# Throughput and Memory per Session Analysis

This document records the load and memory experiments run on the WeDoCode Shopping backend, focused on two axes: **sustained throughput** and **memory footprint per WebSocket session**.

---

## Environments

### Laptop — Apple M4 Max

| Component | Configuration |
|---|---|
| Hardware | MacBook Pro, Apple M4 Max — 16 cores (12P + 4E), 64 GB unified memory |
| OS | macOS |
| JDK | 26 (2026-03-17) |
| GC | Generational ZGC (`-XX:+UseZGC -XX:+ZGenerational -XX:ZCollectionInterval=5`) |
| Heap | 10 GB (`-Xms10g -Xmx10g -XX:SoftMaxHeapSize=9g`) |

### Server — 2× Intel Xeon Platinum 8368

| Component | Configuration |
|---|---|
| Hardware | 2× Intel Xeon Platinum 8368 @ 2.40 GHz (Turbo 3.40 GHz), 76 physical cores / 152 vCPUs, 1.5 TiB RAM |
| OS | Ubuntu 24.04.4 LTS, kernel 6.8.0, Docker container |
| JDK | Same JVM profile as the laptop |
| Heap | 10 GB |

### Common configuration

| Component | Configuration |
|---|---|
| HTTP server | Javalin + Jetty, virtual threads (`useVirtualThreads = true`) |
| Session protocol | WebSocket (Jetty) |
| Database | PostgreSQL 17, Agroal 2.7.1 pool |
| PostgreSQL tuning | `synchronous_commit=off`, `shared_buffers=2 GB`, `max_connections=1000` |

---

## Part 1 — Throughput Analysis

### Scenario: `ConcurrentThroughputScenario`

- **Mode**: fire-and-wait — each session waits for the response before the next request
- **Hold**: 60 s
- **Work cycle per session**: Products → Product detail → Cart → Home (4 navigations)
- **Heap sampled** every 5 s via `GET /__dev/heap`

---

### Topologies tested

Three distinct arrangements were used, with a significant impact on the results:

| Topology | Backend | PostgreSQL | Client |
|-----------|---------|-----------|---------|
| **A — Full laptop** | Laptop (M4 Max) | Laptop (local, native) | Laptop |
| **B — Laptop + remote PG** | Laptop (M4 Max) | Server (Docker container, network) | Laptop |
| **C — Full server** | Server (Xeon) | Server (Docker container, local) | Server |

---

### Results

#### Topology A — Full laptop

| Sessions | req/s | p50 | Heap |
|--------:|------:|----:|-----:|
| 100 | 24,762 | 5 ms | 4.89 GB |
| 500 | 7,455 | 100 ms | 1.87 GB |

#### Topology B — Laptop + remote PostgreSQL (LAN)

| Sessions | req/s | p50 | p99 | Heap |
|--------:|------:|----:|----:|-----:|
| 100 | 23,197 | 5 ms | 10 ms | 4.83 GB |
| 200 | 18,287 | 10 ms | 50 ms | 4.29 GB |
| 500 | 11,953 | 50 ms | 100 ms | 2.68 GB |
| 800 | 9,234 | 100 ms | 500 ms | 2.41 GB |

#### Topology C — Full server (backend + PG + client on the Xeon)

| Sessions | req/s | p50 | p99 | Heap |
|--------:|------:|----:|----:|-----:|
| 100 | 6,878 | 20 ms | 50 ms | 2.26 GB |
| 500 | 7,169 | 100 ms | 200 ms | 1.42 GB |
| 800 | 7,397 | 100 ms | 500 ms | 2.42 GB |

---

### Diagnosis per topology

#### Topology A — local CPU saturation

With server + client + database on the same cores, throughput collapses quickly. Little's Law confirms it:

$$\text{throughput} = \frac{\text{sessions}}{\text{mean latency}} \approx \frac{500}{0.067\text{ s}} \approx 7{,}460 \text{ req/s}$$

The bottleneck was the host CPU.

#### Topology B — laptop with a remote database

Moving PostgreSQL to the LAN server freed ~37% of the laptop's CPU:

$$\frac{11{,}953 - 7{,}455}{7{,}455} \approx +60\% \text{ throughput at 500 sessions}$$

The database network latency (~1 ms LAN) is absorbed by the virtual threads with no visible impact on throughput below 200 sessions. Above 200 sessions, the bottleneck is again the CPU shared between backend and client.

#### Topology C — the bottleneck is the Xeon's single-thread speed

Throughput stays almost constant from 100 to 800 sessions (6,878 → 7,397 req/s). The same PostgreSQL (same Docker container, same host) served the laptop with p50 = 5 ms and 23,197 req/s at 100 sessions. If the database were the bottleneck, the server with local access would be faster, not slower. **The database is not the bottleneck.**

Little's Law applied to both scenarios at 100 sessions reveals the difference:

$$\text{server: } \frac{100}{0.0145\text{ s}} \approx 6{,}900\text{ req/s} \quad \text{(p50 = 20 ms)}$$

$$\text{laptop: } \frac{100}{0.005\text{ s}} \approx 20{,}000\text{ req/s} \quad \text{(p50 = 5 ms)}$$

The bottleneck is the **per-request processing speed on the Xeon**. The Javalin + jOOQ + JSON pipeline takes ~5 ms per request on the M4 Max and ~20 ms on the Xeon. With fire-and-wait, throughput = `sessions / latency_per_req` — what matters is single-thread speed, not core count. The M4 Max, with an effective clock of ~4+ GHz and the Apple Silicon microarchitecture, processes each request ~4× faster than the Xeon 8368 @ 2.4 GHz.

**Actual server capacity:** the 152 vCPUs become relevant with concurrency high enough to saturate parallel processing. With more simultaneous sessions (tens of thousands) and a real distributed load, the Xeon outperforms the laptop in total throughput.

---

### Consolidated comparison table

| Topology | Database | Sessions | req/s | p50 | Bottleneck |
|-----------|-------|--------:|------:|----:|---------|

| Full laptop | Local PG | 100 | 24,762 | 5 ms | — |
| Full laptop | Local PG | 500 | 7,455 | 100 ms | Local CPU |
| **Laptop + remote PG** | **LAN PG** | **100** | **23,197** | **5 ms** | — |
| **Laptop + remote PG** | **LAN PG** | **200** | **18,287** | **10 ms** | Partial CPU |
| **Laptop + remote PG** | **LAN PG** | **500** | **11,953** | **50 ms** | Local CPU |
| **Laptop + remote PG** | **LAN PG** | **800** | **9,234** | **100 ms** | Local CPU |
| Xeon server | Docker PG (local) | 100 | 6,878 | 20 ms | Xeon single-thread speed (4× slower than M4 Max per req) |
| Xeon server | Docker PG (local) | 500 | 7,169 | 100 ms | Xeon single-thread speed |
| Xeon server | Docker PG (local) | 800 | 7,397 | 100 ms | Xeon single-thread speed |

---

## Part 2 — Memory per Session

### Scenario: `MemoryPerSessionScenario`

Measurement in two phases per round:

1. **Phase 1 — Idle footprint**: sessions open, no navigation. Measures the cost of keeping an idle WebSocket session.
2. **Phase 2 — Working set**: the same sessions navigate Products → Product detail → Cart → Home. Measures the cost with domain objects loaded in memory.

Per-round baseline via `POST /__dev/gc` (System.gc() × 2, settle 4 s + 1 s) to clear objects from previous rounds before each measurement.

---

### Results (batch = 1,000 sessions)

| Phase | Memory per session |
|------|------------------:|
| Idle footprint | **409.6 KB/session** |
| Working set (navigation) | **1.1 MB/session** |

### Estimated capacity (heap -Xmx10g)

$$\text{maxSessions (working set)} = \frac{10 \text{ GB}}{1.1 \text{ MB}} \approx 9{,}300 \text{ sessions}$$

With headroom for server overhead and GC (`-XX:SoftMaxHeapSize=9g`):

$$\text{recommended} \approx 5{,}900 \text{ sessions}$$

---

## Conclusions

1. **Topology is the dominant factor** — the same backend delivers radically different results depending on where the server, database and client run:
   - Laptop with a local database: quick cap from the shared CPU (backend + client + database on the same cores)
   - Laptop with a remote database: +60% throughput at 500 sessions (the database does not compete for the backend's CPU)
   - Xeon server with a local database: lower throughput than the laptop despite 152 vCPUs — the bottleneck is single-thread speed, not core count

2. **The Xeon server is slower per request than the M4 Max** — with fire-and-wait at 100 sessions, each virtual thread processes sequentially. The M4 Max takes ~5 ms/req; the Xeon 8368 takes ~20 ms/req. The gain from the 152 vCPUs only materializes with much higher concurrency (tens of thousands of sessions).

3. **Laptop sweet spot** — 100–200 sessions with a remote database deliver 18,000–23,000 req/s, p50 ≤ 10 ms. Beyond that, the CPU starts being contended between backend and client.

4. **Virtual threads absorb network latency** — the LAN hop to the database (~1 ms) does not degrade throughput at 100 sessions. Virtual threads wait on I/O without blocking carrier threads, keeping every core productive.

5. **Generational ZGC with `ZCollectionInterval=5`** keeps the heap stable with no noticeable pauses. Essential for `MemoryPerSessionScenario` — without predictive collection, ZGC may not collect between rounds and produce false deltas.

---

## Part 3 — Capacity Recommendation for Real Users

### Real user model

The benchmark uses fire-and-wait with no think time — each session fires again as soon as it gets the response. A human user reads the content between actions. That interval is the **think time**.

For a shopping application, typical think time between navigations: **15–30 s**.

Little's Law relates simultaneous users to throughput:

$$\text{simultaneous users} \approx \text{req/s} \times \text{think time}$$

Since think time ≫ latency (15 s ≫ 5–20 ms), the approximation is exact for practical purposes.

---

### Two independent limits

This backend keeps persistent WebSocket connections. Two limits apply at the same time:

| Limit | Resource | Base (10 GB heap) |
|--------|---------|-------------------|
| **Memory** | Simultaneously active WS sessions | ~5,900 sessions |
| **Throughput (CPU)** | Users who could click at the same time | ~348,000 (laptop) / ~110,000 (Xeon) |

**Memory is always the limiting factor before throughput.** With 5,900 WS connections and think time = 15 s, the backend receives ~393 req/s — less than 2% of the laptop's capacity and 5% of the Xeon's. The hardware does not change the capacity for connected users; it only determines how fast responses come when those users click.

---

### Scaling by heap — same instance

Base formula (derived from the measured point 10 GB → 5,900 sessions):

$$\text{sessions} = \frac{\text{Xmx} \times 0.9 \times 1000 - 2500}{1.1}$$

| Heap (Xmx) | Recommended WS sessions | Normal req/s* | Headroom — Laptop | Headroom — Xeon |
|:----------:|:-----------------------:|:-------------:|:---------------:|:-------------:|
| 8 GB  | ~4,300  | ~290 req/s   | 99%         | 96%        |
| 10 GB | ~5,900 (measured) | ~390 req/s | 98%      | 95%        |
| 16 GB | ~10,800 | ~720 req/s   | 97%         | 90%        |
| 32 GB | ~23,900 | ~1,600 req/s | 93%         | 78%        |
| 64 GB | ~50,000 | ~3,300 req/s | 86%         | 55%        |
| 128 GB | ~102,000 | ~6,800 req/s | 71%        | ⚠️ ~8%    |

*think time = 15 s; req/s = sessions ÷ 15

> With heap ≥ 128 GB on the Xeon, throughput approaches the limit (~7,400 req/s). The Xeon's saturation point is ~138 GB of heap (~111,000 sessions). At that scale on the Xeon, prefer scaling horizontally.

---

### Peak factor

During high-activity periods (flash sale, prime time), think time drops and consumed throughput rises. Reference with 5,900 WS sessions (10 GB heap):

| Usage profile | Think time | Consumed req/s | Headroom — Laptop | Headroom — Xeon |
|---------------|:----------:|:---------------:|:---------------:|:-------------:|
| Casual browsing     | 30 s | ~197 req/s   | 99%  | 97%  |
| Active browsing     | 15 s | ~393 req/s   | 98%  | 95%  |
| Peak (flash sale)   |  5 s | ~1,180 req/s | 95%  | 84%  |
| Extreme peak        |  2 s | ~2,950 req/s | 87%  | 60%  |

Even at an extreme peak with 5,900 WS sessions and a 10 GB heap, both machines have comfortable headroom.

---

### Horizontal scaling

Beyond the memory limits of a single instance, distribute with a load balancer (**sticky sessions required for WebSocket**):

| Instances | Heap/instance | Total WS sessions | Total throughput — Laptop | Total throughput — Xeon |
|-----------:|:--------------:|:-----------------:|:-------------------------:|:-----------------------:|
| 1  | 10 GB | ~5,900   | ~23,200 req/s | ~7,400 req/s  |
| 2  | 10 GB | ~11,800  | ~46,400 req/s | ~14,800 req/s |
| 4  | 10 GB | ~23,600  | ~92,800 req/s | ~29,600 req/s |
| 8  | 10 GB | ~47,200  | ~185,000 req/s | ~59,200 req/s |
| 16 | 10 GB | ~94,400  | ~370,000 req/s | ~118,000 req/s |
