# teavm.commons

Biblioteca compartilhada para views TeaVM da aplicação **WeDoCode Shopping**. Contém o motor Virtual DOM, o DSL de componentes Spectrum Web Components, interop com APIs do browser e utilitários compartilhados entre as views compiladas via **TeaVM**.

## Responsabilidade

Fornecer a base de renderização e o protocolo de comunicação para o shell TeaVM. As views concretas da aplicação (em `teavm.web`) herdam deste módulo para declarar sua árvore virtual e reagir a ViewStates recebidos do host.

## Estrutura de Pacotes

```
br.com.wdc.shopping.view.teavm.commons/
├── DateUtils.java                     # Formatação de datas para exibição (TeaVM-safe)
├── devtools/
│   └── DevWatcher.java                # Live-reload em modo desenvolvimento (watch de assets)
└── interop/                           # JSO bridges para APIs do browser
    ├── Console.java                   # console.log/error/warn
    ├── JsRunnable.java                # Callback JS sem parâmetros
    ├── JsIntConsumer.java             # Callback JS com parâmetro int
    └── JsStringConsumer.java          # Callback JS com parâmetro string
```

> O motor VDom, o DSL Spectrum, `ViewStateCoordinator`, `ViewScope` e interop WebSocket/WebCrypto estão em `remote.bridge.teavm` (framework genérico, reutilizável por qualquer app TeaVM sobre o protocolo remoto).

## Dependências

- `br.com.wdc.shopping.presentation` — ViewState classes (scope: provided; TeaVM faz tree-shaking)
- `org.teavm:teavm-jso` — JSO API (scope: provided)
- `org.teavm:teavm-jso-apis` — APIs browser (scope: provided)

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.shopping/br.com.wdc.shopping.view.teavm/teavm.commons -am
```
