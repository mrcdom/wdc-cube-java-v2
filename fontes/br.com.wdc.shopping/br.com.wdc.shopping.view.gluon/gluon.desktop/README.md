# gluon.desktop

Launcher desktop JVM para o app **WeDoCode Shopping** com Gluon Mobile + JavaFX. Executa o código compartilhado de `gluon.shared` na JVM padrão (sem compilação nativa), útil para desenvolvimento e testes rápidos antes de compilar para iOS/Android.

Contém apenas as classes de launcher — toda a lógica de UI está em `gluon.shared`.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `ShoppingGluonDesktopLauncher` | Launcher principal: configura classpath JavaFX para desktop e inicia `DesktopShoppingGluonMain` |
| `DesktopShoppingGluonMain` | Entrypoint que instancia e lança `ShoppingGluonMain` (Application JavaFX) |

## Dependências

- `gluon.shared` — UI compartilhada (views, theme, presenters)
- Dependências Gluon Attach para desktop (display, lifecycle)

## Pré-requisitos

- **Java 21+**
- **Maven 3.9+** com o plugin `javafx-maven-plugin` ou `gluonfx-maven-plugin`

## Build e Execução

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.gluon/gluon.desktop
mvn javafx:run
```

Ver [README do módulo pai](../README.md) para contexto completo e comparação entre plataformas.
