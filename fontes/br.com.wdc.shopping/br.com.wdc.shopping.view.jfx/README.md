# br.com.wdc.shopping.view.jfx

Implementação desktop (JavaFX) da aplicação **WeDoCode Shopping**, demonstrando a **independência entre visualização e lógica de apresentação** no padrão **Cube MVP**.

## Motivação

A arquitetura Cube MVP separa rigorosamente os **Presenters** (que controlam estado e navegação) dos **Views** (que renderizam a interface). Os Presenters expõem **ViewStates** — objetos simples com os dados que a view precisa exibir — e a view implementa a interface `CubeView` para se conectar ao ciclo de atualização.

Este módulo prova que essa separação é real e não apenas teórica:

| Aspecto | React (remoto) | JavaFX (desktop) |
|---------|-----------------|-------------------|
| **Onde roda** | Browser via WebSocket | JVM local (JavaFX Application Thread) |
| **Tecnologia de UI** | React 19 + MUI 9 | JavaFX 24 + CSS |
| **Transporte de estado** | Serialização JSON via WebSocket | Acesso direto aos ViewStates em memória |
| **Ciclo de render** | Virtual DOM + reconciliação React | AnimationTimer com dirty-check (16ms) |

Ambas as implementações utilizam **exatamente os mesmos Presenters, ViewStates e regras de negócio**. Apenas a camada de visualização muda.

## Como funciona

### Registro de View Factories

Cada Presenter declara um campo estático `Function<Presenter, CubeView> createView`. O módulo de visualização preenche essas factories na inicialização:

```java
// JFX — acesso direto, renderização local
static {
    LoginPresenter.createView  = p -> new LoginViewJfx(app, p);
    HomePresenter.createView   = p -> new HomeViewJfx(app, p);
    CartPresenter.createView   = p -> new CartViewJfx(app, p);
    // ...
}
```

```java
// React — serialização remota via WebSocket
static {
    LoginPresenter.createView  = LoginReactViewImpl::new;
    HomePresenter.createView   = HomeReactViewImpl::new;
    CartPresenter.createView   = CartReactViewImpl::new;
    // ...
}
```

O Presenter nunca sabe qual tecnologia de UI está sendo usada.

### Ciclo de atualização JFX

```
Presenter.update()
    → view.update()
        → app.markDirty(view)           // marca view com timestamp
            → AnimationTimer (a cada frame)
                → se dirty há ≥ 16ms → view.doUpdate()   // sincroniza ViewState → JavaFX nodes
```

Esse mecanismo evita atualizações redundantes quando múltiplos presenters modificam estado no mesmo ciclo.

## Estrutura

```
br.com.wdc.shopping.view.jfx/
├── src/main/java/.../view/jfx/
│   ├── ShoppingJfxMain.java          # Entry point (JavaFX Application)
│   ├── ShoppingJfxApplication.java   # CubeApplication + AnimationTimer + view factories
│   ├── AbstractViewJfx.java          # Base: CubeView → dirty-marking + list slot sync
│   ├── ShoppingJfxLauncher.java      # IDE-friendly launcher (sem module-path)
│   ├── util/
│   │   ├── JfxDom.java              # DSL fluente para construção de cenas JavaFX
│   │   └── ResourceCatalog.java     # Cache de imagens compartilhadas
│   └── impl/
│       ├── RootViewJfx.java
│       ├── LoginViewJfx.java
│       ├── HomeViewJfx.java
│       ├── ProductsPanelViewJfx.java
│       ├── PurchasesPanelViewJfx.java
│       ├── ProductViewJfx.java
│       ├── CartViewJfx.java
│       ├── ReceiptViewJfx.java
│       ├── home/
│       │   ├── ProductItemViewJfx.java
│       │   └── PurchaseItemViewJfx.java
│       ├── cart/
│       │   └── CartItemViewJfx.java
│       └── receipt/
│           └── ReceiptItemViewJfx.java
├── src/main/resources/
│   └── META-INF/resources/
│       ├── styles/app.css            # Tema Material-inspired
│       └── images/                   # Imagens compartilhadas (módulo presentation)
└── pom.xml
```

## Dependências principais

| Dependência | Versão | Uso |
|-------------|--------|-----|
| JavaFX (javafx-controls) | 24.0.1 | Toolkit de UI desktop |
| H2 Database | (gerenciada) | Banco embarcado |
| Jsoup | 1.18.3 | Renderização de HTML em descrições de produto |
| SLF4J + Logback | (gerenciada) | Logging |

## Pré-requisitos

- **Oracle JDK 26** (arm64 nativo para macOS — necessário para JavaFX)
- **Maven 3.9+**

> **Nota:** O Temurin JDK 26 em macOS Apple Silicon roda via Rosetta (x86_64), o que causa problemas com as bibliotecas nativas do JavaFX. Use o Oracle JDK arm64.

## Build

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

# Build completo (a partir da raiz do projeto)
cd fontes && mvn -q -DskipTests clean install
```

## Execução

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.jfx
mvn javafx:run
```

Ou via IDE usando a classe `ShoppingJfxLauncher.java` (não requer module-path configurado).

## Configuração

O arquivo `work/config/application.toml` permite configurar:

```toml
[app]
# basedir = "work"

[database]
# url = "jdbc:h2:file:..."
# username = "sa"
# password = "sa"
# reset = false
```

Resolução: system property `shopping.config.file` → fallback para `work/config/application.toml`.

## Conclusão

A existência deste módulo lado a lado com a versão React valida o princípio central da arquitetura Cube MVP: **os ViewStates são contratos estáveis entre Presenters e Views, permitindo trocar a tecnologia de visualização sem alterar uma linha de lógica de negócio ou apresentação**.
