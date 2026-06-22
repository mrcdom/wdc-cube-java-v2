# remote.shell.cn1 — shell fino Codename One

Frontend **Codename One (CN1)** do padrão *remote shell* da WDC Shopping. Os
presenters e ViewStates rodam no **servidor** (`remote.host`); este módulo é um
**cliente fino** que fala o protocolo *bridge* por WebSocket e renderiza com a UI
nativa do Codename One — **uma única base Java** que vira app de **iOS, Android,
desktop (Mac/Windows/Linux) e web**.

É um dos seis frontends de referência do projeto (React, Flutter, Vaadin, SWT,
Gluon e este). Todos compartilham os mesmos presenters server-side; só a camada de
renderização muda.

---

## Arquitetura (resumo)

- **Protocolo bridge reimplementado** com APIs do próprio CN1 (sem `okhttp`/`gson`/
  `java.net.http`/reflection — a VM do CN1 é restrita): `ConnectionRequest`
  (`GET /api/session/init`), `com.codename1.io.WebSocket`
  (`ws://host/dispatcher/{appId}`), `JSONParser`. Ver `bridge/BridgeSession`.
- **Cripto de sessão** (`bridge/Cn1Crypto`, `bridge/Cn1ClientStorage`): chave pública
  RSA vinda do `/api/session/init`, AES + HMAC por sessão (login seguro).
- **Não-reativo (estilo Vaadin)**: cada view é **uma classe** que constrói os widgets
  **uma vez** em `build()` e os **muta** em `doUpdate()`. Nada é reconstruído por push;
  um `markDirty` + `flush()` coalescido re-renderiza só o que mudou.
- **DSL de construção** (`util/Cn1Dom`): `render(layout, (dom, r) -> …)` com
  `boxX/boxY/border/container/label/spanLabel/button`. Convenção: `setUIID` é a
  **primeira** ação do lambda (helpers como `FontImage.setMaterialIcon` "assam" cor/
  tamanho a partir do estilo).
- **Montagem por slot** (`bridge/ViewSlot`): container de filho único com guard
  ("re-monta só na transição"). A raiz do app e os pontos de troca de view usam slots.
- **Tema em SCSS** (`common/src/main/scss` + `views/*/_*.scss`) → CSS → `theme.res`.
  **Densidade-independente** (`util/Px.mm` → `convertToPixels`); **responsivo** por
  forma (`app.isExpanded()` = tablet/desktop vs. compacto no telefone), com rebuild ao
  cruzar o breakpoint **ou** girar a tela.
- **URL do backend por destino** (`ServerConfig`): resolve sozinho a URL que o
  emulador/simulador usa para alcançar o backend no host (ver tabela em *Backend*).

### Por que não reusa o core compartilhado

Diferente dos frontends "gordos" (Gluon/SWT/Vaadin), este shell **não** depende de
`presentation`/`domain`/`persistence.client` — como os shells React e Flutter, ele
reimplementa o protocolo com APIs do CN1, mantendo o classpath leve e compatível com a
VM restrita do Codename One.

### Projeto semi-destacado (fora do reactor Maven)

É um projeto Codename One completo, com **parent e plugin próprios**
(`codenameone-maven-plugin`). Por isso **não** está nos `<modules>` de
`view.remote/pom.xml` — o `mvn clean package` da raiz **não** o constrói. Build e
execução são independentes (via `run.sh`/`build.sh`/`android.sh`/`mvnw`), como os
shells não-Java.

---

## Pré-requisito comum: backend no ar

Todas as formas de rodar precisam do backend (presenters server-side) na porta **8080**.
A partir da **raiz do repositório**:

```bash
./work/bin/start-server.sh        # backend, porta 8080
./work/bin/start-h2-server.sh     # se o banco estiver configurado em modo TCP
```

### URL do backend por plataforma (`ServerConfig`)

| Destino                              | URL usada            | Como ajustar |
|--------------------------------------|----------------------|--------------|
| iOS (simulador), desktop, web        | `http://localhost:8080` | automático |
| Android (emulador)                   | `http://10.0.2.2:8080`  | automático (alias do host no emulador) |
| Aparelho real (iOS/Android)          | informar a URL          | `DEVICE_URL` em `ServerConfig` **ou** `-Dcodename1.arg.serverUrl=https://…` |

---

## Como rodar em cada plataforma

> Comandos a partir **desta pasta** (`remote.shell.cn1`), salvo o backend (raiz do repo).

### Builds locais (sem conta Codename One)

