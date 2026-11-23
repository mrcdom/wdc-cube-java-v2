# CLAUDE.md

Guia para trabalhar neste repositório. Para visão arquitetural completa, ver [README.md](README.md) e [docs/](docs/).

## O que é

**WDC Cube Java v2** — referência arquitetural do padrão **Cube MVP** (variação de MVP com presenters hierárquicos, navegação por intents e ViewStates serializáveis). Demonstra um e-commerce (Shopping) com backend Java 25 e **cinco frontends independentes** (React, Flutter, Vaadin, SWT, TeaVM) que compartilham os mesmos presenters/ViewStates — só a camada de renderização muda.

Idioma do projeto: **português** (docs, READMEs, mensagens de commit). Código e identificadores em inglês/português conforme o existente no arquivo.

## Layout

- `fontes/` — todo o código-fonte Maven (multi-módulo). **O build roda a partir daqui, não da raiz.**
  - `br.com.wdc.framework/` — framework reutilizável: `commons`, `domain` (abstrações genéricas: `Repository`, `Page`, `ModelCodec`, `Projection*`, exceções base, segurança/`PermissionModel`, `AppConfig`), `cube`, `cube.remote`, `jooq`, `dependencies` (BOM)
  - `br.com.wdc.shopping/` — app de exemplo: `domain`, `persistence` (agrupador de `persistence.impl` (jOOQ), `persistence.client` (HTTP/okhttp), `persistence.rest` (API Javalin)), `presentation`, `backend`, `scripts`, `tests`, `view.*`
    - **`domain` é organizado por entidade, não por tipo de classe**: `domain.product`, `domain.user`, `domain.purchase`, `domain.purchaseitem` — cada pacote reúne as quatro classes daquela entidade (`Xxx`, `XxxCriteria`, `XxxModelCodec`, `XxxRepository`). Ao criar uma entidade nova, crie o pacote dela com o conjunto completo; não há mais pacotes `model`/`criteria`/`codec`/`repositories`. Fora disso, `domain` guarda `exception`, `security` e os holders de raiz (`ShoppingConfig`, `ShoppingTransactions`).
- `work/` — diretório de runtime: `config/` (TOML), `bin/` (scripts de start), `data/`, `log/`, `frontend/`
- `docs/` — documentação arquitetural detalhada (PT)

## Build & Run

```bash
# Build (Java 25 obrigatório)
export JAVA_HOME=$JAVA25_HOME  # demos/backend em release 25; o framework fica em release 21 (publicado no Central)
cd fontes && mvn clean package          # fat JAR em br.com.wdc.cube.backend/target/
cd fontes && mvn test                   # testes

# Rodar o backend (React/Flutter via WebSocket, porta 8080)
./work/bin/start-server.sh [porta]      # cd para work/, builda se faltar o JAR
./work/bin/start-h2-server.sh           # H2 em modo TCP (config aponta para tcp://localhost por padrão)

# Frontends web (React, Flutter web, remote.shell.teavm, teavm.web → work/frontend/)
./work/bin/build-frontends.sh            # todos; --list mostra alvos/pré-requisitos, --skip-install pula o mvn install
./work/bin/build-frontends.sh react      # um alvo só

# Desenvolvimento: cada módulo tem watch próprio
cd fontes/.../remote.shell.react && npm install && npm run watch
```

Os quatro frontends web depositam seus artefatos em `work/frontend/<nome>`; cada um tem o seu `build.sh` no módulo de origem e o `build-frontends.sh` só orquestra os quatro. `work/frontend/{api.docs,openapi}` são estáticos.

**Armadilha dos alvos TeaVM**: `remote.shell.teavm` e `teavm.web` compilam contra os JARs do `~/.m2`, não contra o reator — sem `mvn install` antes, geram em silêncio um app com a versão anterior do domínio (abre normalmente, falha só na chamada que mudou). O `build-frontends.sh` reinstala por padrão justamente por isso.

