# WDC Cube Java v2

Proposta arquitetural para construção de aplicações web utilizando o padrão **Cube MVP** — uma variação do Model-View-Presenter com presenters hierárquicos, navegação por intents e serialização de estado das views.

Este projeto serve como **referência arquitetural** para novos projetos, demonstrando a implementação completa de um sistema de e-commerce (Shopping) com backend Java e frontend React.

## Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────┐
│                    React 19 + MUI 9                     │
│                  (TypeScript / Parcel)                   │
├─────────────────────────────────────────────────────────┤
│              WebSocket (bidirecional)                    │
├─────────────────────────────────────────────────────────┤
│             Javalin 7 + Virtual Threads                 │
│            (Jetty 12 / porta 8080)                      │
├─────────────────────────────────────────────────────────┤
│                   Presentation                          │
│     Presenters hierárquicos + ViewStates serializáveis  │
├─────────────────────────────────────────────────────────┤
│                  Business (impl)                        │
│        Repositories + Command Pattern (SQL)             │
├─────────────────────────────────────────────────────────┤
│                Business (shared)                        │
│          Modelos de domínio + Contratos                 │
├─────────────────────────────────────────────────────────┤
│                    H2 Database                          │
└─────────────────────────────────────────────────────────┘
```

**Características principais:**

- **Sem frameworks de DI** — injeção via `AtomicReference<T> BEAN` (service locator estático)
- **Virtual Threads** (Java 26) — conexões WebSocket com consumo mínimo de memória
- **Segurança** — RSA + PBKDF2 + AES-GCM para troca de dados entre cliente e servidor
- **Comunicação em tempo real** — WebSocket bidirecional com keep-alive automático

## Estrutura de Módulos

```
fontes/
├── br.com.wdc.framework/                  # Framework base
│   ├── br.com.wdc.framework.commons/      # Utilitários, FP, serialização, SQL, crypto
│   ├── br.com.wdc.framework.cube/         # Cube MVP: presenters, views, navegação
│   └── br.com.wdc.framework.dependencies/ # BOM — gerenciamento centralizado de versões
│
└── br.com.wdc.shopping/                   # Aplicação Shopping
    ├── br.com.wdc.shopping.business/           # Interfaces de serviço
    ├── br.com.wdc.shopping.business.shared/    # Modelos, repositórios, critérios, exceções
    ├── br.com.wdc.shopping.business.impl/      # Persistência (H2 + JDBI + Command Pattern)
    ├── br.com.wdc.shopping.presentation/       # Presenters, ViewStates, navegação, DTOs
    ├── br.com.wdc.shopping.tests/              # Testes unitários e de workflow
    └── br.com.wdc.shopping.view.react/
        ├── br.com.wdc.shopping.view.react.client/    # Frontend React/TypeScript
        ├── br.com.wdc.shopping.view.react.javalin/   # Servidor Javalin (fat JAR)
        └── br.com.wdc.shopping.view.react.skeleton/  # Implementações de view + segurança
```

### Framework

| Módulo | Descrição |
|--------|-----------|
| **framework.commons** | Interfaces funcionais com exceções checked (`ThrowingFunction`, `ThrowingConsumer`, etc.), serialização extensível, abstrações SQL (`SqlDataSource`), utilitários (`CoerceUtils`, `DateUtil`, `Defer`), criptografia (`RSA`, `Base62`) |
| **framework.cube** | Motor do padrão Cube MVP: `CubeApplication`, `CubePresenter`, `AbstractCubePresenter`, `AbstractChildPresenter`, `CubeView`/`CubeViewSlot`, `ViewState`, `CubePlace`, `CubeIntent`, `CubeNavigation` |
| **framework.dependencies** | POM do tipo BOM para gerenciamento centralizado de versões de dependências |

### Shopping — Backend

| Módulo | Descrição |
|--------|-----------|
| **business.shared** | Modelos de domínio (`User`, `Product`, `Purchase`, `PurchaseItem`), interfaces de repositório, classes de critérios para consultas, hierarquia de exceções (`BusinessException`) |
| **business.impl** | Implementação de persistência com Command Pattern SQL (`InsertRowUserCmd`, `FetchProductsCmd`, etc.), `BaseRepository`, `BaseCommand`, DSL SQL (`SqlKeywords`), scripts DDL para H2 |
| **presentation** | `ShoppingApplication`, hierarquia de presenters (Root → Login \| Home → Products/Purchases/Product/Cart/Receipt), ViewStates serializáveis, `CartManager`, `LoginService`, sistema de rotas e navegação |

### Shopping — Frontend

| Módulo | Descrição |
|--------|-----------|
| **view.react.client** | SPA em React 19 + TypeScript + MUI 9, bundled via Parcel. Comunicação WebSocket bidirecional, gerenciamento de reconexão, segurança client-side |
| **view.react.javalin** | Servidor Javalin 7 com Virtual Threads, WebSocket dispatcher, controllers REST, banco H2 embarcado. Gera fat JAR (~11 MB) |
| **view.react.skeleton** | Implementações de view para o servidor (`GenericViewImpl`), segurança (`AppSecurity` — RSA/PBKDF2/AES-GCM, `DataSecurity`), SPI de WebSocket |

## Pré-requisitos

- **Java 26** (Temurin) com preview features
- **Maven 3.9+**
- **Node.js 18+** e **npm** (para o frontend)

## Build

### Backend (Java)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-26.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

cd fontes
mvn clean package
```

