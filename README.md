# WDC Cube Java v2

Proposta arquitetural para construção de aplicações utilizando o padrão **Cube MVP** — uma variação do Model-View-Presenter com presenters hierárquicos, navegação por intents e serialização de estado das views.

Este projeto serve como **referência arquitetural** para novos projetos, demonstrando a implementação completa de um sistema de e-commerce (Shopping) com backend Java e **quatro implementações de frontend independentes** — React (web/remoto), Vaadin (web/server-side), JavaFX (desktop/local) e Android (mobile/Compose) — provando que a camada de visualização é totalmente desacoplada da lógica de apresentação.

## Visão Geral da Arquitetura

```
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  React 19 + MUI  │  │  Vaadin 24 +     │  │  JavaFX 24 +     │  │  Kotlin +        │
│  (Browser / WS)  │  │  Lumo (Browser / │  │  CSS (Desktop /  │  │  Jetpack Compose  │
│                  │  │  Server Push)    │  │  JVM local)      │  │  (Android)       │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │ WebSocket            │ Atmosphere           │ Direto              │ Direto
         │ (JSON delta)         │ (Server Push)        │ em memória          │ em memória
         ▼                     ▼                      ▼                     ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Javalin 7 +     │  │  Jetty 12 +      │  │  ShoppingJfx     │  │  ShoppingAndroid │
│  Virtual Threads │  │  Vaadin Servlet  │  │  Application     │  │  Application     │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                      │                     │
         └─────────────┬───────┴──────────────────────┴─────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────┐
│                   Presentation                          │
│     Presenters hierárquicos + ViewStates serializáveis  │
│     Services com injeção via construtor                 │
├─────────────────────────────────────────────────────────┤
│                   Security (RBAC)                       │
│  HMAC challenge-response + JWT + Secured Repositories   │
├─────────────────────────────────────────────────────────┤
│                   Persistence                           │
│        Repositories + Command Pattern (SQL)             │
├─────────────────────────────────────────────────────────┤
│                     Domain                              │
│      Modelos de domínio + Contratos + Configuração      │
├─────────────────────────────────────────────────────────┤
│                    H2 Database                          │
└─────────────────────────────────────────────────────────┘
```

**Características principais:**

- **Independência de visualização** — mesmos Presenters/ViewStates alimentam React (web), Vaadin (web server-side), JavaFX (desktop) e Android (mobile)
- **Sem frameworks de DI** — injeção via `AtomicReference<T> BEAN` (service locator estático); services recebem dependências no construtor
- **Virtual Threads** (Java 26) — conexões WebSocket com consumo mínimo de memória
- **Segurança RBAC** — autenticação HMAC challenge-response com JWT, controle de acesso por papéis (ADMIN, CUSTOMER, MANAGER), repositórios decorados com verificação de permissões
- **Segurança de transporte** — RSA + PBKDF2 + AES-GCM para troca de dados entre cliente React e servidor
- **Comunicação em tempo real** — WebSocket bidirecional com keep-alive automático
- **Configuração externa** — TOML para todas as implementações

## Estrutura de Módulos

