# teavm.commons

Shared library for the TeaVM views of the **WeDoCode Store** application. It contains the Virtual DOM engine, the Spectrum Web Components DSL, interop with browser APIs and utilities shared among the views compiled via **TeaVM**.

## Responsibility

Provide the rendering foundation and the communication protocol for the TeaVM shell. The application's concrete views (in `teavm.web`) build on this module to declare their virtual tree and react to ViewStates received from the host.

## Package Structure

```
br.com.wdc.store.view.teavm.commons/
├── DateUtils.java                     # Date formatting for display (TeaVM-safe)
├── devtools/
│   └── DevWatcher.java                # Live reload in development mode (asset watching)
└── interop/                           # JSO bridges for browser APIs
    ├── Console.java                   # console.log/error/warn
    ├── JsRunnable.java                # JS callback with no parameters
    ├── JsIntConsumer.java             # JS callback with an int parameter
    └── JsStringConsumer.java          # JS callback with a string parameter
```

> The VDom engine, the Spectrum DSL, `ViewStateCoordinator`, `ViewScope` and the WebSocket/WebCrypto interop live in `remote.bridge.teavm` (a generic framework, reusable by any TeaVM app over the remote protocol).

## Dependencies

- `br.com.wdc.store.presentation` — Scope classes (scope: provided; TeaVM does tree shaking)
- `org.teavm:teavm-jso` — JSO API (scope: provided)
- `org.teavm:teavm-jso-apis` — browser APIs (scope: provided)

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.store/br.com.wdc.store.view.teavm/teavm.commons -am
```
