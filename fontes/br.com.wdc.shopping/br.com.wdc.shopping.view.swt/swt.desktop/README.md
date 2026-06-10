# swt.desktop

App desktop nativo completo do **WeDoCode Shopping** usando SWT — versão standalone com Presenters e persistência locais (sem servidor remoto).

Contém apenas o entry point e o wiring da aplicação. Toda a lógica de visualização está em `swt.commons`; toda a lógica de negócio está em `br.com.wdc.shopping.presentation` e `br.com.wdc.shopping.persistence`.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `ShoppingSwtMain` | Entry point: inicializa banco H2, instancia o `Display` SWT, cria o `Shell`, inicia o event loop |
| `ShoppingSwtApplication` | Implementa `SwtApp`: instancia e registra as views de `swt.commons`, conecta Presenters locais, gerencia o render loop (Timer 16ms → `flushDirtyViews`) e a pilha de navegação |

## Dependências

- `swt.commons` — views e componentes SWT compartilhados
- `br.com.wdc.shopping.persistence` — acesso a dados (H2 + JOOQ)
- `br.com.wdc.shopping.scripts` — scripts de inicialização (dados de exemplo)

## Pré-requisitos

- **Java 21**
- **Maven 3.9+**
- SWT selecionado automaticamente via profile Maven por plataforma (macOS aarch64, Linux x86_64, Windows x86_64)

## Build e Execução

```bash
cd fontes && mvn clean package -pl br.com.wdc.shopping/br.com.wdc.shopping.view.swt/swt.desktop -am
java -jar target/swt.desktop-1.0.0.jar
```