```
fontes/
├── br.com.wdc.framework/                  # Framework base
│   ├── br.com.wdc.framework.commons/      # Utilitários, FP, serialização, SQL, crypto
│   ├── br.com.wdc.framework.cube/         # Cube MVP: presenters, views, navegação
│   └── br.com.wdc.framework.dependencies/ # BOM — gerenciamento centralizado de versões
│
└── br.com.wdc.shopping/                   # Aplicação Shopping
    ├── br.com.wdc.shopping.domain/             # Modelos, repositórios, critérios, config
    ├── br.com.wdc.shopping.persistence/        # Persistência (H2 + JDBI + Command Pattern)
    ├── br.com.wdc.shopping.presentation/       # Presenters, ViewStates, navegação, DTOs
    ├── br.com.wdc.shopping.scripts/            # Scripts DDL (DBCreate, DBReset)
    ├── br.com.wdc.shopping.tests/              # Testes unitários e de workflow
    ├── br.com.wdc.shopping.api/                # REST API controllers (Javalin)
    ├── br.com.wdc.shopping.api-client/         # REST client (OkHttp + Gson) para Android
    ├── br.com.wdc.shopping.view.react/         # 📄 [README](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/README.md)
    │   ├── br.com.wdc.shopping.view.react.client/    # Frontend React/TypeScript
    │   ├── br.com.wdc.shopping.view.react.javalin/   # Servidor Javalin (fat JAR)
    │   └── br.com.wdc.shopping.view.react.skeleton/  # Implementações de view + segurança
    ├── br.com.wdc.shopping.view.vaadin/        # 📄 [README](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.vaadin/README.md)
    │                                           # Frontend Vaadin 24 (server-side, Lumo theme)
    ├── br.com.wdc.shopping.view.jfx/           # 📄 [README](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.jfx/README.md)
    │                                           # Frontend JavaFX desktop
    └── br.com.wdc.shopping.view.android/       # 📄 [README](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.android/README.md)
                                                # Frontend Android (Kotlin + Jetpack Compose)
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
| **domain** | Modelos de domínio (`User`, `Product`, `Purchase`, `PurchaseItem`), interfaces de repositório, classes de critérios para consultas, hierarquia de exceções (`BusinessException`), contratos de segurança (`SecurityContext`, `AuthenticationService`, `Role`) |
| **persistence** | Implementação de persistência com Command Pattern SQL (`InsertRowUserCmd`, `FetchProductsCmd`, etc.), `BaseRepository`, `BaseCommand`, DSL SQL (`SqlKeywords`), scripts DDL para H2, **decorators de segurança** (`SecuredUserRepository`, etc.) que verificam permissões RBAC |
| **presentation** | `ShoppingApplication` com proxy delegates de SecurityContext, hierarquia de presenters (Root → Login \| Home → Products/Purchases/Product/Cart/Receipt), ViewStates serializáveis, services com injeção via construtor, `CartManager`, sistema de rotas e navegação |

### Shopping — Frontend (View Implementations)

> **Princípio central:** todas as implementações usam exatamente os mesmos Presenters, ViewStates e regras de negócio. Apenas a camada de renderização muda.

| Módulo | Descrição |
|--------|-----------|
| **view.react** | Visualização remota via browser — [detalhes](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/README.md) |
| **view.react.client** | SPA em React 19 + TypeScript + MUI 9, bundled via Parcel. Comunicação WebSocket bidirecional, gerenciamento de reconexão, segurança client-side |
| **view.react.javalin** | Servidor Javalin 7 com Virtual Threads, WebSocket dispatcher, controllers REST, banco H2 embarcado. Gera fat JAR (~11 MB) |
| **view.react.skeleton** | Implementações de view para o servidor (`GenericViewImpl`), segurança (`AppSecurity` — RSA/PBKDF2/AES-GCM, `DataSecurity`), SPI de WebSocket |
| **view.vaadin** | Visualização web server-side com Vaadin 24 + Lumo theme + Jetty 12 embarcado — [detalhes](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.vaadin/README.md) |
| **view.jfx** | Visualização desktop com JavaFX 24 + CSS Material-inspired — [detalhes](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.jfx/README.md) |
| **view.android** | App Android nativo com Kotlin + Jetpack Compose + Material 3 — [detalhes](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.android/README.md) |
| **api** | Controllers REST (Javalin) para expor repositórios como endpoints HTTP, filtro de segurança JWT (`SecurityFilter`), endpoints de autenticação (`AuthApiController`) |
| **api-client** | Client REST (OkHttp + Gson) que implementa as interfaces de repositório e `AuthenticationService` via HTTP, com Bearer token automático |

## Pré-requisitos

- **Java 26** com preview features
  - **Oracle JDK 26** (arm64) — necessário para JavaFX em macOS Apple Silicon
  - **Temurin 26** — suficiente para a versão React
- **Maven 3.9+**
- **Node.js 20+** e **npm** (para o frontend React)

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

### Versão React (Web)

```bash
# Via script
cd br.com.wdc.shopping/br.com.wdc.shopping.view.react/br.com.wdc.shopping.view.react.javalin
./start-server.sh [porta]

