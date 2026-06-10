# gluon.ios

Launcher iOS para o app **WeDoCode Shopping** com Gluon Mobile + JavaFX. Compila o código compartilhado de `gluon.shared` para nativo ARM64 (device) ou x86_64 (simulador) via **GraalVM Native Image** e configura as dependências Gluon Attach específicas da plataforma iOS.

Contém apenas a classe de launcher — toda a lógica de UI está em `gluon.shared`.

## Classe

| Classe | Responsabilidade |
|--------|------------------|
| `ShoppingGluonIosLauncher` | Launcher iOS: configura classpath nativo Gluon para iOS e inicializa `ShoppingGluonMain` |

## Dependências

- `gluon.shared` — UI compartilhada (views, theme, presenters)
- `com.gluonhq.attach:*` — implementações iOS (display, lifecycle, storage, statusbar)

## Pré-requisitos

- **macOS** com **Xcode** e Command Line Tools instalados
- **GraalVM** com suporte a Native Image (recomendado: `graalvm-gluon-23`)
- **Maven 3.9+** com o plugin `gluonfx-maven-plugin`

## Build e Deploy

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.gluon/gluon.ios

# Build nativo para simulador
mvn gluonfx:build -Pios-sim

# Deploy no simulador
mvn gluonfx:run -Pios-sim

# Build nativo para device físico (requer certificado Apple)
mvn gluonfx:build -Pios
mvn gluonfx:install gluonfx:run -Pios
```

Ver [README do módulo pai](../README.md) para pré-requisitos completos e instruções de ambiente.
