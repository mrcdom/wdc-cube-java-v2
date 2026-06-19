# CLAUDE.md

Guia para trabalhar neste repositório. Para visão arquitetural completa, ver [README.md](README.md) e [docs/](docs/).

## O que é

**WDC Cube Java v2** — referência arquitetural do padrão **Cube MVP** (variação de MVP com presenters hierárquicos, navegação por intents e ViewStates serializáveis). Demonstra um e-commerce (Shopping) com backend Java 21 e **seis frontends independentes** (React, Flutter, Vaadin, SWT, Gluon, TeaVM) que compartilham os mesmos presenters/ViewStates — só a camada de renderização muda.

Idioma do projeto: **português** (docs, READMEs, mensagens de commit). Código e identificadores em inglês/português conforme o existente no arquivo.

## Layout

- `fontes/` — todo o código-fonte Maven (multi-módulo). **O build roda a partir daqui, não da raiz.**
  - `br.com.wdc.framework/` — framework reutilizável: `commons`, `domain` (abstrações genéricas: `Repository`, `Page`, `ModelCodec`, `Projection*`, exceções base, segurança/`PermissionModel`, `AppConfig`), `cube`, `cube.remote`, `jooq`, `dependencies` (BOM)
  - `br.com.wdc.shopping/` — app de exemplo: `domain`, `persistence` (agrupador de `persistence.impl` (jOOQ), `persistence.client` (HTTP/okhttp), `persistence.rest` (API Javalin)), `presentation`, `backend`, `scripts`, `tests`, `view.*`
- `work/` — diretório de runtime: `config/` (TOML), `bin/` (scripts de start), `data/`, `log/`, `frontend/`
- `docs/` — documentação arquitetural detalhada (PT)

## Build & Run

```bash
# Build (Java 21 obrigatório)
export JAVA_HOME=<jdk-21>     # ou defina JAVA21_HOME
cd fontes && mvn clean package          # fat JAR em br.com.wdc.cube.backend/target/
cd fontes && mvn test                   # testes

# Rodar o backend (React/Flutter via WebSocket, porta 8080)
./work/bin/start-server.sh [porta]      # cd para work/, builda se faltar o JAR
./work/bin/start-h2-server.sh           # H2 em modo TCP (config aponta para tcp://localhost por padrão)

# Frontend React (assets vão direto para remote.host/.../resources)
cd fontes/.../remote.shell.react && npm install && npm run watch
```

Config externa: `work/config/application.toml` (resolução: system property `shopping.config.file` → fallback para esse arquivo). `application.local.toml` para overrides locais.

## Persistência: jOOQ (NÃO JDBI)

A camada de persistência foi **migrada de JDBI + Command Pattern para jOOQ** (branch `evolucoes-estruturais`). O README ainda descreve o modelo antigo — está desatualizado nesse ponto.

- Repositórios (`*RepositoryImpl`) estendem `BaseRepositoryImpl` e obtêm o `DSLContext` via `dsl()` — que lê `ShoppingDSLContext.BEAN` (holder **do app**, em `persistence.impl`, não no framework). O `framework.jooq` recebe o `DSLContext` por injeção (`JsonQueryBuilder.setDSLContextSupplier`).
- Classes jOOQ geradas ficam em `br.com.wdc.shopping.persistence.impl.scheme.*` (`Tables`, `Sequences`, `tables.EnProduct`, etc.) — `scheme` = esquema do banco; jOOQ é só a tecnologia de geração.
- Não existem mais classes `*Cmd.java` (Command Pattern SQL foi removido).
- Helpers de query JSON em `br.com.wdc.framework.jooq` (`JsonQuery`, `JsonQueryBuilder`, `JsonChildQueryBuilder`).

### Transações (JTA/JDBC)

Controle programático estilo CMT via `TransactionService` (`framework.domain.transaction`, contrato) + `TransactionServiceImpl`/`TransactionScope` (`framework.persistence`, impl dual-mode). Detalhes: [docs/camada-de-dados.md → Transações](docs/camada-de-dados.md#transações-atomicidade-e-modo-dual-jtajdbc).

- Modo escolhido em `application.toml`: `database.transaction = "jta"` (Narayana + Agroal XA) ou `"non-jta"` (JDBC direto, padrão). Tecnologia concreta (Agroal/Narayana) só no host `br.com.wdc.cube.backend` (`supports/`); `framework.persistence` é neutro (`javax.sql.DataSource` + `jakarta.transaction`).
- **Per-módulo (sem holder global)**: cada módulo expõe seus holders — `ShoppingDSLContext` (impl) e `ShoppingTransactions` (domain) — populados pelo backend (composition root). `TransactionServiceImpl(Supplier<DataSource>)` é ligado ao DataSource do módulo. **Não** existem `SqlDataSource.BEAN` nem `TransactionService.BEAN` globais. O TM JTA permanece único por JVM (coordenador).
- Repositórios **não** demarcam transação. O `DSLContext` usa `TransactionAwareConnectionProvider` (`framework.jooq`): dentro de `ShoppingTransactions.BEAN.get().required(...)` compartilha a conexão do escopo; fora, conexão avulsa (autocommit).
- Fronteira aberta nos casos de uso de escrita: checkout (`CartManager.doPurchase`) e handlers REST (`RepositoryApiRoutes.transactional(...)`).
- **Transação remota dirigida pelo cliente (REST)**: o cliente HTTP (`RestTransactionService`, impl de `TransactionService` em `persistence.client`) torna **várias** chamadas REST atômicas — `begin`/`commit`/`rollback` via `/api/tx/*` (`TxApiController`), propagando o `txId` no header `X-Tx-Id`. Server-side: SPI `RemoteTransactionCoordinator` (`framework.persistence`, **não** no domain — é server-only) + holder `RemoteTransactions.COORDINATOR` (`persistence.rest`), populado pelos composition roots (`BusinessContext`, `TestEnvironment`). Mesma API `required(...)` que o modo in-process — fronteira simétrica Host/cliente. Detalhes: [docs/camada-de-dados.md → Transações Remotas](docs/camada-de-dados.md#transações-remotas-dirigidas-pelo-cliente-sobre-rest).

## Convenções

- **Injeção de dependências sem framework de DI**: service locator estático via `AtomicReference<T> BEAN` (ex.: `ShoppingDSLContext.BEAN`). Services recebem dependências no construtor.
- **Virtual Threads** (Java 21) para conexões WebSocket.
- **Segurança RBAC**: HMAC challenge-response + JWT; repositórios decorados (`SecuredXxxRepository`); papéis ADMIN/CUSTOMER/MANAGER (modelo allow-wins). Transporte React: RSA + PBKDF2 + AES-GCM.
- **Nomenclatura**: `*ViewState`, `*ViewImpl`, `*Presenter`, `*RepositoryImpl`, `*Criteria`, `Apply*Criteria`.
- **Formatação Java**: `fontes/wedocode-java-formatter.xml`. Frontend: Prettier (configurado no `.vscode/settings.json`, format-on-save).

## Mensagens de commit

Padrão observado no histórico: **emoji + `[TIPO]` em maiúsculas + descrição em português**.

```
🐛 [BUGFIX] descrição do que foi corrigido
✨ [FEATURE] descrição da funcionalidade
🧹 [CHORE] limpeza / refactor menor
```

Branch principal: `main`. Branch de trabalho atual: `evolucoes-estruturais`.
