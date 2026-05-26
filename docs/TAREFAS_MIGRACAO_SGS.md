# Levantamento de evolucoes do fork sgs

## Contexto
- Origem comparada: `/Users/mrcdom/Works/Basis/sgs_showcase/sgs-source/sgs`
- Destino: este repositorio (`wdc-cube-java-v2`)
- Regra aplicada: ignorar renomeacoes de pacote, naming e mudancas de dominio/exemplo.
- Foco: comportamento e logica (backend websocket + react skeleton + protocolo cliente).

## Escopo analisado
- Backend websocket e bootstrap HTTP:
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/DispatcherHandler.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/DispatcherHandler.java)
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/controller/DispatcherController.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/controller/DispatcherController.java)
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/controller/IndexHtmlController.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/controller/IndexHtmlController.java)
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/JavalinApplication.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/JavalinApplication.java)
- Core da app react no servidor:
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/viewimpl/ApplicationReactImpl.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/viewimpl/ApplicationReactImpl.java)
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/util/AppSecurity.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/util/AppSecurity.java)
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/util/DataSecurity.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/util/DataSecurity.java)
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/util/GenericViewImpl.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/util/GenericViewImpl.java)
- Protocolo do cliente react:
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.client/src/scripts/app/FlushRequestContext.ts](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.client/src/scripts/app/FlushRequestContext.ts)
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.client/src/scripts/app/Application.ts](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.client/src/scripts/app/Application.ts)
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.client/src/scripts/app/ViewGarbageCollector.ts](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.client/src/scripts/app/ViewGarbageCollector.ts)
  - [fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.client/src/scripts/app/types.ts](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.client/src/scripts/app/types.ts)

## Achados relevantes (o que o fork tem e aqui ainda nao)

## Decisao arquitetural (confirmada)
- Decisao aprovada: opcao 3 (hibrida).
- Modelo alvo para este projeto:
  - fila global de apps sujas + flush periodico em background (tick base de 50ms)
  - wake-up imediato para eventos criticos (ex.: submit do usuario, reconnect, full resend)
- Objetivo: combinar previsibilidade sob carga com baixa latencia percebida.

### 1) Registro central de apps e flush global de views sujas
No fork existe um registry global de sessoes com:
- fila lock-free de apps sujas
- flush periodico em background (50ms)
- expiracao periodica de sessoes (30s)

No projeto atual:
- cada app agenda push por instancia com delay fixo
- `removeExpireds()` existe, mas nao esta integrado a um ciclo global explicito no backend

Impacto:
- maior risco de latencia irregular de atualizacao de UI
- possibilidade de ciclo de vida inconsistente entre sessoes (flush/expiracao)

Direcao de migracao adotada para este item:
- implementar modo hibrido (tick + wake-up), nao apenas tick puro.
- manter coalescencia por app para evitar duplicacao de enfileiramento.

### 2) Handshake/reconexao websocket mais robusto
No fork, o fluxo de websocket trata explicitamente:
- reconexao com `requestId` regressivo
- flag para full resend de estado em nova sessao WS
- guardas para fechar sessao em falha criptografica (ex.: AEADBadTag)

No projeto atual:
- ha validacoes boas de sessao, mas sem o mesmo nivel de tratamento para crypto-failure/reload obrigatorio
- full resend na reconexao nao esta explicitamente garantido no mesmo padrao

Impacto:
- risco de cliente ficar com estado invalido apos reconnect
- risco de loop de erros criptograficos sem recuperacao limpa

### 3) Sincronizacao de navegacao (hash/path) com assinatura mais defensiva
No fork:
- servidor observa `path` recebido no request/ping
- valida assinatura e evita reset indevido em paths triviais (`/`)
- controla reenvio de URI quando assinatura invalida

No atual:
- o cliente envia `path` no ping, mas o servidor nao usa este caminho no ciclo principal de `sendResponse()`
- assinatura/historico seguem outro fluxo (`sign`) e sem toda a logica de reconnect/hash sync do fork

Impacto:
- risco de descompasso URL x estado interno da app
- navegacao pode ficar dependente de eventos especificos e nao do estado real da conexao

