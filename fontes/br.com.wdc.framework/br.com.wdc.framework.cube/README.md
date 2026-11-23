# WDC Framework — Cube

Motor do padrão **Cube MVP** — gerencia presenters hierárquicos, views, navegação por intents e estado reativo.

---

## Conceitos

```mermaid
graph LR
    App["CubeApplication"]
    Nav["CubeNavigation"]
    Place["CubePlace"]
    Intent["CubeIntent"]
    Pres["CubePresenter"]
    View["CubeView"]
    Slot["CubeViewSlot"]
    State["ViewState"]

    App -->|"contém"| Pres
    App -->|"navega via"| Nav
    Nav -->|"passos"| Place
    Place -->|"factory →"| Pres
    Pres -->|"gerencia"| View
    View -->|"compõe em"| Slot
    Intent -->|"carrega parâmetros"| Pres
    Pres -.->|"estado"| State
```

### Ciclo de Navegação

1. O app declara a árvore de places como constantes: `CubePlace.root("public", RootPresenter::new)` e, abaixo dela, `ROOT.child("home", HomePresenter::new)`. O caminho de cada place sai da árvore, e o nome é único nela.
2. Monta um `CubeIntent` com parâmetros (key-value, query-string compatível).
3. Chama `app.navigate(place, intent)` — ou `app.go("home?x=1")`, que acha o place pelo nome (nome desconhecido vai para a raiz). O motor percorre `place.getPath()` da raiz até o destino:
   - Instancia os presenters que faltam pela factory do place.
   - Invoca `applyParameters(intent, initialization, deepest)` em cada presenter.
   - Remove presenters que não fazem mais parte da hierarquia (chama `release()`).
   - Invoca `commitComputedState()` para todos.
5. A UI é atualizada por `CubeView.update()`.

---

## Classes Principais

| Classe/Interface | Responsabilidade |
|------------------|-----------------|
| `CubeApplication` | Singleton da app — mantém mapa de presenters e atributos, coordena navegação |
| `CubePresenter` | Contrato para presenters gerenciados pela navegação |
| `AbstractCubePresenter<A>` | Base genérica — associa app + view, delega `update()`/`release()` |
| `AbstractChildPresenter<A>` | Presenters filhos cujo ciclo é gerenciado pelo owner (não pela navegação) |
| `CubeView` | Interface da view — `instanceId()`, `update()`, `release()` |
| `CubeViewSlot` | Slot onde uma `CubeView` é encaixada na view pai |
| `CubePlace` | Nó da árvore de places — nome (único na árvore), pai, caminho, factory de presenter; o id cresce com a profundidade |
| `CubeIntent` | Encapsula destino (place) + parâmetros tipados (parse de query-string) |
| `CubeNavigation<T>` | Engine que executa transições: cria, reutiliza e destrói presenters |
| `CubeSkeleton` | Interface para comunicação remota: `submit(eventCode, formData)`, `syncState()` |
| `ViewState` | Marker interface para estados da view |
| `PresenterBase` | Interface raiz: `commitComputedState()` + `release()` |

---

## Utilitários

| Classe | Descrição |
|--------|-----------|
| `QueryStringBuilder` | Serializa parâmetros de `CubeIntent` em query-string |
| `QueryStringParser` | Deserializa query-string para `CubeIntent` |
| `ArrayUtils` | Operações em arrays |
| `ReflectArrayCompat` | Compatibilidade com reflexão em arrays (TeaVM) |

---

## Coordenadas Maven

```xml
<dependency>
    <groupId>io.github.mrcdom.wdc.java</groupId>
    <artifactId>framework-cube</artifactId>
    <version>2.0.0</version>
</dependency>
```
