# WDC Store — TeaVM Native (Tauri)

A [Tauri 2](https://tauri.app/) project that packages the TeaVM SPA as a native application for **macOS**, **Android** and **iOS**. Tauri uses the operating system's native WebView (WKWebView on macOS/iOS, System WebView on Android), resulting in lightweight binaries — no embedded Chromium.

## Prerequisites

- **Rust** toolchain (`rustup`)
- **Tauri CLI**: `cargo install tauri-cli --version "^2"`
- **Java 25** + **Maven** (to compile the `teavm.web` module)
- **Android**: Android SDK (`ANDROID_HOME`), NDK, `adb`
- **iOS**: Xcode, `xcrun simctl`

## Build

The `build.sh` script automates the whole process: it compiles the `teavm.web` module (Java → JS) and then packages it with Tauri.

```bash
# Desktop (macOS)
./build.sh desktop

# Desktop in dev mode (hot reload via devUrl)
./build.sh desktop --dev

# Android — build + deploy to the running phone emulator (API at http://10.0.2.2:8080)
./build.sh android --deploy
./build.sh android --deploy --form-factor tablet         # running tablet emulator
./build.sh android --api-url http://192.168.1.8:8080 --deploy

# iOS — build + deploy to the booted iPhone simulator
./build.sh ios --deploy
./build.sh ios --deploy --form-factor tablet             # booted iPad simulator
```

### Options

| Option | Description |
|-------|-----------|
| `desktop` / `android` / `ios` | Target platform (1st argument; default `desktop`) |
| `--dev` | Dev mode — desktop only, uses `devUrl` from `tauri.conf.json` |
| `--deploy` | Install and open on the emulator/simulator after the build |
| `--api-url <url>` | API server URL (default: `http://10.0.2.2:8080` for Android — the emulator's alias for the host —, `http://localhost:8080` otherwise; a physical device on the LAN needs the host's LAN IP) |
| `--form-factor <phone\|tablet>` | Which running emulator / booted simulator to deploy to (default: `phone`; `tablet` picks an iPad simulator or a tablet emulator) |

The step that compiles the web module runs `mvn clean install -pl teavm.commons,teavm.web`; the `presentation`/`persistence.client` JARs come from `~/.m2`, so run `./work/bin/build-frontends.sh teavm.web` (which reinstalls them) after changing them.

## Structure

```mermaid
graph TD
    root["teavm.native/"]
    buildsh["build.sh\n<small>Unified build script (desktop/android/ios)</small>"]
    pom["pom.xml\n<small>Maven declaration (no Java code)</small>"]
    srcTauri["src-tauri/"]
    tauriConf["tauri.conf.json\n<small>Tauri configuration (window, bundle, security)</small>"]
    cargoToml["Cargo.toml\n<small>Rust dependencies</small>"]
    src["src/"]
    libRs["lib.rs\n<small>Tauri entry point</small>"]
    mainRs["main.rs\n<small>Bootstrap (desktop)</small>"]
    icons["icons/\n<small>App icons (custom desktop + Android)</small>"]
    capabilities["capabilities/\n<small>Tauri permissions (window, webview)</small>"]
    gen["gen/\n<small>Generated code (Android Studio project, Xcode project)</small>"]

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

## Android Icons

The custom icons live in `src-tauri/icons/android/` and are copied automatically by `build.sh` to `src-tauri/gen/android/.../res/` before the build, overwriting Tauri's default icons:

```mermaid
graph TD
    root["icons/android/"]
    mdpi["mipmap-mdpi/<br/><small>48×48</small>"]
    hdpi["mipmap-hdpi/<br/><small>72×72</small>"]
    xhdpi["mipmap-xhdpi/<br/><small>96×96</small>"]
    xxhdpi["mipmap-xxhdpi/<br/><small>144×144</small>"]
    xxxhdpi["mipmap-xxxhdpi/<br/><small>192×192</small>"]
    anydpi["mipmap-anydpi-v26/<br/><small>Adaptive icon XML (API 26+)</small>"]
    values["values/<br/><small>Adaptive icon background color</small>"]

    root --> mdpi
    root --> hdpi
    root --> xhdpi
    root --> xxhdpi
    root --> xxxhdpi
    root --> anydpi
    root --> values
```

## Build Flow

```mermaid
graph TD
    buildSh["build.sh"]
    step1["1/2 — Compiles teavm.web via Maven<br/>Java → app.js + index.html"]
    step2["2/2 — Packages with Tauri via Cargo"]
    desktop[".app bundle (macOS)"]
    android["APK debug aarch64 + adb install"]
    ios[".app simulator aarch64-sim + xcrun install"]

    buildSh --> step1
    buildSh --> step2
    step2 --> desktop
    step2 --> android
    step2 --> ios
```
