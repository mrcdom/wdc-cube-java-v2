# WDC Framework — Persistence

Controle de transação **programático estilo CMT** (Container-Managed Transaction), com modo **dual JTA/JDBC** — implementação neutra de tecnologia: depende apenas de `javax.sql.DataSource` (JDBC) e `jakarta.transaction` (JTA), nunca de um pool ou TransactionManager concreto.

> Visão arquitetural completa: [docs/camada-de-dados.md → Transações](../../../docs/camada-de-dados.md#transações-atomicidade-e-modo-dual-jtajdbc).

## Dependências

| Artefato | Papel |
|----------|-------|
| `br.com.wdc.framework.domain` | Contrato `TransactionService` / `TransactionContext` e exceções |
| `br.com.wdc.framework.commons` | `SqlDataSource` (holder do `DataSource`) |
| `jakarta.transaction:jakarta.transaction-api` | Interfaces JTA (vendor-neutral) |

**Não depende** de Agroal, Narayana ou drivers XA — isso vive no host (ex.: `br.com.wdc.cube.backend`).

## Componentes

- **`TransactionServiceImpl`** — implementa o contrato `TransactionService` (em `framework.domain.transaction`) com as propagações `required` / `requiresNew` / `mandatory` / `supports` / `notSupported` / `never` (cada uma com a variante `…Call` que retorna valor). Commita em retorno normal; reverte em exceção (repropagada) ou `setRollbackOnly()`.
- **`TransactionScope`** — *frame* de transação ligado à thread (`ThreadLocal`), dual-mode:
  - **JDBC** (padrão): commit/rollback direto na `Connection`.
  - **JTA**: delega ao `TransactionManager`; a conexão, vinda de um `DataSource` JTA-aware, enlista como recurso XA.
  - Reentrância (`required`): participante compartilha a conexão do *owner*; só o *owner* finaliza.
- **`JtaTransactionManager`** — holder estático (`AtomicReference<jakarta.transaction.TransactionManager>`), populado pelo bootstrap JTA do host.

## Uso

Não há holder global: cada módulo expõe o seu (ex.: `ShoppingTransactions` em `shopping.domain`), populado pelo backend com uma instância ligada ao `DataSource` do módulo.

```java
// Registrado pelo bootstrap do módulo, ligado ao DataSource do módulo:
ShoppingTransactions.BEAN.set(new TransactionServiceImpl(() -> moduleDataSource));

// No caso de uso (camada de serviço / handler / presenter):
ShoppingTransactions.BEAN.get().required(tx -> {
    repositoryA.insert(x);
    repositoryB.update(y);   // commita junto, ou reverte tudo
});
```

Os repositórios não mudam: o `DSLContext` (jOOQ) é construído sobre o `TransactionAwareConnectionProvider` (em `framework.jooq`), que entrega a conexão do `TransactionScope` corrente quando há transação ativa.

## Modo JTA (XA)

Habilitado pelo host via `application.toml` (`database.transaction = "jta"`). O host inicializa o `TransactionManager` Narayana e configura o pool (Agroal) com integração XA; este módulo só consome as interfaces padrão. Em recurso único (apenas o banco), o commit é 1PC; com múltiplos recursos XA, 2PC.