### 4) Protocolo de GC de views entre servidor e cliente
No fork:
- resposta pode incluir `releasedViews` e `activeViews`
- cliente usa esses campos para limpeza/sweep deterministico de scopes

No atual:
- existe coletor local ([ViewGarbageCollector.ts](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.client/src/scripts/app/ViewGarbageCollector.ts)), mas sem contrato completo com servidor
- resposta servidor atual nao envia `releasedViews`/`activeViews`

Impacto:
- maior risco de acumulo de scopes obsoletos em sessoes longas
- limpeza menos previsivel em cenarios de reconnect e churn de views

### 5) Serializacao/commit de estado por view no ciclo de resposta
No fork, antes de serializar estado sujo ha commit por view no ciclo de resposta/flush.
No atual, o commit acontece em nivel de `presenterMap` global.

Impacto:
- possivel diferenca de timing de estado computado
- risco de enviar state intermediario em casos com dependencia entre views

### 6) Hardening de cookies de bootstrap
No fork, cookies de bootstrap sao emitidos com politicas mais restritivas (ex.: secure) e janela de validade mais coerente com login/reload.
No atual, bootstrap existe, mas com politica de cookie diferente.

Impacto:
- menor robustez de sessao em ambientes HTTPS/proxy
- chance maior de condicoes de corrida no primeiro handshake

### 7) Inicializacao/configuracao de AppSecurity
No fork, AppSecurity e inicializado de forma centralizada no bootstrap com suporte a chaves configuraveis e ciclo de vida controlado.
No atual, AppSecurity esta como enum singleton com fallback hardcoded via propriedades.

Impacto:
- menor flexibilidade operacional para rotacao/configuracao de chaves
- maior acoplamento de seguranca ao codigo

## Backlog proposto (ordem recomendada)

### P0 - Critico (consistencia de sessao e estado)
1. Criar `ApplicationRegistry` no modulo react skeleton para centralizar:
   - map de instancias
   - fila global de apps sujas
  - tarefas periodicas de flush e expiracao
  - sinalizador de wake-up imediato para eventos criticos
2. Migrar `ApplicationReactImpl` para o modelo hibrido (dirty queue global + wake-up + flush lock-safe).
3. Implementar full resend em reconnect websocket:
   - detectar regressao de `requestId`
   - marcar necessidade de reenvio integral (views + uri)
4. Tratar falhas criptograficas no websocket com close code de reload obrigatorio.

### P0.1 - Sequenciamento da opcao 3 (hibrida)
1. Fase A - Introduzir Registry e fila global mantendo compatibilidade com push atual.
2. Fase B - Rotear `markDirty` para fila global e ativar tick de 50ms.
3. Fase C - Adicionar wake-up imediato para eventos interativos e reconnect.
4. Fase D - Remover agendamento por instancia antigo quando os testes de regressao estiverem verdes.

## Contrato de concorrencia (Fase A/B da opcao 3)

### Objetivo
Definir regras de exclusao e ordenacao entre:
- thread de request/resposta WebSocket
- thread periodica de flush global
- eventos de reconnect/close websocket

