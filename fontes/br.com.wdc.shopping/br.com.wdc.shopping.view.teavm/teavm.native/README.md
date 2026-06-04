# WDC Shopping — TeaVM Native (Tauri)

Projeto [Tauri 2](https://tauri.app/) que empacota o SPA TeaVM como aplicativo nativo para **macOS**, **Android** e **iOS**. O Tauri utiliza a WebView nativa do sistema operacional (WKWebView no macOS/iOS, System WebView no Android), resultando em binários leves — sem Chromium embarcado.

## Pré-requisitos

- **Rust** toolchain (`rustup`)
- **Tauri CLI**: `cargo install tauri-cli --version "^2"`
- **Java 21** + **Maven** (para compilar o módulo `teavm.web`)
- **Android**: Android SDK (`ANDROID_HOME`), NDK, `adb`
- **iOS**: Xcode, `xcrun simctl`

## Build

O script `build.sh` automatiza todo o processo: compila o módulo `teavm.web` (Java → JS) e em seguida empacota com o Tauri.

```bash
# Desktop (macOS)
./build.sh desktop

# Desktop em dev mode (hot reload via devUrl)
./build.sh desktop --dev

# Android — build + deploy no dispositivo conectado
./build.sh android --api-url http://192.168.1.8:8080 --deploy

# iOS — build + deploy no simulador
./build.sh ios --api-url http://localhost:8080 --deploy
./build.sh ios --sim-name "iPhone 16 Pro" --deploy
```

### Opções

| Opção | Descrição |
|-------|-----------|
| `desktop` / `android` / `ios` | Plataforma alvo (1º argumento) |
| `--dev` | Dev mode — desktop only, usa `devUrl` do `tauri.conf.json` |
| `--deploy` | Instalar e abrir no dispositivo/simulador após o build |
| `--api-url <url>` | URL do servidor API (padrão: `localhost:8080`; Android auto-detecta IP da rede) |
| `--sim-name <name>` | Nome do simulador iOS (padrão: `"iPhone 16 Pro"`) |

## Estrutura

```mermaid
graph TD
    root["br.com.wdc.shopping.view.teavm.native/"]
    buildsh["build.sh\n<small>Script unificado de build (desktop/android/ios)</small>"]
    pom["pom.xml\n<small>Declaração Maven (sem código Java)</small>"]
    srcTauri["src-tauri/"]
    tauriConf["tauri.conf.json\n<small>Configuração Tauri (janela, bundle, segurança)</small>"]
    cargoToml["Cargo.toml\n<small>Dependências Rust</small>"]
    src["src/"]
    libRs["lib.rs\n<small>Entry point Tauri</small>"]
    mainRs["main.rs\n<small>Bootstrap (desktop)</small>"]
    icons["icons/\n<small>Ícones do app (desktop + Android customizados)</small>"]
    capabilities["capabilities/\n<small>Permissões Tauri (window, webview)</small>"]
    gen["gen/\n<small>Código gerado (Android Studio project, Xcode project)</small>"]

    root --> buildsh
    root --> pom
    root --> srcTauri
    srcTauri --> tauriConf
    srcTauri --> cargoToml
    srcTauri --> src
    srcTauri --> icons
    srcTauri --> capabilities
    srcTauri --> gen
    src --> libRs
    src --> mainRs
```

## Ícones Android

Os ícones customizados ficam em `src-tauri/icons/android/` e são copiados automaticamente pelo `build.sh` para `src-tauri/gen/android/.../res/` antes do build, sobrescrevendo os ícones padrão do Tauri:

```mermaid
graph TD
    root["icons/android/"]
    mdpi["mipmap-mdpi/<br/><small>48×48</small>"]
    hdpi["mipmap-hdpi/<br/><small>72×72</small>"]
    xhdpi["mipmap-xhdpi/<br/><small>96×96</small>"]
    xxhdpi["mipmap-xxhdpi/<br/><small>144×144</small>"]
    xxxhdpi["mipmap-xxxhdpi/<br/><small>192×192</small>"]
    anydpi["mipmap-anydpi-v26/<br/><small>Adaptive icon XML (API 26+)</small>"]
    values["values/<br/><small>Cor de fundo do ícone adaptativo</small>"]

    root --> mdpi
    root --> hdpi
    root --> xhdpi
    root --> xxhdpi
    root --> xxxhdpi
    root --> anydpi
    root --> values
```

## Fluxo de Build

```mermaid
graph TD
    buildSh["build.sh"]
    step1["1/2 — Compila teavm.web via Maven<br/>Java → app.js + index.html"]
    step2["2/2 — Empacota com Tauri via Cargo"]
    desktop[".app bundle (macOS)"]
    android["APK debug aarch64 + adb install"]
    ios[".app simulador aarch64-sim + xcrun install"]

    buildSh --> step1
    buildSh --> step2
    step2 --> desktop
    step2 --> android
    step2 --> ios
```
