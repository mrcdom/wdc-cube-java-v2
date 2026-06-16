# CLAUDE.md

Guia para trabalhar neste repositório. Para visão arquitetural completa, ver [README.md](README.md) e [docs/](docs/).

## O que é

**WDC Cube Java v2** — referência arquitetural do padrão **Cube MVP** (variação de MVP com presenters hierárquicos, navegação por intents e ViewStates serializáveis). Demonstra um e-commerce (Shopping) com backend Java 21 e **seis frontends independentes** (React, Flutter, Vaadin, SWT, Gluon, TeaVM) que compartilham os mesmos presenters/ViewStates — só a camada de renderização muda.

Idioma do projeto: **português** (docs, READMEs, mensagens de commit). Código e identificadores em inglês/português conforme o existente no arquivo.

## Layout

- `fontes/` — todo o código-fonte Maven (multi-módulo). **O build roda a partir daqui, não da raiz.**
  - `br.com.wdc.framework/` — framework reutilizável: `commons`, `cube`, `cube.remote`, `jooq`, `dependencies` (BOM)
  - `br.com.wdc.shopping/` — app de exemplo: `domain`, `persistence`, `persistence.rest`, `persistence.client`, `presentation`, `backend`, `scripts`, `tests`, `view.*`
- `work/` — diretório de runtime: `config/` (TOML), `bin/` (scripts de start), `data/`, `log/`, `frontend/`
- `docs/` — documentação arquitetural detalhada (PT)

## Build & Run

```bash
# Build (Java 21 obrigatório)
export JAVA_HOME=<jdk-21>     # ou defina JAVA21_HOME
cd fontes && mvn clean package          # fat JAR em br.com.wdc.shopping.backend/target/
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

- Repositórios (`*RepositoryImpl`) usam `JooqDSLContext.BEAN.get()` para obter o `DSLContext`.
- Classes jOOQ geradas ficam em `br.com.wdc.shopping.persistence.jooq.*` (`Tables`, `Sequences`, `tables.EnProduct`, etc.).
- Não existem mais classes `*Cmd.java` (Command Pattern SQL foi removido).
- Helpers de query JSON em `br.com.wdc.framework.jooq` (`JsonQuery`, `JsonQueryBuilder`, `JsonChildQueryBuilder`).

## Convenções

- **Injeção de dependências sem framework de DI**: service locator estático via `AtomicReference<T> BEAN` (ex.: `JooqDSLContext.BEAN`). Services recebem dependências no construtor.
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
