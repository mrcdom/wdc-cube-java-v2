# WDC Shopping — Business Implementation

Camada de persistência do sistema Shopping. Implementa os repositórios definidos em `business.shared` usando **JDBI**, **Command Pattern** e uma **DSL SQL** própria. O banco de dados utilizado é **H2** (embarcado).

## Dependências

| Artefato | Papel |
|----------|-------|
| `br.com.wdc.shopping.business.shared` | Modelos de domínio, interfaces de repositório, critérios |
| `br.com.wdc.shopping.presentation.shared` | DTOs compartilhados com a camada de apresentação |
| `org.jdbi:jdbi3-core` | Acesso a dados JDBC fluente |
| `com.google.code.gson:gson` | Parsing de JSON (usado na deserialização de rows) |
| `commons-io` | Utilitários de I/O |
| `slf4j-api` | Logging |

## Estrutura de Pacotes

```
br.com.wdc.shopping.business.impl
│
├── concurrent/                                # Adaptadores de concorrência
│   ├── ScheduledExecutorAdapter.java          # Adapta ScheduledExecutorService → ScheduledExecutor
│   └── ScheduledExecutorServiceDelegate.java  # Delegate para ScheduledExecutorService
│
└── sgbd/
    ├── dsl/                                   # DSL SQL
    │   ├── SqlKeywords.java                   # Constantes e funções SQL (SELECT, WHERE, AND, ...)
    │   └── SqlList.java                       # Builder de SQL com projeção tipada (ResultSet → tipo)
    │
    ├── utils/                                 # Classes base reutilizáveis
    │   ├── DbTable.java                       # Base para definição de tabelas (DDL)
    │   ├── DbField.java                       # Metadado de coluna (nome, tipo, nullable, etc.)
    │   ├── BaseRow.java                       # Base para row objects (change tracking)
    │   ├── BaseRepository.java                # Base para repositórios (DataSource + exceções)
    │   ├── BaseCommand.java                   # Base para comandos SQL (parâmetros bind)
    │   ├── BaseApplyCriteria.java             # Base para aplicadores de critérios
    │   └── SqlUtils.java                      # Utilitários (sequences, JSON fields, comma helper)
    │
    ├── ddl/                                   # Definição de esquema
    │   ├── tables/                            # Definições de tabela
    │   │   ├── EnUser.java                    # Tabela EN_USER + Row + sequence SQ_USER
    │   │   ├── EnProduct.java                 # Tabela EN_PRODUCT + Row + sequence SQ_PRODUCT
    │   │   ├── EnPurchase.java                # Tabela EN_PURCHASE + Row + sequence SQ_PURCHASE
    │   │   └── EnPurchaseItem.java            # Tabela EN_PURCHASE_ITEM + Row + sequence SQ_PURCHASE_ITEM
    │   └── scripts/
    │       ├── DBCreate.java                  # Criação do esquema (idempotente)
    │       └── DBReset.java                   # Reset de dados (carga inicial)
    │
    └── repository/                            # Implementações de repositórios
        ├── RepositoryBootstrap.java           # Inicializa os BEANs estáticos
        │
        ├── user/                              # Repositório de usuários
        │   ├── UserRepositoryImpl.java        # Implementa UserRepository
        │   ├── InsertRowUserCmd.java           # INSERT
        │   ├── UpdateRowUserCmd.java           # UPDATE
        │   ├── FetchUsersCmd.java              # SELECT (com projeção e CTE)
        │   ├── CountUsersCmd.java              # COUNT
        │   ├── DeleteUsersCmd.java             # DELETE
        │   └── ApplyUserCriteria.java          # Aplica UserCriteria → cláusulas WHERE
        │
        ├── product/                           # Repositório de produtos
        │   ├── ProductRepositoryImpl.java
        │   ├── InsertProductRowCmd.java
        │   ├── UpdateProductRowCmd.java
        │   ├── FetchProductsCmd.java
        │   ├── CountProductsCmd.java
        │   ├── DeleteProductsCmd.java
        │   └── ApplyProductCriteria.java
        │
        ├── purchase/                          # Repositório de compras
        │   ├── PurchaseRepositoryImpl.java
        │   ├── InsertRowPurchaseCmd.java
        │   ├── UpdateRowPurchaseCmd.java
        │   ├── FetchPurchaseCmd.java
        │   ├── CountPurchasesCmd.java
        │   ├── DeletePurchasesCmd.java
        │   └── ApplyPurshaseCriteria.java
        │
        └── purchaseitem/                      # Repositório de itens de compra
            ├── PurchaseItemRepositoryImpl.java
            ├── InsertRowPurchaseItemCmd.java
            ├── UpdateRowPurchaseItemCmd.java
            ├── FetchPurchaseItemsCmd.java
            ├── CountPurchaseItemsCmd.java
            ├── DeletePurchaseItemsCmd.java
            └── ApplyPurshaseItemCriteria.java
```

