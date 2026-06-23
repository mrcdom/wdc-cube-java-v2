# Transação Remota — Identidade do Cliente e Posse da Transação

> Documento de **desenho** (parte implementado, parte proposta para evolução). Complementa a seção
> [Transações Remotas Dirigidas pelo Cliente](camada-de-dados.md#transações-remotas-dirigidas-pelo-cliente-sobre-rest)
> da camada de dados, aprofundando **quem é o dono de uma transação remota** e como esse dono é
> identificado com unicidade e confiança.

## Índice

- [Contexto — o incidente que originou a discussão](#contexto--o-incidente-que-originou-a-discussão)
- [Estado atual (implementado)](#estado-atual-implementado)
- [Evolução proposta — posse por contexto, não por usuário](#evolução-proposta--posse-por-contexto-não-por-usuário)
- [Generalização — qual é a unidade de posse](#generalização--qual-é-a-unidade-de-posse)
- [Unicidade dos identificadores](#unicidade-dos-identificadores)
- [Identificadores emitidos pelo servidor](#identificadores-emitidos-pelo-servidor)
- [Modelo de confiança — identificadores forjados](#modelo-de-confiança--identificadores-forjados)
- [Resumo das decisões e status](#resumo-das-decisões-e-status)

---

## Contexto — o incidente que originou a discussão

A transação remota dirigida pelo cliente torna **várias chamadas REST** atômicas: o cliente abre a
transação (`begin` → `txId`), propaga o `txId` no header **`X-Tx-Id`** nas escritas seguintes (que se
juntam à mesma transação física no servidor) e finaliza com `commit`/`rollback`.

O frontend **TeaVM** (`FetchHttpTransport`) não implementava `setTransactionIdSupplier` nem enviava o
header `X-Tx-Id`. O resultado, num checkout:

```
begin            ──────────▶ tx aberta no servidor (txId)          ✅ (begin não exige header)
insert(compra)   [sem header] ─▶ NÃO entra na tx → AUTOCOMMIT      ⚠️ grava órfão
commit           [sem header] ─▶ 403 "Cabeçalho X-Tx-Id ausente"   ❌
rollback         [sem header] ─▶ 403                               ❌
```

A compra **não** era confirmada na visão do cliente, mas a linha já tinha sido autocommitada — uma
**compra órfã** no banco. Quebra de atomicidade silenciosa: o pior tipo, sem erro visível ao usuário.

A correção (commit `9bca9322`) teve duas frentes:

1. **Propagação** — `FetchHttpTransport`/`OkHttpTransport` passam a anexar `X-Tx-Id` em toda
   requisição (espelhando o que o transporte JVM já fazia).
2. **Defesa server-side** — uma **guarda de atomicidade**: se chega uma escrita **sem** `X-Tx-Id` mas
   o solicitante **tem transação remota aberta**, o servidor **rejeita** (em vez de autocommitar um
   órfão). É a rede de proteção contra uma futura falha de propagação.

A guarda exige saber "**este dono tem transação aberta?**" — e é aí que entra a questão central deste
documento: **como o dono de uma transação é identificado.**

---

## Estado atual (implementado)

O dono (`ownerKey`) é derivado em `TxApiController.currentOwnerKey(Context)`:

| Situação | `ownerKey` |
|---|---|
| Autenticado | `user:<userId>` |
| Sem login (segurança desligada) | `anon:<X-Client-Id>` |

- O **`X-Client-Id`** é um identificador de sessão de cliente gerado **por instância da app** nos
  transports (`OkHttpTransport`: `UUID.randomUUID()` por instância; `FetchHttpTransport`/TeaVM:
  `crypto.randomUUID()` estático por carga de página). Enviado em toda requisição.
- Os namespaces `user:`/`anon:` separados evitam que um cliente anônimo colida com (ou se passe por)
  um usuário real; no modo autenticado o `X-Client-Id` é **ignorado** (não-spoofável).
- A guarda usa `RemoteTransactionCoordinator.hasOpenTransactionForOwner(ownerKey)`, implementada em
  **O(1)** com um índice `dono → contagem de transações abertas`, mantido em `begin`/`finish`/`reap`.

Esse estado já fecha o bug de atomicidade. O restante deste documento é **evolução proposta**.

---

## Evolução proposta — posse por contexto, não por usuário

### O problema residual

No modo autenticado a granularidade da guarda é **por usuário**. Isso embute um falso-positivo:

```
Aba A (João)  begin ─▶ tx aberta (dono user:João)
Aba B (João)  insert(perfil) [sem X-Tx-Id] ─▶ guarda vê "user:João tem tx aberta" → REJEITA ❌
```

A escrita single-shot **legítima** da aba B é barrada porque a guarda confunde **usuário** com
**fluxo de trabalho**. Raro no modelo atual (UI single-thread, checkout sequencial), mas é uma
armadilha de contrato.

### A proposta

Chavear a transação **sempre por contexto de cliente** (a aba), **não** pela identidade:

> O `ownerKey` deixa de ser uma identidade de segurança e vira um **rótulo de correlação puro**.

A chave da simplificação é que **autorização** e **propriedade da transação** são **ortogonais**:

- **Autorização** ("esta requisição pode fazer esta operação?") → `SecurityFilter` + `SecurityContext`,
  por requisição, independente. Vale autenticado ou não — sem login, o usuário só faz o que o contexto
  de segurança permitir.
- **Posse da transação** ("a qual transação lógica esta requisição pertence?") → o id do contexto (a aba).

Com isso, a guarda vira **por aba** automaticamente, e o falso-positivo das duas abas desaparece. O
`verifyOwner` continua válido, agora comparando ids de aba (uma aba não finaliza a transação de outra).

### Mudança concreta (pequena)

A infraestrutura de envio do id **já está commitada**; falta essencialmente a derivação no servidor:

```java
// hoje: user:<id> se autenticado, anon:<clientId> senão
// proposta: sempre o id da aba (correlação pura), independente de auth
var tabId = ctx.header(CLIENT_HEADER);
return (tabId != null && !tabId.isBlank()) ? tabId : null;
```

Do lado do cliente, **para o navegador já é praticamente por aba**: cada aba carrega seu próprio
*realm* JS / instância TeaVM, logo o id estático já é independente por aba. Ajustes opcionais:

- ler o id de `sessionStorage` (que é **por-aba**, ao contrário do `localStorage`) para sobreviver a
  um *reload* dentro da mesma aba;
- renomear `X-Client-Id` → `X-Tab-Id`/`X-Session-Id` (cosmético, deixa a semântica explícita).

---

## Generalização — qual é a unidade de posse

"Por aba" é só o caso comum de um princípio mais geral:

> A unidade de posse é **um contexto de execução single-thread dono das suas transações**.

| Contexto | Como obtém o id | Observações |
|---|---|---|
| **Worker server-side** (dentro do backend) | — | **Não usa** a tx remota: usa `ShoppingTransactions.BEAN.required(...)` (transação local JTA/JDBC). A máquina remota é só para clientes via REST. |
| **Aba do navegador** | id local por aba | Single-thread; *realm* próprio. |
| **Web Worker (browser)** | `self.crypto.randomUUID()` em memória | Single-threaded, *realm* separado → equivale a uma aba. **Não** tem `sessionStorage`/`localStorage` (Web Storage não é exposta a Workers); segura o id em memória. XHR/`fetch` funcionam em *dedicated workers*. |
| **Processo JVM independente** (CLI, importador) | UUID por instância do transport | Isolado por construção. |
| **Worker JVM multi-thread** (tx concorrentes) | id **por thread** | Id por processo fica grosso demais p/ a guarda (falso-positivo entre threads). Ver abaixo. |

### O caso multi-thread

Um worker com *thread pool* abrindo transações concorrentes tem o mesmo falso-positivo das "duas
abas", agora **entre threads do mesmo processo** (e frequentemente do mesmo usuário, onde o
namespacing por `userId` não desambigua nada). Dois pontos:

- O **caminho feliz já funciona** com id por processo: se cada thread usa `required()`, toda escrita
  carrega seu `X-Tx-Id` → caminho *resume*, a guarda nem é consultada. O problema só aparece ao
  **misturar** tx concorrente com escrita single-shot header-less concorrente.
- Para correção total, o id deve ser **por thread/escopo de tx**, alinhado ao `ThreadLocal CURRENT`
  que o `RestTransactionService` já usa para o estado da transação.

---

## Unicidade dos identificadores

### O espaço de ids não é o problema

UUIDv4 = **122 bits** aleatórios. Pelo limite do aniversário, seriam necessários **~26 trilhões** de
ids simultaneamente ativos para ~1 chance em 1 bilhão de colisão. Para qualquer carga realista,
colisão é irrelevante — **desde que o RNG seja forte**. O risco está nas bordas:

1. **RNG fraco no fallback.** O `generateClientId` do TeaVM cai em `Math.random()` se
   `crypto.randomUUID` não existir — e `crypto.randomUUID()` **exige secure context**; num deploy em
   **HTTP puro** (não-localhost) cairia no `Math.random()`, fraco.
   **Correção:** usar `crypto.getRandomValues()` (que **não** exige secure context e funciona em
   Workers) para montar o UUID, descendo a `Math.random()` só se `crypto` inexistir.
2. **Clonagem de processo vivo** (fork / container clonado após gerar o id) → dois processos com o
   mesmo id. Regra: gerar **lazy em runtime**, nunca assar no build.
3. **O servidor confia cegamente no id do cliente** — sem verificação de unicidade global.

### Chave composta

A variante robusta propôs largar o prefixo `user:`, mas ele era um **isolamento de colisão**. Dá para
ter os dois — **compor**:

```
ownerKey = "<userId>:<tabId>"   (autenticado)
ownerKey = "anon:<tabId>"       (sem login)
```

- **Granularidade por aba:** o `tabId` distingue as duas abas do mesmo usuário → resolve o falso-positivo.
- **Isolamento de colisão:** mesmo que dois usuários sorteassem o mesmo `tabId`, o `userId` os mantém
  distintos — o *blast radius* de uma colisão fica confinado a **um único usuário**.
- **Inadivinhabilidade:** o atacante precisaria do `userId` (talvez enumerável) **e** do `tabId` (o
  segredo aleatório).

> **Duas propriedades distintas, não confundir:** *unicidade* (dois contextos ativos não compartilham
> id → correção da correlação/guarda) e *inadivinhabilidade* (outro cliente não prevê seu id → defesa
> em profundidade do limite de transação). UUIDv4 forte cobre ambas; `Math.random` enfraquece ambas.

### Estratégia em dois níveis

O risco de colisão **escala com o número de ids** e **enfraquece com namespacing fraco** — então
tratamos os dois eixos de forma diferente:

- **Tier-1 (app/aba):** poucos ids por backend, ancorados no `userId`. **Geração local**, sem ida ao
  servidor. Probabilístico, suficiente.
- **Tier-2 (worker JVM multi-thread):** muitos ids, frequentemente do mesmo usuário. Aqui a autoridade
  do servidor compensa → **id emitido pelo servidor** (próxima seção).

---

## Identificadores emitidos pelo servidor

Para o tier-2, o id vem do servidor — **mas só vale a pena se for autoritativo**. Um
`UUID.randomUUID()` server-side **não compra nada** sobre a geração local (mesma probabilidade). A
fonte tem de ser **monotônica/autoritativa**.

### Esboço do endpoint

```
POST /api/client/id      (requer autenticação, mesmo filtro de /api/repo/*)
Request:  {}
Response: {"clientId": "<id autoritativo>"}
```

```java
public final class ClientIdApiController {

    // Eixo aleatório de BAIXA cardinalidade: um por processo (prefira config 'node.id' se houver).
    private static final String NODE_ID = resolveNodeId();
    // Eixo determinístico de ALTA cardinalidade: único dentro do processo.
    private static final AtomicLong SEQ = new AtomicLong();

    public static void configure(JavalinConfig config, String prefix) {
        config.routes.post(prefix + "/api/client/id", ClientIdApiController::issue);
    }

    private static void issue(Context ctx) {
        SecurityEnforcer.requireAuthenticated();
        ctx.json(Map.of("clientId", NODE_ID + "-" + SEQ.incrementAndGet()));
    }
}
```

A ideia central: **mover a suposição probabilística do eixo de alta cardinalidade (threads) para o de
baixa cardinalidade (processos)**. O `NODE_ID` é o único pedaço aleatório (um por processo de backend —
um punhado deles); o contador é determinístico.

- **Único no processo:** `AtomicLong` → impossível repetir.
- **Único entre processos/restarts:** `NODE_ID` diferente por processo → `nodeId-counter` nunca colide
  com outro processo, mesmo que os contadores reiniciem do zero.
- **Zero bookkeeping:** não há registry de ids emitidos para limpar — a não-reutilização é estrutural.

**Alternativa (autoridade global estrita):** `SEQ` vira uma **sequence do banco**, único entre nós sem
depender do `NODE_ID`, ao custo de um hit no banco por emissão.

### Reinício do backend

No restart, o `AtomicLong SEQ` **volta a zero** — então a unicidade depende de o **prefixo mudar a
cada boot**:

- **`NODE_ID` = UUID por processo** (caminho primário): prefixo novo a cada boot → `nodeId-0, …` nunca
  colidem com os ids do processo anterior, mesmo com o contador zerado. **Restart-safe de graça.**
- **`NODE_ID` = `node.id` configurado** (para identidade estável em cluster): prefixo **igual** após o
  restart + contador zerado → **reemitiria ids já entregues**. ⚠️ Para manter o `node.id` estável, é
  preciso um **componente por-boot** adicional: `clientId = <nodeId>-<bootId>-<seq>`, onde `bootId` é
  gerado a cada start (UUID curto, timestamp de boot, ou contador de boot persistido). A *sequence do
  banco* também resolve sozinha (a autoridade sobrevive ao restart).

> **Regra:** unicidade através do restart exige um componente **por-boot** no id. O UUID-por-processo
> já é esse componente; um `node.id` estável precisa de um `bootId` extra.

**Segredo de assinatura:** se o id for assinado (HMAC) ou virar claim de JWT, o `SERVER_SECRET` **não
pode** ser gerado por processo — senão todo id/token emitido antes do restart falha na verificação
(rejeição em massa). Tem de ser **estável** (config / secret store), como qualquer chave de JWT.

**Transações em voo:** o registry do coordenador é em memória — um restart **perde todas** as
transações abertas. O cliente recebe "tx desconhecida/expirada" no `commit` seguinte; é o **mesmo modo
de falha do *reaper*** (timeout ocioso), e o contrato do cliente da API de tx remota já deve prever
"minha tx sumiu → refaço o bloco". O restart é a versão "tudo de uma vez" disso. Não é recuperável na
prática (conexão/locks se foram).

### Integração no cliente — `ThreadLocal`

```java
private final String appClientId = UUID.randomUUID().toString();      // tier-1, por instância
private static final ThreadLocal<String> THREAD_CLIENT_ID = new ThreadLocal<>();  // tier-2

/** Chamado UMA vez no startup de uma thread worker dona de transações próprias. */
public void bindServerIssuedClientId() {
    THREAD_CLIENT_ID.set(parse(transport.postJson("/api/client/id", "{}")));
}

private String currentClientId() {              // valor enviado em X-Client-Id
    var t = THREAD_CLIENT_ID.get();
    return t != null ? t : appClientId;         // default: id da app, sem round-trip
}
```

O caminho comum (UI/aba, processo single-thread) nunca chama `bindServerIssuedClientId` → usa o id da
app, **zero ida ao servidor**. Cada worker faz **um** trip no seu startup, amortizado sobre todas as
transações que ele fizer.

### Cluster

- **Unicidade entre nós:** garantida pelo `NODE_ID` no prefixo (ou pela sequence do banco).
- **Roteamento:** uma transação remota vive (conexão aberta) **no nó que atendeu o `begin`** —
  `commit`/escritas precisam cair no mesmo nó → o deploy em cluster já exige **afinidade de sessão**
  (sticky) por `txId`. Isto é um *constraint* **pré-existente** da tx remota, não introduzido pelo
  endpoint de emissão. A afinidade real é por `txId`/sessão, não por `clientId`.

---

## Modelo de confiança — identificadores forjados

> **Pergunta:** o `X-Client-Id` é auto-afirmado pelo cliente. Um terceiro consegue forjar um id? Dá
> para detectar e descartar?

### No desenho stateless, não há detecção estrutural — e não precisa

O servidor não guarda "ids válidos", então não distingue um id legítimo de um inventado. O que protege
**não é o id — é o token**:

- A chave é **composta e amarrada ao token**: `userId:clientId`, onde o `userId` vem do
  `SecurityContext` (do JWT), **não** do que o cliente diz. Uma forja fica namespaced sob o **próprio
  `userId` do atacante**; ele não produz o `userId` de outra pessoa sem o token dela.
- **Hijack cross-user é bloqueado pelo token, não pelo id.** Para finalizar a tx da vítima, o atacante
  precisaria do `txId` (entregue só a quem abriu) **e** casar `vítimaUserId:vítimaClientId` — e o
  `userId` vem do token da vítima.
- Dentro do **mesmo** usuário, forjar id não cruza fronteira nenhuma (é o mesmo principal).

### A guarda é para cliente **cooperativo**, não é controle de segurança

Um cliente malicioso pode **evadir a guarda trocando de `clientId` a cada requisição** (abre sob X,
escreve header-less sob Y), ou simplesmente **não usar transação**. Em ambos os casos só corrompe a
**própria** transação; as escritas seguem autorizadas individualmente.

> Não dá para *forçar* atomicidade num cliente hostil via headers. Atomicidade garantida independente
> do cliente tem de morar **no servidor** — como o checkout já faz com `transactional()` /
> `CartManager.doPurchase`, que envolvem a transação server-side.

### Se você QUISER detecção real

Aceitar **só** ids emitidos pelo servidor exige tornar o id **verificável** — e a stack do projeto já
tem a ferramenta (HMAC + JWT). O id vira um **token assinado**:

```java
// emissão (servidor)
String payload  = userId + ":" + NODE_ID + "-" + SEQ.incrementAndGet();
String clientId = payload + "." + base64(hmacSha256(SERVER_SECRET, payload));

// verificação por requisição (stateless)
var i = clientId.lastIndexOf('.');
var payload = clientId.substring(0, i);
if (!constantTimeEquals(clientId.substring(i + 1), base64(hmac(SERVER_SECRET, payload))))
    throw new AccessDeniedException("X-Client-Id forjado");        // não foi o servidor que emitiu
if (!payload.startsWith(securityContext.userId() + ":"))
    throw new AccessDeniedException("X-Client-Id de outro dono");  // assinado, mas não é seu
```

- **Stateless:** verifica recomputando o MAC, sem registry. Detecta qualquer id fabricado (sem o
  segredo não há MAC válido) e qualquer reuso cross-user (o `userId` embutido não bate com o token).
- **Entrega ainda mais limpa:** assar essa claim **dentro do JWT no login** → verificação **grátis**
  (o servidor já valida o JWT em toda requisição) e **sem round-trip extra** (tier-1); emissão
  assinada via endpoint p/ tier-2. Custo: perde-se o caminho local sem-trip (a menos que venha no
  JWT) + gestão do segredo.

> **Distinção-chave:** o **contador** dá **unicidade**; só **assinatura/registry** dá **autenticidade**.
> São requisitos diferentes — escolha conforme o modelo de ameaça.

---

## Robustez operacional (implementada)

Revisão adversarial do desenho expôs furos de **ciclo de vida e operação** da transação (não da
identidade). Resolvidos no coordenador (`RemoteTransactionCoordinatorImpl`) e no decorador
`transactional`:

| # | Furo | Resolução |
|---|---|---|
| 1 | Sem teto de transações abertas → esgotamento do pool / DoS | Teto **global** (`maxOpen`) e **por dono** (`maxOpenPerOwner`) no `begin`; estouro → `TransactionLimitExceededException` → **429**. Bound *soft* (verificação não-atômica, aceitável). |
| 2 | Reaper só por ociosidade → cliente que "pinga" segura conexão pra sempre | Tempo de vida **absoluto** (`maxLifetimeMs`, 10 min) além do idle. |
| 3 | Sem observabilidade | Contadores (`begun`/`committed`/`rolledBack`/`reaped`/`rejectedByLimit`) + gauges via `stats()` → `RemoteTransactionStats`; logs nos eventos (reaper, rejeição). Sem dependência nova. |
| 4 | `commit`/`rollback` não-idempotentes → desfecho ambíguo se a resposta se perde | Desfecho lembrado por janela de retenção (5 min): retry com mesmo desfecho é **no-op de sucesso**; oposto → **409**. Reaper grava `rolledback` (desambigua). Novo **`GET /api/tx/status`** → `open`/`committed`/`rolledback`/`unknown`. |
| 5 | Guarda devolvia 403 (semântica de autorização) | **409 Conflict** via `TransactionConflictException` — conflito de estado, não negação de acesso. |

### Contrato do cliente — serialização dentro de uma transação (#7)

O coordenador **rejeita uso concorrente do mesmo `txId`** (`resume` faz CAS em `inUse` →
"transação remota em uso concorrente"). Logo, **as requisições de uma mesma transação devem ser
serializadas** pelo cliente — nada de pipelining/paralelismo dentro de um bloco `required`. Isto é
satisfeito naturalmente pelo modelo "uma thread por ação" (ver [re-entrância](#re-entrância-do-escopo-no-cliente-assíncrono-6)):
o bloco roda sequencialmente numa única thread.

### Política de transação anônima (#8)

**Decisão:** transação remota **exige autenticação** — `TxApiController.begin` chama
`SecurityEnforcer.requireAuthenticated()`. O namespace `anon:<clientId>` existe **apenas** para
deployments com a **camada de segurança inteira desligada** (testes/local), não para "usuário anônimo
num servidor seguro". Num servidor com segurança ativa, não há transação sem dono autenticado.

### Re-entrância do escopo no cliente assíncrono (#6)

**Investigado — não é bug.** No navegador (TeaVM, event-loop single-thread) o isolamento do
`ThreadLocal CURRENT` se sustenta porque **(1)** cada ação roda na sua própria green-thread
(`safeAction` → `new Thread().start()`) e **(2)** o `ThreadLocal` do TeaVM é **por-thread** (bytecode
de `TThreadLocal`: threads não-main usam `map` keyed por `currentThread().key`). Uma ação concorrente
durante um `required` suspenso lê seu próprio `CURRENT` (vazio) e **não** herda o `txId`. Invariante
documentada em `RestTransactionService` (refactor que rode ações inline ou reuse threads de pool
quebraria o isolamento).

---

## Resumo das decisões e status

| Item | Status |
|---|---|
| Propagação do `X-Tx-Id` no TeaVM + guarda de atomicidade + índice O(1) | ✅ Implementado (commit `9bca9322`) |
| Namespace de dono `user:`/`anon:` + envio de `X-Client-Id` | ✅ Implementado |
| Teto de tx abertas (global + por dono) → 429 | ✅ Implementado |
| Tempo de vida absoluto além do idle | ✅ Implementado |
| `commit`/`rollback` idempotentes + `GET /api/tx/status` | ✅ Implementado |
| Guarda devolve 409 (não 403) | ✅ Implementado |
| Observabilidade (`stats()` + logs) | ✅ Implementado |
| Re-entrância do `ThreadLocal` no cliente assíncrono | ✅ Investigado — não é bug (invariante documentada) |
| Contrato de serialização dentro da tx / política de begin anônimo | ✅ Documentado |
| Posse por aba (correlação pura, segurança ortogonal) | 🔭 Proposto |
| Chave composta `userId:tabId` | 🔭 Proposto (reconcilia granularidade + unicidade) |
| Endurecer fallback do `generateClientId` (`crypto.getRandomValues`) | 🔭 Proposto (melhoria isolada, vale já) |
| Id por thread (worker multi-thread) via `ThreadLocal` | 🔭 Proposto |
| Endpoint `POST /api/client/id` com fonte monotônica (tier-2) | 🔭 Proposto |
| Id assinado (HMAC/JWT) para detectar forja | 🔭 Opcional, só se "aceitar só ids emitidos" for requisito |

**Princípios que guiam o desenho:**

- Autorização (token) e posse de transação (id de contexto) são **ortogonais** — não sobrecarregar um
  com o papel do outro.
- A unidade de posse é um **contexto single-thread**; a aba é só o caso comum no navegador.
- Unicidade: tornar **determinístico o eixo que escala** (threads via contador) e deixar a
  aleatoriedade no **eixo que não escala** (processos).
- A guarda é **correção para cliente cooperativo**; atomicidade contra cliente hostil é server-side.
