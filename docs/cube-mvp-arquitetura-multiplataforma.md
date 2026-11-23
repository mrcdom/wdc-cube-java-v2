# Cube MVP — Arquitetura de Apresentação Multiplataforma em Java

[Contextualização](#contextualização)  
[Requisitos Fundamentais](#requisitos-fundamentais)  
[A Pilha Tecnológica](#a-pilha-tecnológica)  
[Java e o Sistema de Tipos](#java-e-o-sistema-de-tipos)  
[Javalin — O Servidor HTTP](#javalin-o-servidor-http)  
[Arquitetura da Aplicação de Referência](#arquitetura-da-aplicação-de-referência)  
[Perspectiva de Camadas](#perspectiva-de-camadas)  
[O Padrão Cube MVP](#o-padrão-cube-mvp)  
[Visão de Estados](#visão-de-estados)  
[Estruturas de Controle](#estruturas-de-controle)  
[Controle de Rotas — Places e Intents](#controle-de-rotas-places-e-intents)  
[Controle de Acesso — Guardas de Navegação](#controle-de-acesso-guardas-de-navegação)  
[Diagrama de Classes](#diagrama-de-classes)  
[Composições de View — Slots](#composições-de-view-slots)  
[A Camada de Visão — Cinco Implementações Independentes](#a-camada-de-visão-cinco-implementações-independentes)  
[Apresentação Remota — O Protocolo Host/Shell](#apresentação-remota-o-protocolo-hostshell)  
[O Scope como Contrato de Serialização](#o-scope-como-contrato-de-serialização)  
[Conclusões](#conclusões)  
[Trabalhos Futuros](#trabalhos-futuros)

---

## Contextualização

Este projeto é a materialização em Java da arquitetura de Visão Passiva Hierárquica definida em 2015 ([referência](https://docs.google.com/document/d/1Rcbt3gIjdX50j2gr2zqs9VqTQVg8527h9lOUF1xrhag/edit?tab=t.0#heading=h.3wkx7ie3r9yu)). O WDC Cube Java v2 mantém a mesma lógica de apresentação — os mesmos Presenters, os mesmos ViewStates, as mesmas regras de negócio e de navegação — e a executa em cinco implementações de frontend completamente independentes:

| Implementação | Tecnologia | Plataformas |
|---|---|---|
| `view.remote / remote.shell.react` | React 19 + TypeScript + MUI 9 | Web (browser) |
| `view.remote / remote.shell.flutter` | Flutter + Dart | Web, Desktop, Mobile |
| `view.vaadin` | Vaadin 25 + Java | Web (server-side push) |
| `view.swt` | Eclipse SWT + Java | Desktop nativo (Windows, Linux, macOS) |
| `view.teavm` | TeaVM + Tauri 2 + Java | Web, Desktop, Android, iOS |

O princípio central da arquitetura é que a camada de apresentação não sabe nada sobre qual tecnologia renderiza a interface. A view é um artefato plugável — uma implementação de um contrato Java que recebe estado e notifica ações.

Este artigo descreve como essa arquitetura foi consolidada, que problemas foram resolvidos, que inovações surgiram (em especial o protocolo de Apresentação Remota), e o que ainda permanece como trabalho futuro.

---

## Requisitos Fundamentais

Os requisitos a seguir orientaram as decisões arquiteturais do projeto e permitem avaliar o que foi conquistado e o que permanece desafiador.

**Lógica de navegação independente da biblioteca de View** ✅  
O sistema de navegação baseado em `CubeNavigation`, `CubePlace` e `CubeIntent` é puramente Java e não referencia nenhuma tecnologia de UI.

**Maximizar a porção de código comprometida apenas com tecnologias de vida longa** ✅  
Java existe há mais de 30 anos. A camada de apresentação — Presenters e ViewStates — reside num módulo Maven sem dependências de UI.

**Modelo de construção de software independente do ambiente de execução** ✅  
A mesma lógica de apresentação compila e executa em browser (React, TeaVM, Vaadin), desktop (SWT, Flutter, TeaVM/Tauri) e mobile (Flutter, TeaVM/Tauri).

**Representação consistente do estado na URL do navegador** ✅  
O sistema de navegação serializa o estado ativo em query-string. O protocolo de Apresentação Remota sincroniza o hash da URL entre shell e host.

**Restabelecimento preciso do estado após F5** ✅  
Cada implementação reconstrói o estado a partir da URL no ciclo de `applyParameters`. O modo Remoto usa o `sessionId` para reconectar à sessão server-side.

**Testar todos os fluxos de navegação via views mock** ✅  
Quem cria as views é o catálogo `ScopeViews` que o host entrega à aplicação (`setScopeViews`). Nos testes, a aplicação mock registra views mock para cada classe de escopo e a navegação roda sem nenhuma UI.

**Mais de uma implementação para a mesma visão** ✅  
Esse é hoje o principal ponto de validação da arquitetura: cinco implementações coexistindo sobre os mesmos Presenters.

**Contratos e sistemas de tipos para rastreabilidade** ✅  
Java oferece verificação estática em tempo de compilação. O registro `ScopeViews.register(CartScope.class, CartViewVaadin::new)` garante que cada implementação de view receba o tipo correto de escopo (`(app, CartScope) -> CubeView`).

**Gestão do escopo de propagação do estado nas composições** ✅  
Slots (`ScopeSlot`) são lambdas que encapsulam a atribuição do escopo de um filho a um ponto de composição específico do pai, sem expor o Presenter pai.

**Composição de Views independente da tecnologia** ✅  
O conceito de slot é tecnologia-agnóstico. Cada implementação de view trata o slot à sua maneira.

**Manutenção da complexidade sob controle** ✅  
A hierarquia de Presenters divide responsabilidades. Cada Presenter trata de um fragmento do estado total da aplicação.

---

## A Pilha Tecnológica

A pilha tecnológica do lado servidor — onde a lógica vive — é deliberadamente minimalista:

| Categoria | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java (Temurin/Microsoft) | 21 |
| Build | Maven | 3.9+ |
| Servidor HTTP | Javalin | 7.2.4 |
| Banco de dados | H2 | 2.5.252 |
| Acesso a dados | jOOQ | 3.21.9 |
| Serialização | Gson | 2.13.2 |
| Logging | SLF4J + Logback | 2.0.20 / 1.6.7 |
| Testes | JUnit | 4.13.2 |

Notavelmente ausente desta lista está qualquer framework de injeção de dependências (Spring, CDI, Guice). A injeção é feita via `AtomicReference<T>` estático — um service locator simples e transparente. Essa escolha elimina uma camada de mágica que, nas aplicações do ecossistema Spring, normalmente obscurece o fluxo de execução e dificulta a instrumentação das fábricas de view.

### Java e o Sistema de Tipos

Java oferece um sistema de tipos estático robusto com suporte nativo de ferramentas (refatoração global, rastreabilidade de símbolos, navegação por código). A verificação em tempo de compilação elimina uma classe inteira de erros de integração entre Presenter e View — se o tipo de Presenter esperado pela implementação de view não bate com o declarado pelo sistema, o compilador recusa o build.

### Javalin — O Servidor HTTP

Javalin é um framework HTTP leve para Java/Kotlin. No projeto, serve dois papéis distintos:

1. **API REST** (`persistence.rest`) — expõe os repositórios de domínio para consumo pelo módulo `persistence.client`, usado pelas implementações de view client-side (TeaVM).
2. **WebSocket** (`view.remote / remote.host`) — canal bidirecional do protocolo de Apresentação Remota. Cada conexão WebSocket é tratada por uma Virtual Thread (Java 21), permitindo escalar para milhares de sessões simultâneas sem overhead de threading tradicional.

---

## Arquitetura da Aplicação de Referência

A aplicação de referência é um sistema de e-commerce simplificado: **WeDoCode Shopping**. É deliberadamente funcional — permite login, navegação por catálogo de produtos, carrinho e geração de recibos — mas mantida pequena o suficiente para que a arquitetura seja o foco, não a complexidade de negócio.

Os módulos estão organizados em dois grupos:

**Framework** — biblioteca genérica e reutilizável:

| Módulo | Responsabilidade |
|---|---|
| `framework.commons` | Interfaces funcionais com checked exceptions, serialização extensível, abstração SQL, criptografia (RSA, Base62), logging multiplataforma |
| `framework.cube` | Motor do Cube MVP: presenters, views, navegação, intents, slots |
| `framework.cube.remote` | Protocolo de Apresentação Remota: serialização de ViewStates, dispatcher WebSocket, bridges para React/Flutter/TeaVM |
| `framework.jooq` | Suporte jOOQ: helpers de query JSON (`JsonQuery`, `JsonQueryBuilder`, `JsonChildQueryBuilder`); o `DSLContext` é injetado pela aplicação (sem holder global) |
| `framework.dependencies` | BOM Maven para versões centralizadas |

**Shopping** — a aplicação em si:

| Módulo | Responsabilidade |
|---|---|
| `shopping.domain` | Modelos de domínio, repositórios, critérios de consulta |
| `shopping.persistence` | H2 + jOOQ (classes geradas) |
| `shopping.persistence.rest` | REST controllers (Javalin) |
| `shopping.persistence.client` | REST client (OkHttp + Gson) |
| `shopping.presentation` | Presenters, ViewStates, navegação — **tecnologia-agnóstico** |
| `shopping.view.*` | Cinco implementações de frontend |

---

## Perspectiva de Camadas

```mermaid
graph TD
    subgraph Client["Clientes (5 implementações)"]
        react["React 19 (browser)"]
        flutter["Flutter (mobile/desktop/web)"]
        vaadin["Vaadin (browser, server-side)"]
        swt["SWT (desktop nativo)"]
        teavm["TeaVM + Tauri (web/desktop/Android/iOS)"]
    end

    subgraph Presentation["Camada de Apresentação (Java — tecnologia-agnóstica)"]
        presenters["Presenters + ViewStates"]
        navigation["Sistema de Navegação (Places, Intents, CubeNavigation)"]
    end

    subgraph Backend["Backend (Javalin + H2)"]
        rest["REST API (persistence.rest)"]
        db[("H2")]
    end

    react -->|"WebSocket (JSON delta)"| presenters
    flutter -->|"WebSocket (JSON delta)"| presenters
    vaadin -->|"Acesso direto em memória + Server Push"| presenters
    swt -->|"Acesso direto em memória"| presenters
    teavm -->|"REST (Fetch API)"| rest
    presenters --> navigation
    presenters --> rest
    rest --> db
```

A separação é estrutural, não apenas conceitual: o módulo `shopping.presentation` não possui dependências de nenhuma tecnologia de UI. Ele depende apenas de `framework.cube`, `shopping.domain` e `slf4j-api`. Qualquer tecnologia capaz de implementar a interface `CubeView` pode ser plugada.

---

## O Padrão Cube MVP

O Cube MVP é uma variação do Passive View com dois princípios centrais:

1. **A View tem acesso ao Scope, mas não ao Presenter.** O Scope é um objeto Java simples com campos públicos (`Prop`s e ações) — o estado público da tela. O Presenter é responsável por nutrir o Scope antes de sinalizar `update()`.

2. **Nenhuma consulta cruzada entre Presenter e View.** O Presenter notifica a View pelo Scope: a mudança numa `Prop` chama o `update()` da view que desenha aquele escopo. A View notifica o Presenter disparando as ações do Scope (os campos `onXxx`). Não há retorno de valores nessas comunicações.

O nome "Cube" reflete a terceira dimensão da arquitetura: além do eixo Presenter/View, há o eixo da **hierarquia** — Presenters são compostos em árvore, cada nó responsável por uma porção do estado total da aplicação.

### O Presenter

Todo Presenter navegável estende `AbstractCubePresenter<ShoppingApplication>` e define seu Scope como classe interna estática:

```java
public class CartPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // Scope — estado público, lido pela view para renderização. Cada campo observado é uma Prop:
    // set() compara com equals e, se mudou, pede o redesenho — o presenter não chama update() depois
    public static class CartScope extends Scope {
        // classId do protocolo bridge: identifica a view remota que desenha este escopo
        public static final String CLASS_ID = "7eb485e5f843";

        public final Prop<List<CartItem>> items = prop(Collections.emptyList());
        public final Prop<Integer> errorCode = prop(0);
        public final Prop<String> errorMessage = prop();

        // Ações que a view dispara (o serializador remoto as ignora)
        public Action onConfirm = Action.NOOP;
    }

    // bind: o presenter passa a ser avisado quando uma prop do estado muda
    public final CartScope state = bind(new CartScope());

    private ScopeSlot ownerSlot;

    public CartPresenter(ShoppingApplication app) {
        super(app);
        // Ligada uma vez; a guarda manda a falha não tratada ao app e republica o endereço ao fim
        // Com código de evento: o skeleton do escopo despacha a ela o evento 1 de um cliente remoto
        this.state.onConfirm = action(1, this::confirm);
    }

    // Chamado pelo motor de navegação ao ativar este Presenter
    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        this.state.items.set(this.app.getCart().getCartItems());

        // O pai põe o escopo num Prop<Scope> dele; a view do pai é que pede a view deste escopo
        this.ownerSlot = new CartKeys(this.app, intent).parentSlot();
        this.ownerSlot.setScope(this.state);
        return true;
    }

    // Corpo da ação: só trata as falhas esperadas, sem try/catch genérico
    private void confirm() {
        // lógica de negócio...
        this.state.errorCode.set(0);   // a mudança já pede o redesenho
    }
}
```

O Presenter nunca cria view: ele só nutre o escopo e o entrega ao pai pelo slot. A view é da camada de view — cada host monta um catálogo `ScopeViews` (classe do escopo → fábrica `(app, scope) -> CubeView`) e o entrega à aplicação com `setScopeViews`; `app.viewOf(scope)` cria a view na primeira vez que é pedida, sem que o Presenter saiba qual tecnologia de UI está sendo usada. O `CLASS_ID` estático do escopo é o classId do protocolo bridge, e o skeleton remoto é do escopo (`scope.skeleton(CLASS_ID)`), que despacha cada evento à ação ligada com aquele código.

Todo Presenter é navegável (`AbstractCubePresenter<A>`, gerenciado pelo motor de navegação). Um pedaço de tela que não precisa de navegação própria — como os painéis de produtos e de compras da Home — **não ganha um Presenter**: é um **escopo menor**, declarado como classe interna do escopo de quem o mostra:

```java
public static class HomeScope extends Scope {

    public static class ProductsPanelScope extends Scope {
        public static final String CLASS_ID = "a1b2c3d4e5f6";

        public final Prop<List<ProductInfo>> products = prop();
        public ActionOf<Long> onOpenProduct = ActionOf.noop();
    }

    public final Prop<ProductsPanelScope> productsPanel = prop();
    // ...
}
```

O `HomePresenter` liga as ações do segmento (`actionOf(productsPanel, 1, FormParam.asLong("p.productId"), this::openProduct)`) e só o põe no escopo (`state.productsPanel.set(productsPanel)`). Quem escolhe a view é a camada de view, como na versão TypeScript: cada host monta um catálogo `ScopeViews` (classe do escopo → fábrica) e o entrega à aplicação com `setScopeViews`; a view da Home pede a do painel com `app.viewOf(scope)`, que a cria na primeira vez. Daí em diante uma mudança no segmento redesenha só a view dele (antes disso, redesenha o dono), e a view é liberada junto com o Presenter. O host remoto não registra nada: `fallback(RemoteViewImpl::forScope)` faz uma view remota para qualquer escopo, com o `CLASS_ID` dele. A view nunca conhece o Presenter — só lê o escopo e dispara as ações dele —, então um Presenter pode orquestrar vários segmentos sem uma classe por segmento.

No modo remoto o segmento é uma view como qualquer outra: tem o seu `classId`, o seu skeleton (o despacho por código das ações ligadas nele) e o pai o referencia pelo id da view (`"productsPanelViewId"`).

**Linhas de lista também são segmentos.** Cada linha do carrinho é um `CartScope.CartItemScope`, com view própria e ações ligadas ao próprio item (`onRemove`, `onModifyQuantity(quantidade)` — a linha não manda o id do produto de volta: ela *é* o item). O `CartScope` guarda `Prop<List<CartItemScope>> rows`, que o serializador escreve como `"rowsViewIds": [...]`, na ordem. O `CartPresenter` reaproveita a linha pelo id do produto enquanto o item fica, cria a de um item novo (a view nasce quando o carrinho a pede) e libera (`detach`) a de um item que saiu; mudar uma quantidade redesenha só aquela linha, e o total (`CartScope.total`) vem do servidor.

---

## Visão de Estados

A aplicação de referência possui 8 *places* organizados em 4 níveis hierárquicos:

```mermaid
graph TD
    ROOT["ROOT"]
    ROOT --> OPEN["OPEN<br/><small>guarda: se autenticado → RESTRICTED</small>"]
    ROOT --> RESTRICTED["RESTRICTED<br/><small>guarda: se não autenticado → OPEN</small>"]
    OPEN --> LOGIN["LOGIN<br/><small>/login</small>"]
    RESTRICTED --> HOME["HOME<br/><small>/home</small>"]
    HOME --> PRODUCT["PRODUCT<br/><small>/home/product?pr-id=N</small>"]
    HOME --> CART["CART<br/><small>/home/cart</small>"]
    HOME --> RECEIPT["RECEIPT<br/><small>/home/receipt?rc-id=N</small>"]
```

Os estados são aditivos. O estado `/home/product?pr-id=42` implica a ativação encadeada de `RootPresenter → RestrictedPresenter → HomePresenter → ProductPresenter`. Cada Presenter na cadeia recebe a URL e extrai os parâmetros que lhe cabem.

A parte dinâmica da URL — os parâmetros de query-string — é encapsulada em `CubeIntent`, e cada place a lê e escreve por uma classe de chaves tipadas (`XxxKeys`, sobre o `CubeKeys` do framework):

```java
// Navegar para o detalhe do produto 42, a partir do estado atual
new ProductKeys(app).withProductId(42L).flip();
```

**Cada place tem uma classe de chaves, que declara o place** a partir do place das chaves que ela estende — a árvore de classes é a árvore de places, e o caminho até um place é dado por ela:

```java
public class HomeKeys extends RestrictedKeys {
    public static final String PREFIX = "hm";
    public static final CubePlace PLACE = RestrictedKeys.PLACE.child("home", HomePresenter::new);
    // ...
}

public final class ProductKeys extends HomeKeys {
    public static final String PREFIX = "pr";
    public static final CubePlace PLACE = HomeKeys.PLACE.child("product", ProductPresenter::new);

    private static final class P {
        static final String PRODUCT_ID = PREFIX + "-id";   // só esta classe conhece o nome
    }

    public Long productId() { /* 0 ou mais, ou null: o endereço vem de fora, o getter nunca lança */ }
    public ProductKeys withProductId(Long id) { /* null (ou o padrão) tira o nome do endereço */ }
}
```

- **Prefixo de duas letras por classe**, único na aplicação; todo nome que ela declara começa por ele (`pr-id`). O `PlaceKeysTest` confere.
- **Um place filho herda os getters do pai** para ler o que os níveis de cima passaram; o único nome reaproveitado de propósito é o slot de composição (`parentSlot()`).
- **Sem intent, as chaves partem do estado atual** (`app.newIntent()`); dentro do `applyParameters`, um redirecionamento usa o intent em trânsito: `new HomeKeys(app, intent).flip()`.
- Um place declarado num campo estático só entra na árvore quando sua classe carrega; por isso `ShoppingApplication.PLACES` lista todos.

---

## Estruturas de Controle

### Controle de Rotas — Places e Intents

O motor de navegação `CubeNavigation` executa transições entre states da seguinte forma:

1. Para cada `CubePlace` do caminho até o destino (`place.getPath()`, da raiz para baixo), verifica se já existe uma instância do Presenter correspondente.  
2. Se não existe, instancia pela factory do place.  
3. Invoca `applyParameters(intent, initialization, deepest)` em cada Presenter da cadeia, na ordem da hierarquia.  
4. Invoca `release()` nos Presenters que não fazem mais parte do state ativo, na ordem inversa (do mais profundo para o mais raso).  
5. Invoca `commitComputedState()` em todos — oportunidade para sincronizar estados derivados.  
6. Propaga `update()` às views afetadas.

Ao navegar de `/home/product?pr-id=5` para `/home/cart`, a sequência é:

- `applyParameters` invocado para `Root`, `Restricted`, `Home`, `Cart`  
- `release()` invocado para `Product`  

Presenters compartilhados entre o state anterior e o novo não são recriados — apenas notificados com `applyParameters(intent, false, ...)`.

### Controle de Acesso — Guardas de Navegação

`OpenPresenter` e `RestrictedPresenter` funcionam como guardas. Cada um verifica uma condição no início do `applyParameters` e, se não satisfeita, redireciona para outro state:

```java
@Override
public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
    if (this.app.getSubject() != null) {
        // Usuário já autenticado — redirecionar para área restrita
        new RestrictedKeys(this.app, intent).flip();
        return false;
    }
    // Continuar navegação normal...
    return true;
}
```

O retorno `false` interrompe a cadeia de navegação — os Presenters mais profundos não são ativados.

---

## Diagrama de Classes

```mermaid
classDiagram
    class CubeApplication {
        +navigate() CubeNavigation
        +newIntent() CubeIntent
        +go(String path)
        +setScopeViews(scopeViews)
        +viewOf(scope) CubeView
        +releaseView(scope)
        +liveViews() List
    }

    class CubePresenter {
        <<interface>>
        +applyParameters(intent, init, deepest) boolean
        +publishParameters(intent)
    }

    class AbstractCubePresenter~A~ {
        +app: A
        #bind(scope) Scope
        +scope() Scope
        +view() CubeView
        +update()
        +release()
    }

    class Scope {
        <<abstract>>
        #prop(initial) Prop
        +setUpdater(updater)
        +view() CubeView
        +skeleton(classId) CubeSkeleton
        +onSync(syncState)
        +drawnBy(view)$ Scope
    }

    class ScopeViews {
        +register(scopeClass, factory) ScopeViews
        +fallback(factory) ScopeViews
    }

    class Prop {
        +get() T
        +set(value)
    }

    class CubeView {
        <<interface>>
        +instanceId() String
        +update()
        +release()
    }

    class ScopeSlot {
        <<interface>>
        +setScope(scope)
    }

    class CubePlace {
        +id: int
        +name: String
        +parent: CubePlace
        +child(name, factory) CubePlace
        +getPath() List
        +find(name) CubePlace
    }

    class CubeIntent {
        +setParameter(key, value)
        +getParameter(key) Object
        +getSlot(name) ScopeSlot
        +setSlot(name, slot)
    }

    CubePresenter <|.. AbstractCubePresenter
    AbstractCubePresenter --> Scope : estado
    Scope --> CubeView : desenhado por
    CubePlace --> CubePresenter : instancia
    CubeIntent --> ScopeSlot
    ScopeSlot --> Scope : recebe
    CubeApplication --> AbstractCubePresenter : mantém
    CubeApplication --> ScopeViews : viewOf(scope)
    ScopeViews --> CubeView : cria
```

A aplicação é um grafo de instâncias de `AbstractCubePresenter`. Cada instância conhece só o seu Scope (o campo `state`, vinculado com `bind`). A view de cada Scope é criada pela camada de view: o catálogo `ScopeViews` do host diz qual fábrica desenha cada classe de escopo, e `CubeApplication.viewOf(scope)` a cria na primeira vez que é pedida. O Presenter nunca referencia uma view — quando muito, `view()` devolve a `CubeView` que desenha o seu escopo, se já houver uma.

---

## Composições de View — Slots

Um slot é um ponto de composição declarado pelo Presenter pai, que permite que um Presenter filho se encaixe na árvore de escopos sem que haja acoplamento direto entre eles.

```java
// HomeScope guarda o escopo do Presenter filho (produto, carrinho, recibo)
public final Prop<Scope> content = prop();

// HomePresenter declara um slot de conteúdo
private final ScopeSlot contentSlot = this::setContent;

private void setContent(Scope scope) {
    this.state.content.set(scope);   // só redesenha se mudou
}
```

O slot `contentSlot` é registrado no `CubeIntent` antes de seguir para o Presenter filho:

```java
@Override
public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
    // ...
    if (!deepest) {
        // Propagar slot para o Presenter filho
        new HomeKeys(this.app, intent).withParentSlot(this.contentSlot);
    }
    return true;
}
```

O Presenter filho recebe o slot e o usa para se encaixar:

```java
// ProductPresenter recebe o slot e entrega o seu escopo
this.ownerSlot = new ProductKeys(this.app, intent).parentSlot();
this.ownerSlot.setScope(this.state);
```

A view pai pede a view do escopo que estiver ali — `app.viewOf(state.content.get())` — seja lá qual for o Presenter filho que o colocou. O host só desenha o escopo do Presenter raiz (`viewOf(getRootPresenter().state)`); todas as outras views são pedidas pela view que segura o escopo. Até um escopo ganhar view, uma mudança nele redesenha o dono (o escopo que o segura num `Prop` o adota); depois, só a própria view. No modo remoto, um campo `Prop<Scope>` sai no JSON como `"<campo>ViewId"` (`content` → `"contentViewId"`). Essa mecânica funciona idêntica em todas as cinco implementações de view.

---

## A Camada de Visão — Cinco Implementações Independentes

### A Interface CubeView

Toda implementação de view implementa `CubeView`:

```java
public interface CubeView {
    String instanceId();  // identificador único — usado no protocolo remoto
    void update();        // Presenter notifica que o estado mudou
    void release();       // ciclo de vida encerrado
}
```

### O Ciclo de Vida de uma View

1. Alguém pede a view de um escopo com `app.viewOf(scope)` — o host, para o escopo raiz, ou a view que segura o escopo num `Prop`. Na primeira vez, a fábrica registrada no `ScopeViews` instancia a view com `(app, scope)` e ela é desenhada uma vez.  
2. A view armazena a referência à aplicação e ao Scope — nunca ao Presenter.  
3. Quando `update()` é chamado (uma `Prop` do escopo mudou), a view lê os campos do Scope e reconcilia a interface.  
4. Quando o usuário interage, a view dispara a ação `onXxx` do Scope, que o Presenter ligou.  
5. Quando o Presenter é liberado (`release()`), `app.releaseView(scope)` libera a view do escopo e as dos segmentos e linhas dentro dele.

### As Implementações

**view.swt** — Implementação nativa para desktop com Eclipse SWT (Windows, Linux, macOS). Cada view estende `AbstractViewSwt` e usa a DSL declarativa `SwtDom` para construir composições. O ciclo de atualização usa `Display.asyncExec()` para garantir execução na thread de eventos:

```java
public class CartViewSwt extends AbstractViewSwt {
    public Supplier<CartScope> stateSupplier;   // ligado pela fábrica registrada no ScopeViews

    @Override
    public void doUpdate() {
        var state = this.stateSupplier.get();
        this.itemList.setItems(state.items.get().stream()
            .map(item -> item.name + " × " + item.quantity)
            .toArray(String[]::new));
    }
}
```

**view.vaadin** — Implementação para browser via Vaadin Flow (server-side push). O Vaadin gerencia o transporte Atmosphere (WebSocket/Long Polling) e a reconciliação de componentes no DOM. A view usa `ui.access()` para garantir thread-safety nas atualizações:

```java
public class CartViewVaadin extends AbstractViewVaadin<CartScope> {

    // O escopo desenhado fica no campo state da base
    public CartViewVaadin(ShoppingVaadinApplication app, CartScope state) {
        super("cart", app, state, new VerticalLayout());
    }

    @Override
    public void doUpdate() {
        this.grid.setItems(state.items.get());
    }
}
```

**view.teavm** — As views Java são compiladas para JavaScript pelo plugin TeaVM. No browser, o resultado é um SPA completo. Para desktop, Android e iOS, o SPA é empacotado num WebView via Tauri 2 (Rust). As views estendem `AbstractVDomView` e implementam `render()` retornando uma árvore `VNode`:

```java
public class CartViewVDom extends AbstractVDomView<CartScope> {

    public CartViewVDom(ShoppingTeaVMApplication app, CartScope state) {
        super("cart", app, state);
    }

    @Override
    protected VNode render() {
        return div(Css.CART).children(
            ul().children(state.items.get().stream()
                .map(item -> li().text(item.name + " × " + item.quantity))
                .toArray(VNode[]::new)),
            spButton("accent").on("click", onConfirm).children(
                span().text("Confirmar"))
        );
    }
    private final JSFunction onConfirm = e -> this.state.onConfirm.run();
}
```

**view.remote / remote.shell.react** — Shell React em TypeScript. A view é um componente funcional que renderiza o estado recebido via WebSocket e emite eventos de volta:

```tsx
const CartView: React.FC<{ scope: ViewScope<CartScope> }> = ({ scope }) => {
  const state = scope.useSnapshot();
  return (
    <Stack>
      {state.items.map(item => (
        <Typography key={item.id}>{item.name} × {item.quantity}</Typography>
      ))}
      <Button onClick={() => scope.submit(EventCodes.CONFIRM)}>Confirmar</Button>
    </Stack>
  );
};
```

**view.remote / remote.shell.flutter** — Shell Flutter em Dart. O mesmo protocolo WebSocket, mas com widgets Flutter para renderização:

```dart
class CartViewWidget extends StatelessWidget {
  final ViewScope<CartScope> scope;
  const CartViewWidget({required this.scope});

  @override
  Widget build(BuildContext context) {
    final state = scope.watch(context);
    return Column(children: [
      ...state.items.map((item) => Text('${item.name} × ${item.quantity}')),
      ElevatedButton(
        onPressed: () => scope.submit(EventCodes.confirm),
        child: const Text('Confirmar'),
      ),
    ]);
  }
}
```

---

## Apresentação Remota — O Protocolo Host/Shell

O protocolo de Apresentação Remota transfere estado e recebe eventos, tornando o cliente um thin shell sem lógica de negócio.

### Analogia com Remote Desktop

O protocolo é análogo a um Remote Desktop, mas operando na camada de dados da aplicação em vez de pixels. O servidor mantém toda a lógica — Presenters, ViewStates, regras de negócio, navegação. O cliente renderiza estados serializados e envia eventos de interação de volta.

```mermaid
sequenceDiagram
    participant Shell as Shell (React/Flutter)
    participant WS as WebSocket
    participant Host as Host (Java/Javalin)
    participant Presenters as Presenters + ViewStates

    Shell->>WS: CONNECT /dispatcher/{sessionId}
    WS->>Host: onConnect
    Host->>Presenters: inicializar sessão
    Host->>WS: { dirty views inicial }
    WS->>Shell: renderizar estados

    Shell->>WS: { requestId, eventCode, formData }
    WS->>Host: onMessage
    Host->>Presenters: syncClientToServer() → submit()
    Presenters->>Presenters: lógica + navegação
    Host->>WS: { dirty ViewStates (apenas os modificados) }
    WS->>Shell: reconciliar views afetadas
```

### Segurança do Canal

O protocolo implementa criptografia de sessão em duas camadas:

1. **Handshake RSA**: no início da sessão, o shell gera uma chave AES-256 efêmera, encripta com a chave pública RSA do servidor e envia via cookie `app_signature`. O servidor decripta e deriva a chave de sessão via PBKDF2 (salt + 250.000 iterações).  
2. **Tráfego AES-GCM**: após o handshake, todos os payloads são cifrados com a chave AES-256 de sessão.

Adicionalmente, a URL da sessão é assinada com HMAC-SHA256, impedindo que um cliente forje navegações para estados que não lhe foram concedidos.

### Serialização Delta de ViewStates

Uma propriedade crucial do protocolo é que apenas os ViewStates que mudaram (*dirty*) são enviados a cada ciclo. Isso é possível porque o Presenter marca explicitamente sua view como dirty ao chamar `app.markDirty(view)`. O host coleta apenas as views marcadas, serializa seus ViewStates para JSON e transmite o delta.

O Scope é serializado pelo `ScopeSerializer` do framework, que inspeciona os campos `public` por reflection:

```json
{
  "#": "cart:0",
  "items": [
    { "id": 1, "name": "Produto A", "price": 49.90, "quantity": 2 }
  ],
  "errorCode": 0,
  "errorMessage": null
}
```

O campo `"#"` é o `instanceId` que vincula o estado JSON à instância de view correspondente no shell.

### Virtual Threads para Escalabilidade

Cada sessão WebSocket é gerenciada por uma Virtual Thread (Java 21). Diferente de threads de plataforma, Virtual Threads são leves o suficiente para alocar uma por sessão ativa — o servidor pode manter milhares de sessões simultâneas sem o overhead de thread pools tradicionais.

---

## O Scope como Contrato de Serialização

Além de ser o estado público lido pela view para renderização, o `Scope` serve como contrato de serialização para o modo remoto.

O `Scope` não implementa serialização: seus campos são `Prop`s (e as ações, campos comuns). A serialização é feita exclusivamente pelo `ScopeSerializer` do framework:

- Itera todos os campos `public` do Scope via reflection
- Pula campos `static` e `transient`
- Injeta `"#": instanceId` como primeiro campo do objeto JSON
- Serializa campos `CubeView`/`Scope` como `"<nome>Id": vsid` (referência por ID)
- Suporta primitivos, `String`, coleções, `Map`, `Record` e POJOs com campos públicos

O `Scope` em si nunca implementa lógica de serialização — isso é responsabilidade do framework, não do código de aplicação.

---

## Conclusões

O modelo arquitetural provou-se robusto ao longo de uma década de evolução tecnológica significativa — e mais importante, expandiu-se para domínios que inicialmente eram trabalho futuro.

Os princípios que se mostraram mais duradouros:

**A hierarquia de Presenters como unidade de modularidade.** A divisão do estado da aplicação em Presenters hierárquicos mantém a complexidade localizada. Adicionar um novo módulo significa criar um novo Presenter e a sua classe de chaves (que declara o place), listá-la em `ShoppingApplication.PLACES` e implementar uma view em cada tecnologia de interesse — sem tocar no restante da aplicação.

**O Scope como fronteira entre lógica e apresentação.** O Scope é simultaneamente simples (campos públicos Java) e poderoso (serialização automática, diff incremental no modo remoto). Ele resolve o problema de transferência de dados sem criar objetos de transferência de valor separados — o mesmo objeto serve ao Presenter para nutrir a view e ao protocolo remoto para transmitir o estado.

**O slot como mecanismo de composição sem acoplamento.** O slot isola o Presenter pai do filho. O pai não sabe qual tipo de Presenter filho estará no slot — apenas que receberá um `Scope`. O filho não sabe em que parte da tela pai ele estará.

**O catálogo de views como ponto de extensão.** O `ScopeViews` que cada host entrega à aplicação (`setScopeViews`) é o único ponto de variabilidade entre implementações de view. Basta registrar outras fábricas por classe de escopo para trocar toda a camada de UI — e o host remoto nem registra: `fallback(RemoteViewImpl::forScope)` desenha qualquer escopo pelo seu `CLASS_ID`.

O **modo de Apresentação Remota** é a inovação central: ele demonstra que a separação entre lógica de apresentação e renderização não é apenas uma boa prática arquitetural — é uma propriedade física da arquitetura. O Presenter genuinamente não sabe onde a view roda: pode ser um processo local na mesma JVM, pode ser um browser a milhares de quilômetros de distância, ou um dispositivo móvel conectado via WebSocket.

A escolha do Java foi determinante. O sistema de tipos estático elimina uma classe inteira de erros de integração e torna o código mais rastreável. A plataforma JVM, com Virtual Threads, permite escalar o modo remoto a um custo computacional viável. O compilador Maven reproduz resultados determinísticos. E a longevidade da linguagem garante que o código escrito hoje ainda será compreensível em 2036.

---

## Trabalhos Futuros

- **Carga dinâmica de módulos**: a arquitetura suporta o conceito, mas não há implementação de referência para lazy loading de módulos Java no modo remoto.  
- **Rascunhos persistentes**: a instância da aplicação no backend pode ser descartada por diversos motivos — reinício do servidor, fechamento e reabertura da página, expiração de sessão. Ao retomar, o Presenter poderia usar um identificador presente na URL para buscar em storage externo (banco de dados, Redis, etc.) os dados de rascunho previamente salvos. Para o usuário, seria imperceptível que se trata de uma nova instância de aplicação no backend — o estado do formulário estaria preservado exatamente como foi deixado.  
- **Portabilidade do modo remoto para outros ambientes**: o protocolo host/shell está documentado o suficiente para ser implementado em Swift (iOS nativo) ou Kotlin (Android nativo) sem Flutter.  
- **Testes de integração end-to-end do protocolo remoto**: hoje os fluxos de navegação são testados via views mock. Seria valioso adicionar testes que exercitem o ciclo completo — WebSocket → Presenter → Scope serializado → deserialização no shell.
