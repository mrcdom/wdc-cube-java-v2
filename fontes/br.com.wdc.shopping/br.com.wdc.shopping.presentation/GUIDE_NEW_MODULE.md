# Guia: Como Criar um Novo Módulo (Feature)

Este documento explica, passo a passo, como adicionar uma nova feature ao sistema Shopping utilizando o padrão Cube MVP. Usaremos como exemplo a criação de um módulo fictício **"Wishlist"** (lista de desejos).

## Visão Geral

Criar um novo módulo envolve alterações em **3 camadas**:

| Camada | Módulo Maven | O que criar |
|--------|-------------|-------------|
| **Presentation** | `br.com.wdc.shopping.presentation` | Presenter, Scope, Service, DTOs |
| **Skeleton** (backend view) | `remote.host` | ViewImpl (wiring presenter ↔ React) |
| **React Client** (frontend) | `remote.shell.react` | Componente React + registro |

---

## Passo 1 — Presentation: Criar o Presenter e Scope

### 1.1 Criar a estrutura de pacotes

```
presenter/restricted/wishlist/
├── WishlistPresenter.java
├── WishlistScope.java
├── WishlistService.java          (se precisar acessar dados)
└── structs/
    └── WishlistItem.java         (DTOs, se houver)
```

### 1.2 Criar o Scope

O Scope define quais dados a view vai exibir. Implemente `Scope` e o método `write()` para serializar em JSON:

```java
package br.com.wdc.shopping.presentation.presenter.restricted.wishlist;

import org.apache.commons.lang3.StringUtils;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;

public class WishlistScope extends Scope {

    // Campos observados: set() de um valor diferente pede o redesenho sozinho
    public final Prop<List<WishlistItem>> items = prop(List.of());
    public final Prop<Integer> errorCode = prop(0);
    public final Prop<String> errorMessage = prop();

    // Ações que a view dispara — o presenter as liga no construtor; o serializador as ignora
    public ActionOf<Long> onRemoveItem = ActionOf.noop();
    public Action onOpenProducts = Action.NOOP;

    @Override
    public void write(String instanceId, ExtensibleObjectOutput json) {
        json.beginObject();
        {
            json.name("id").value(instanceId);

            json.name("items").beginArray();
            if (this.items != null) {
                this.items.forEach(item -> {
                    json.beginObject();
                    {
                        json.name("id").value(item.id);
                        json.name("name").value(item.name);
                        json.name("price").value(item.price);
                    }
                    json.endObject();
                });
            }
            json.endArray();

            if (StringUtils.isNotBlank(this.errorMessage)) {
                json.name("errorMessage").value(this.errorMessage);
            }
        }
        json.endObject();
    }
}
```

> **Regra**: O `write()` deve sempre emitir `json.name("id").value(instanceId)` como primeiro campo. Este é o identificador que o frontend usa para mapear o estado à view correta.

### 1.3 Criar o Presenter

O presenter contém a lógica de apresentação. Estenda `AbstractCubePresenter<ShoppingApplication>`:

```java
package br.com.wdc.shopping.presentation.presenter.restricted.wishlist;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;

public class WishlistPresenter extends AbstractCubePresenter<ShoppingApplication> {

    private static final Logger LOG = LoggerFactory.getLogger(WishlistPresenter.class);

    // Factory de view — será injetada pelo skeleton no bloco static {}
    public static Function<WishlistPresenter, CubeView> createView;

    // Estado público — acessível pela ViewImpl para serialização
    public final WishlistScope state = new WishlistScope();

    // Slot recebido do presenter pai (HomePresenter → contentSlot)
    private CubeViewSlot ownerSlot;

    public WishlistPresenter(ShoppingApplication app) {
        super(app);
        this.state.onRemoveItem = actionOf(this::removeItem);
        this.state.onOpenProducts = action(() -> new HomeKeys(this.app).flip());
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        if (initialization) {
            this.ownerSlot = new WishlistKeys(this.app, intent).parentSlot();
            this.view = createView.apply(this);

            // Carregar dados iniciais
            this.loadItems();
        }

        this.ownerSlot.setView(this.view);
        return true;
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        // Publicar parâmetros de URL, se houver
    }

    // :: User Actions — corpo das ações ligadas no construtor

    private void removeItem(Long itemId) {
        // lógica de negócio...
        this.loadItems();
    }

    // :: Data load

    private void loadItems() {
        // A lista nova pede o redesenho (a view será marcada dirty e enviada ao frontend)
        this.state.items.set(WishlistService.BEAN.loadItems(this.app.getSubject().getId()));
    }
}
```

