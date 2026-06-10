# remote.shell.probe

Probe Java para execução de cenários sobre o **remote.host** da aplicação **WeDoCode Shopping**.

## Propósito

Executa cenários programáticos contra o host via protocolo de apresentação remota. Serve para:

- **Benchmarks de throughput** — mede quantas requisições por segundo o host suporta com múltiplas sessões paralelas
- **Medição de memória** — quantifica o consumo de memória por sessão ativa
- **Validação comportamental** — verifica que fluxos completos (ex: comprar produto) funcionam corretamente via protocolo

Não é um thin client de renderização — não exibe UI. É uma ferramenta de observabilidade e teste da camada de protocolo.

## Cenários

| Classe | Tipo | Descrição |
|--------|------|-----------|
| `ConcurrentThroughputScenario` | Benchmark | Dispara N sessões paralelas e mede throughput agregado |
| `MemoryPerSessionScenario` | Benchmark | Abre K sessões e mede o heap por sessão |
| `ComprarProdutoScenario` | Behavioral | Fluxo completo: login → home → produto → carrinho → recibo |
| `ScenarioAssert` | Utilitário | Asserções para validação de estados de view |

## Pré-requisitos

- **Java 21**
- **Maven 3.9+**
- Backend rodando (porta 8080 por padrão)

## Execução

```bash
# Throughput concorrente
mvn exec:java -Dexec.mainClass="br.com.wdc.shopping.view.remote.shell.probe.ConcurrentThroughputScenario"

# Memória por sessão
mvn exec:java -Dexec.mainClass="br.com.wdc.shopping.view.remote.shell.probe.MemoryPerSessionScenario"

# Exemplo básico
mvn exec:java -Dexec.mainClass="br.com.wdc.shopping.view.remote.shell.probe.ExampleMain"
```

## Resultados de benchmark

Resultados históricos estão em `src/docs/benchmarks/`:

- `concurrent-throughput-laptop.txt` — resultados em laptop de desenvolvimento
- `concurrent-throughput-server.txt` — resultados em servidor de produção

## Dependências

- `remote.bridge.java` (framework) — base para conexão WebSocket com o host