| Plataforma            | Comando                       | Requisitos |
|-----------------------|-------------------------------|-----------|
| **Simulador CN1**     | `./run.sh simulator`          | JDK instalado |
| **Desktop (rodar)**   | `./run.sh desktop`            | JDK instalado |
| **Desktop (jar)**     | `./build.sh jar`              | gera `javase/target/*.jar` executável |
| **Android (emulador)**| `./android.sh --gen`          | Android SDK + emulador rodando (ver abaixo) |
| **iOS (sim/device)**  | `./build.sh ios_source`       | macOS + Xcode (abre projeto em `ios/target`) |

#### Android — `android.sh` (recomendado)

O `./build.sh android_source` gera um projeto Gradle, mas o CN1 o **recria a cada
geração** e zera ajustes do ambiente local. O `android.sh` reaplica tudo e roda:

```bash
./android.sh --gen          # regenera + corrige + installDebug + abre no emulador
./android.sh                # sem regerar: só corrige, instala e abre
./android.sh --gen --patch  # regenera + corrige, sem buildar → abrir/rodar no IntelliJ/Android Studio
```

Requisitos: **Android SDK** com *cmdline-tools* e ao menos uma plataforma **estável**
(ex.: `android-36`); **JDK 17** para o Gradle (o Gradle 8.1 não roda sob JDK > 19; o
script força isso); um **emulador (AVD) já rodando**. O `android.sh` cuida de: fixar
`compileSdk/targetSdk` na maior plataforma estável, apontar o Gradle ao JDK 17 e o
`sdk.dir`. (HTTP cleartext já vem de build hint.)

Fluxo IDE: rode `./android.sh --gen --patch`, **abra o projeto gerado** (`android/
target/*-android-source`) na IDE e dê Run — **não** regere de dentro dela.

#### iOS

```bash
./build.sh ios_source       # gera o projeto Xcode em ios/target
```
Abra o `.xcodeproj`/workspace no **Xcode** e rode no simulador ou no dispositivo.

### Builds no servidor Codename One (exigem conta)

Geram o artefato nativo final na nuvem do Codename One:

| Plataforma                | Comando                  |
|---------------------------|--------------------------|
| Android (APK/AAB)         | `./build.sh android`     |
| iOS (device)              | `./build.sh ios`         |
| iOS (App Store)           | `./build.sh ios_release` |
| Web (JavaScript)          | `./build.sh javascript`  |
| macOS desktop             | `./build.sh mac_desktop` |
| macOS nativo (sem JVM)    | `./build.sh mac_native`  |
| Windows desktop           | `./build.sh windows_desktop` |
| Windows (UWP/device)      | `./build.sh windows_device` |
| Linux nativo              | `./build.sh linux_device` |

Veja `./build.sh help` para a lista completa.

### Configurações do Codename One

```bash
./run.sh settings    # abre o editor de settings (codenameone_settings.properties)
./run.sh update      # atualiza as libs do Codename One
```

---

## Estrutura

| Pasta | Papel |
|-------|-------|
| `common/` | App: lifecycle, protocolo bridge, views, widgets, tema SCSS e `codenameone_settings.properties` |
| `javase/` | Porta desktop/**simulador** (validável localmente, sem build server) |
| `android/`, `ios/`, `javascript/`, `linux/` | Portas dos alvos nativos |
| `build.sh` | Builds (locais e de servidor) por alvo |
| `run.sh` | Simulador/desktop/settings |
| `android.sh` | Build/run local no emulador Android (corrige o projeto gerado a cada vez) |

### Pacotes em `common`

```
…shell.cn1
├─ ShoppingCn1RemoteApp   # Lifecycle: form, conexão, re-sync por push, slot raiz, splash
├─ ServerConfig           # URL do backend por destino
├─ Sel / *Sel             # UIIDs (seletores de tema) por pacote
├─ bridge/                # BridgeSession, Cn1Crypto, Cn1ClientStorage, ViewSlot, Abstract*View
├─ util/                  # Cn1Dom (DSL), Px, Guard, Money, Json, Images, Decor, …
├─ widgets/               # BackButton, CardHeader, IconButton, HtmlText
└─ views/                 # login, home, product, cart, receipt (+ RootCn1View)
```

Classe de entrada: `ShoppingCn1RemoteApp` (estende `com.codename1.system.Lifecycle`);
`mainName`/`packageName` em `common/codenameone_settings.properties`.

---

## Validação rápida do tema/código

```bash
# compila o common (Java + SCSS→CSS→theme.res); use sempre 'clean' (a IDE deixa .class stale)
./mvnw -q -pl common -DskipTests clean process-classes
```

> Para a visão arquitetural completa do projeto, ver o README.md da raiz e a pasta `docs/`.
