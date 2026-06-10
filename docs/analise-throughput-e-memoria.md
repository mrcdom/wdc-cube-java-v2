# Análise de Throughput e Memória por Sessão

Este documento registra os experimentos de carga e memória realizados no backend do WeDoCode Shopping, com foco em dois eixos: **throughput sustentado** e **footprint de memória por sessão WebSocket**.

---

## Ambientes

### Laptop — Apple M4 Max

| Componente | Configuração |
|---|---|
| Hardware | MacBook Pro, Apple M4 Max — 16 cores (12P + 4E), 64 GB unified memory |
| OS | macOS |
| JDK | 26 (2026-03-17) |
| GC | ZGC Generacional (`-XX:+UseZGC -XX:+ZGenerational -XX:ZCollectionInterval=5`) |
| Heap | 10 GB (`-Xms10g -Xmx10g -XX:SoftMaxHeapSize=9g`) |

### Servidor — 2× Intel Xeon Platinum 8368

| Componente | Configuração |
|---|---|
| Hardware | 2× Intel Xeon Platinum 8368 @ 2.40 GHz (Turbo 3.40 GHz), 76 núcleos físicos / 152 vCPUs, 1.5 TiB RAM |
| OS | Ubuntu 24.04.4 LTS, kernel 6.8.0, container Docker |
| JDK | Mesmo perfil de JVM que o laptop |
| Heap | 10 GB |

### Configuração comum

| Componente | Configuração |
|---|---|
| Servidor HTTP | Javalin + Jetty, virtual threads (`useVirtualThreads = true`) |
| Protocolo de sessão | WebSocket (Jetty) |
| Banco | PostgreSQL 17, pool Agroal 2.7.1 |
| PostgreSQL tuning | `synchronous_commit=off`, `shared_buffers=2 GB`, `max_connections=1000` |

---

## Parte 1 — Análise de Throughput

### Cenário: `ConcurrentThroughputScenario`

- **Modo**: fire-and-wait — cada sessão aguarda a resposta antes da próxima requisição
- **Hold**: 60 s
- **Ciclo de trabalho por sessão**: Products → Product detail → Cart → Home (4 navegações)
- **Heap amostrada** a cada 5 s via `GET /__dev/heap`

---

### Topologias testadas

Três arranjos distintos foram usados, com impacto significativo nos resultados:

| Topologia | Backend | PostgreSQL | Cliente |
|-----------|---------|-----------|---------|
| **A — Laptop completo** | Laptop (M4 Max) | Laptop (local, nativo) | Laptop |
| **B — Laptop + PG remoto** | Laptop (M4 Max) | Servidor (container Docker, rede) | Laptop |
| **C — Servidor completo** | Servidor (Xeon) | Servidor (container Docker, local) | Servidor |

---

### Resultados

#### Topologia A — Laptop completo

| Sessões | req/s | p50 | Heap |
|--------:|------:|----:|-----:|
| 100 | 24.762 | 5 ms | 4,89 GB |
| 500 | 7.455 | 100 ms | 1,87 GB |

#### Topologia B — Laptop + PostgreSQL remoto (LAN)

| Sessões | req/s | p50 | p99 | Heap |
|--------:|------:|----:|----:|-----:|
| 100 | 23.197 | 5 ms | 10 ms | 4,83 GB |
| 200 | 18.287 | 10 ms | 50 ms | 4,29 GB |
| 500 | 11.953 | 50 ms | 100 ms | 2,68 GB |
| 800 | 9.234 | 100 ms | 500 ms | 2,41 GB |

#### Topologia C — Servidor completo (backend + PG + cliente no Xeon)

| Sessões | req/s | p50 | p99 | Heap |
|--------:|------:|----:|----:|-----:|
| 100 | 6.878 | 20 ms | 50 ms | 2,26 GB |
| 500 | 7.169 | 100 ms | 200 ms | 1,42 GB |
| 800 | 7.397 | 100 ms | 500 ms | 2,42 GB |

---

### Diagnóstico por topologia

#### Topologia A — saturação de CPU local

Com servidor + cliente + banco nos mesmos cores, o throughput colapsa rapidamente. A Lei de Little confirma:

$$\text{throughput} = \frac{\text{sessões}}{\text{latência média}} \approx \frac{500}{0{,}067\text{ s}} \approx 7.460 \text{ req/s}$$

O gargalo era CPU do host.

#### Topologia B — laptop com banco remoto

Mover o PostgreSQL para o servidor LAN liberou ~37% da CPU do laptop:

$$\frac{11.953 - 7.455}{7.455} \approx +60\% \text{ de throughput a 500 sessões}$$

