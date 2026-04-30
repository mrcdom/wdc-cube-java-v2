# br.com.wdc.shopping.scripts

Módulo responsável pelos **scripts de banco de dados** da aplicação Shopping. Contempla tanto a criação do schema a partir do zero (bootstrap) quanto a migração incremental de uma versão anterior para o estado atual.

## Propósito

| Cenário | Descrição |
|---------|-----------|
| **Bootstrap** | Cria todas as tabelas, sequências e dados iniciais em um banco vazio. |
| **Migração** | Evolui um banco existente de uma versão anterior para a versão corrente, aplicando alterações estruturais e de dados de forma ordenada. |

## Estrutura atual

```
src/main/java/br/com/wdc/shopping/scripts/sgbd/
├── DBCreate.java        — Cria tabelas e sequências caso não existam
├── DBReset.java         — Popula o banco com dados de referência (seed)
├── MigrationRunner.java — Executa migrações e registra steps já executados
└── schema/
    └── EnMigrationLog.java — Definição da tabela EN_MIGRATION_LOG
```

## Abordagem híbrida SQL + Java

Os scripts são implementados como **classes Java** que executam SQL via JDBC. Isso permite:

- Lógica condicional (verificar se tabela/coluna já existe antes de alterar)
- Transformação de dados em memória quando a migração é complexa
- Reuso de entidades e repositórios do módulo `persistence`
- Controle transacional explícito por etapa
- Testes unitários sobre a lógica de migração

## Convenção para migrações

Cada roteiro de migração é uma classe Java cujos métodos públicos com prefixo `step` representam as ações sequenciais. O `MigrationRunner` descobre esses métodos automaticamente, ordena pelo número após o prefixo, pula os já executados (consultando `EN_MIGRATION_LOG`) e registra cada step concluído.

### Classe de migração

```java
public class Migration_0001_AddPurchaseStatus {

    private final Connection connection;

    public Migration_0001_AddPurchaseStatus(Connection connection) {
        this.connection = connection;
    }

    public void step1_adicionarColunaStatus() throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE PURCHASE ADD COLUMN STATUS VARCHAR(20) DEFAULT 'PENDING'");
        }
    }

    public void step2_popularValoresPadrao() throws SQLException {
        // Lógica Java + SQL para transformar dados existentes
    }

    public void step3_criarIndice() throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX IDX_PURCHASE_STATUS ON PURCHASE(STATUS)");
        }
    }
}
```

### Execução via MigrationRunner

```java
new MigrationRunner(connection)
    .run(new Migration_0001_AddPurchaseStatus(connection))
    .run(new Migration_0002_OutraMigracao(connection));
```

- Métodos são ordenados numericamente pelo número após `step` (ex.: `step1` antes de `step10`)
- Steps já executados são ignorados automaticamente (idempotência)
- Cada step bem-sucedido é registrado na tabela `EN_MIGRATION_LOG`
- Se um step falha, a execução para com `SQLException`

**Onde `XXXX`** no nome da classe é um número sequencial (ex.: `0001`, `0002`) que define a ordem de execução dos roteiros.

## Dependências

- `br.com.wdc.shopping.persistence` — entidades e repositórios
- `br.com.wdc.shopping.presentation` — constantes de domínio
- H2 Database — driver JDBC (ambiente de desenvolvimento/teste)
- SLF4J + Logback — logging

## Execução

O `DBCreate` é invocado na inicialização da aplicação e cria as tabelas (incluindo `EN_MIGRATION_LOG`) caso não existam. Migrações incrementais são executadas via `MigrationRunner` após o bootstrap. Não há framework de migração externo — o controle é feito diretamente pelo código Java.
