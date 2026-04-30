# fontes/

Raiz do código-fonte do projeto **WeDoCode Shopping** — um e-commerce de referência construído com a arquitetura **Cube MVP** sobre Java 26.

---

## Estrutura

```
fontes/
├── pom.xml                          → POM agregador (reactor) de todo o projeto
├── wedocode-java-formatter.xml      → Perfil de formatação para IDEs (Eclipse/IntelliJ)
├── br.com.wdc.framework/            → Módulos de infraestrutura reutilizáveis
│   ├── br.com.wdc.framework.commons/       → Utilitários gerais (funções, helpers)
│   ├── br.com.wdc.framework.cube/          → Motor do padrão Cube MVP (scopes, presenters, views)
│   └── br.com.wdc.framework.dependencies/  → BOM de dependências (versões centralizadas)
└── br.com.wdc.shopping/             → Aplicação de e-commerce (domínio + frontends)
    ├── br.com.wdc.shopping.domain/          → Modelo de domínio (entidades, critérios, repositórios)
    ├── br.com.wdc.shopping.persistence/     → Implementação de repositórios (SQL/JDBI, H2)
    ├── br.com.wdc.shopping.presentation/    → Presenters da camada Cube MVP
    ├── br.com.wdc.shopping.scripts/         → Scripts SGBD (seed, reset, DDL)
    ├── br.com.wdc.shopping.tests/           → Testes automatizados (JUnit 4)
    ├── br.com.wdc.shopping.view.react/      → Frontend web (React 19 + Javalin + Virtual Threads)
    └── br.com.wdc.shopping.view.jfx/        → Frontend desktop (JavaFX)
```

---

## Módulos de Framework (`br.com.wdc.framework`)

| Módulo | Propósito |
|--------|-----------|
| **commons** | Interfaces funcionais (`ThrowingRunnable`, `ThrowingConsumer`, etc.), utilitários de uso geral. |
| **cube** | Implementação do padrão arquitetural Cube MVP — gerenciamento de scopes hierárquicos, ciclo de vida de presenters e binding de views. |
| **dependencies** | POM do tipo BOM que centraliza versões de dependências externas (JDBI, Gson, SLF4J, etc.). |

## Módulos da Aplicação (`br.com.wdc.shopping`)

| Módulo | Camada | Propósito |
|--------|--------|-----------|
| **domain** | Domínio | Entidades (`User`, `Product`, `Purchase`, `PurchaseItem`), critérios de busca, interfaces de repositório, utilitários de projeção (`ProjectionValues`, `ProjectionList`). |
| **persistence** | Infraestrutura | Implementação concreta dos repositórios usando JDBI + H2. Schema definido via classes `En*`, comandos SQL (Fetch/Insert/Update/Delete). |
| **presentation** | Aplicação | Presenters que orquestram a lógica de negócio e conectam domínio às views via Cube MVP. |
| **scripts** | Infraestrutura | DDL de criação de tabelas, dados de seed (`DBReset`), suporte a testes e inicialização. |
| **tests** | Teste | Suíte de testes JUnit 4 para repositórios (82 testes) e integração de serviços. |
| **view.react** | UI (Web) | Frontend React 19 + Material UI, servidor Javalin com Virtual Threads, comunicação segura RSA+AES-GCM. |
| **view.jfx** | UI (Desktop) | Frontend JavaFX que reutiliza os mesmos presenters da versão web. |

---

## Build

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
cd fontes/
mvn clean install
```

Requisitos: **Java 26** (com `--enable-preview`) e **Maven 3.9+**.