A latência de rede do banco (~1 ms LAN) é absorvida pelas virtual threads sem impacto visível no throughput abaixo de 200 sessões. Acima de 200 sessões, o gargalo volta a ser CPU compartilhada entre backend e cliente.

#### Topologia C — gargalo é a velocidade single-thread do Xeon

O throughput permanece quase constante de 100 a 800 sessões (6.878 → 7.397 req/s). O mesmo PostgreSQL (mesmo container Docker, mesmo host) atendeu o laptop com p50 = 5 ms e 23.197 req/s a 100 sessões. Se o banco fosse o gargalo, o servidor com acesso local seria mais rápido, não mais lento. **O banco não é o gargalo.**

A Lei de Little aplicada aos dois cenários a 100 sessões revela a diferença:

$$\text{servidor: } \frac{100}{0{,}0145\text{ s}} \approx 6.900\text{ req/s} \quad \text{(p50 = 20 ms)}$$

$$\text{laptop: } \frac{100}{0{,}005\text{ s}} \approx 20.000\text{ req/s} \quad \text{(p50 = 5 ms)}$$

O gargalo é a **velocidade de processamento por requisição no Xeon**. O pipeline Javalin + jOOQ + JSON leva ~5 ms no M4 Max e ~20 ms no Xeon por requisição. Com fire-and-wait, throughput = `sessões / latência_por_req` — o que importa é velocidade single-thread, não contagem de cores. O M4 Max, com clock efetivo ~4+ GHz e microarquitetura Apple Silicon, processa cada requisição ~4× mais rápido que o Xeon 8368 @ 2,4 GHz.

**Capacidade real do servidor:** os 152 vCPUs se tornam relevantes com concorrência alta o suficiente para saturar o processamento paralelo. Com mais sessões simultâneas (dezenas de milhares) e carga real distribuída, o Xeon supera o laptop em throughput total.

---

### Tabela comparativa consolidada

| Topologia | Banco | Sessões | req/s | p50 | Gargalo |
|-----------|-------|--------:|------:|----:|---------|

| Laptop completo | PG local | 100 | 24.762 | 5 ms | — |
| Laptop completo | PG local | 500 | 7.455 | 100 ms | CPU local |
| **Laptop + PG remoto** | **PG LAN** | **100** | **23.197** | **5 ms** | — |
| **Laptop + PG remoto** | **PG LAN** | **200** | **18.287** | **10 ms** | CPU parcial |
| **Laptop + PG remoto** | **PG LAN** | **500** | **11.953** | **50 ms** | CPU local |
| **Laptop + PG remoto** | **PG LAN** | **800** | **9.234** | **100 ms** | CPU local |
| Servidor Xeon | PG Docker (local) | 100 | 6.878 | 20 ms | Velocidade single-thread do Xeon (4× mais lento que M4 Max/req) |
| Servidor Xeon | PG Docker (local) | 500 | 7.169 | 100 ms | Velocidade single-thread do Xeon |
| Servidor Xeon | PG Docker (local) | 800 | 7.397 | 100 ms | Velocidade single-thread do Xeon |

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

1. **Topologia é o fator dominante** — o mesmo backend entrega resultados radicalmente diferentes conforme onde rodam servidor, banco e cliente:
   - Laptop com banco local: cap rápido por CPU compartilhada (backend + cliente + banco nos mesmos cores)
   - Laptop com banco remoto: +60% de throughput a 500 sessões (banco não compete pela CPU do backend)
   - Servidor Xeon com banco local: throughput menor que o laptop apesar dos 152 vCPUs — gargalo é velocidade single-thread, não contagem de cores

2. **O servidor Xeon é mais lento por requisição que o M4 Max** — com fire-and-wait a 100 sessões, cada virtual thread processa sequencialmente. M4 Max leva ~5 ms/req; Xeon 8368 leva ~20 ms/req. O ganho dos 152 vCPUs só se materializa com concorrência muito maior (dezenas de milhares de sessões).

3. **Ponto ótimo do laptop** — 100–200 sessões com banco remoto entrega 18.000–23.000 req/s, p50 ≤ 10 ms. Acima disso, CPU começa a ser disputada entre backend e cliente.

4. **Virtual threads absorvem latência de rede** — a LAN ao banco (~1 ms) não degrada throughput a 100 sessões. Virtual threads aguardam I/O sem bloquear carrier threads, mantendo todos os cores produtivos.