## Arquitetura

### Padrões Utilizados

| Padrão | Implementação | Finalidade |
|--------|--------------|------------|
| **Repository** | `XxxRepositoryImpl extends BaseRepository` | Fachada de acesso a dados por entidade |
| **Command** | `VerbEntityCmd extends BaseCommand` | Cada operação SQL é uma classe isolada |
| **Criteria** | `ApplyXxxCriteria extends BaseApplyCriteria` | Traduz critérios de negócio em cláusulas WHERE |
| **Row (Change Tracking)** | `EnXxx.Row extends BaseRow` | Rastreia quais campos foram modificados para UPDATE parcial |
| **Table Metadata** | `EnXxx extends DbTable` | Define esquema (colunas, tipos, DDL, sequences) |
| **SQL DSL** | `SqlList` + `SqlKeywords` | Construção programática de SQL legível |

### Fluxo de uma Consulta

```
1. Camada de apresentação chama:
   ProductRepository.BEAN.get().fetch(criteria)

2. ProductRepositoryImpl.fetch():
   - Abre TransactionContext (connection do DataSource)
   - Delega para FetchProductsCmd.byCriteria(connection, criteria)

3. FetchProductsCmd:
   - Constrói SQL com SqlList (SELECT, FROM, WITH CTE)
   - Usa ApplyProductCriteria para gerar WHERE
   - Usa SqlList.field() / strColumn() para projeção tipada
   - Executa via JDBI: handle.createQuery(sql).map(...)
   - Deserializa resultado via JSON_OBJECT → Gson → Modelo

4. Retorna List<Product> para o chamador
```

### Fluxo de uma Escrita

```
1. Camada de apresentação chama:
   ProductRepository.BEAN.get().insert(product)

2. ProductRepositoryImpl.insert():
   - Abre TransactionContext
   - Delega para InsertProductRowCmd.run(connection, product)

3. InsertProductRowCmd:
   - Cria EnProduct.Row e popula campos modificados
   - Gera ID via sequence se necessário
   - Monta INSERT INTO ... VALUES usando SqlList + param()
   - Executa via JDBI: handle.createUpdate(sql).execute()
   - Retorna o ID gerado no modelo original
```

## Componentes Principais

### SqlList — Builder de SQL

`SqlList` estende `ArrayList<String>` e oferece construção fluente de SQL:

```java
var sql = new SqlList();
sql.ln(SELECT);
var fId = sql.i64Column(en.id);       // registra coluna + retorna extractor tipado
var fName = sql.strColumn(en.name);
sql.ln(FROM, en.tableRef());
sql.ln(WHERE_TRUE);
sql.ln(AND, en.id, EQUAL, param("id", 42));

// Execução com projeção tipada
query.map((rs, _) -> {
    var item = new Product();
    item.id = fId.apply(rs);      // Long
    item.name = fName.apply(rs);  // String
    return item;
});
```

Os métodos `i64Column()`, `strColumn()`, `f64Column()`, etc., registram a coluna na projeção e retornam uma `ThrowingFunction<ResultSet, T>` que extrai o valor tipado pela posição.

### SqlKeywords — Constantes SQL

Interface com constantes (`SELECT`, `WHERE`, `AND`, `ORDER_BY`, ...) e funções auxiliares (`COUNT(...)`, `IN(...)`, `EXISTS(...)`, `BETWEEN(...)`, `ORDER_BY(...)`). Implementada por `BaseCommand` e `BaseApplyCriteria`.

### DbTable + DbField — Definição de Esquema

Cada tabela é definida como classe que estende `DbTable`:

```java
public class EnUser extends DbTable {
    public static final EnUser INSTANCE = new EnUser("");

    public final DbField id;
    public final DbField userName;
    public final DbField password;
    public final DbField name;

    public EnUser(String alias) {
        super(alias);
        this.id = mkBigint("ID", false);
        this.userName = mkVarChar("USERNAME", 255, false);
        this.password = mkChar("PASSWORD", 32, false);
        this.name = mkVarChar("NAME", 255, false);
    }
}
```

- `INSTANCE` (sem alias) — usado para DDL e acesso direto
- Instâncias com alias (`new EnUser("u")`) — usadas em queries com JOINs

### Row (Change Tracking)

Cada `EnXxx` contém uma classe `Row` interna que rastreia quais campos foram alterados. O `InsertRowCmd` e `UpdateRowCmd` usam `isXxxChanged()` para incluir apenas campos modificados no SQL:

