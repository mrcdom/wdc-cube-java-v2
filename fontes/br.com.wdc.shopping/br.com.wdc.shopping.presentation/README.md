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

```mermaid
graph TD
    root["br.com.wdc.shopping.presentation"]
    root --> ShoppingApp["ShoppingApplication.java"]
    root --> ProxyWrapper["ProxyRepositoryWrapper.java"]
    root --> PlaceAttr["PlaceAttributes.java"]
    root --> PlaceParams["PlaceParameters.java"]

    root --> function["function/"]
    function --> GoAction["GoAction.java"]

    root --> exception["exception/"]
    exception --> WrongPlace["WrongPlace.java"]
    exception --> WrongParams["WrongParametersException.java"]
    exception --> ProductNotFound["ProductNotFoundException.java"]
    exception --> PurchaseNotFound["PurchaseNotFoundException.java"]

    root --> presenter["presenter/"]
    presenter --> Routes["Routes.java"]
    presenter --> RootP["RootPresenter.java (+ RootViewState)"]

    presenter --> open["open/"]
    open --> OpenP["OpenPresenter.java"]
    open --> login["login/"]
    login --> LoginP["LoginPresenter.java (+ LoginViewState)"]
    login --> LoginSvc["LoginService.java"]
    login --> LoginStructs["structs/Subject.java"]

    presenter --> restricted["restricted/"]
    restricted --> RestrictedP["RestrictedPresenter.java"]
    restricted --> home["home/"]
    home --> HomeP["HomePresenter.java (+ HomeViewState)"]
    home --> HomeStructs["structs/PurchaseInfo.java"]
    home --> products_panel["products/"]
    products_panel --> ProdPanelP["ProductsPanelPresenter.java (+ ViewState)"]
    home --> purchases_panel["purchases/"]
    purchases_panel --> PurchPanelP["PurchasesPanelPresenter.java (+ ViewState)"]
    purchases_panel --> PurchPanelSvc["PurchasesPanelService.java"]

    restricted --> products["products/"]
    products --> ProductP["ProductPresenter.java (+ ProductViewState)"]
    products --> ProductSvc["ProductService.java"]
    products --> ProductStructs["structs/ProductInfo.java"]

    restricted --> cart["cart/"]
    cart --> CartP["CartPresenter.java (+ CartViewState)"]
    cart --> CartMgr["CartManager.java"]
    cart --> CartStructs["structs/CartItem.java"]

    restricted --> receipt["receipt/"]
    receipt --> ReceiptP["ReceiptPresenter.java (+ ReceiptViewState)"]
    receipt --> ReceiptSvc["ReceiptService.java"]
    receipt --> ReceiptStructs["structs/ReceiptForm.java, ReceiptItem.java"]
```

## Hierarquia de Navegação

O sistema possui 8 places organizados em 4 níveis:

```mermaid
graph TD
    ROOT["ROOT (public)"]
    ROOT --> OPEN["OPEN (open)<br/><small>guarda: se autenticado → RESTRICTED</small>"]
    ROOT --> RESTRICTED["RESTRICTED (restricted)<br/><small>guarda: se não autenticado → OPEN</small>"]
    OPEN --> LOGIN["LOGIN (login)<br/><small>área pública</small>"]
    RESTRICTED --> HOME["HOME (home)<br/><small>área restrita (requer Subject)</small>"]
    HOME --> PRODUCT["PRODUCT (product)<br/><small>?productId=N</small>"]
    HOME --> CART["CART (cart)"]
    HOME --> RECEIPT["RECEIPT (receipt)<br/><small>?purchaseId=N</small>"]
```

A navegação é definida em `Routes.java` usando o builder `app.navigate().step(Place).execute(intent)`:

```java
// Navegar para o detalhe de um produto
var intent = app.newIntent();
intent.setParameter(PlaceParameters.PRODUCT_ID, productId);
Routes.product(app, intent);
```

Os presenters `OpenPresenter` e `RestrictedPresenter` funcionam como **guardas de navegação**: redirecionam automaticamente com base na presença/ausência do `Subject`.

## Hierarquia de Presenters

