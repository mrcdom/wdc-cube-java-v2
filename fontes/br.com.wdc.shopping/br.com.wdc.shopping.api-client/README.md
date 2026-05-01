# br.com.wdc.shopping.api-client

Módulo **cliente REST** da aplicação Shopping. Implementa as interfaces de repositório e autenticação do domínio sobre HTTP, usando **OkHttp** e **Gson**. Permite que frontends remotos (Android, testes de integração) acessem o backend via API REST com autenticação JWT transparente.

## Dependências

| Artefato | Papel |
|----------|-------|
| `br.com.wdc.shopping.domain` | Modelos de domínio, interfaces de repositório, contratos de segurança |
| `com.squareup.okhttp3:okhttp` | Cliente HTTP (4.12.0) |
| `com.google.code.gson:gson` | Serialização/deserialização JSON |
| `slf4j-api` | Logging |

## Estrutura de Pacotes

```
br.com.wdc.shopping.api.client
├── RestConfig.java                    — Infraestrutura HTTP compartilhada (OkHttp + Gson)
├── RestAuthClient.java                — Gerenciador de tokens + fluxo HMAC challenge-response
├── RestAuthenticationService.java     — Implementação de AuthenticationService sobre REST
├── RestRepositoryBootstrap.java       — Bootstrap: registra implementações REST nos BEANs
├── RestUserRepository.java            — UserRepository via REST
├── RestProductRepository.java         — ProductRepository via REST (inclui imagens)
├── RestPurchaseRepository.java        — PurchaseRepository via REST
└── RestPurchaseItemRepository.java    — PurchaseItemRepository via REST
```

## Arquitetura

O módulo espelha a API server-side, implementando as mesmas interfaces de domínio:

```
┌─────────────────────────────────────────────────────┐
│                    Domínio                           │
│  UserRepository  ProductRepository  AuthService ...  │
├─────────────────────────────────────────────────────┤
│                  api-client                          │
│  RestUserRepo    RestProductRepo    RestAuthService  │
│       │               │                  │          │
│       └───────────────┼──────────────────┘          │
│                       ▼                             │
│                  RestConfig                          │
│            (OkHttp + Gson + Auth)                    │
│                       │                             │
│                  RestAuthClient                      │
│            (tokens + HMAC + RSA)                     │
└───────────────────────┼─────────────────────────────┘
                        ▼
                 HTTP → Javalin API
```

## Componentes

### RestConfig

Infraestrutura HTTP compartilhada por todos os repositórios:

| Método | Descrição |
|--------|-----------|
| `postJson(path, body)` | POST autenticado (Bearer token automático) |
| `postJsonNullable(path, body)` | POST autenticado, retorna `null` em 404 |
| `postJsonPublic(path, body)` | POST sem autenticação (endpoints `/api/auth/*`) |
| `getJson(path)` | GET sem autenticação |
| `getBytes(path)` | GET binário autenticado (imagens) |
| `putBytes(path, data)` | PUT binário autenticado (upload de imagens) |
| `addProjection(body, projection)` | Serializa projeção de campos no body JSON |

Configuração do Gson:
- `OffsetDateTimeAdapter` — datas como ISO-8601
- `CircularRefExclusionStrategy` — exclui `PurchaseItem.purchase` e `Product.image`
- Timeouts: 30s (connect, read, write)

### RestAuthClient

Gerenciador de autenticação no lado cliente:

| Método | Descrição |
|--------|-----------|
| `login(userName, passwordHash)` | Fluxo completo: challenge → HMAC-SHA256 digest → login → armazena tokens |
| `refresh()` | Renova tokens via refresh endpoint |
| `logout()` | Invalida tokens no servidor e limpa estado local |
| `encryptPassword(plainPassword)` | Criptografa senha com RSA (chave pública da sessão) |
| `accessToken()` | Retorna token atual (usado por `RestConfig` para auto-auth) |
| `isAuthenticated()` | Verifica se há sessão ativa |

**Fluxo de autenticação:**

```
1. GET /api/auth/challenge → { nonce, expiresAt }
2. digest = HMAC-SHA256(key=passwordHash, data=userName+nonce)
3. POST /api/auth/login { userName, digest, nonce }
   → { accessToken, refreshToken, publicKey, expiresAt }
4. Requests subsequentes: Authorization: Bearer <accessToken>
```

### RestAuthenticationService

Implementa `AuthenticationService` do domínio sobre HTTP:

| Método | Delegação |
|--------|-----------|
| `challenge()` | `GET /api/auth/challenge` → `ChallengeResult` |
| `login(userName, digest, nonce)` | `POST /api/auth/login` → `AuthResult` |
| `refresh(refreshToken)` | `POST /api/auth/refresh` → `AuthResult` |
| `logout(refreshToken)` | `POST /api/auth/logout` |
| `resolveToken()` | Retorna `null` (operação server-side apenas) |

Ao receber tokens de login/refresh, atualiza automaticamente o `RestAuthClient` para que chamadas subsequentes aos repositórios incluam o Bearer token.

### RestRepositoryBootstrap

Bootstrap que registra todas as implementações REST nos `BEAN` estáticos do domínio:

```java
var config = new RestConfig("http://localhost:8080");
RestRepositoryBootstrap.initialize(config);
// UserRepository.BEAN       → RestUserRepository
// ProductRepository.BEAN    → RestProductRepository
// PurchaseRepository.BEAN   → RestPurchaseRepository
// PurchaseItemRepository.BEAN → RestPurchaseItemRepository
// AuthenticationService.BEAN  → RestAuthenticationService

// Ao finalizar:
RestRepositoryBootstrap.release();
```

### Repositórios REST

Cada `RestXxxRepository` implementa a interface de domínio correspondente, mapeando operações para chamadas HTTP:

| Operação | HTTP | Path |
|----------|------|------|
| `insert(entity)` | POST | `/api/repo/{entity}/insert` |
| `update(newEntity, oldEntity)` | POST | `/api/repo/{entity}/update` |
| `insertOrUpdate(entity)` | POST | `/api/repo/{entity}/upsert` |
| `delete(criteria)` | POST | `/api/repo/{entity}/delete` |
| `count(criteria)` | POST | `/api/repo/{entity}/count` |
| `fetch(criteria)` | POST | `/api/repo/{entity}/fetch` |
| `fetchById(id, projection)` | POST | `/api/repo/{entity}/fetchById` |

O `RestProductRepository` possui adicionalmente:
- `fetchImage(id)` → `GET /api/repo/product/{id}/image` (bytes PNG)
- `updateImage(id, data)` → `PUT /api/repo/product/{id}/image` (upload)

### Tratamento de Referências Circulares

O Gson exclui automaticamente `PurchaseItem.purchase` e `Product.image` via `CircularRefExclusionStrategy`. Para compensar:
- `RestPurchaseItemRepository` injeta manualmente `purchaseId` no body JSON antes de enviar
- Imagens de produto são transferidas por endpoints binários dedicados

## Uso

O módulo é usado por:

| Consumidor | Contexto |
|-----------|---------|
| **Android** (`view.android`) | Modo remoto — acessa backend Javalin via rede |
| **Testes de integração** (`shopping.tests`) | `RestXxxRepositoryTest` valida API end-to-end |
| **Javalin** (`view.react.javalin`) | Registra `RestAuthenticationService` para propagação de auth |
