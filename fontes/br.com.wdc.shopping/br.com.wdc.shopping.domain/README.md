# br.com.wdc.shopping.domain

Módulo de **domínio puro** da aplicação Shopping. Define modelos, contratos de repositório, critérios de consulta, exceções de negócio e configuração — sem dependência de frameworks de persistência ou apresentação.

## Estrutura

```
src/main/java/br/com/wdc/shopping/domain/
├── ShoppingConfig.java              — Diretórios da aplicação (config, data, log, temp)
│
├── config/
│   └── AppConfig.java               — Carrega configuração TOML (application.toml)
│
├── model/                           — Modelos de domínio (POJOs)
│   ├── User.java                    — Usuário (id, userName, password, name)
│   ├── Product.java                 — Produto (id, name, price, description, image)
│   ├── Purchase.java                — Compra (id, buyDate, user, items)
│   └── PurchaseItem.java            — Item de compra (id, amount, price, purchase, product)
│
├── repositories/                    — Contratos de repositório (interfaces)
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── PurchaseRepository.java
│   └── PurchaseItemRepository.java
│
├── criteria/                        — Objetos de critério para consultas
│   ├── UserCriteria.java
│   ├── ProductCriteria.java
│   ├── PurchaseCriteria.java
│   └── PurchaseItemCriteria.java
│
├── exception/                       — Exceções de negócio
│   ├── BusinessException.java       — Base para exceções de negócio
│   ├── AccessDeniedException.java   — Acesso negado (permissão insuficiente)
│   ├── InvalidCartItemException.java
│   └── OfflineException.java
│
├── security/                        — Contratos e modelos de segurança (RBAC)
│   ├── AuthenticationService.java   — Contrato de autenticação (challenge + login)
│   ├── AuthResult.java              — Resultado de login (tokens, publicKey, expiresAt)
│   ├── ChallengeResult.java         — Resultado de challenge (nonce, expiresAt)
│   ├── PasswordUtil.java            — HMAC-SHA256 digest para autenticação
│   ├── Role.java                    — Enum de papéis (ADMIN, CUSTOMER, MANAGER) com permissões
│   ├── SecurityContext.java         — Contexto de segurança do usuário autenticado
│   └── SecurityContextHolder.java   — ThreadLocal para propagação do SecurityContext
│
└── utils/                           — Utilitários de projeção
    ├── ProjectionValues.java        — Valores sentinela para indicar campos desejados
    ├── ProjectionList.java          — Lista com critério associado
    └── ProjectionSet.java           — Set com critério associado
```

## Princípios

- **Sem dependências de infraestrutura** — apenas SLF4J e Commons IO
- **Modelos simples** — POJOs com campos públicos, sem anotações de persistência
- **Repositórios como interfaces** — registrados via `AtomicReference<XxxRepository> BEAN` (Service Locator leve)
- **Critérios tipados** — cada entidade tem seu `XxxCriteria` com filtros, projeção, paginação e ordenação
- **Segurança declarativa** — `Role` enum define permissões no formato `entity:operation`; `SecurityContextHolder` propaga contexto via ThreadLocal

## Modelos

| Modelo | Campos principais |
|--------|-------------------|
| `User` | `id`, `userName`, `password`, `name`, `roles` |
| `Product` | `id`, `name`, `price`, `description`, `image` |
| `Purchase` | `id`, `buyDate`, `user`, `items` |
| `PurchaseItem` | `id`, `amount`, `price`, `purchase`, `product` |

## Repositórios

Todos os repositórios seguem o mesmo contrato:

```java
public interface XxxRepository {
    AtomicReference<XxxRepository> BEAN = new AtomicReference<>();

    boolean insert(Xxx entity);
    boolean update(Xxx newEntity, Xxx oldEntity);
    boolean insertOrUpdate(Xxx entity);
    int delete(XxxCriteria criteria);
    int count(XxxCriteria criteria);
    List<Xxx> fetch(XxxCriteria criteria);
    Xxx fetchById(Long id, Xxx projection);
}
```

A implementação concreta é injetada no `BEAN` pela camada de persistência na inicialização.

## Critérios

Cada `XxxCriteria` oferece uma API fluente para construir consultas:

```java
var criteria = new ProductCriteria()
    .withProductId(42L)
    .withProjection(projection)
    .withOffset(0)
    .withLimit(20)
    .withOrderBy(ProductCriteria.OrderBy.NAME_ASC);
```

## Projeção

O mecanismo de projeção permite indicar quais campos devem ser carregados. Um objeto "projeção" é um modelo cujos campos não-nulos indicam que aquela coluna deve ser incluída no SELECT. `ProjectionValues` fornece valores sentinela para cada tipo primitivo.

## Configuração

`AppConfig` carrega configurações de `work/config/application.toml` (ou caminho definido via `-Dshopping.config.file`). `ShoppingConfig` resolve os diretórios padrão da aplicação (`config/`, `data/`, `log/`, `temp/`).

## Segurança (RBAC)

O pacote `security/` define os contratos de autenticação e autorização:

| Classe | Responsabilidade |
|--------|-----------------|
| `Role` | Enum com papéis (ADMIN, CUSTOMER, MANAGER) e suas permissões no formato `entity:operation` |
| `SecurityContext` | Contexto imutável do usuário autenticado (userId, userName, roles, permissions) |
| `SecurityContextHolder` | ThreadLocal para propagação do contexto entre camadas |
| `AuthenticationService` | Contrato de autenticação: `challenge()` → `ChallengeResult`, `login(user, digest, nonce)` → `AuthResult` |
| `PasswordUtil` | HMAC-SHA256 para gerar digest de autenticação sem trafegar senha em texto plano |
| `AccessDeniedException` | Exceção lançada quando o usuário não possui permissão para a operação |

O modelo é **allow-wins**: a permissão efetiva é a união de todos os papéis do usuário.

## Dependências

- SLF4J — logging
- Commons IO — utilitários de I/O