**Pontos-chave:**
- `createView` é `static` e será atribuída pelo skeleton
- `applyParameters()` é chamado pelo sistema de navegação
- O state é criado com `bind(new WishlistScope())`: `set` de um valor diferente numa `Prop` marca a view como "dirty" para envio ao frontend, sem `this.update()`. Este só é preciso quando se mexe *dentro* de um valor já atribuído (um item de uma lista)
- Ações são campos do state (`Action`, `ActionOf<T>`, `ActionOf2<T, U>`) ligados no construtor com `action(...)`/`actionOf(...)`/`actionOf2(...)`. A guarda manda a falha não tratada para `app.alertUnexpectedError()` e, ao fim, republica o endereço — o corpo não tem `try/catch` genérico, só trata as falhas esperadas
- Erros de validação usam `errorCode` + `errorMessage` no state

### 1.4 Criar o Service (opcional)

```java
package br.com.wdc.shopping.presentation.presenter.restricted.wishlist;

public enum WishlistService {
    BEAN;

    public List<WishlistItem> loadItems(Long userId) {
        // Acessar repositório via BEAN estático
        return WishlistRepository.BEAN.get()
            .fetch(new WishlistCriteria().withUserId(userId))
            .stream().map(WishlistItem::create).toList();
    }
}
```

---

## Passo 2 — Registrar a Rota de Navegação

### 2.1 Criar `WishlistKeys`

Todo place tem uma classe de chaves, no pacote do seu presenter, que **declara o place** e é a única que conhece os nomes que ele lê e escreve no intent. Ela estende as chaves do place pai — abaixo de `HomeKeys`, a wishlist exige autenticação. O nome do place vai no endereço (`#wishlist?...`) e precisa ser único na árvore; o prefixo (duas letras) precisa ser único na aplicação.

```java
public final class WishlistKeys extends HomeKeys {

    public static final String PREFIX = "wl";

    public static final CubePlace PLACE = HomeKeys.PLACE.child("wishlist", WishlistPresenter::new);

    private static final class P {
        /** A categoria filtrada. */
        static final String CATEGORY = PREFIX + "-category";
    }

    public WishlistKeys(ShoppingApplication app) {
        this(app, null);
    }

    public WishlistKeys(ShoppingApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    // Getter que nunca lança (o endereço vem de fora); setter que tira o nome do endereço no valor padrão
    public String category() { ... }
    public WishlistKeys withCategory(String category) { ... }
}
```

### 2.2 Registrar em `ShoppingApplication.PLACES`

```java
public static final List<CubePlace> PLACES = List.of(
        RootKeys.PLACE,
        // ...
        WishlistKeys.PLACE);
```

A lista carrega as classes de chaves — sem ela, um F5 em `#wishlist` cairia na raiz, porque o place só entra na árvore quando `WishlistKeys` carrega. O `PlaceKeysTest` falha se alguma ficar de fora.

O **caminho hierárquico** sai da árvore: `new WishlistKeys(app).flip()` instancia os presenters necessários (Root → Restricted → Home → Wishlist) e chama `applyParameters` em cada um.

---

## Passo 3 — Skeleton: expor o presenter ao bridge (backend)

### 3.1 Escolher um VIEW_ID (classId)

Um identificador hexadecimal de 12 caracteres, único entre os presenters (ex.: `"d1e2f3a4b5c6"`). É o mesmo que os shells remotos usam para achar a view (`VIEW_ID` no TeaVM/SWT, `BaseViewClass.FC` no React).

### 3.2 Dar um código de evento a cada ação

O código vai na própria ligação da ação (Passo 1.3), junto com como ler os valores do `formData` que o cliente envia:

