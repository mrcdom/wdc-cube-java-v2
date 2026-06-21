# remote.shell.cn1 — shell fino Codename One

Frontend **Codename One** do padrão *remote shell*: os presenters/ViewStates rodam no
servidor (`remote.host`); este módulo é um **cliente fino** que fala o protocolo bridge por
WebSocket e renderiza com a UI nativa do Codename One (iOS/Android/desktop/web a partir de
uma única base Java).

## Por que não reusa o core compartilhado

Diferente dos frontends "gordos" (Gluon/SWT/Vaadin), este shell **não** depende de
`presentation`/`domain`/`persistence.client`. Como os shells **React** e **Flutter** (que
falam o mesmo protocolo em JS/Dart), ele **reimplementa o protocolo bridge** com APIs do
próprio Codename One:

- `com.codename1.io.ConnectionRequest` — `GET /api/session/init`
- `com.codename1.io.WebSocket` — canal `ws://host/dispatcher/{appId}` (embutido no runtime CN1, sem cn1lib)
- `com.codename1.io.JSONParser` — mensagens JSON
- `com.codename1.ui.*` — renderização

Isso mantém o classpath leve e respeita a VM restrita do Codename One (sem `okhttp`, `gson`,
`java.net.http`, reflection ou `java.time` no caminho do cliente).

## Projeto semi-destacado (fora do reactor Maven)

É um projeto Codename One completo, com **parent e plugin próprios** (`codenameone-maven-plugin`).
Por isso **não** está listado em `view.remote/pom.xml` `<modules>` — o `mvn clean package` da
raiz **não** o constrói. Build e execução são independentes (via `run.sh`/`mvnw`), igual aos
shells não-Java.

## Como rodar

```bash
# 1. backend no ar (presenters server-side), porta 8080
./work/bin/start-server.sh

# 2. simulador Codename One (a partir desta pasta)
./run.sh simulator
```

## Estrutura

| Pasta | Papel |
|-------|-------|
| `common/` | Código do app (lifecycle + protocolo + views) e `codenameone_settings.properties` |
| `javase/` | Porta desktop/**simulador** (validável localmente, sem build server) |
| `android/`, `ios/`, `javascript/`, `linux/` | Alvos nativos — exigem conta/build-server do Codename One (fase futura) |

Classe de entrada: `ShoppingCn1RemoteApp` (`common`, estende `com.codename1.system.Lifecycle`).

## Status (em construção)

- **Fase 0b (atual)** — `ShoppingCn1RemoteApp` é o *probe do protocolo*: conecta no backend,
  faz o handshake mínimo (`secret` placeholder, sem cripto), recebe o push de estado e navega
  para `login`, exibindo o JSON recebido. Prova que o CN1 fala o bridge.
- **Próximas** — render das views a partir dos snapshots → submissão de eventos → handshake de
  cripto (login seguro, RSA+PBKDF2+AES-GCM) → demais telas → builds nativos.

> Dica: o archetype do Codename One inclui um skill `.claude/skills/codename-one` com
> referências de API/build. Foi removido deste commit para mantê-lo enxuto; pode ser
> re-adicionado se a equipe quiser o guia versionado no repo.
