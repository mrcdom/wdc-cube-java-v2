# gluon.android

Launcher Android para o app **WeDoCode Shopping** com Gluon Mobile + JavaFX. Compila o código compartilhado de `gluon.shared` para nativo ARM64/ARM32 via **GraalVM Native Image** e configura as dependências Gluon Attach específicas da plataforma Android.

Contém apenas a classe de launcher — toda a lógica de UI está em `gluon.shared`.

## Classe

| Classe | Responsabilidade |
|--------|------------------|
| `ShoppingGluonAndroidLauncher` | Launcher Android: configura classpath nativo Gluon, inicializa `ShoppingGluonMain` via `javafxports-application` |

## Dependências

- `gluon.shared` — UI compartilhada (views, theme, presenters)
- `br.com.wdc.shopping.persistence` — acesso a dados via API REST
- `br.com.wdc.shopping.scripts` — scripts de inicialização
- `com.h2database:h2` — banco H2 embarcado
- `com.gluonhq.attach:*` — implementações Android (display, lifecycle, storage, statusbar)

## Pré-requisitos

- **GraalVM** com suporte a Native Image (recomendado: `graalvm-gluon-23`)
- **Android SDK** + **NDK**
- **Maven 3.9+** com o plugin `gluonfx-maven-plugin`

## Build e Deploy

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.gluon/gluon.android

# Build nativo Android
mvn gluonfx:build -Pandroid

# Deploy no device/emulador conectado
mvn gluonfx:install gluonfx:run -Pandroid
```

Ver [README do módulo pai](../README.md) para pré-requisitos completos e instruções de ambiente.
