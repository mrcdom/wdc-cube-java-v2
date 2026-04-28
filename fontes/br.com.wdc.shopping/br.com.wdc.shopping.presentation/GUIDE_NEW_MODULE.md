# Guia: Como Criar um Novo Módulo (Feature)

Este documento explica, passo a passo, como adicionar uma nova feature ao sistema Shopping utilizando o padrão Cube MVP. Usaremos como exemplo a criação de um módulo fictício **"Wishlist"** (lista de desejos).

## Visão Geral

Criar um novo módulo envolve alterações em **3 camadas**:

| Camada | Módulo Maven | O que criar |
|--------|-------------|-------------|
| **Presentation** | `br.com.wdc.shopping.presentation` | Presenter, ViewState, Service, DTOs |
| **Skeleton** (backend view) | `br.com.wdc.shopping.view.react.skeleton` | ViewImpl (wiring presenter ↔ React) |
| **React Client** (frontend) | `br.com.wdc.shopping.view.react.client` | Componente React + registro |

---

## Passo 1 — Presentation: Criar o Presenter e ViewState

### 1.1 Criar a estrutura de pacotes

```
presenter/restricted/wishlist/
├── WishlistPresenter.java
├── WishlistViewState.java
├── WishlistService.java          (se precisar acessar dados)
└── structs/
    └── WishlistItem.java         (DTOs, se houver)
```

### 1.2 Criar o ViewState

O ViewState define quais dados a view vai exibir. Implemente `ViewState` e o método `write()` para serializar em JSON:

```java
package br.com.wdc.shopping.presentation.presenter.restricted.wishlist;

import org.apache.commons.lang3.StringUtils;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.ViewState;

public class WishlistViewState implements ViewState {

    public List<WishlistItem> items;
    public int errorCode;
    public String errorMessage;

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
import br.com.wdc.shopping.presentation.PlaceAttributes;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.Routes;

public class WishlistPresenter extends AbstractCubePresenter<ShoppingApplication> {

    private static final Logger LOG = LoggerFactory.getLogger(WishlistPresenter.class);

    // Factory de view — será injetada pelo skeleton no bloco static {}
    public static Function<WishlistPresenter, CubeView> createView;

    // Estado público — acessível pela ViewImpl para serialização
    public final WishlistViewState state = new WishlistViewState();

    // Slot recebido do presenter pai (HomePresenter → contentSlot)
    private CubeViewSlot ownerSlot;

    public WishlistPresenter(ShoppingApplication app) {
        super(app);
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        if (initialization) {
            this.ownerSlot = intent.getViewSlot(PlaceAttributes.SLOT_OWNER);
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

    // :: User Actions — métodos chamados pela ViewImpl

    public void onRemoveItem(Long itemId) {
        try {
            // lógica de negócio...
            this.loadItems();
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, "Removing wishlist item", caught);
        }
    }

    public void onOpenProducts() {
        try {
            Routes.home(this.app);
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, "Going to home", caught);
        }
    }

    // :: Data load

    private void loadItems() {
        this.state.items = WishlistService.BEAN.loadItems(this.app.getSubject().getId());
        this.update(); // marca a view como dirty → será enviada ao frontend
    }
}
```

**Pontos-chave:**
- `createView` é `static` e será atribuída pelo skeleton
- `applyParameters()` é chamado pelo sistema de navegação
- `this.update()` marca a view como "dirty" para envio ao frontend
- Erros inesperados vão para `app.alertUnexpectedError()`
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

### 2.1 Adicionar o Place em `Routes.java`

```java
public enum Place implements CubePlace {
    ROOT("public", Routes::root, RootPresenter::new),
    LOGIN("public/login", Routes::login, LoginPresenter::new),
    HOME("home", Routes::home, HomePresenter::new),
    CART("cart", Routes::cart, CartPresenter::new),
    PRODUCT("product", Routes::product, ProductPresenter::new),
    RECEIPT("receipt", Routes::receipt, ReceiptPresenter::new),

    // NOVO:
    WISHLIST("wishlist", Routes::wishlist, WishlistPresenter::new);

    // ... resto do enum inalterado
}
```

### 2.2 Adicionar o método de navegação

```java
// :: Wishlist

public static boolean wishlist(ShoppingApplication app) {
    return wishlist(app, app.newIntent());
}

public static boolean wishlist(ShoppingApplication app, CubeIntent intent) {
    //@formatter:off
    return app.navigate()
            .step(Place.ROOT)
            .step(Place.HOME)      // ← nível pai (requer autenticação)
            .step(Place.WISHLIST)  // ← novo place
            .execute(intent);
    //@formatter:on
}
```

A cadeia de `step()` define o **caminho hierárquico**. O sistema instancia os presenters necessários (Root → Home → Wishlist) e chama `applyParameters` em cada um.

---

## Passo 3 — Skeleton: Criar a ViewImpl (backend)

### 3.1 Escolher um VIEW_ID (VID)

Gere um identificador único de 12 caracteres hexadecimais. Esse VID vincula o backend ao frontend:

```
VID = "d1e2f3a4b5c6"
```

> **Regra**: O VID deve ser idêntico no Java (`GenericViewImpl`) e no TypeScript (`BaseViewClass.FC`).

### 3.2 Criar a ViewImpl