```java
public WishlistPresenter(ShoppingApplication app) {
    super(app);
    this.state.onRemoveItem = actionOf(1, FormParam.asLong("p.itemId"), this::removeItem);
    this.state.onOpenProducts = action(2, () -> new HomeKeys(this.app).flip());
}
```

- `FormParam.asLong/asInteger/asString(nome)` cobrem o comum; `FormParam.of(nome, conversão)` cobre um valor que chega codificado (a senha do login chega cifrada).
- O código é o contrato com os clientes remotos: tem de ser o mesmo nos shells. Um código repetido falha na construção do presenter; um código desconhecido vindo do cliente é erro (não é mais ignorado em silêncio).
- As views locais (Vaadin, SWT, teavm.web) não usam o código: chamam `state.onRemoveItem.run(id)` direto.

### 3.3 O skeleton

Com os códigos nas ações, o skeleton é uma linha — o `AbstractPresenter` despacha cada evento para a ação do seu código:

```java
public CubeSkeleton skeleton() {
    return skeleton("d1e2f3a4b5c6");
}
```

Se o cliente também sincroniza campos antes dos eventos, passe o `syncState` (como o login faz com o nome do usuário; `setSilently` porque o cliente já mostra o que enviou):

```java
public CubeSkeleton skeleton() {
    return skeleton("c677cda52d14", formData -> {
        if (formData.containsKey("userName")) {
            state.userName.setSilently(CoerceUtils.asString(formData.get("userName")));
        }
    });
}
```

### 3.4 Registrar a factory no host remoto

No bloco `static {}` de `ShoppingApplicationImpl` (`remote.host`):

```java
WishlistPresenter.createView = p -> new RemoteViewImpl((RemoteApplication) p.app, p, p.state, p.skeleton());
```

O `RemoteViewImpl` serializa o `state` (o valor de cada `Prop`; as ações ficam de fora) e encaminha os eventos ao skeleton.

---

## Passo 4 — React Client: Criar o Componente

### 4.1 Criar o arquivo da view

Crie `src/scripts/views/WishlistView.tsx`:

```tsx
import React from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import app, { type ViewProps } from '@root/App'
import { BaseViewClass } from '@root/utils/ViewUtils'

// :: Actions — devem corresponder aos event codes na ViewImpl
const ON_REMOVE_ITEM = 1
const ON_OPEN_PRODUCTS = 2

// :: View State — espelha os campos serializados pelo Scope.write()
export type WishlistScope = {
  items?: { id: number; name: string; price: number }[]
  errorMessage?: string
}

class WishlistViewClass extends BaseViewClass<ViewProps, WishlistScope> {

  override render({ className }: ViewProps): React.ReactNode {
    const { state } = this

    return (
      <Box className={className}>
        <Typography variant="h5">Minha Lista de Desejos</Typography>

        {state.items?.map((item) => (
          <Box key={item.id}>
            <Typography>{item.name} - R$ {item.price.toFixed(2)}</Typography>
            <Button onClick={() => this.emitRemoveItem(item.id)}>Remover</Button>
          </Box>
        ))}

        <Button onClick={this.emitOpenProducts}>Voltar</Button>
      </Box>
    )
  }

  // :: Emissores — enviam eventos ao backend

  readonly emitRemoveItem = (itemId: number) => {
    const { vsid } = this
    app.setFormField(vsid, 'itemId', itemId)  // campo lido em syncClientToServer/submit
    app.submit(vsid, ON_REMOVE_ITEM)          // eventCode = 1
  }

  readonly emitOpenProducts = () => {
    app.submit(this.vsid, ON_OPEN_PRODUCTS)   // eventCode = 2
  }
}

// O VID aqui DEVE ser idêntico ao VID no WishlistReactViewImpl.java
export default BaseViewClass.FC(WishlistViewClass, 'd1e2f3a4b5c6')
```

**Regras importantes:**
- Os **event codes** (1, 2, ...) devem corresponder ao `switch` em `ViewImpl.submit()`
- O **VID** no `BaseViewClass.FC()` deve ser idêntico ao da `GenericViewImpl` no Java
- O **tipo do state** deve espelhar os campos emitidos por `Scope.write()`
- Use `app.setFormField()` para enviar dados antes do `app.submit()`

