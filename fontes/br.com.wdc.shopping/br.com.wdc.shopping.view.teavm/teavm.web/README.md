# WDC Shopping — TeaVM Web

Código-fonte Java das views TeaVM e compilação para JavaScript. Este módulo contém a implementação completa da UI usando **Virtual DOM** (VNode API) com Spectrum Web Components, que é compilada pelo TeaVM para um SPA executável no browser.

## Arquitetura

```mermaid
graph TD
    browser["Browser (JavaScript)"]
    indexHtml["index.html<br/><small>Carrega Spectrum WC + CSS + app.js</small>"]
    appJs["js/app.js<br/><small>Java compilado para JS pelo TeaVM</small>"]
    main["Main.java<br/><small>Entry point</small>"]
    app["ShoppingTeaVMApplication.java<br/><small>Wiring de factories e render loop</small>"]
    base["AbstractViewTeaVM.java<br/><small>Classe base (legacy HtmlDom)</small>"]
    vdom["vdom/<br/><small>AbstractVDomView.java — VDom base (render + diff + useCallback)</small>"]
    fetch["FetchHttpTransport.java<br/><small>XMLHttpRequest (REST API calls)</small>"]
    crypto["BrowserCryptoProvider.java<br/><small>Web Crypto API (HMAC-SHA256)</small>"]
    sched["ScheduledExecutorBrowser.java<br/><small>setTimeout/setInterval</small>"]
    interop["interop/<br/><small>JSO bridges (Console, Timers, Fetch)</small>"]
    repo["repo/<br/><small>Repositórios via REST API</small>"]
    util["util/<br/><small>DateUtils</small>"]
    views["views/<br/><small>RootViewVDom · LoginViewVDom · HomeViewVDom<br/>ProductsPanelViewVDom · ProductViewVDom · CartViewVDom<br/>ReceiptViewVDom · PurchasesPanelViewVDom</small>"]

    browser --> indexHtml
    browser --> appJs
    appJs --> main
    appJs --> app
    appJs --> base
    appJs --> vdom
    appJs --> fetch
    appJs --> crypto
    appJs --> sched
    appJs --> interop
    appJs --> repo
    appJs --> util
    appJs --> views
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

```mermaid
graph TD
    root["target/classes/META-INF/resources/teavm/"]
    indexHtml["index.html<br/><small>Página HTML (carrega Spectrum WC + app.js)</small>"]
    js["js/"]
    appJs["app.js<br/><small>Java compilado para JavaScript (~8000 métodos)</small>"]

    root --> indexHtml
    root --> js
    js --> appJs
```

## Executar

O módulo não tem servidor próprio. O SPA é servido como recurso estático pelo servidor Javalin (`backend`) ou embutido pelo módulo `teavm.native` (Tauri):

```
http://localhost:8080/teavm
```

## Estrutura do Projeto

```mermaid
graph TD
    root["br.com.wdc.shopping.view.teavm.web/"]
    buildsh["build.sh<br/><small>Script de build simplificado</small>"]
    pom["pom.xml<br/><small>Configuração Maven + plugin TeaVM</small>"]
    srcMain["src/main/"]
    java["java/.../view/teavm/"]
    main["Main.java"]
    appCls["ShoppingTeaVMApplication.java"]
    baseCls["AbstractViewTeaVM.java<br/><small>Legacy (HtmlDom)</small>"]
    vdom["vdom/<br/><small>AbstractVDomView.java — VDom base (render/diff/useCallback)</small>"]
    interop["interop/<br/><small>Console, Timers, FetchApi</small>"]
    repo["repo/<br/><small>Repositórios REST</small>"]
    util["util/<br/><small>DateUtils</small>"]
    views["views/<br/><small>8 views VDom (Login, Home, Products, etc.)</small>"]
    webapp["webapp/<br/><small>index.html + css/app.css (Utility classes + componentes + ícones)</small>"]

    root --> buildsh
    root --> pom
    root --> srcMain
    srcMain --> java
    srcMain --> webapp
    java --> main
    java --> appCls
    java --> baseCls
    java --> vdom
    java --> interop
    java --> repo
    java --> util
    java --> views
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

### Carrinho Vazio

![Carrinho Vazio](docs/screenshots/04-cart-empty.png)

### Recibo

![Recibo](docs/screenshots/05-receipt.png)

### Histórico de Compras

![Histórico](docs/screenshots/06-history.png)

## Guias

- [Como criar uma nova View](docs/GUIDE_NEW_VIEW.md) — passo a passo para implementar uma view VDom neste projeto
