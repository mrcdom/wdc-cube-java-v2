# br.com.wdc.shopping.api

Módulo de **API REST** da aplicação Shopping. Expõe os repositórios de domínio como endpoints HTTP via **Javalin**, com autenticação HMAC challenge-response, autorização JWT e serialização Jackson.

## Dependências

| Artefato | Papel |
|----------|-------|
| `br.com.wdc.shopping.domain` | Modelos de domínio, interfaces de repositório, contratos de segurança |
| `io.javalin:javalin` | Framework HTTP (7.1.0) |
| `com.fasterxml.jackson` | Serialização JSON |
| `slf4j-api` | Logging |

## Estrutura de Pacotes

```
br.com.wdc.shopping.api
├── RepositoryApiRoutes.java          — Configuração central de rotas e filtros
├── ApiObjectMapper.java              — ObjectMapper Jackson com mixins e adapters
├── AuthApiController.java            — Endpoints de autenticação (challenge, login, refresh, logout)
├── UserApiController.java            — CRUD de usuários
├── ProductApiController.java         — CRUD de produtos + upload/download de imagens
├── PurchaseApiController.java        — CRUD de compras
├── PurchaseItemApiController.java    — CRUD de itens de compra
│
└── security/
    └── SecurityFilter.java           — Before-filter: valida Bearer JWT, popula SecurityContextHolder
```

## Endpoints

### Autenticação (`/api/auth/`)

| Método | Path | Descrição |
|--------|------|-----------|
| GET | `/api/auth/challenge` | Gera nonce com TTL para HMAC challenge-response |
| POST | `/api/auth/login` | Valida digest HMAC + nonce, retorna `{accessToken, refreshToken, publicKey, expiresAt}` |
| POST | `/api/auth/refresh` | Renova tokens via `refreshToken` |
| POST | `/api/auth/logout` | Invalida refresh token |

### Repositórios (`/api/repo/`)

Todos os endpoints de repositório seguem o mesmo padrão CRUD:

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/api/repo/{entity}/insert` | Insere entidade |
| POST | `/api/repo/{entity}/update` | Atualiza entidade |
| POST | `/api/repo/{entity}/upsert` | Insert ou update |
| POST | `/api/repo/{entity}/delete` | Remove por critério |
| POST | `/api/repo/{entity}/count` | Conta por critério |
| POST | `/api/repo/{entity}/fetch` | Busca por critério (com projeção, paginação, ordenação) |
| POST | `/api/repo/{entity}/fetchById` | Busca por ID (com projeção) |
| GET | `/api/repo/{entity}/{id}` | Busca por ID (projeção padrão) |

Onde `{entity}` é: `user`, `product`, `purchase`, `purchase-item`.

#### Endpoints adicionais de produto

| Método | Path | Descrição |
|--------|------|-----------|
| GET | `/api/repo/product/{id}/image` | Download da imagem (PNG) — **público, sem autenticação** |
| PUT | `/api/repo/product/{id}/image` | Upload da imagem (bytes) |

## Segurança

### SecurityFilter

Before-filter registrado em `/api/repo/*` (condicional — só ativo quando `AuthenticationService.BEAN` está configurado):

1. Extrai `Authorization: Bearer <token>` do header
2. Valida o JWT via `AuthenticationService.resolveToken()`
3. Popula `SecurityContextHolder` com o contexto do usuário (userId, roles, permissions)
4. Rotas públicas (terminadas em `/image`) ignoram autenticação
5. Retorna 401 para tokens ausentes/inválidos

O `SecurityContextHolder` é limpo automaticamente em um `after` handler registrado por `RepositoryApiRoutes`.

### Tratamento de Erros

- `AccessDeniedException` → HTTP 403 (Forbidden)
- Token ausente/inválido → HTTP 401 (Unauthorized)

### Senha via RSA

O `UserApiController` suporta recebimento de senha criptografada com RSA (`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`). Se um `SecurityContext` com chave privada RSA estiver presente, a senha é descriptografada server-side antes de persistir.

## Componentes

### RepositoryApiRoutes

Classe utilitária (construtor privado) que configura o Javalin:

```java
RepositoryApiRoutes.configure(javalinConfig);
```

- Registra `SecurityFilter` como before-filter (se auth habilitado)
- Registra handler para `AccessDeniedException` → 403
- Registra `after` handler para limpar `SecurityContextHolder`
- Registra endpoints de autenticação (`AuthApiController`)
- Registra endpoints CRUD de todas as entidades

### ApiObjectMapper

`ObjectMapper` Jackson compartilhado, configurado com:

| Configuração | Efeito |
|-------------|--------|
| `OffsetDateTime` ↔ ISO-8601 | Serialização de datas como string ISO |
| `PurchaseItem.purchase` → `@JsonIgnore` | Evita referência circular |
| `Product.image` → `@JsonIgnore` | Imagens servidas por endpoint dedicado |
| `User.password` → `WRITE_ONLY` | Senha nunca retorna nas respostas |
| `NON_NULL` inclusion | Campos nulos omitidos |
| `FAIL_ON_UNKNOWN_PROPERTIES = false` | Tolerância a campos desconhecidos |

### Projeção

Os endpoints `fetch` e `fetchById` aceitam um campo `"projection"` no body JSON, indicando quais campos da entidade devem ser retornados. O `ApiObjectMapper.parseProjection()` converte o JSON em uma instância do modelo de domínio com os campos sentinela de `ProjectionValues`.
