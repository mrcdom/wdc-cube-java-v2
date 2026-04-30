# br.com.wdc.shopping.tests

Módulo de **testes automatizados** da aplicação Shopping. Contém testes que exercitam desde serviços de negócio até workflows completos na camada de apresentação, sem necessidade de servidor HTTP ou navegador.

## Estrutura

```
src/main/java/br/com/wdc/shopping/test/
├── UserRepositoryTest.java            — CRUD completo do UserRepository (17 testes)
├── ProductRepositoryTest.java         — CRUD completo do ProductRepository (19 testes)
├── PurchaseRepositoryTest.java        — CRUD completo do PurchaseRepository (20 testes)
├── PurchaseItemRepositoryTest.java    — CRUD completo do PurchaseItemRepository (23 testes)
├── ShoppingLoginTest.java             — Testes de fluxo de login
├── ShoppingServiceTest.java           — Testes de serviços de negócio (queries, projeções)
├── ShoppingWorkflowTest.java          — Teste de workflow completo na camada de apresentação
│
├── mock/
│   ├── ShoppingApplicationMock.java   — Mock da aplicação (substitui ShoppingApplication)
│   └── viewimpl/                      — Mocks de views (substituem implementações reais)
│       ├── AbstractViewMock.java      — Base para todos os mocks de view
│       ├── RootViewMock.java
│       ├── LoginViewMock.java
│       ├── RestrictedViewMock.java
│       ├── CartViewMock.java
│       ├── ProductViewMock.java
│       ├── ProductsPanelViewMock.java
│       ├── PurchasesPanelViewMock.java
│       └── ReceiptViewMock.java
│
└── util/
    ├── BaseBusinessTest.java                   — Setup: H2 in-memory, DataSource, repositórios
    ├── BasePresentationTest.java               — Setup: BaseBusinessTest + ShoppingApplicationMock
    ├── ScheduledExecutorForTest.java           — Interface de executor para testes
    ├── ScheduledExecutorForTestAsync.java      — Executor assíncrono
    ├── ScheduledExecutorForTestSyncDirect.java — Executor síncrono direto
    └── ScheduledExecutorForTestSyncDelayed.java— Executor síncrono com delay
```

## Abordagem

### Testes de repositório (`*RepositoryTest`)

Exercitam diretamente cada operação de cada repositório — `insert`, `update`, `insertOrUpdate`, `delete`, `count`, `fetch`, `fetchById` — incluindo:

- **Projeções** (`ProjectionValues`) para verificar que apenas campos solicitados são retornados
- **Critérios de filtro** (por ID, por campos específicos, por FK)
- **Paginação** (`offset` / `limit`) e **ordenação** (`OrderBy.ACENDING` / `DESCENDING`)
- **Casos de borda**: IDs inexistentes, delete sem resultados, constraints de FK

Cada teste parte de um banco limpo com dados de seed (`DBReset`), garantindo determinismo.

| Classe | Repositório | Testes |
|---|---|---|
| `UserRepositoryTest` | `UserRepository` | 17 |
| `ProductRepositoryTest` | `ProductRepository` + `fetchImage` | 19 |
| `PurchaseRepositoryTest` | `PurchaseRepository` | 20 |
| `PurchaseItemRepositoryTest` | `PurchaseItemRepository` | 23 |

### Testes de serviço (`ShoppingServiceTest`)

Exercitam diretamente os serviços e repositórios do domínio (login, consulta de produtos, compras, recibos) sobre um banco H2 in-memory populado com dados de seed (`DBReset`).

### Testes de apresentação (`ShoppingLoginTest`, `ShoppingWorkflowTest`)

Simulam a interação do usuário **no nível de apresentação** usando mocks de view. Cada view mock expõe o `state` e o `presenter`, permitindo:

1. Chamar ações do presenter como o usuário faria (ex.: `loginView.presenter.onEnter()`)
2. Inspecionar o estado resultante (ex.: `restrictedView.state.errorCode`)
3. Navegar entre telas verificando transições (ex.: login → home → produto → carrinho → recibo)

#### Exemplo: workflow completo de compra

```java
public class ShoppingWorkflowTest extends BasePresentationTest {

    @Test
    public void testComprarProduto() throws Exception {
        Routes.login(this.app);
        var rootView = this.app.getRootView();

        // Login
        var loginView = LoginViewMock.cast(rootView.state.contentView);
        loginView.state.userName = "admin";
        loginView.state.password = "admin";
        loginView.presenter.onEnter();

        // Navegar para produto
        var restrictedView = RestrictedViewMock.cast(rootView.state.contentView);
        restrictedView.presenter.onOpenProduct(DBReset.PEN_DRIVE2GB_ID);

        // Adicionar ao carrinho
        var produtoView = ProductViewMock.cast(restrictedView.state.contentView);
        produtoView.presenter.onAddToCart(1);

        // Finalizar compra
        var carrinhoView = CartViewMock.cast(restrictedView.state.contentView);
        carrinhoView.presenter.onBuy();

        // Verificar recibo
        var reciboView = ReceiptViewMock.cast(restrictedView.state.contentView);
        Assert.assertNotNull(reciboView.state.receipt);
    }
}
```

Este teste navega por **5 telas** (login → home → produto → carrinho → recibo) exercitando presenters reais com repositórios reais — apenas as views são mocks.

## Infraestrutura de teste

### `BaseBusinessTest`

- Cria banco H2 in-memory (`jdbc:h2:mem:wedocode-shopping`)
- Configura `SqlDataSource`, `ScheduledExecutor` e `RepositoryBootstrap`
- No `@Before` de cada teste, executa `DBCreate` + `DBReset` (banco limpo a cada teste)

### `BasePresentationTest`

Estende `BaseBusinessTest` e instancia `ShoppingApplicationMock`, que registra view mocks via `Presenter.createView`.

### View mocks

Cada mock (`LoginViewMock`, `CartViewMock`, etc.) estende `AbstractViewMock<P>` e expõe:
- `state` — o `ViewState` do presenter (acesso direto para leitura/escrita)
- `presenter` — o presenter real para invocar ações
- `cast(CubeView)` — método estático para obter o mock com asserção de tipo

## Dependências

- `br.com.wdc.shopping.persistence` — repositórios reais
- `br.com.wdc.shopping.presentation` — presenters e serviços reais
- `br.com.wdc.shopping.scripts` — `DBCreate` / `DBReset` para setup do banco
- H2 Database — banco in-memory
- JUnit 4 — framework de testes
- Logback — logging nos testes
