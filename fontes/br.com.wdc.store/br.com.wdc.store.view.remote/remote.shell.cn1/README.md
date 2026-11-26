# remote.shell.cn1 — Codename One thin shell

**Codename One (CN1)** frontend for the WDC Store *remote shell* pattern. The
presenters and their scopes run on the **server** (`remote.host`); this module is a
**thin client** that speaks the *bridge* protocol over WebSocket and renders with
Codename One's native UI — **a single Java codebase** that becomes an app for **iOS,
Android, desktop (Mac/Windows/Linux) and web**.

It is one of the project's reference frontends (React, Flutter, Vaadin, SWT,
TeaVM and this one). They all share the same server-side presenters; only the
rendering layer changes.

---

## Architecture (summary)

- **Bridge protocol reimplemented** with CN1's own APIs (no `okhttp`/`gson`/
  `java.net.http`/reflection — the CN1 VM is restricted): `ConnectionRequest`
  (`GET /api/session/init`), `com.codename1.io.WebSocket`
  (`ws://host/dispatcher/{appId}`), `JSONParser`. See `bridge/BridgeSession`.
- **Channel crypto, remote protocol 1** (`bridge/Cn1Crypto`, `bridge/P256`, spec in
  `docs/bridge-skeleton-protocol.md` §6): `/api/session/init` gives only the `appId`. On every
  WebSocket open, the first and only message is `{"protocol":1,"clientKey":…}` (Base64
  SubjectPublicKeyInfo of an ephemeral P-256 key); nothing else goes out — no ping, no request —
  until the host answers `{"protocol":1,"serverKey":…}`. Both ends derive the connection key with
  HKDF-SHA256 (ECDH x-coordinate, salt = `appId`, info = `wdc-cube-remote/aes-256-gcm/v1`). Then the
  bootstrap message (`{"event":[]}` + storage), the requests that waited, and the 15 s keep-alive.
  Storage values and the password travel as `Base64(IV ‖ AES-256-GCM ciphertext ‖ tag)`, a random
  12-byte IV per value. A refusal (`{"error":"unsupported-protocol"}` / close `4002`) shows "out of
  date" on the splash, without retry or reconnection.
- **Own P-256** (`bridge/P256`): Codename One has AES-GCM (`Cipher`), `Hmac` and `SecureRandom`, but
  no ECDH or HKDF. `P256` does key generation, scalar multiplication (Jacobian coordinates,
  fixed 256-step Montgomery ladder), SPKI encoding/decoding with point validation and ECDH on CN1's
  `BigInteger`; HKDF is in `Cn1Crypto` on `Hmac`. CN1's `BigInteger.modInverse` gives wrong results
  for the P-256 prime, so inverses are `z^(p-2) mod p`.
- **Non-reactive (Vaadin style)**: each view is **one class** that builds its widgets
  **once** in `build()` and **mutates** them in `doUpdate()`. Nothing is rebuilt on push;
  a coalesced `markDirty` + `flush()` re-renders only what changed.
- **Construction DSL** (`util/Cn1Dom`): `render(layout, (dom, r) -> …)` with
  `boxX/boxY/border/container/label/spanLabel/button`. Convention: `setUIID` is the
  **first** action of the lambda (helpers such as `FontImage.setMaterialIcon` "bake" color/
  size from the style).
- **Slot mounting** (`widgets/Slot`): single-child container with a guard
  ("re-mounts only on transition"). The app root and the view-swap points use slots.
- **SCSS theme** (`common/src/main/scss` + `views/*/_*.scss`) → CSS → `theme.res`.
  **Density-independent** (`util/Px.mm` → `convertToPixels`); **responsive** by
  form factor (`app.isExpanded()` = tablet/desktop vs. compact on the phone), with a rebuild when
  crossing the breakpoint **or** rotating the screen.
- **Backend URL per target** (`ServerConfig`): automatically resolves the URL the
  emulator/simulator uses to reach the backend on the host (see the table under *Backend*).