### 4.2 Registrar a view

No arquivo `src/scripts/index.tsx`, importe e registre:

```tsx
import WishlistView from './views/WishlistView'

app.registerComponents(
  BrowserView,
  SlotView,
  RootView,
  LoginView,
  RestrictedView,
  CartView,
  ReceiptView,
  ProductView,
  ProductsPanel,
  PurchasesPanel,
  WishlistView,        // NOVO
)
```

---

## Passo 5 — Invocar a Navegação

Agora você pode navegar para o novo módulo a partir de qualquer presenter:

```java
// Em HomePresenter: um campo no state e a ligação no construtor
public Action onOpenWishlist = Action.NOOP;              // HomeScope
this.state.onOpenWishlist = action(() -> new WishlistKeys(this.app).flip());
```

E no frontend, adicionar um botão ou link que emita o evento correspondente.

---

## Resumo — Checklist

- [ ] **Presentation**
  - [ ] Criar `WishlistScope extends Scope` (campos `Prop`, criados com `prop(...)`)
  - [ ] Criar `WishlistPresenter extends AbstractCubePresenter<ShoppingApplication>`
  - [ ] Criar `WishlistService` (enum BEAN) se precisar de dados
  - [ ] Criar DTOs em `structs/` com `projection()` + `create()`
  - [ ] Criar `WishlistKeys extends HomeKeys` (prefixo único, `PLACE = HomeKeys.PLACE.child(...)`)
  - [ ] Registrar `WishlistKeys.PLACE` em `ShoppingApplication.PLACES`

- [ ] **Skeleton**
  - [ ] Escolher VID único (12 hex chars)
  - [ ] Dar um código de evento a cada ação (`action(1, ...)`, `actionOf(2, FormParam..., ...)`)
  - [ ] `skeleton()` → `return skeleton(VID);` (com `syncState`, se houver)
  - [ ] Registrar a factory no static block de `ShoppingApplicationImpl`

- [ ] **React Client**
  - [ ] Criar `WishlistView.tsx` com `BaseViewClass.FC(Class, VID)`
  - [ ] Definir event codes (devem bater com os códigos das ações do presenter)
  - [ ] Definir type do state (deve bater com os campos do Scope)
  - [ ] Registrar em `index.tsx` via `app.registerComponents()`

---

## Variante: Segmento (Painel)

Se o novo módulo for um **painel embutido** dentro de outra view (como o painel de produtos dentro da Home), ele **não ganha presenter**: é um escopo menor, classe interna do escopo de quem o mostra. O presenter dono liga as ações dele e o encaixa numa view:

```java
public static class HomeScope extends Scope {

    public static class WishlistPanelScope extends Scope {
        public static final String CLASS_ID = "e1f2a3b4c5d6";   // a view remota do segmento
        public static BiFunction<ShoppingApplication, WishlistPanelScope, CubeView> createView;

        public final Prop<List<WishlistItem>> items = prop(List.of());
        public ActionOf<Long> onRemoveItem = ActionOf.noop();
    }

    public final Prop<WishlistPanelScope> wishlistPanel = prop();
}

// Em HomePresenter: no construtor, as ações (com código de evento, no skeleton do segmento)
wishlistPanel.onRemoveItem = actionOf(wishlistPanel, 1, FormParam.asLong("p.itemId"), this::removeItem);

// Na inicialização: encaixa numa view (a fábrica do host) e põe no escopo da Home
this.state.wishlistPanel.set(attach(this.wishlistPanel, HomeScope.WishlistPanelScope.createView));
```

O segmento:
- **Não** tem presenter, classe de chaves nem place
- Redesenha só a própria view quando muda (o `attach` liga uma à outra) e é liberado com o presenter dono
- No host remoto é uma view como qualquer outra: `new RemoteViewImpl(app, scope, WishlistPanelScope.CLASS_ID)`; o pai o referencia como `"wishlistPanelViewId"`
- Nas views locais, a view do pai acha a do segmento por `state.wishlistPanel.get().view()`
