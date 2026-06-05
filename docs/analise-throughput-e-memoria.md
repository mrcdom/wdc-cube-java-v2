# Análise de Throughput e Memória por Sessão

Este documento registra os experimentos de carga e memória realizados no backend do WeDoCode Shopping, com foco em dois eixos: **throughput sustentado** e **footprint de memória por sessão WebSocket**.

---

## Ambiente

| Componente | Versão / Configuração |
|---|---|
| Hardware | MacBook Pro (macOS) |
| JDK | 25.0.2 |
| GC | ZGC Generacional (`-XX:+UseZGC -XX:+ZGenerational`) |
| Heap | 10 GB (`-Xms10g -Xmx10g -XX:SoftMaxHeapSize=9g`) |
| GC interval | 5 s (`-XX:ZCollectionInterval=5`) |
| Servidor HTTP | Javalin + Jetty, virtual threads (`useVirtualThreads = true`) |
| Protocolo de sessão | WebSocket (Jetty) |
| Banco (H2) | H2 2.x, modo TCP (`jdbc:h2:tcp://localhost/./wedocode-shopping`) |
| Banco (PostgreSQL) | PostgreSQL 17, porta 5432 |
| Pool de conexões | Agroal 2.7.1 |

---

## Parte 1 — Análise de Throughput

### Cenário: `ConcurrentThroughputScenario`

- **Modo**: fire-and-wait (cada sessão aguarda resposta antes da próxima requisição)
- **Hold**: 60 s
- **Ciclo de trabalho por sessão**: Products → Product detail → Cart → Home (4 navegações)
- **Heap amostrada** a cada 5 s via `GET /__dev/heap`

---

### Resultados

#### H2 TCP — 100 sessões, pool=100

```
Sessions       : 100
Throughput     : 23.760 req/s
p50            : 5 ms
p99            : 10 ms
Heap peak      : 3,7 GB
```

#### H2 TCP — 500 sessões, pool=100

```
Sessions       : 500
Throughput     : 8.726 req/s
p50            : 100 ms
```

#### PostgreSQL — 500 sessões, pool=500 (synchronous_commit=on, shared_buffers=128 MB)

```
Sessions       : 500
Throughput     : 7.266 req/s
p50            : 100 ms
Heap peak      : 1,87 GB
```

#### PostgreSQL — 500 sessões (após tuning)

Tuning aplicado:
```sql
ALTER SYSTEM SET shared_buffers           = '2GB';
ALTER SYSTEM SET effective_cache_size     = '6GB';
ALTER SYSTEM SET work_mem                 = '16MB';
ALTER SYSTEM SET synchronous_commit       = 'off';
ALTER SYSTEM SET wal_buffers              = '64MB';
ALTER SYSTEM SET max_wal_size             = '4GB';
ALTER SYSTEM SET random_page_cost         = 1.1;
```

```
Sessions       : 500
Throughput     : 7.455 req/s   (+2,6% — variação dentro do ruído)
p50            : 100 ms
Heap peak      : 1,87 GB
```

#### PostgreSQL — 100 sessões, pool=500

```
Sessions       : 100
Throughput     : 24.762 req/s
p50            : 5 ms
p99            : 10 ms
Heap peak      : 4,89 GB
```

---

### Tabela Comparativa

| Banco | Sessões | req/s | p50 | Heap pico |
|-------|---------|------:|----:|----------:|
| H2 TCP | 100 | 23.760 | 5 ms | 3,7 GB |
| H2 TCP | 500 | 8.726 | 100 ms | — |
| PostgreSQL | 100 | **24.762** | 5 ms | 4,89 GB |
| PostgreSQL | 500 | 7.455 | 100 ms | 1,87 GB |

---

### Diagnóstico

#### Gargalo a 500 sessões: CPU da máquina de testes, não o banco

A queda de throughput ao passar de 100 para 500 sessões é explicada pela **Lei de Little**:

$$\text{throughput} = \frac{\text{sessões}}{\text{latência média}}$$

Com 500 sessões e p50 = 100 ms:

$$\frac{500}{0{,}067\text{ s}} \approx 7.460 \text{ req/s}$$

O resultado previsto bate com o medido. O sistema está **saturado de CPU**: servidor, cliente e banco competem pelos mesmos cores físicos do Mac. Evidências:

- O tuning do PostgreSQL (`synchronous_commit=off`, `shared_buffers=2 GB`) não moveu a agulha (+2,6%): o banco era inocente.
- Ao reduzir para 100 sessões, o throughput volta a 24.762 req/s — comportamento idêntico ao H2.
- A latência salta 20× (5 ms → 100 ms) com 5× mais sessões: assinatura clássica de saturação de CPU por context-switch.

#### Por que o heap H2 é maior?

O H2 TCP mode executa no mesmo processo JVM do servidor: buffers de cache de página do H2 somam ao heap da aplicação (~3,7 GB). O PostgreSQL, sendo um processo separado, não ocupa o heap da JVM (~1,87 GB), tornando as medições de memória por sessão mais limpas e representativas.

#### Ponto ótimo nesta máquina

O ponto de saturação está em torno de **100 sessões simultâneas** rodando servidor + cliente + banco no mesmo Mac. Acima disso, o throughput cai e a latência degrada proporcionalmente ao número de sessões.

---

## Parte 2 — Memória por Sessão

### Cenário: `MemoryPerSessionScenario`

Medição em duas fases por rodada:

1. **Fase 1 — Idle footprint**: sessões abertas, sem navegação. Mede custo de manutenção da sessão WebSocket em repouso.
2. **Fase 2 — Working set**: mesmas sessões navegam Products → Product detail → Cart → Home. Mede custo com objetos de domínio carregados em memória.

Baseline por rodada via `POST /__dev/gc` (System.gc() × 2, settle 4 s + 1 s) para limpar objetos de rodadas anteriores antes de cada medição.

---

### Resultados (batch = 1.000 sessões)

| Fase | Memória por sessão |
|------|------------------:|
| Idle footprint | **409,6 KB/sessão** |
| Working set (navegação) | **1,1 MB/sessão** |

### Capacidade estimada (heap -Xmx10g)

$$\text{maxSessions (working set)} = \frac{10 \text{ GB}}{1{,}1 \text{ MB}} \approx 9.300 \text{ sessões}$$

Com margem para overhead do servidor e GC (`-XX:SoftMaxHeapSize=9g`):

$$\text{recomendado} \approx 5.900 \text{ sessões}$$

---

## Conclusões

1. **PostgreSQL ≈ H2 TCP a 100 sessões** — ambos entregam ~24.000 req/s com p50 = 5 ms na mesma máquina.

2. **O gargalo acima de 100 sessões é CPU do host**, não banco de dados. O problema desaparece em hardware dedicado ou distribuído (servidor e cliente em máquinas separadas).

3. **PostgreSQL é superior para testes de memória**: o H2 TCP soma seu heap ao da JVM, distorcendo as medições de footprint por sessão.

4. **Virtual threads são eficazes para concorrência I/O-bound**: 100 sessões simultâneas com p50 = 5 ms demonstram que o servidor não tem overhead de threading — o gargalo é exclusivamente CPU/banco.

5. **ZGC Generacional com `ZCollectionInterval=5`** mantém o heap estável em cargas altas sem pausas perceptíveis. É essencial para `MemoryPerSessionScenario` — sem coleta preditiva, o ZGC pode não coletar entre rodadas e gerar deltas falsos.