### Why it does not reuse the shared core

Unlike the "fat" frontends (SWT/Vaadin/TeaVM), this shell does **not** depend on
`presentation`/`domain`/`persistence.client` — like the React and Flutter shells, it
reimplements the protocol with CN1 APIs, keeping the classpath light and compatible with
Codename One's restricted VM.

### Semi-detached project (outside the Maven reactor)

It is a complete Codename One project, with its **own parent and plugin**
(`codenameone-maven-plugin`). That is why it is **not** in the `<modules>` of
`view.remote/pom.xml` — the root `mvn clean package` does **not** build it. Build and
execution are independent (via `run.sh`/`build.sh`/`android.sh`/`mvnw`), like the
non-Java shells.

---

## Common prerequisite: backend up

Every way of running it needs the backend (server-side presenters) on port **8080**.
From the **repository root**:

```bash
./work/bin/start-server.sh        # backend, port 8080
./work/bin/start-h2-server.sh     # if the database is configured in TCP mode
```

### Backend URL per platform (`ServerConfig`)

| Target                               | URL used             | How to adjust |
|--------------------------------------|----------------------|--------------|
| iOS (simulator), desktop, web        | `http://localhost:8080` | automatic |
| Android (emulator)                   | `http://10.0.2.2:8080`  | automatic (host alias in the emulator) |
| Real device (iOS/Android)            | provide the URL         | `DEVICE_URL` in `ServerConfig` **or** `-Dcodename1.arg.serverUrl=https://…` |

---

## How to run on each platform

> Commands from **this folder** (`remote.shell.cn1`), except the backend (repo root).

### Local builds (no Codename One account)

| Platform              | Command                       | Requirements |
|-----------------------|-------------------------------|-----------|
| **CN1 simulator**     | `./run.sh simulator`          | JDK installed |
| **Desktop (run)**     | `./run.sh desktop`            | JDK installed |
| **Desktop (jar)**     | `./build.sh jar`              | produces an executable `javase/target/*.jar` |
| **Android (emulator)**| `./android.sh --gen`          | Android SDK + running emulator (see below) |
| **iOS (sim/device)**  | `./build.sh ios_source`       | macOS + Xcode (opens the project in `ios/target`) |

#### Android — `android.sh` (recommended)

`./build.sh android_source` generates a Gradle project, but CN1 **recreates it on every
generation** and wipes local environment tweaks. `android.sh` reapplies everything and runs:

```bash
./android.sh --gen          # regenerate + fix + installDebug + open in the emulator
./android.sh                # without regenerating: just fix, install and open
./android.sh --gen --patch  # regenerate + fix, without building → open/run in IntelliJ/Android Studio
```

Requirements: **Android SDK** with *cmdline-tools* and at least one **stable** platform
(e.g. `android-36`); **JDK 17** for Gradle (Gradle 8.1 does not run under JDK > 19; the
script enforces this); an **emulator (AVD) already running**. `android.sh` takes care of: pinning
`compileSdk/targetSdk` to the highest stable platform, pointing Gradle at JDK 17, and
`sdk.dir`. (Cleartext HTTP comes from a build hint.)

IDE flow: run `./android.sh --gen --patch`, **open the generated project** (`android/
target/*-android-source`) in the IDE and hit Run — do **not** regenerate from inside it.

#### iOS

```bash
./build.sh ios_source       # generates the Xcode project in ios/target
```
Open the `.xcodeproj`/workspace in **Xcode** and run it on the simulator or on the device.

### Builds on the Codename One server (require an account)

These targets **do not compile locally**: the code is sent to the **Codename One
cloud** to be compiled, and the artifact is sent back. Because of that, the first time, the command
**opens the browser for you to log in** to a Codename One account (the credentials
are stored; manage them with `./run.sh settings`). This is not an error — it is the normal flow of
a *cloud build*.

> **`*` = requires a Codename One account** (cloud build with browser login). The commands
> **without** `*` (previous section — simulator, desktop, `jar`, `android_source`, `ios_source`)
> are 100% local and do not ask for an account.