### Mapa atual (pontos sensiveis)
- Agendamento por instancia em [ApplicationReactImpl.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/viewimpl/ApplicationReactImpl.java#L368).
- Marcacao de dirty em [ApplicationReactImpl.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/viewimpl/ApplicationReactImpl.java#L322).
- Ciclo WS em [DispatcherHandler.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/DispatcherHandler.java#L148).
- Infra de scheduler central em [BusinessContext.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/BusinessContext.java#L45).

### Invariantes obrigatorios
1. Exclusao de envio por app:
  - nunca enviar dois frames concorrentes para a mesma app/sessao.
2. Sem perda de dirty:
  - se uma view for marcada dirty durante flush/request, deve aparecer no proximo ciclo.
3. Coalescencia por app:
  - uma app dirty entra uma vez na fila global ate ser processada.
4. Sem flush quando indisponivel:
  - nao flushar quando `wsSession == null`.
5. Request tem precedencia logica:
  - enquanto request estiver em progresso, flush de background nao deve drenar estados da mesma app.

### Modelo de flags sugerido por app
- `dirtyQueued` (AtomicBoolean): evita enqueue duplicado.
- `inResponse` (volatile): guarda de exclusao entre flush de background e `sendResponse`.
- `needsFullResend` (volatile): sinaliza full resend apos reconnect/rollback de requestId.
- `sendLock` (ReentrantLock): serializa escrita no socket por app.

### Protocolo de execucao (A/B)
1. `markDirty(view)`:
  - adiciona em `dirtyViewMap`.
  - se `dirtyQueued` mudar `false -> true`, enfileira app no `ApplicationRegistry.dirtyQueue`.
2. Tick global (50ms):
  - drena fila lock-free de apps.
  - para cada app: reseta `dirtyQueued=false`, tenta `flushDirtyViews()`.
3. `flushDirtyViews()`:
  - aborta se `inResponse==true`, `wsSession==null` ou sem dirty.
  - drena snapshot de dirty (removendo do mapa apenas o que entrou no snapshot).
  - envia via `sendLock`.
4. `sendResponse(request)`:
  - seta `inResponse=true` no inicio e `false` no finally.
  - processa dispatch + response.
  - se detectar reconnect (`requestId` regressivo ou troca de ws), marca full resend.

### Wake-up imediato (preparacao da Fase C)
Na Fase A/B, manter gancho pronto para wake-up (sem desligar tick):
- submit de usuario (evento real, nao ping)
- reconnect websocket
- alteracao de URI/historico

### Criterios de aceite da Fase A/B
1. Nao existe envio concorrente para mesma sessao em teste de stress.
2. Nenhuma view dirty e perdida sob carga de eventos (burst).
3. Reconnect rapido nao gera estado zumbi nem quebra de sessao.
4. Sem regressao funcional no protocolo atual (`requestId`, `ping`, `states`, `uri`).
5. Expiracao periodica continua removendo sessoes desconectadas.

### Riscos e mitigacoes
- Risco: starvation de app com muito churn.
  - Mitigacao: re-enfileirar app ao final se ainda houver dirty apos flush.
- Risco: close websocket concorrente ao envio.
  - Mitigacao: validar `wsSession` imediatamente antes do send e capturar erro de envio sem derrubar scheduler.
- Risco: dupla drenagem (request + background).
  - Mitigacao: `inResponse` como guarda obrigatoria em flush de background.

### Capacidade do scheduler (nota de tuning)
Estado atual observado:
- O backend configura `ScheduledExecutor.BEAN` com `newScheduledThreadPool(1, ...)` em [BusinessContext.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/BusinessContext.java#L95).
- O adapter suporta `execute(...)` imediato, `schedule(...)` e `scheduleAtFixedRate(...)` em [ScheduledExecutorAdapter.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.persistence/src/main/java/br/com/wdc/shopping/persistence/concurrent/ScheduledExecutorAdapter.java#L16).

Implicacao para opcao 3:
- Com apenas 1 worker, tick periodico, wake-up imediato e tarefas de expiracao competem pela mesma fila de execucao.

Recomendacao para Fase B/C:
1. Iniciar com 1 worker para manter previsibilidade e simplificar observabilidade.
2. Instrumentar tempo de execucao do flush e backlog da fila global.
3. Subir para 2 workers se houver atraso sistematico do tick (drift) ou fila acumulando sob carga.

## Checklist de execucao - Fase A (sem portar logica ainda)

Objetivo da Fase A:
- Introduzir estrutura de registry/fila global e ciclo de vida no backend mantendo comportamento funcional atual.
- Nao desligar ainda o push por instancia; apenas preparar convivio controlado.

### 1) Arquitetura e fronteiras
1. Revisar responsabilidades atuais em [ApplicationReactImpl.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/viewimpl/ApplicationReactImpl.java).
2. Definir fronteira: registry gerencia instancias/expiracao/flush; app gerencia estado da sessao e serializacao de resposta.
3. Definir API minima do registry (get, getOrCreate, remove, init, shutdown, markDirty).
4. Critério de pronto: contrato de API fechado e sem ambiguidades de ownership.

### 2) Ciclo de vida no backend
1. Mapear pontos de startup/shutdown em [BusinessContext.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/BusinessContext.java) e [JavalinApplication.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/JavalinApplication.java).
2. Definir em qual ponto inicializar o registry (apos scheduler estar pronto).
3. Definir em qual ponto parar o registry (antes de zerar BEANs de infraestrutura).
4. Critério de pronto: ordem de init/stop definida com garantia de idempotencia.

### 3) Integracao com WebSocket handler
1. Revisar fluxo de criacao/uso/remocao de app em [DispatcherHandler.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/DispatcherHandler.java).
2. Definir transicao de lookup/criacao para o registry sem alterar protocolo WS atual.
3. Definir limpeza no close/error para evitar handler vivo sem app e app viva sem handler.
4. Critério de pronto: fluxo onConnect/onMessage/onClose/onError mapeado com estados validos.

### 4) Coexistencia com push por instancia (modo compatibilidade)
1. Manter `schedulePush/executePush` ativo em [ApplicationReactImpl.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/viewimpl/ApplicationReactImpl.java#L368).
2. Introduzir modo dual controlado por feature flag de backend (default: compatibilidade).
3. Definir regra de precedencia temporaria para evitar dupla drenagem (preferencia request path + guardas de inResponse).
4. Critério de pronto: nenhuma mudanca de payload/resposta observavel no cliente.

### 5) Estrategia de observabilidade minima
1. Adicionar logs de ciclo de vida (init/stop registry, enqueue/dequeue app, flush skip reason).
2. Adicionar contadores simples: apps registradas, apps enfileiradas, flush executados, flush ignorados.
3. Definir formato de log com `appId` e `requestId` quando aplicavel.
4. Critério de pronto: capacidade de diagnosticar race e backlog por log sem debugger.

### 6) Testes de seguranca de mudanca (gates da Fase A)
1. Smoke websocket: conectar, enviar evento, receber `states`, fechar, reconectar.
2. Regressao de protocolo: manter comportamento de `requestId`, `ping`, `uri`, `states`.
3. Expiracao: sessao desconectada deve ser removida no ciclo periodico.
4. Critério de pronto: gates verdes sem alteracao funcional percebida na UI.

### 7) Riscos especificos da Fase A
1. Inicializacao em ordem errada (registry antes do scheduler).
2. Dupla origem de verdade de instance map (app local x registry).
3. Close websocket concorrente com callback periodico.
4. Mitigacao: registrar ownership unico do mapa, e tornar teardown defensivo e idempotente.

### 8) Definition of Done - Fase A
1. Registry inicializa e finaliza de forma previsivel no ciclo do backend.
2. Lookup/criacao/remocao de app passa por um unico ponto de orquestracao.
3. Push por instancia continua funcional (sem regressao) enquanto o modo hibrido pleno nao entra.
4. Logs/metricas basicas permitem validar saude da fila global.
5. Documento de design atualizado e aprovado para iniciar Fase B.

## Checklist de execucao - Fase B (troca efetiva de markDirty para fila global)

Objetivo da Fase B:
- Fazer a transicao real do disparo de dirty para o registry global.
- Ativar tick de 50ms como caminho principal de flush.
- Preservar corretude funcional e evitar perda de estado sob concorrencia.

### 1) Alteracao de fluxo no app
1. Atualizar [ApplicationReactImpl.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/viewimpl/ApplicationReactImpl.java) para que `markDirty(...)` enfileire via registry global.
2. Introduzir flag de coalescencia por app (`dirtyQueued`) para impedir enqueue duplicado.
3. Preparar `flushDirtyViews()` dedicado (snapshot + envio), sem depender de timer local por instancia.
4. Critério de pronto: dirty passa a ser conduzido pelo registry, sem alterar payload enviado ao cliente.

### 2) Registry como orquestrador principal
1. Garantir no registry global:
  - drenagem da fila lock-free
  - reset de `dirtyQueued` antes da tentativa de flush
  - captura de erro por app sem interromper ciclo global
2. Implementar regra de re-enqueue quando app ainda estiver dirty apos flush.
3. Critério de pronto: ciclo periodico processa N apps sem starvation visivel.

### 3) Exclusao entre request e flush de background
1. Consolidar guarda `inResponse` na app para bloquear drenagem concorrente durante `sendResponse(...)`.
2. Garantir que `sendResponse(...)` mantenha semantica atual de dispatch + response.
3. Definir claramente a ordem:
  - request thread tem precedencia logica
  - background flush atua como propagacao assíncrona de dirty acumulado
4. Critério de pronto: ausencia de dupla drenagem observada em log/teste.

### 4) Persistencia temporaria do caminho legado (rollback seguro)
1. Manter caminho antigo encapsulado por feature flag (on/off) durante validacao.
2. Planejar rollback rapido para modo por instancia se gate critico falhar.
3. Critério de pronto: chave de rollback documentada e testada em ambiente local.

### 5) Testes-gate da Fase B
1. Concorrencia:
  - burst de submits simultaneos
  - verificar ausencia de estado perdido em `states`
2. Reconnect:
  - reconnect rapido sem ficar com view stale
3. Throughput:
  - multiplas sessoes com dirty concorrente
  - monitorar atraso do tick de 50ms
4. Protocolo:
  - manter `requestId`, `ping`, `uri`, `states` sem regressao
5. Critério de pronto: todos os gates passam com modo global ativado.

### 6) Observabilidade obrigatoria da Fase B
1. Incluir contadores de runtime:
  - enqueue_total
  - dequeue_total
  - flush_success_total
  - flush_skip_in_response_total
  - flush_skip_no_ws_total
2. Incluir medicao de latencia:
  - tempo de fila por app (enqueue -> flush)
  - duracao de flush por app
3. Critério de pronto: evidência objetiva de que o modo global esta estavel.

### 7) Riscos especificos da Fase B e mitigacao
1. Risco: regressao de latencia perceptivel em interacao do usuario.
  - Mitigacao: manter trilho para wake-up imediato na Fase C.
2. Risco: backlog com scheduler saturado.
  - Mitigacao: ajustar pool para 2 workers se houver drift sistematico.
3. Risco: app sem ws sendo re-enfileirada continuamente.
  - Mitigacao: short-circuit no flush + limpeza por expiracao.

### 8) Definition of Done - Fase B
1. `markDirty` usa registry global como caminho principal.
2. Tick periodico de 50ms esta ativo e estavel sob carga normal.
3. Nao ha perda de dirty nem envio concorrente por app.
4. Regressao funcional zero no protocolo websocket atual.
5. Equipe valida prontidao para iniciar Fase C (wake-up imediato).

## Checklist de execucao - Fase C (wake-up imediato sobre o tick)

Objetivo da Fase C:
- Manter tick de 50ms como base de estabilidade.
- Reduzir latencia percebida em eventos interativos com wake-up imediato.
- Evitar tempestade de execucoes com coalescencia e rate limit por app.

### 1) Eventos que devem disparar wake-up
1. Submit de usuario (eventos de interacao, nao ping).
2. Reconnect websocket com sessao valida.
3. Full resend necessario (ex.: regressao de `requestId` / troca de canal WS).
4. Mudanca relevante de URI/historico quando houver dirty pendente.
5. Criterio de pronto: lista de gatilhos implementada e logada com motivo.

### 2) API de wake-up no registry
1. Introduzir operacao explicita no registry: `triggerImmediateFlush(appId)`.
2. Implementar sem bypass de guardas de concorrencia da app (`inResponse`, `wsSession`, `sendLock`).
3. Reusar fila global e coalescencia (evitar criar canal paralelo de flush).
4. Criterio de pronto: wake-up reaproveita a mesma infraestrutura do tick.

### 3) Controle de explosao (storm protection)
1. Debounce curto por app para wake-up consecutivo (janela pequena, ex.: 5-20ms).
2. Limite de wake-up por app por segundo (soft cap com fallback para tick).
3. Garantir que tick sempre consegue drenar fila mesmo sob wake-up intenso.
4. Criterio de pronto: sem starvation do tick global sob burst de eventos.

### 4) Integracao com fluxo WS atual
1. Acoplar wake-up nos pontos corretos de [DispatcherHandler.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/DispatcherHandler.java).
2. Nao alterar contrato de mensagem com cliente nesta fase (`requestId`, `ping`, `states`, `uri`).
3. Preservar caminho de erro/close atual para sessao invalida.
4. Criterio de pronto: sem mudanca de protocolo no cliente React.

### 5) Testes-gate da Fase C
1. Latencia interativa:
  - medir tempo submit -> frame com `states` antes/depois do wake-up.
2. Estabilidade:
  - manter teste de longa duracao (30+ min) sem crescimento anormal de fila.
3. Concorrencia:
  - reconexao + submits concorrentes sem perda de estado.
4. Regressao:
  - todos os gates da Fase B continuam verdes.
5. Criterio de pronto: ganho mensuravel de latencia sem degradar estabilidade.

### 6) Observabilidade obrigatoria da Fase C
1. Contadores adicionais:
  - immediate_wakeup_total
  - immediate_wakeup_dropped_total (por debounce/rate limit)
  - immediate_wakeup_success_total
2. Metricas de efetividade:
  - p50/p95 de latencia submit -> flush
  - proporcao wake-up bem sucedido vs fallback tick
3. Criterio de pronto: dashboard/log suficiente para decisao de rollout.

### 7) Rollout seguro
1. Feature flag separada para wake-up imediato (independente da fila global).
2. Rollout progressivo:
  - local/dev -> homologacao -> producao parcial -> producao total.
3. Critérios de rollback imediato:
  - aumento de erro WS
  - backlog crescente persistente
  - piora de p95 de latencia ou CPU anormal
4. Criterio de pronto: plano de rollback testado e documentado.

### 8) Definition of Done - Fase C
1. Wake-up imediato ativo para gatilhos definidos.
2. Tick de 50ms permanece como rede de seguranca funcional.
3. Sem regressao de protocolo e sem perda de estado sob concorrencia.
4. Melhoria de latencia interativa comprovada por metrica.
5. Sistema apto para Fase D (retirada completa do legado por instancia).

## Go/No-Go para producao (apos Fase C)

Go:
1. Todos os gates de Fase B e C aprovados.
2. Erros WS estaveis ou menores que baseline.
3. p95 de latencia interativa igual ou melhor que baseline.
4. Backlog da fila global sem crescimento sustentado.

No-Go:
1. Evidencia de perda de dirty/state ou envio concorrente por app.
2. Crescimento de fila sem recuperacao em carga normal.
3. Reconnect instavel com sessao zumbi.
4. Regressao funcional no cliente (navegacao/atualizacao de views).

## Checklist de execucao - Fase D (retirada do legado por instancia e fechamento)

Objetivo da Fase D:
- Remover definitivamente o caminho antigo de push por instancia.
- Consolidar o modelo hibrido como unico caminho suportado.
- Encerrar a migracao com codigo limpo, observavel e operavel.

### 1) Remocao do caminho legado
1. Remover de [ApplicationReactImpl.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/viewimpl/ApplicationReactImpl.java):
  - `schedulePush(...)`
  - `executePush(...)`
  - estado associado ao timer legado por instancia
2. Garantir que `markDirty(...)` e wake-up usem apenas registry global.
3. Criterio de pronto: nao existe mais agendamento de flush por instancia no modulo react skeleton.

### 2) Limpeza de feature flags temporarias
1. Remover flags de compatibilidade criadas nas fases A/B/C.
2. Manter apenas flags permanentes de operacao (se justificadas).
3. Atualizar defaults para caminho unico (global + wake-up).
4. Criterio de pronto: matriz de configuracao simplificada e sem caminhos mortos.

### 3) Hardening final de concorrencia
1. Revisar invariantes no codigo final:
  - sem envio concorrente por app
  - sem perda de dirty
  - coalescencia por app preservada
2. Revisar guardas de shutdown para impedir flush apos teardown de infra.
3. Revisar tratamento de excecao em loop global para nao interromper scheduler.
4. Criterio de pronto: invariantes validados por testes e logs de execucao.

### 4) Performance e capacidade
1. Reavaliar pool de scheduler (1 vs 2 workers) com base em metrica real.
2. Consolidar limites de wake-up (debounce/rate limit) com valores operacionais definitivos.
3. Registrar baseline final de p50/p95 de latencia e backlog maximo aceitavel.
4. Criterio de pronto: capacidade documentada para carga alvo do ambiente.

### 5) Testes finais de regressao
1. Suite completa de concorrencia/reconnect/longa duracao.
2. Teste de resiliencia com fechamento de WS durante flush.
3. Teste de stress de multiplas sessoes com churn alto de views.
4. Criterio de pronto: nenhum gate critico falhando apos remocao do legado.

### 6) Operacao e suporte
1. Atualizar runbook de diagnostico com:
  - sintomas comuns
  - sinais de backlog
  - procedimento de mitigacao
2. Atualizar guia de observabilidade (metricas e logs obrigatorios).
3. Atualizar documentacao de arquitetura para refletir modelo final.
4. Criterio de pronto: time de operacao consegue diagnosticar incidentes sem conhecimento implícito.

### 7) Definition of Done - Fase D
1. Caminho legado por instancia removido do codigo.
2. Modelo hibrido e unico caminho de flush em producao.
3. Metricas de latencia/estabilidade dentro dos limites acordados.
4. Documentacao e runbook finalizados.
5. Programa de migracao encerrado com aceite tecnico.

## Encerramento do programa de migracao

Checklist final de aceite:
1. Todas as fases (A, B, C, D) com DoD cumprido.
2. Sem regressao funcional do cliente React no protocolo WS.
3. Sem regressao de seguranca nas validacoes de sessao/crypto.
4. Plano de continuidade definido para evolucoes futuras (sem reintroduzir legado).

### P1 - Alto (protocolo cliente-servidor)
5. Alinhar sincronizacao de navegacao:
   - consumir `path` no servidor durante o ciclo de request/ping
   - consolidar validacao de assinatura da URI no mesmo padrao do fork
6. Estender protocolo de resposta com `releasedViews` e `activeViews`.
7. Adaptar cliente react para usar contrato completo de GC coordenado com servidor (release/sweep).

### P1 - Alto (seguranca operacional)
8. Refatorar inicializacao de [AppSecurity.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.react/react.skeleton/src/main/java/br/com/wdc/shopping/view/react/skeleton/util/AppSecurity.java) para bootstrap central (sem hardcode de chave em codigo).
9. Revisar emissao de cookies em [IndexHtmlController.java](fontes/br.com.wdc.shopping/br.com.wdc.shopping.backend/src/main/java/br/com/wdc/shopping/backend/controller/IndexHtmlController.java): `Secure`, validade, consistencia com fluxo de handshake.

### P2 - Medio (coerencia de estado)
10. Revisar estrategia de `commitComputedState` no response loop para reduzir diferencas de timing por view.
11. Ajustar pontos de lock/sincronizacao no dispatch/response para evitar race entre request e flush de background.

### P3 - Baixo (hardening e observabilidade)
12. Padronizar logs de reconnect, stale close e cleanup de sessao com IDs correlacionaveis.
13. Adicionar contadores/metricas de:
    - tamanho da fila de dirty apps
    - tempo medio de flush
    - numero de reconnects com full resend

## Plano de validacao (quando comecar a portar)
1. Teste manual de reconnect rapido (F5, queda de rede, reopen de aba).
2. Teste de sessao invalida (cookie/assinatura alterada) com close code esperado.
3. Teste de navegacao por hash/path (deep-link, back/forward, refresh).
4. Teste de stress de eventos (burst de submits) para confirmar ausencia de perda de estado.
5. Teste de longa duracao (30+ min) para validar GC de views e expiracao de sessoes.

## Fora de escopo deste backlog
- Renomeacao de pacotes/classes/modulos.
- Mudancas de dominio da app exemplo (SGS vs Shopping).
- Features funcionais especificas do dominio SGS (telas/modulos nao existentes no Shopping).

## Observacao final
Documento inicialmente criado para planejamento e evoluido com execucao da Fase A.

Implementado na Fase A:
1. Registry global de aplicacoes react com init/shutdown no ciclo do backend.
2. Delegacao de lookup/criacao/remocao/expiracao para o registry.
3. Modo de compatibilidade preservado:
  - push por instancia continua sendo o caminho principal por padrao.
  - flush global periodico permanece desativado por default e pode ser habilitado via propriedade de sistema.