# Ou diretamente
java --enable-preview -jar target/br.com.wdc.shopping.view.react.javalin-1.0.0.jar [porta]
```

- **Aplicação:** http://localhost:8080
- **Health check:** http://localhost:8080/health
- **Porta padrão:** 8080 (configurável via `work/config/application.toml`, argumento CLI ou variável `SERVER_PORT`)

### Versão Vaadin (Web — Server-Side)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

cd br.com.wdc.shopping/br.com.wdc.shopping.view.vaadin
java --enable-preview -cp "$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout):target/classes" \
  br.com.wdc.shopping.view.vaadin.ShoppingVaadinMain
```

- **Aplicação:** http://localhost:8090
- UI inteiramente server-side — sem código JavaScript/TypeScript customizado

### Versão JavaFX (Desktop)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

cd br.com.wdc.shopping/br.com.wdc.shopping.view.jfx
mvn javafx:run
```

Ou via IDE usando a classe `ShoppingJfxLauncher.java` (não requer module-path configurado).

### Versão Android (Mobile)

```bash
# Compile as dependências Java para mavenLocal com perfil Android-compat
cd fontes
./build-android-deps.sh

# Abra fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.android no Android Studio
# Sync Gradle → Build → Run
```

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

## Configuração

Ambas as implementações usam o arquivo `work/config/application.toml` para configuração externa:

```toml
[app]
# basedir = "work"

[database]
# url = "jdbc:h2:file:..."
# username = "sa"
# password = "sa"
# reset = false

[server]
# port = 8080    # apenas Javalin

[security]
# jwt.secret = "sua-chave-secreta"   # habilita autenticação JWT na API REST
```

Resolução: system property `shopping.config.file` → fallback para `work/config/application.toml`.

## Segurança (RBAC)

O sistema implementa autenticação e autorização em múltiplas camadas:

### Autenticação — HMAC Challenge-Response

```
Client                            Server
  │                                 │
  │──── GET /api/auth/challenge ───→│  Gera nonce com TTL
  │←─── { nonce, expiresAt } ──────│
  │                                 │
  │  passwordHash = MD5(password)   │
  │  digest = HMAC-SHA256(          │
  │    key=passwordHash,            │
  │    data=userName+nonce)         │
  │                                 │
  │──── POST /api/auth/login ─────→│  Valida digest + nonce
  │     { userName, digest, nonce } │  Cria sessão + JWT
  │←─── { accessToken,             │
  │       refreshToken,             │
  │       publicKey, expiresAt } ──│
  │                                 │
  │──── Bearer token em /api/repo/* │  SecurityFilter valida JWT
```

A senha nunca trafega em texto plano — apenas o digest HMAC-SHA256 com nonce de uso único.

### Autorização — Papéis e Permissões

| Papel | Permissões |
|-------|-----------|
| **ADMIN** | `user:*`, `product:*`, `purchase:*`, `purchase-item:*`, `data:all` |
| **CUSTOMER** | `product:read`, `purchase:read/write`, `purchase-item:read/write` |
| **MANAGER** | `product:read/write`, `purchase:read`, `purchase-item:read` |

Modelo **allow-wins**: permissão efetiva = união de todos os papéis do usuário.

### Camadas de Segurança

| Camada | Componente | Responsabilidade |
|--------|-----------|-----------------|
| **HTTP** | `SecurityFilter` | Valida Bearer JWT, popula `SecurityContextHolder` |
| **Repositório** | `SecuredXxxRepository` | Verifica permissões, restringe escopo ao userId (non-admin) |
| **Apresentação** | `SecurityContextDelegate` (proxy) | Propaga `SecurityContext` para a thread corrente em cada chamada |
| **Transporte** | `AppSecurity` (React) | RSA + PBKDF2 + AES-GCM para dados sensíveis via WebSocket |

### Configuração

```toml
[security]
jwt.secret = "sua-chave-secreta-aqui"
```

Se `jwt.secret` não estiver configurado, a API opera sem autenticação (modo desenvolvimento/testes locais).

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
| Linguagem | Java (Oracle/Temurin) | 26 |
| Build | Maven | 3.9+ |
| Servidor HTTP | Javalin | 7.1.0 |
| Web UI (server-side) | Vaadin | 24.6.3 |
| Servlet Container | Jetty | 12 |
| Desktop UI | JavaFX | 24.0.1 |
| Mobile UI | Jetpack Compose + Material 3 | 2024.12 |
| Mobile Language | Kotlin | 2.1 |
| Image Loading | Coil | 2.7 |
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
