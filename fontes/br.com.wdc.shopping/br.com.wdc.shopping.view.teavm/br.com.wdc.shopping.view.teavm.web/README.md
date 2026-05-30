# WDC Shopping — TeaVM Web

Código-fonte Java das views TeaVM e compilação para JavaScript. Este módulo contém a implementação completa da UI usando **Virtual DOM** (VNode API) com Spectrum Web Components, que é compilada pelo TeaVM para um SPA executável no browser.

## Arquitetura

```
Browser (JavaScript)
├── index.html                          ← Carrega Spectrum WC + CSS + app.js
└── js/app.js                           ← Java compilado para JS pelo TeaVM
    ├── Main.java                       ← Entry point
    ├── ShoppingTeaVMApplication.java   ← Wiring de factories e render loop
    ├── AbstractViewTeaVM.java          ← Classe base (legacy HtmlDom)
    ├── vdom/
    │   └── AbstractVDomView.java       ← Classe base VDom (render + diff + useCallback)
    ├── FetchHttpTransport.java         ← XMLHttpRequest (REST API calls)
    ├── BrowserCryptoProvider.java      ← Web Crypto API (HMAC-SHA256)
    ├── ScheduledExecutorBrowser.java   ← setTimeout/setInterval
    ├── interop/                        ← JSO bridges (Console, Timers, Fetch)
    ├── repo/                           ← Repositórios via REST API
    ├── util/                           ← DateUtils
    └── views/                          ← 8 views (VDom)
        ├── RootViewVDom.java
        ├── LoginViewVDom.java
        ├── HomeViewVDom.java
        ├── ProductsPanelViewVDom.java
        ├── ProductViewVDom.java
        ├── CartViewVDom.java
        ├── ReceiptViewVDom.java
        └── PurchasesPanelViewVDom.java
```

## Virtual DOM

As views utilizam a API `VNode` para construir árvores virtuais de forma declarativa. O framework realiza diff eficiente entre renders, aplicando apenas as mutações necessárias no DOM real:

```java
@Override
protected VNode render() {
    return div(Css.ROOT).children(
      div(Css.WRAPPER).children(
        h5(Css.TITLE).text(state.product.name),
        spButton("accent")
          .boolAttr("disabled", state.items.isEmpty())
          .children(span(Css.ICON_ADD), span().text("Adicionar"))
          .on("click", onAddToCart)));
}
```

### Otimizações de Referência

O diff de event listeners compara por **identidade de referência** — mesma referência = zero custo no DOM. Duas técnicas garantem estabilidade:

1. **Stable fields** — listeners sem parâmetros extraídos para campos `final`:
   ```java
   private final EventListener<Event> onBack = evt -> presenter.onOpenProducts();
   ```

2. **`useCallback(key, listener)`** — cache de listeners paramétricos (análogo ao React `useCallback`):
   ```java
   .on("click", useCallback("remove-" + itemId, mkOnRemove(itemId)))
   ```
   Retorna instância cacheada se a key já existe; após cada render, keys não utilizadas são descartadas automaticamente.

### Padrão Compact Css

Cada view define uma interface `Css` privada com aliases compactos para as classes do framework:

```java
@SuppressWarnings({"java:S1214", "static-access"})
private interface Css {
    CssUtility u = CssUtility.INSTANCE;
    CssComponents c = CssComponents.INSTANCE;
    CssIcons icon = CssIcons.INSTANCE;

    String ROOT = u.PAGE_SCROLL_ROOT;
    String ERROR = clsx(c.ALERT_ERROR, u.MB_12);
    String ICON_ADD = clsx(icon.BAG_PLUS, u.MR_6);
}
```

Isso permite escrever `Css.ROOT` em vez de `CssUtility.PAGE_SCROLL_ROOT`, mantendo o render method limpo e legível.

## Comunicação com o Servidor

As views se comunicam com o back-end Javalin via REST API usando `XMLHttpRequest` (implementado em `FetchHttpTransport`). A autenticação utiliza HMAC-SHA256 via Web Crypto API (`BrowserCryptoProvider`).

## Responsividade

O layout adapta-se a telas pequenas (iPhone SE) utilizando classes utilitárias responsivas. No cabeçalho, textos auxiliares são ocultados em telas estreitas, mantendo apenas os ícones essenciais.

## Build

```bash
# Via script (recomendado)
JAVA21_HOME=<caminho-para-jdk-21> bash build.sh

# Ou diretamente com Maven
JAVA_HOME=$JAVA21_HOME mvn process-classes -DskipTests
```

### Output

```
target/classes/META-INF/resources/teavm/
├── index.html    ← Página HTML (carrega Spectrum WC + app.js)
└── js/
    └── app.js    ← Java compilado para JavaScript (~8000 métodos)
```

## Executar

O módulo não tem servidor próprio. O SPA é servido como recurso estático pelo servidor Javalin (`backend`) ou embutido pelo módulo `teavm.native` (Tauri):

```
http://localhost:8080/teavm
```

## Estrutura do Projeto

```
br.com.wdc.shopping.view.teavm.web/
├── build.sh                    ← Script de build simplificado
├── pom.xml                     ← Configuração Maven + plugin TeaVM
└── src/main/
    ├── java/.../view/teavm/
    │   ├── Main.java
    │   ├── ShoppingTeaVMApplication.java
    │   ├── AbstractViewTeaVM.java          ← Legacy (HtmlDom)
    │   ├── vdom/
    │   │   └── AbstractVDomView.java       ← VDom base (render/diff/useCallback)
    │   ├── FetchHttpTransport.java
    │   ├── BrowserCryptoProvider.java
    │   ├── BrowserSessionStorage.java
    │   ├── IntentSigner.java
    │   ├── ScheduledExecutorBrowser.java
    │   ├── interop/            ← Console, Timers, FetchApi
    │   ├── repo/               ← Repositórios REST
    │   ├── util/               ← DateUtils
    │   └── views/              ← 8 views VDom (Login, Home, Products, etc.)
    └── webapp/
        ├── index.html
        └── css/app.css         ← Utility classes + componentes + ícones
```

## Screenshots

### Login

![Login](docs/screenshots/01-login.png)

### Lista de Produtos

![Produtos](docs/screenshots/02-products.png)

### Detalhe do Produto

![Detalhe do Produto](docs/screenshots/03-product-detail.png)

### Carrinho

![Carrinho](docs/screenshots/04-cart.png)

### Recibo

![Recibo](docs/screenshots/05-receipt.png)

### Histórico de Compras

![Histórico](docs/screenshots/06-history.png)