O fat JAR será gerado em:
```
br.com.wdc.shopping/br.com.wdc.shopping.view.react/br.com.wdc.shopping.view.react.javalin/target/br.com.wdc.shopping.view.react.javalin-1.0.0.jar
```

### Frontend (React)

```bash
cd br.com.wdc.shopping/br.com.wdc.shopping.view.react/br.com.wdc.shopping.view.react.client

npm install        # instalar dependências
npm run build      # build de produção
npm run watch      # modo desenvolvimento (hot reload)
```

Os assets compilados são gerados diretamente em `br.com.wdc.shopping.view.react.skeleton/src/main/resources/META-INF/resources`.

## Execução

```bash
# Via script
cd br.com.wdc.shopping/br.com.wdc.shopping.view.react/br.com.wdc.shopping.view.react.javalin
./start-server.sh [porta]

# Ou diretamente
java --enable-preview -jar target/br.com.wdc.shopping.view.react.javalin-1.0.0.jar [porta]
```

- **Aplicação:** http://localhost:8080
- **Health check:** http://localhost:8080/health
- **Porta padrão:** 8080 (configurável via argumento ou variável `SERVER_PORT`)

## Testes

```bash
cd fontes
mvn test
```

Estrutura de testes:

| Classe base | Finalidade |
|-------------|------------|
| `BaseBusinessTest` | Testes de lógica de negócio e repositórios |
| `BasePresentationTest` | Testes de workflow completo com mocks de view |

## Padrão Cube MVP

O Cube MVP organiza a aplicação em uma **hierarquia de presenters** com navegação baseada em **intents**:

```
RootPresenter (container)
├── LoginPresenter (aberto)
└── HomePresenter (restrito/autenticado)
    ├── ProductsPanelPresenter (painel)
    ├── PurchasesPanelPresenter (painel)
    ├── ProductPresenter
    ├── CartPresenter
    └── ReceiptPresenter
```

Cada presenter possui um **ViewState** serializável que é transmitido ao frontend via WebSocket. O frontend renderiza com base no estado recebido e envia eventos de volta ao presenter correspondente.

## Convenções de Nomenclatura

| Tipo | Padrão | Exemplo |
|------|--------|---------|
| Comando SQL | `Verbo` + `Entidade` + `Cmd` | `InsertRowUserCmd` |
| View State | `*ViewState` | `ProductViewState` |
| View Impl | `*ViewImpl` | `RootReactViewImpl` |
| Presenter | `*Presenter` | `CartPresenter` |
| Repositório | `*RepositoryImpl` | `UserRepositoryImpl` |
| Critério | `*Criteria` | `ProductCriteria` |
| Aplicador de critério | `Apply` + `Entidade` + `Criteria` | `ApplyProductCriteria` |

## Dependências Principais

| Categoria | Tecnologia | Versão |
|-----------|-----------|--------|
| Linguagem | Java (Temurin) | 26 |
| Build | Maven | 3.9+ |
| Servidor HTTP | Javalin | 7.1.0 |
| Servlet Container | Jetty | 12 |
| Banco de dados | H2 | 2.4.240 |
| Acesso a dados | JDBI | 3.52.1 |
| Serialização | Gson | 2.13.2 |
| Logging | SLF4J + Logback | 2.0.16 / 1.5.32 |
| Testes | JUnit | 4.13.2 |
| Frontend | React | 19 |
| UI Components | MUI | 9 |
| Bundler | Parcel | 2.13.3 |
| Linguagem (frontend) | TypeScript | ES2024 |

## Licença

Este projeto é distribuído sob a [MIT License](https://github.com/mrcdom/wdc-cube-java-v2/blob/main/LICENSE).

Copyright (c) 2026 Marcelo Domingos / WeDoCode Consultoria LTDA
