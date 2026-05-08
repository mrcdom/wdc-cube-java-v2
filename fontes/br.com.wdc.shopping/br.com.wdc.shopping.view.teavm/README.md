# WDC Shopping — View TeaVM

Implementação web da aplicação **WeDoCode Shopping** compilada de Java para JavaScript via [TeaVM 0.14.0](https://teavm.org/), utilizando **Bootstrap 5** para a interface gráfica.

## Visão Geral

Este módulo demonstra a **independência entre visualização e lógica de apresentação** no padrão **Cube MVP**: todo o código Java (domain, api-client, presentation e views) é compilado para JavaScript pelo TeaVM e executado diretamente no browser — sem servidor Java em runtime.

A UI é construída programaticamente em Java usando a API `HtmlDom`, que gera elementos HTML nativos estilizados com Bootstrap 5 e Bootstrap Icons. O resultado é um SPA (Single Page Application) leve, responsivo e com layout adaptável para dispositivos móveis.

## Arquitetura

```
Browser (JavaScript)
├── index.html                          ← Carrega Bootstrap 5 CSS/JS + app.js
├── js/app.js                           ← Java compilado para JS pelo TeaVM
│   ├── Main.java                       ← Entry point
│   ├── ShoppingTeaVMApplication.java   ← Wiring de factories e render loop
│   ├── AbstractViewTeaVM.java          ← Classe base para views
│   ├── FetchHttpTransport.java         ← XMLHttpRequest (REST API calls)
│   ├── BrowserCryptoProvider.java      ← Web Crypto API (HMAC-SHA256)
│   ├── ScheduledExecutorBrowser.java   ← setTimeout/setInterval
│   ├── interop/                        ← JSO bridges (Console, Timers, Fetch)
│   ├── repo/                           ← Repositórios via REST API
│   ├── theme/                          ← Constantes Bootstrap (ícones, cores)
│   ├── util/                           ← HtmlDom builder, DateUtils
│   └── views/                          ← Implementações das views
│       ├── RootViewTeaVM.java
│       ├── LoginViewTeaVM.java
│       ├── HomeViewTeaVM.java
│       ├── ProductsPanelViewTeaVM.java
│       ├── ProductViewTeaVM.java
│       ├── CartViewTeaVM.java
│       ├── ReceiptViewTeaVM.java
│       └── PurchasesPanelViewTeaVM.java
└── API REST (Javalin)                  ← Servidor back-end (porta 8080)
```

## Tecnologias

| Componente | Tecnologia | Versão |
|------------|-----------|--------|
| Compilador AOT | [TeaVM](https://teavm.org/) | 0.14.0 |
| UI Framework | [Bootstrap](https://getbootstrap.com/) | 5.3.3 |
| Ícones | [Bootstrap Icons](https://icons.getbootstrap.com/) | 1.11.3 |
| Linguagem | Java | 21 |
| Build | Maven | — |
| Interop JS | TeaVM JSO (`@JSBody`, `@JSFunctor`) | — |

## Como Funciona

### Compilação Java → JavaScript

O TeaVM compila as classes Java (views, presenters, domain, api-client) para um único arquivo JavaScript (`app.js`). O compilador realiza:

- **Dead code elimination** — remove classes/métodos não utilizados
- **Minificação** — otimiza o tamanho do bundle
- **Interop direto** — chamadas a APIs do browser (DOM, Fetch, Crypto) via anotações JSO, sem bridge pesado

### Construção da UI

A classe `HtmlDom` fornece uma API fluente para construção de elementos HTML em Java:

```java
dom.div("d-flex align-items-center gap-2", container -> {
    dom.button("btn btn-primary", btn -> {
        dom.icon(BsIcons.CART);
        dom.span(null, txt -> txt.setTextContent("Adicionar"));
        btn.addEventListener("click", evt -> presenter.onAddToCart(1));
    });
});
```

Isso gera HTML nativo com classes Bootstrap, sem Virtual DOM ou framework JavaScript adicional.

### Comunicação com o Servidor

As views se comunicam com o back-end Javalin via REST API usando `XMLHttpRequest` (implementado em `FetchHttpTransport`). A autenticação utiliza HMAC-SHA256 via Web Crypto API (`BrowserCryptoProvider`).

### Responsividade

O layout é responsivo e adapta-se a telas pequenas (iPhone SE) utilizando classes Bootstrap responsivas (`d-none d-sm-inline`, `flex-column-reverse flex-sm-row`, etc.). No cabeçalho, textos auxiliares são ocultados em telas estreitas, mantendo apenas os ícones essenciais.

## Build

### Pré-requisitos

- **Java 21** (ex: Microsoft OpenJDK 21)
- **Maven** 3.8+

### Compilar

```bash
# Definir JAVA21_HOME
export JAVA21_HOME=/Library/Java/JavaVirtualMachines/microsoft-21.jdk/Contents/Home

# Compilar (a partir da raiz do módulo)
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.teavm
bash build.sh
```

Ou diretamente com Maven:

```bash
JAVA_HOME=$JAVA21_HOME mvn process-classes -DskipTests
```

### Output

O build gera os artefatos em `target/classes/META-INF/resources/teavm/`:

```
target/classes/META-INF/resources/teavm/
├── index.html    ← Página HTML (carrega Bootstrap + app.js)
└── js/
    └── app.js    ← Java compilado para JavaScript (~8000 métodos)
```

### Executar

O módulo não tem servidor próprio — o arquivo `app.js` é servido como recurso estático pelo servidor Javalin (módulo `view.react.javalin`):

```
http://localhost:8080/teavm
```

## Estrutura do Projeto

```
br.com.wdc.shopping.view.teavm/
├── build.sh                    ← Script de build simplificado
├── pom.xml                     ← Configuração Maven + plugin TeaVM
├── src/main/
│   ├── java/.../view/teavm/
│   │   ├── Main.java
│   │   ├── ShoppingTeaVMApplication.java
│   │   ├── AbstractViewTeaVM.java
│   │   ├── FetchHttpTransport.java
│   │   ├── BrowserCryptoProvider.java
│   │   ├── JsonParsing.java
│   │   ├── ScheduledExecutorBrowser.java
│   │   ├── interop/            ← Console, Timers, FetchApi
│   │   ├── repo/               ← Repositórios REST
│   │   ├── theme/              ← BsIcons, BsColors, BsStyles
│   │   ├── util/               ← HtmlDom, DateUtils
│   │   └── views/              ← 8 views (Login, Home, Products, etc.)
│   └── webapp/
│       └── index.html
└── docs/
    └── screenshots/
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