**Publicação do framework no Maven Central**: só `br.com.wdc.framework`, como `io.github.mrcdom.wdc.java:framework-*` — `cd fontes/br.com.wdc.framework && mvn -Prelease clean deploy` (detalhes no [README do framework](fontes/br.com.wdc.framework/README.md#publicação-no-maven-central)). O `framework-parent` não herda do pom raiz, de propósito: assim os demos não entram na resolução de quem consome.

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

- **Injeção de dependências sem framework de DI**: service locator estático via `AtomicReference<T> BEAN` (ex.: `ShoppingDSLContext.BEAN`). Os serviços da apresentação (`XxxService`, `CartManager`) leem o `BEAN` por um método privado (`private static ProductRepository productRepository() { return ProductRepository.BEAN.get(); }`), sem campo nem parâmetro de construtor; componentes de infraestrutura (ex.: `TransactionServiceImpl`) recebem as dependências no construtor.
- **Virtual Threads** (Java 21) para conexões WebSocket.
- **Segurança RBAC**: HMAC challenge-response + JWT; a autorização fica nos endpoints REST (`SecurityEnforcer`), a fronteira por onde passa o teavm.web — nos hosts em que os presenters rodam no servidor, o presenter decide cada consulta e os repositórios são usados direto (`XxxRepository.BEAN.get()`); papéis ADMIN/CUSTOMER/MANAGER (modelo allow-wins). Transporte React: RSA + PBKDF2 + AES-GCM.
- **Nomenclatura**: `*Scope`, `*ViewImpl`, `*Presenter`, `*RepositoryImpl`, `*Criteria`, `Apply*Criteria`.
- **Critérios**: cada campo de um `*Criteria` é um `Criterion` (`framework.domain.criteria`) — `Criterion` (eq/ne/in/isNull), `ComparableCriterion` (gt/ge/lt/le/between) ou `TextCriterion` (like/ilike/containing/startingWith). Pedidos acumulam em `AND`; `or()` torna o campo disjuntivo. **O campo nunca é `null`** — `criteria.xxx() == null` compila e é sempre falso; use `hasXxx()`. A tradução para jOOQ é única, no `CriterionTranslator`; cada entidade só mapeia campo→coluna no seu `ApplyConditions`.
- **Entidades (implementam `KeyedEntity`) têm API fluente**, igual à dos `*Criteria`: campo `private`, acessor `campo()` e setter `withCampo(...)` que devolve `this`. Não há campos públicos — construa encadeando (`new Product().withName(x).withPrice(y)`), inclusive nas relações (`withPurchase(new Purchase().withId(id))`). Structs de apresentação (`presentation.**.structs`) seguem outro padrão: continuam com campos públicos.
- **Scope com `Prop`** (`framework.cube`): `XxxScope extends Scope`, em arquivo próprio ao lado do presenter (`CartScope.java`, como o `xxx.scope.ts` da versão TS); um escopo que só existe dentro dele (segmento, linha de lista) é classe interna pública estática dele (`CartScope.CartItemScope`). Cada campo observado é `public final Prop<T> x = prop(...)`, e o presenter cria o estado com `bind(new XxxScope())`. `set` de um valor diferente (`equals`) já pede o redesenho — **não** chame `this.update()` depois; ele só é preciso quando se mexe *dentro* de um valor já atribuído (o carrinho altera itens no lugar). A view lê com `get()`; a view que consome uma mensagem de exibição única durante o desenho usa `setSilently`. Ações (`Action`/`ActionOf`/`ActionOf2`) são campos comuns do state, ligados no construtor com o código de evento do bridge e a leitura do `formData`: `actionOf(2, FormParam.asLong("p.productId"), this::removeProduct)`. O presenter **não cria view nem declara skeleton**: o escopo declara `public static final String CLASS_ID` (classId do bridge) e o skeleton é do escopo (`scope.skeleton(CLASS_ID)`, que despacha às ações ligadas com código; `syncState` vira `state.onSync(formData -> ...)` no construtor). Composição: o filho se entrega ao pai com `ownerSlot.setScope(this.state)` (`ScopeSlot`, lido via `keys.parentSlot()`), o pai guarda `public final Prop<Scope> content = prop()` e a view do pai desenha `app.viewOf(state.content.get())`; o host só desenha o escopo do presenter raiz.
- **Segmentos**: um pedaço de tela sem navegação própria (os painéis da Home) não ganha presenter — é um escopo menor, classe interna pública estática do escopo dono (`HomeScope.ProductsPanelScope`), com `CLASS_ID` estático. O presenter dono liga as ações (`actionOf(segment, código, FormParam..., fn)`) e só o põe num `Prop` do escopo dono — não conhece view. A view é da camada de view: cada host monta um catálogo `ScopeViews` (classe do escopo → fábrica; o host remoto usa `fallback(RemoteViewImpl::forScope)`, que lê o `CLASS_ID`) e o entrega com `setScopeViews`; a view do pai pede a do segmento com `app.viewOf(scope)` (criada na primeira vez; antes disso, mudar o segmento redesenha o dono). Remotamente sai como `"<campo>ViewId"`. Linhas de lista também: `Prop<List<XxxRowScope>>` sai como `"<campo>ViewIds"`; o presenter reaproveita a linha pela identidade do item e chama `detach` na que saiu (ver `CartPresenter.syncRows`).
- **Places e chaves**: cada place tem sua `XxxKeys` (`CubeKeys`), que declara o place e é a única que conhece os nomes dele no intent (prefixo de duas letras); listar em `ShoppingApplication.PLACES`. Navegar: `new ProductKeys(app).withProductId(id).flip()`.
- **Formatação Java**: `fontes/wedocode-java-formatter.xml`. Frontend: Prettier (configurado no `.vscode/settings.json`, format-on-save).

## Mensagens de commit

Padrão observado no histórico: **emoji + `[TIPO]` em maiúsculas + descrição em português**.

```
🐛 [BUGFIX] descrição do que foi corrigido
✨ [FEATURE] descrição da funcionalidade
🧹 [CHORE] limpeza / refactor menor
```

Branch principal: `main`. Branch de trabalho atual: `evolucoes-estruturais`.
