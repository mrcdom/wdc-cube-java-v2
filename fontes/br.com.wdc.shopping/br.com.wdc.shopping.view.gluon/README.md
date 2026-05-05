# br.com.wdc.shopping.view.gluon

Implementação **multiplataforma móvel** (iOS e Android) e **desktop** da aplicação **WeDoCode Shopping** utilizando [Gluon Mobile](https://gluonhq.com/) + JavaFX, com compilação nativa via GraalVM Native Image. Compartilha os mesmos Presenters, ViewStates e lógica de negócio das demais implementações (React, Vaadin, JavaFX, Swing) — apenas a camada de visualização é específica.

## Screenshots

### Login

![Tela de Login](docs/screenshots/01-login.png)

Card centralizado com campos de usuário/senha. Credenciais padrão: `admin` / `admin`.

### Página Inicial — Produtos e Histórico

![Página Inicial](docs/screenshots/02-home.png)

Catálogo de produtos com cards clicáveis e histórico de compras com paginação.

### Detalhe do Produto

![Detalhe do Produto](docs/screenshots/03-product-detail.png)

Imagem, descrição, seletor de quantidade e botão de adicionar ao carrinho.

### Carrinho de Compras

![Carrinho](docs/screenshots/04-cart.png)

Lista de itens com preço, quantidade e remoção individual. Total calculado em tempo real.

### Listagem de Compras

![Listagem de Compras](docs/screenshots/06-purchases.png)

Histórico de compras realizadas com paginação.

### Recibo de Compra

![Recibo](docs/screenshots/05-receipt.png)

Confirmação de compra com recibo detalhado.

## Motivação

O Gluon Mobile permite escrever uma **única base de código JavaFX** que é compilada nativamente para iOS, Android e Desktop (AOT via GraalVM). O módulo demonstra que a separação **Cube MVP** funciona inclusive em cenários de compilação ahead-of-time com acesso a APIs nativas de dispositivo (display, lifecycle, storage, statusbar).

| Aspecto | Desktop (JVM) | iOS (nativo) | Android (nativo) |
|---------|---------------|--------------|------------------|
| **Runtime** | JVM HotSpot | GraalVM Native Image | GraalVM Native Image |
| **Target** | `host` | `ios` / `ios-sim` | `android` |
| **APIs nativas** | Gluon Attach (desktop) | Gluon Attach (iOS) | Gluon Attach (Android) |
| **Persistência** | API REST (remota) | API REST (remota) | API REST (remota) |

## Estrutura de Módulos

```mermaid
graph TD
    root["view.gluon (POM agregador)"]
    root --> shared["shared<br/><small>UI compartilhada (views, theme, util)</small>"]
    root --> desktop["desktop<br/><small>Launcher JVM</small>"]
    root --> android["android<br/><small>Launcher + deps Android nativas</small>"]
    root --> ios["ios<br/><small>Launcher + deps iOS nativas</small>"]
```

### Módulo Shared

Contém toda a lógica de visualização reutilizada entre as plataformas:

| Pacote | Conteúdo |
|--------|----------|
| `view.gluon` | `ShoppingGluonMain` (Application), `ShoppingGluonApplication`, `AbstractViewGluon` |
| `view.gluon.impl` | Views: `RootViewGluon`, `LoginViewGluon`, `HomeViewGluon`, `CartViewGluon`, `ProductViewGluon`, `ReceiptViewGluon`, `ProductsPanelViewGluon`, `PurchasesPanelViewGluon` |
| `view.gluon.theme` | `GluonStyles`, `GluonColors`, `GluonIcons` |
| `view.gluon.util` | `GluonDom`, `ResourceCatalog` |

### Módulos de Plataforma

Cada módulo de plataforma fornece apenas um **Launcher** que configura o classpath nativo e as dependências Gluon Attach específicas:

- `ShoppingGluonDesktopLauncher` — executa em JVM padrão
- `ShoppingGluonIosLauncher` — compilado para ARM64 iOS
- `ShoppingGluonAndroidLauncher` — compilado para Android

## Pré-requisitos

- **Java 21+** (Microsoft OpenJDK ou similar)
- **Maven 3.9+**
- **GraalVM** (para compilação nativa iOS/Android) — recomendado: `graalvm-gluon-23`
- **Xcode** + Command Line Tools (para iOS)
- **Android SDK** + NDK (para Android)

## Como Executar

### Desktop (JVM)

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.gluon/br.com.wdc.shopping.view.gluon.desktop
mvn javafx:run
```

### iOS (Simulador)

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.gluon/br.com.wdc.shopping.view.gluon.ios

# Build completo + deploy no simulador
./build-sim.sh

# Apenas deploy (reutiliza build anterior)
./build-sim.sh --deploy

# Build sem lançar
./build-sim.sh --no-launch
```

Requer um simulador iOS bootado. Para iniciar um:

```bash
xcrun simctl boot <UDID>
```

### iOS (Dispositivo físico)

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.gluon/br.com.wdc.shopping.view.gluon.ios
mvn gluonfx:build gluonfx:package -Pgluonfx-ios
```

### Android

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.gluon/br.com.wdc.shopping.view.gluon.android
mvn gluonfx:build gluonfx:package
```

## Tecnologias

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| JavaFX | 21.0.7 | UI toolkit |
| Gluon Attach | 4.0.22 | APIs nativas (display, lifecycle, storage, statusbar) |
| GluonFX Maven Plugin | 1.0.25 | Compilação nativa (GraalVM substrate) |
| H2 Database | — | Persistência local (iOS/Android) |
| SLF4J + Logback | — | Logging |

## Arquitetura

Segue o padrão **Cube MVP** do projeto:

```mermaid
graph TD
    P["Presenter (presentation)"]
    P -->|"createView factory"| V["ViewImpl (view.gluon.impl)<br/><small>extends AbstractViewGluon&lt;P&gt;</small>"]
    V -->|"doUpdate()"| N["JavaFX Nodes (renderização)"]
```

O ciclo de atualização utiliza o `AnimationTimer` do JavaFX para sincronizar as mudanças de estado dos Presenters com a árvore de nós JavaFX, evitando atualizações redundantes via dirty-check.