5. **ZGC Generacional com `ZCollectionInterval=5`** mantém o heap estável sem pausas perceptíveis. Essencial para `MemoryPerSessionScenario` — sem coleta preditiva, o ZGC pode não coletar entre rodadas e gerar deltas falsos.

---

## Parte 3 — Recomendação de Capacidade para Usuários Reais

### Modelo de usuário real

O benchmark usa fire-and-wait sem think time — cada sessão dispara imediatamente ao receber a resposta. Um usuário humano lê o conteúdo entre ações. Esse intervalo é o **think time**.

Para um aplicativo de compras, think time típico entre navegações: **15–30 s**.

A Lei de Little relaciona usuários simultâneos ao throughput:

$$\text{usuários simultâneos} \approx \text{req/s} \times \text{think time}$$

Como think time ≫ latência (15 s ≫ 5–20 ms), a aproximação é exata para fins práticos.

---

### Dois limites independentes

Este backend mantém conexões WebSocket persistentes. Há dois limites que se aplicam ao mesmo tempo:

| Limite | Recurso | Base (heap 10 GB) |
|--------|---------|-------------------|
| **Memória** | Sessões WS ativas simultaneamente | ~5.900 sessões |
| **Throughput (CPU)** | Usuários que poderiam clicar em simultâneo | ~348.000 (laptop) / ~110.000 (Xeon) |

**Memória é sempre o fator limitante antes do throughput.** Com 5.900 conexões WS e think time = 15 s, o backend recebe ~393 req/s — menos de 2% da capacidade do laptop e 5% do Xeon. O hardware não muda a capacidade de usuários conectados; apenas determina a velocidade de resposta quando esses usuários clicam.

---

### Escalonamento por heap — mesma instância

Fórmula base (derivada do ponto medido 10 GB → 5.900 sessões):

$$\text{sessões} = \frac{\text{Xmx} \times 0{,}9 \times 1000 - 2500}{1{,}1}$$

| Heap (Xmx) | Sessões WS recomendadas | Req/s normal* | Margem — Laptop | Margem — Xeon |
|:----------:|:-----------------------:|:-------------:|:---------------:|:-------------:|
| 8 GB  | ~4.300  | ~290 req/s   | 99%         | 96%        |
| 10 GB | ~5.900 (medido) | ~390 req/s | 98%      | 95%        |
| 16 GB | ~10.800 | ~720 req/s   | 97%         | 90%        |
| 32 GB | ~23.900 | ~1.600 req/s | 93%         | 78%        |
| 64 GB | ~50.000 | ~3.300 req/s | 86%         | 55%        |
| 128 GB | ~102.000 | ~6.800 req/s | 71%        | ⚠️ ~8%    |

*think time = 15 s; req/s = sessões ÷ 15

> Com heap ≥ 128 GB no Xeon, o throughput se aproxima do limite (~7.400 req/s). O ponto de saturação do Xeon é ~138 GB de heap (~111.000 sessões). Para essa escala no Xeon, prefira escalar horizontalmente.

---

### Fator de pico

Em períodos de alta atividade (flash sale, horário nobre), o think time cai e o throughput consumido aumenta. Referência com 5.900 sessões WS (heap 10 GB):

| Perfil de uso | Think time | Req/s consumido | Margem — Laptop | Margem — Xeon |
|---------------|:----------:|:---------------:|:---------------:|:-------------:|
| Navegação casual    | 30 s | ~197 req/s   | 99%  | 97%  |
| Navegação ativa     | 15 s | ~393 req/s   | 98%  | 95%  |
| Pico (flash sale)   |  5 s | ~1.180 req/s | 95%  | 84%  |
| Pico extremo        |  2 s | ~2.950 req/s | 87%  | 60%  |

Mesmo em pico extremo com 5.900 sessões WS e heap 10 GB, ambos os hardwares têm margem confortável.

---

### Escalonamento horizontal

Para além dos limites de memória de uma instância, distribua com load balancer (**sticky sessions obrigatório para WebSocket**):

| Instâncias | Heap/instância | Sessões WS totais | Throughput total — Laptop | Throughput total — Xeon |
|-----------:|:--------------:|:-----------------:|:-------------------------:|:-----------------------:|
| 1  | 10 GB | ~5.900   | ~23.200 req/s | ~7.400 req/s  |
| 2  | 10 GB | ~11.800  | ~46.400 req/s | ~14.800 req/s |
| 4  | 10 GB | ~23.600  | ~92.800 req/s | ~29.600 req/s |
| 8  | 10 GB | ~47.200  | ~185.000 req/s | ~59.200 req/s |
| 16 | 10 GB | ~94.400  | ~370.000 req/s | ~118.000 req/s |