```mermaid
graph TD
    RootP["RootPresenter<br/><small>contentSlot → view filha<br/>Inner: RootViewState</small>"]

    RootP --> OpenP["OpenPresenter<br/><small>Guarda: redireciona se autenticado</small>"]
    RootP --> RestrictedP["RestrictedPresenter<br/><small>Guarda: redireciona se não autenticado</small>"]

    OpenP --> LoginP["LoginPresenter<br/><small>Autenticação<br/>Inner: LoginViewState<br/>Serviço: LoginService</small>"]

    RestrictedP --> HomeP["HomePresenter<br/><small>contentSlot + painéis + CartManager<br/>Inner: HomeViewState</small>"]

    HomeP --> ProdPanelP["ProductsPanelPresenter<br/><small>AbstractChildPresenter<br/>Inner: ProductsPanelViewState</small>"]
    HomeP --> PurchPanelP["PurchasesPanelPresenter<br/><small>AbstractChildPresenter<br/>Inner: PurchasesPanelViewState</small>"]
    HomeP --> ProductP["ProductPresenter<br/><small>Detalhe + adicionar ao carrinho<br/>Inner: ProductViewState</small>"]
    HomeP --> CartP["CartPresenter<br/><small>Edição + efetivação de compra<br/>Inner: CartViewState</small>"]
    HomeP --> ReceiptP["ReceiptPresenter<br/><small>Comprovante<br/>Inner: ReceiptViewState</small>"]
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
- `securityContext` — contexto de segurança (`SecurityContext`) com roles e permissões
- `cart` — carrinho de compras (`CartManager`)
- `createDelegate(Class, Object)` — hook para criação de proxies de repositório (ver `ProxyRepositoryWrapper`)
- `go(String)` / `go(CubeIntent)` — navegação programática
- `alertUnexpectedError(...)` — exibição de erros via `RootPresenter`

### ProxyRepositoryWrapper

Classe utilitária que cria proxies dinâmicos (via `java.lang.reflect.Proxy`) para repositórios, envolvendo cada chamada com o `SecurityContext`. Usado em ambientes JVM multi-threaded (desktop, servidor). Não compatível com TeaVM/GraalVM native.

### ViewState (inner class)

Cada presenter define um `ViewState` como **classe estática interna** (`public static class XxxViewState implements ViewState`). A interface `ViewState` é marcadora (sem métodos) — a serialização é feita automaticamente pelo `ViewStateSerializer` via reflection dos campos públicos:

```java
public static class ProductViewState implements ViewState {
    public ProductInfo product;
    public int errorCode;
    public String errorMessage;
}
```

O JSON gerado pelo serializer usa `"#"` como chave para o `instanceId`:

```json
{ "#": "view-instance-id", "product": {...}, "errorCode": 0, "errorMessage": null }
```
```

### Services

Serviços são classes com injeção de dependências via construtor (`ShoppingApplication` ou repositório específico), fazendo a ponte entre a camada de apresentação e os repositórios:

| Serviço | Responsabilidade |
|---------|------------------|
| `LoginService` | Autenticação via HMAC challenge-response (usa `AuthenticationService`) |
| `ProductService` | Carga de produtos (por ID, lista sem descrição) |
| `PurchasesPanelService` | Consulta de compras (paginação, contagem) |
| `ReceiptService` | Carga de recibos |

### DTOs (structs)

Objetos `Serializable` usados para transferir dados entre camadas. Cada DTO possui:

- **Campos públicos** (sem getters/setters)
- **`projection()`** — retorna uma instância do modelo de domínio indicando quais campos carregar
- **`create(Model)`** — factory method que converte do modelo de domínio para o DTO

| DTO | Pacote | Campos principais |
|-----|--------|------------------|
| `Subject` | `open/login/structs/` | id, nickName |
| `ProductInfo` | `restricted/products/structs/` | id, image, name, description, price |
| `CartItem` | `restricted/cart/structs/` | id, image, name, price, quantity |
| `PurchaseInfo` | `restricted/home/structs/` | id, date, total, items (nomes) |
| `ReceiptForm` | `restricted/receipt/structs/` | date, total, items |
| `ReceiptItem` | `restricted/receipt/structs/` | id, description, value, quantity |

### Constantes

- **`PlaceAttributes`** — identificadores de view slots e atributos transientes de navegação
- **`PlaceParameters`** — nomes de parâmetros de URL (`userId`, `productId`, `purchaseId`)

### CartManager

Gerenciador de carrinho in-memory com sistema de eventos:

- `addProduct(ProductInfo, quantity)` — adiciona ou incrementa item
- `modifyProductQuantity(productId, quantity)` — altera quantidade
- `removeProduct(productId)` — remove item
- `commit(Subject)` — persiste a compra diretamente via repositórios
- `addCommitListener(Runnable)` / `addChangeListener(Runnable)` — listeners para recarregar painéis

## Convenções

### Organização de pacotes por feature

Cada feature segue a estrutura:
```mermaid
graph TD
    feature["presenter/restricted/&lt;feature&gt;/"]
    feature --> Presenter["&lt;Feature&gt;Presenter.java<br/><small>(inner: &lt;Feature&gt;ViewState)</small>"]
    feature --> Service["&lt;Feature&gt;Service.java (opcional)"]
    feature --> structs["structs/ (opcional)"]
    structs --> DTO["&lt;FeatureDTO&gt;.java"]
```

### Padrão de um Presenter

```java
public class XxxPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // 1. Logger
    private static final Logger LOG = LoggerFactory.getLogger(XxxPresenter.class);

    // 2. ViewState (inner class — interface marcadora, serialização via reflection)
    public static class XxxViewState implements ViewState {
        public int errorCode;
        public String errorMessage;
        // ... campos específicos
    }

    // 3. Factory de view (injetada pelo skeleton)
    public static Function<XxxPresenter, CubeView> createView;

    // 4. Estado público
    public final XxxViewState state = new XxxViewState();

    // 5. Campos internos
    private CubeViewSlot ownerSlot;

    // 6. Constructor
    public XxxPresenter(ShoppingApplication app) { super(app); }

    // 7. Cube API (applyParameters, publishParameters)
    // 8. User Actions (onXxx)
    // 9. Messages (alertXxx, errorXxx)
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