| Platform                  | Command                       |     |
|---------------------------|-------------------------------|-----|
| Android (APK/AAB)         | `./build.sh android`          | `*` |
| iOS (device)              | `./build.sh ios`              | `*` |
| iOS (App Store)           | `./build.sh ios_release`      | `*` |
| Web (JavaScript)          | `./build.sh javascript`       | `*` (Enterprise) |
| macOS desktop             | `./build.sh mac_desktop`      | `*` |
| macOS native (no JVM)     | `./build.sh mac_native`       | `*` |
| Windows desktop           | `./build.sh windows_desktop`  | `*` |
| Windows (UWP/device)      | `./build.sh windows_device`   | `*` |
| Linux native              | `./build.sh linux_device`     | `*` |

> **Web (JavaScript)** is the most particular case: unlike Android/iOS, it **has no
> local `_source` variant** (the Java→JS transpilation runs only on the build server) and requires the
> **Enterprise** plan. When it finishes, `./build.sh javascript` **deposits the web app in
> `work/frontend/remote.shell.cn1/`** (+ `context.html`), just like the TeaVM/React shells —
> the backend landing page lists the folder and links `/remote.shell.cn1/index.html`. For a
> **web frontend that does not depend on the CN1 cloud**, use the **TeaVM** shell (`view.teavm`).

See `./build.sh help` for the full list.

### Codename One settings

```bash
./run.sh settings    # opens the settings editor (codenameone_settings.properties)
./run.sh update      # updates the Codename One libs
```

---

## Structure

| Folder | Role |
|-------|-------|
| `common/` | App: lifecycle, bridge protocol, views, widgets, SCSS theme and `codenameone_settings.properties` |
| `javase/` | Desktop/**simulator** port (verifiable locally, without the build server) |
| `android/`, `ios/`, `javascript/`, `linux/` | Ports for the native targets |
| `build.sh` | Builds (local and server) per target |
| `run.sh` | Simulator/desktop/settings |
| `android.sh` | Local build/run on the Android emulator (fixes the generated project each time) |

### Packages in `common`

```
…shell.cn1
├─ StoreCn1RemoteApp   # Lifecycle: form, connection, re-sync on push, root slot, splash
├─ ServerConfig           # Backend URL per target
├─ Sel / *Sel             # UIIDs (theme selectors) per package
├─ bridge/                # BridgeSession, Cn1Crypto, P256, Cn1ClientStorage, AbstractCn1View, AbstractItemCn1View
├─ util/                  # Cn1Dom (DSL), Px, Guard, Money, Json, Images, Decor, …
├─ widgets/               # BackButton, CardHeader, IconButton, HtmlText, QtyStepper, Slot
└─ views/                 # login, home, product, cart, receipt (+ RootCn1View)
```

Entry class: `StoreCn1RemoteApp` (extends `com.codename1.system.Lifecycle`);
`mainName`/`packageName` in `common/codenameone_settings.properties`.

---

## Quick theme/code validation

```bash
# compiles common (Java + SCSS→CSS→theme.res); always use 'clean' (the IDE leaves stale .class files)
./mvnw -q -pl common -DskipTests clean process-classes
```

### Crypto tests

`common/src/test/java/…/bridge` has JUnit 5 tests that check `P256` and `Cn1Crypto` against the
JVM's own cryptography (JCA): ECDH both ways, SPKI read by each side, invalid points refused, HKDF on
RFC 5869 test case 1, AES-GCM values deciphered across. `Cn1CryptoTest` installs the JavaSE port
headless (no simulator) for `Cipher`, `SecureRandom` and `Base64`.

They run with `./mvnw test -pl common,javase`, from the `javase` module, which mounts
`common/src/test/java`: next to Codename One's own provider (which runs only `com.codename1.testing.UnitTest`
classes), its Surefire has the JUnit Platform provider.

> For the project's complete architectural overview, see the root README.md and the `docs/` folder.
