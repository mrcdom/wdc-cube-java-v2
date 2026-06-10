# remote.host

Instância concreta do host do protocolo de apresentação remota para a aplicação **WeDoCode Shopping**. É o servidor WebSocket que recebe eventos dos shells (React, TeaVM, Flutter, SWT), despacha para os Presenters e devolve os ViewStates modificados.

## Responsabilidade

Fazer o wiring da aplicação Shopping com a infraestrutura `remote.backend` do framework: registra a `ShoppingApplicationImpl` no registry, configura o servidor Javalin e sobe o host na porta configurada.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `RemoteHostBootstrap` | Entry point: configura Javalin, registra rotas HTTP/WS via `remote.backend`, inicia o servidor |
| `ShoppingApplicationImpl` | `RemoteApplication` concreta: instancia as views/presenters da aplicação Shopping, wiring completo do grafo de objetos |

## Dependências

- `br.com.wdc.shopping.presentation` — Presenters e ViewStates da aplicação
- `br.com.wdc.shopping.persistence` — Repositórios e acesso a dados
- `remote.backend` — Infraestrutura do protocolo remoto (framework)
- `io.javalin:javalin` 7.2.0 — HTTP/WebSocket

## Execução

O host é iniciado pelo `br.com.wdc.shopping.backend` (que orquestra backend REST + host remoto na mesma JVM). Para executar apenas o host remoto isolado:

```bash
cd fontes && mvn clean package -pl br.com.wdc.shopping/br.com.wdc.shopping.view.remote/remote.host -am
java -cp target/... br.com.wdc.shopping.view.remote.host.RemoteHostBootstrap
```

A porta padrão é configurada em `work/config/application.toml`.

## Build

```bash
cd fontes && mvn clean install -pl br.com.wdc.shopping/br.com.wdc.shopping.view.remote/remote.host -am
```
