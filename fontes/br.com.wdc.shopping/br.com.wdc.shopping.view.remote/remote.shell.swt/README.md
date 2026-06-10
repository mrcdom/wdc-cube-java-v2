# remote.shell.swt

Shell SWT desktop para o protocolo de apresentação remota do **WeDoCode Shopping**. Renderiza as views da aplicação usando widgets SWT nativos, comunicando-se com o `remote.host` via WebSocket — sem lógica de negócio local, sem banco de dados.

Reutiliza as implementações de views de `swt.commons`, sobrepondo-as com versões "remote" que obtêm o estado via `ViewStateSnapshot` (ao invés de Presenters locais).

## Arquitetura

```
remote.host  <──WebSocket──>  remote.shell.swt
                              ├── RemoteViewContext       (contexto compartilhado: HostClient, imagens)
                              ├── ShoppingSwtRemoteApp    (lifecycle: conecta, navega, gerencia views)
                              ├── ShoppingSwtRemoteMain   (entry point: SWT Display, event loop)
                              └── *ViewSwtRemote          (views SWT que consomem ViewStateSnapshot)
```

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `ShoppingSwtRemoteMain` | Entry point: inicializa SWT Display, cria Shell, inicia event loop |
| `ShoppingSwtRemoteApp` | Ciclo de vida da aplicação: conecta ao host, navega entre views conforme URI recebida |
| `RemoteViewContext` | Contexto compartilhado entre views: `HostClient`, cache de imagens, tema |
| `RemoteProductImageRepository` | Busca imagens de produto via HTTP a partir do endpoint do host |
| `RootViewSwtRemote` | View raiz: container principal, gerencia a view ativa |
| `LoginViewSwtRemote` | Tela de login — submete credenciais ao host via `HostClientSession.submit()` |
| `HomeViewSwtRemote` | Home: lista de produtos e histórico de compras |
| `ProductViewSwtRemote` | Detalhe do produto |
| `ProductsPanelViewSwtRemote` | Painel de listagem de produtos |
| `CartViewSwtRemote` | Carrinho de compras |
| `ReceiptViewSwtRemote` | Recibo de compra |
| `PurchasesPanelViewSwtRemote` | Histórico de compras |

## Dependências

- `swt.commons` — Widgets e componentes SWT compartilhados
- `remote.bridge.java` — `HostClient`, `ViewStateSnapshot`, protocolo WebSocket
- `br.com.wdc.shopping.presentation` — Classes ViewState
- `org.eclipse.platform:org.eclipse.swt.*` 3.128.0 (platform-specific via profile Maven)

## Pré-requisitos

- **Java 21**
- **Maven 3.9+**
- `remote.host` rodando (porta 8080 por padrão)

## Build e Execução

```bash
cd fontes && mvn clean package -pl br.com.wdc.shopping/br.com.wdc.shopping.view.remote/remote.shell.swt -am
java -jar target/remote.shell.swt-1.0.0.jar --endpoint=http://localhost:8080
```