```java
var row = new EnUser.Row();
row.id(bean.id);
row.userName(bean.userName);  // marca userNameChanged = true
// password e name NÃO são setados → não entram no INSERT
```

### BaseCommand — Parâmetros Bind

Todos os comandos usam `param(name, value)` para parâmetros SQL (prevenção de SQL injection via JDBI bind):

```java
sql.ln(AND, en.id, EQUAL, param("id", userId));
// Gera: AND U.ID = :id
// E registra: bind("id", userId)
```

### ApplyCriteria — Tradução de Critérios

Cada entidade possui um `ApplyXxxCriteria` que traduz os campos do `XxxCriteria` em cláusulas WHERE:

```java
public void apply(SqlList sql) {
    if (criteria.userId() != null) {
        sql.ln(AND, root.id, EQUAL, param("userId", criteria.userId()));
    }
    if (criteria.userName() != null) {
        sql.ln(AND, root.userName, EQUAL, param("userName", criteria.userName()));
    }
}
```

### Projeção com JSON_OBJECT

Os comandos `FetchXxxCmd` usam `JSON_OBJECT` do H2 para serializar o resultado como JSON no banco e depois deserializar com Gson. Isso simplifica o mapeamento de JOINs complexos (ex: Purchase + PurchaseItem + Product):

```java
var fields = FetchUsersCmd.fields(projection, en);
var fJsonData = sql.strColumn(SqlUtils.toJsonField(fields), AS, "json_data");
// Gera: JSON_OBJECT('ID': U.ID, 'NAME': U.NAME) AS json_data
```

### RepositoryBootstrap

Inicializa todos os `BEAN` estáticos dos repositórios:

```java
RepositoryBootstrap.initialize();
// UserRepository.BEAN → UserRepositoryImpl
// ProductRepository.BEAN → ProductRepositoryImpl
// PurchaseRepository.BEAN → PurchaseRepositoryImpl
// PurchaseItemRepository.BEAN → PurchaseItemRepositoryImpl
```

### DBCreate / DBReset

- `DBCreate` — criação idempotente do esquema (verifica se tabelas existem via `DatabaseMetaData`)
- `DBReset` — carga inicial de dados (usuários, produtos de exemplo)

## Convenções de Nomenclatura

| Tipo | Padrão | Exemplo |
|------|--------|---------|
| Tabela DDL | `En` + Entidade | `EnUser`, `EnProduct` |
| Nome da tabela | `EN_` + ENTIDADE | `EN_USER`, `EN_PRODUCT` |
| Sequence | `SQ_` + ENTIDADE | `SQ_USER`, `SQ_PRODUCT` |
| Repositório | Entidade + `RepositoryImpl` | `UserRepositoryImpl` |
| Insert | `InsertRow` + Entidade + `Cmd` | `InsertRowUserCmd` |
| Update | `UpdateRow` + Entidade + `Cmd` | `UpdateRowUserCmd` |
| Fetch | `Fetch` + Entidades + `Cmd` | `FetchUsersCmd` |
| Count | `Count` + Entidades + `Cmd` | `CountUsersCmd` |
| Delete | `Delete` + Entidades + `Cmd` | `DeleteUsersCmd` |
| Critério | `Apply` + Entidade + `Criteria` | `ApplyUserCriteria` |

## Esquema do Banco

```
EN_USER
├── ID          BIGINT NOT NULL (PK, SQ_USER)
├── USERNAME    VARCHAR(255) NOT NULL
├── PASSWORD    CHAR(32) NOT NULL
└── NAME        VARCHAR(255) NOT NULL

EN_PRODUCT
├── ID          BIGINT NOT NULL (PK, SQ_PRODUCT)
├── NAME        VARCHAR(255) NOT NULL
├── PRICE       NUMERIC(10,2) NOT NULL
├── DESCRIPTION VARCHAR(4096)
└── IMAGE       VARBINARY(65536)

EN_PURCHASE
├── ID          BIGINT NOT NULL (PK, SQ_PURCHASE)
├── USER_ID     BIGINT NOT NULL (FK → EN_USER)
└── BUY_DATE    TIMESTAMP NOT NULL

EN_PURCHASE_ITEM
├── ID          BIGINT NOT NULL (PK, SQ_PURCHASE_ITEM)
├── PURCHASE_ID BIGINT NOT NULL (FK → EN_PURCHASE)
├── PRODUCT_ID  BIGINT NOT NULL (FK → EN_PRODUCT)
├── AMOUNT      INTEGER NOT NULL
└── PRICE       NUMERIC(10,2) NOT NULL
```
