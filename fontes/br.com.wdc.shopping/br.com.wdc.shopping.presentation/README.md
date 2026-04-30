# WDC Shopping — Presentation

Camada de apresentação do sistema Shopping, implementada com o padrão **Cube MVP**. Contém os presenters, view states, serviços de apresentação, DTOs e o sistema de navegação. Esta camada é agnóstica de tecnologia de view — não depende de React, Javalin ou qualquer framework de UI.

## Dependências

| Artefato | Papel |
|----------|-------|
| `br.com.wdc.shopping.domain` | Modelos de domínio, repositórios, critérios |
| `br.com.wdc.framework.cube` | Motor Cube MVP (presenters, views, navegação) |
| `slf4j-api` | Logging |
| `jsr305` | Anotações `@Nullable` (scope provided) |

## Estrutura de Pacotes

```
br.com.wdc.shopping.presentation
├── ShoppingApplication.java           # Aplicação Cube (raiz)
├── PlaceAttributes.java               # Constantes de atributos de navegação
├── PlaceParameters.java               # Constantes de parâmetros de URL
│
├── function/
│   └── GoAction.java                  # Interface funcional de navegação
│
├── exception/
│   ├── WrongPlace.java                # Navegação para place inválido
│   ├── WrongParametersException.java  # Parâmetros de navegação inválidos
│   ├── ProductNotFoundException.java  # Produto não encontrado
│   └── PurchaseNotFoundException.java # Compra não encontrada
│
└── presenter/
    ├── Routes.java                    # Definição de places e funções de navegação
    ├── RootPresenter.java             # Presenter raiz (container)
    ├── RootViewState.java             # Estado da view raiz
    │
    ├── open/login/                    # Área pública
    │   ├── LoginPresenter.java        # Presenter de login
    │   ├── LoginViewState.java        # Estado da tela de login
    │   ├── LoginService.java          # Serviço de autenticação
    │   └── structs/
    │       └── Subject.java           # Usuário autenticado (DTO)
    │
    └── restricted/                    # Área autenticada
        ├── home/
        │   ├── HomePresenter.java     # Presenter principal (container)
        │   ├── HomeViewState.java     # Estado da tela principal
        │   ├── HomeService.java       # Serviço de compras/consultas
        │   ├── structs/
        │   │   └── PurchaseInfo.java  # DTO de compra resumida
        │   ├── products/
        │   │   ├── ProductsPanelPresenter.java   # Painel de produtos (child)
        │   │   └── ProductsPanelViewState.java   # Estado do painel
        │   └── purchases/
        │       ├── PurchasesPanelPresenter.java  # Painel de compras (child)
        │       └── PurchasesPanelViewState.java  # Estado do painel
        │
        ├── products/
        │   ├── ProductPresenter.java  # Detalhe de produto
        │   ├── ProductViewState.java  # Estado do detalhe
        │   ├── ProductService.java    # Serviço de produtos
        │   └── structs/
        │       └── ProductInfo.java   # DTO de produto
        │
        ├── cart/
        │   ├── CartPresenter.java     # Carrinho de compras
        │   ├── CartViewState.java     # Estado do carrinho
        │   ├── CartManager.java       # Lógica do carrinho (in-memory)
        │   ├── CartService.java       # Serviço de efetivação de compra
        │   └── structs/
        │       └── CartItem.java      # DTO de item do carrinho
        │
        └── receipt/
            ├── ReceiptPresenter.java  # Comprovante de compra
            ├── ReceiptViewState.java  # Estado do comprovante
            ├── ReceiptService.java    # Serviço de recibos
            └── structs/
                ├── ReceiptForm.java   # DTO do recibo
                └── ReceiptItem.java   # DTO de item do recibo
```

## Hierarquia de Navegação

O sistema possui 6 places organizados hierarquicamente:

```
ROOT (public)
├── LOGIN (public/login)       ← área pública
└── HOME (home)                ← área restrita (requer Subject)
    ├── PRODUCT (product)      ← detalhe de produto (?productId=N)
    ├── CART (cart)             ← carrinho de compras
    └── RECEIPT (receipt)       ← comprovante (?purchaseId=N)
```

A navegação é definida em `Routes.java` usando o builder `app.navigate().step(Place).execute(intent)`:

```java
// Navegar para o detalhe de um produto
var intent = app.newIntent();
intent.setParameter(PlaceParameters.PRODUCT_ID, productId);
Routes.product(app, intent);
```

O `RootPresenter` decide automaticamente se direciona para `LOGIN` ou `HOME` baseado na presença do `Subject`.

## Hierarquia de Presenters

```
RootPresenter (AbstractCubePresenter)
│   Gerencia: contentSlot → view filha
│   Estado: RootViewState { contentView, errorMessage }
│
├── LoginPresenter (AbstractCubePresenter)
│       Gerencia: autenticação
│       Estado: LoginViewState { userName, password, errorMessage }
│       Serviço: LoginService
│
└── HomePresenter (AbstractCubePresenter)
    │   Gerencia: contentSlot + painéis filhos + CartManager
    │   Estado: HomeViewState { nickName, cartItemCount, contentView,
    │           productsPanelView, purchasesPanelView, errorMessage }
    │
    ├── ProductsPanelPresenter (AbstractChildPresenter)
    │       Gerencia: lista de produtos
    │       Estado: ProductsPanelViewState { products[] }
    │
    ├── PurchasesPanelPresenter (AbstractChildPresenter)
    │       Gerencia: histórico paginado
    │       Estado: PurchasesPanelViewState { purchases[], page, pageSize, totalCount }
    │
    ├── ProductPresenter (AbstractCubePresenter)
    │       Gerencia: detalhe de produto + adicionar ao carrinho
    │       Estado: ProductViewState { product, errorMessage }
    │       Parâmetro: PRODUCT_ID
    │
    ├── CartPresenter (AbstractCubePresenter)
    │       Gerencia: edição do carrinho + efetivação de compra
    │       Estado: CartViewState { items[], errorMessage }
    │
    └── ReceiptPresenter (AbstractCubePresenter)
            Gerencia: exibição do comprovante
            Estado: ReceiptViewState { receipt, notifySuccess }
            Parâmetro: PURCHASE_ID
```

