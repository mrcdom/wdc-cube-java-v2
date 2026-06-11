# gluon.ios

Launcher iOS para o app **WeDoCode Shopping** com Gluon Mobile + JavaFX. Compila o código compartilhado de `gluon.shared` para nativo ARM64 (device) via **GraalVM Native Image** e configura as dependências Gluon Attach específicas da plataforma iOS.

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
- Variáveis de ambiente: `GRAALVM_HOME`, `JAVA21_HOME` (ou `JAVA_HOME`)

## Build e Deploy

Todo o build/deploy é feito via `build.sh`. O script detecta automaticamente o simulador com status `Booted` do form factor selecionado.

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.gluon/gluon.ios

# Build nativo + deploy no simulador (phone)
./build.sh --native --sim --deploy

# Build nativo + deploy no simulador (tablet)
./build.sh --native --sim --tablet --deploy

# Apenas deploy (reutiliza build anterior)
./build.sh --deploy-only --sim

# Deploy em simulador específico
./build.sh --deploy-only --sim --sim-name="iPhone 17 Pro"

# Build nativo para device físico (requer certificado Apple)
./build.sh --native --deploy
```

### Apple Silicon (arm64)

No Apple Silicon o profile `-Pios-sim` do GluonFX não funciona (GluonFX Substrate 0.0.68 usa x86_64 como target fixo para `ios-sim` e o arquivo `vmone-ios-macos-x64.zip` nunca foi publicado). O `build.sh` aplica automaticamente o workaround:

1. Compila com o target `ios` (arm64 device)
2. Usa `vtool` para mudar o platform tag do binário de `IOS` → `IOSSIMULATOR`
3. Substitui as variáveis no `Default-Info.plist` e gera `Info.plist`
4. Copia os ícones PNG diretamente para o bundle (Assets.car gerado pelo link step usa platform=ios e é ignorado pelo Simulator)
5. Assina ad-hoc com `codesign -s -`

Ver [README do módulo pai](../README.md) para pré-requisitos completos e instruções de ambiente.