```java
package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.util.Map;
import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.wishlist.WishlistPresenter;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class WishlistReactViewImpl extends GenericViewImpl {

    private static final String VID = "d1e2f3a4b5c6";

    private final WishlistPresenter presenter;

    public WishlistReactViewImpl(WishlistPresenter presenter) {
        super(presenter.app(), VID);
        this.presenter = presenter;
    }

    // Deserializar dados do formulário do frontend para o presenter.state
    @Override
    public void syncClientToServer(Map<String, Object> formData) {
        // Extrair campos que o frontend envia antes de um evento
        // Exemplo: nada a sincronizar neste caso
    }

    // Mapear event codes a métodos do presenter
    @Override
    public void submit(int eventCode, int eventQtde, Map<String, Object> formData)
            throws Exception {
        switch (eventCode) {
            case 1 -> {
                var itemId = CoerceUtils.asLong(formData.get("itemId"));
                this.presenter.onRemoveItem(itemId);
            }
            case 2 -> this.presenter.onOpenProducts();
        }
    }

    // Serializar o estado do presenter para JSON
    @Override
    public void writeState(ExtensibleObjectOutput json) {
        this.presenter.state.write(this.instanceId, json);
    }
}
```

**Responsabilidades da ViewImpl:**
- `syncClientToServer()` — deserializa campos do formulário para `presenter.state`
- `submit(eventCode, ...)` — mapeia event codes numéricos a métodos do presenter
- `writeState()` — delega a serialização para `presenter.state.write()`

### 3.3 Registrar a factory no bloco static de ApplicationReactImpl

No arquivo `ApplicationReactImpl.java`, adicione no bloco `static {}`:

```java
static {
    RootPresenter.createView = RootReactViewImpl::new;
    LoginPresenter.createView = LoginReactViewImpl::new;
    HomePresenter.createView = HomeReactViewImpl::new;
    ProductPresenter.createView = ProductReactViewImpl::new;
    CartPresenter.createView = CartReactViewImpl::new;
    ReceiptPresenter.createView = ReceiptReactViewImpl::new;

    // NOVO:
    WishlistPresenter.createView = WishlistReactViewImpl::new;
}
```

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

// :: View State — espelha os campos serializados pelo ViewState.write()
export type WishlistViewState = {
  items?: { id: number; name: string; price: number }[]
  errorMessage?: string
}

class WishlistViewClass extends BaseViewClass<ViewProps, WishlistViewState> {

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
- O **tipo do state** deve espelhar os campos emitidos por `ViewState.write()`
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
// Em HomePresenter ou qualquer outro presenter
public void onOpenWishlist() {
    try {
        Routes.wishlist(this.app);
    } catch (Exception caught) {
        this.app.alertUnexpectedError(LOG, "Going to wishlist", caught);
    }
}
```

E no frontend, adicionar um botão ou link que emita o evento correspondente.

---

## Resumo — Checklist

- [ ] **Presentation**
  - [ ] Criar `WishlistViewState implements ViewState` com `write()`
  - [ ] Criar `WishlistPresenter extends AbstractCubePresenter<ShoppingApplication>`
  - [ ] Criar `WishlistService` (enum BEAN) se precisar de dados
  - [ ] Criar DTOs em `structs/` com `projection()` + `create()`
  - [ ] Adicionar `Place.WISHLIST` em `Routes.java`
  - [ ] Adicionar método `Routes.wishlist()` com cadeia de steps

- [ ] **Skeleton**
  - [ ] Escolher VID único (12 hex chars)
  - [ ] Criar `WishlistReactViewImpl extends GenericViewImpl`
  - [ ] Implementar `syncClientToServer()`, `submit()`, `writeState()`
  - [ ] Registrar factory em `ApplicationReactImpl` static block

- [ ] **React Client**
  - [ ] Criar `WishlistView.tsx` com `BaseViewClass.FC(Class, VID)`
  - [ ] Definir event codes (devem bater com ViewImpl.submit)
  - [ ] Definir type do state (deve bater com ViewState.write)
  - [ ] Registrar em `index.tsx` via `app.registerComponents()`

---

## Variante: Presenter Filho (Painel)

Se o novo módulo for um **painel embutido** dentro de outra view (como ProductsPanel dentro de Home), use `AbstractChildPresenter` em vez de `AbstractCubePresenter`:

```java
public class WishlistPanelPresenter extends AbstractChildPresenter<ShoppingApplication> {

    public static Function<WishlistPanelPresenter, CubeView> createView;
    public final WishlistPanelViewState state = new WishlistPanelViewState();
    public final HomePresenter owner;

    public WishlistPanelPresenter(ShoppingApplication app, HomePresenter owner) {
        super(app);
        this.owner = owner;
    }

    @Override
    protected CubeView onCreateView() {
        return createView.apply(this);
    }

    @Override
    protected void onInitialize() {
        // Carregar dados iniciais
    }
}
```

O presenter pai instancia e gerencia o ciclo de vida:

```java
// Em HomePresenter.applyParameters():
this.wishlistPanel = new WishlistPanelPresenter(this.app, this);
this.state.wishlistPanelView = this.wishlistPanel.initialize();
```

O `AbstractChildPresenter`:
- **Não** aparece no `Routes`
- **Não** tem `applyParameters` / `publishParameters`
- É gerenciado manualmente pelo pai (`initialize()` / `release()`)
- Possui `onCreateView()` e `onInitialize()` como ciclo de vida