### Dois tipos de presenter

| Tipo | Classe base | Ciclo de vida | Uso |
|------|------------|---------------|-----|
| **Presenter navegável** | `AbstractCubePresenter<ShoppingApplication>` | Gerenciado pelo sistema de navegação via `applyParameters` | Places no `Routes` |
| **Presenter filho (painel)** | `AbstractChildPresenter<ShoppingApplication>` | Gerenciado manualmente pelo presenter pai via `initialize()`/`release()` | Painéis embutidos na Home |

## Componentes do Módulo

### ShoppingApplication

Classe abstrata que estende `CubeApplication`. Mantém o estado global:

- `subject` — usuário autenticado (`Subject`)
- `cart` — carrinho de compras (`CartManager`)
- `go(String)` / `go(CubeIntent)` — navegação programática
- `alertUnexpectedError(...)` — exibição de erros via `RootPresenter`

### ViewState

Cada presenter possui um `ViewState` que implementa `ViewState.write()` para serializar o estado em JSON. Este JSON é enviado ao frontend via WebSocket.

```java
public class ProductViewState implements ViewState {
    public ProductInfo product;
    public int errorCode;
    public String errorMessage;

    @Override
    public void write(String instanceId, ExtensibleObjectOutput json) {
        json.beginObject();
        json.name("id").value(instanceId);
        // ... serialização dos campos
        json.endObject();
    }
}
```

### Services

Serviços são implementados como `enum` singleton (`BEAN`), fazendo a ponte entre a camada de apresentação e os repositórios da camada de negócio:

| Serviço | Responsabilidade |
|---------|-----------------|
| `LoginService` | Autenticação (busca `Subject` por user/password) |
| `ProductService` | Carga de produtos (por ID, lista sem descrição) |
| `HomeService` | Consulta de compras (paginação, contagem) |
| `CartService` | Efetivação de compra (persiste `Purchase` + `PurchaseItem`) |
| `ReceiptService` | Carga de recibos |

### DTOs (structs)

Objetos `Serializable` usados para transferir dados entre camadas. Cada DTO possui:

- **Campos públicos** (sem getters/setters)
- **`projection()`** — retorna uma instância do modelo de domínio indicando quais campos carregar
- **`create(Model)`** — factory method que converte do modelo de domínio para o DTO

| DTO | Origem | Campos principais |
|-----|--------|------------------|
| `Subject` | `User` | id, nickName |
| `ProductInfo` | `Product` | id, image, name, description, price |
| `CartItem` | `ProductInfo` | id, image, name, price, quantity |
| `PurchaseInfo` | `Purchase` | id, date, total, items (nomes) |
| `ReceiptForm` | `Purchase` | date, total, items |
| `ReceiptItem` | `PurchaseItem` | id, description, value, quantity |

### Constantes

- **`PlaceAttributes`** — identificadores de view slots e atributos transientes de navegação
- **`PlaceParameters`** — nomes de parâmetros de URL (`userId`, `productId`, `purchaseId`)

### CartManager

Gerenciador de carrinho in-memory com sistema de eventos:

- `addProduct(ProductInfo, quantity)` — adiciona ou incrementa item
- `modifyProductQuantity(productId, quantity)` — altera quantidade
- `removeProduct(productId)` — remove item
- `commit(Subject)` — persiste a compra via `CartService`
- `addCommitListener(Runnable)` / `addChangeListener(Runnable)` — listeners para recarregar painéis

## Convenções

### Organização de pacotes por feature

Cada feature segue a estrutura:
```
presenter/restricted/<feature>/
├── <Feature>Presenter.java      # Presenter
├── <Feature>ViewState.java      # Estado serializável
├── <Feature>Service.java        # Serviço (enum BEAN)
└── structs/
    └── <FeatureDTO>.java        # DTOs
```

### Padrão de um Presenter

```java
public class XxxPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // 1. Logger
    private static final Logger LOG = LoggerFactory.getLogger(XxxPresenter.class);

    // 2. Factory de view (injetada pelo skeleton)
    public static Function<XxxPresenter, CubeView> createView;

    // 3. Estado público
    public final XxxViewState state = new XxxViewState();

    // 4. Campos internos
    private CubeViewSlot ownerSlot;

    // 5. Constructor
    public XxxPresenter(ShoppingApplication app) { super(app); }

    // 6. Cube API (applyParameters, publishParameters)
    // 7. User Actions (onXxx)
    // 8. Messages (alertXxx, errorXxx)
}
```

### Padrão de erros

Os presenters não lançam exceções para a view. Em vez disso, usam `errorCode` + `errorMessage` no ViewState e chamam `this.update()`:

```java
private void alertProductNotFound() {
    this.state.errorCode = 3;
    this.state.errorMessage = "Código do produto não localizado.";
    this.update();
}
```

Erros inesperados são delegados ao `RootPresenter`:

```java
this.app.alertUnexpectedError(LOG, "mensagem de contexto", caught);
```
