# WDC Framework — Domain

The **contracts** of the domain, with no technology behind them: what a repository offers, how a query is asked for, how a transaction is opened, who may do what, and how an action on a resource is written. The implementations live elsewhere — jOOQ in `framework.jooq`, transactions in `framework.persistence`, HTTP in each app's client module — so the code written against these contracts runs the same with direct database access or over HTTP.

> Full architectural view: [docs/data-layer.md](../../../docs/data-layer.md).

## Dependencies

| Artifact | Role |
|----------|------|
| `framework-commons` | Utilities, serialization, logging |

Nothing else: no JDBC, no jOOQ, no HTTP client. It compiles for the JVM and for JavaScript (TeaVM).

## Packages

| Package | Contents |
|---------|----------|
| `repository` | `Repository<E, C, K>` — the generic repository contract (insert, update, delete, count, fetch) |
| `criteria` | `Criteria` — the fields, and the alternative groups (`anyOf`, server-side only) for a disjunction across fields —, and the field types of a criteria: `Criterion` (eq/ne/in/isNull), `ComparableCriterion` (gt/ge/lt/le/between), `TextCriterion` (like/ilike/containing/startingWith) |
| `projection` | `ProjectionValues`, `ProjectionList`, `ProjectionSet` — which fields (and which slice of a collection) a query loads |
| `pagination` | `Page` |
| `codec` | `ModelCodec` — entity ↔ JSON, shared by the REST server and client |
| `transaction` | `TransactionService` / `TransactionContext` — CMT-style programmatic transactions (`required`, `requiresNew`, ...) |
| `resource` | The resource calls: an action on a resource as an object, named by an HTTP-like verb (below) |
| `security` | `SecurityContext` (who the requester is, and their roles), `AuthenticationService`, password and crypto helpers |
| `security.policy` | The data access policy of an API open to external clients: `DataPolicy` (grants per role), `DataGrant` (rows, fields read, masked, written), `DataAccess` (applies them to a request and to each returned row), `RowScope`, `Mask`, `EntityDescriptor` — how to use it: [Security Guide](../docs/security-guide.md); design: [Security Architecture](../../../docs/security-architecture.md) |
| `config` | `AppConfig` |
| `exception` | `BusinessException`, `AccessDeniedException`, `InvalidRequestException`, the transaction exceptions |

## Resource calls

Each action on a resource is a small class: the request is gathered with fluent `withXxx(...)` setters and `run()` answers it, a new instance per use. The suffix is a verb, as in HTTP, and each verb has a base class that keeps its guarantees:

| Suffix | HTTP | Safe | Idempotent | Base | The call implements |
|--------|------|------|------------|------|---------------------|
| `Query` | `GET` / `QUERY` | yes | yes | `ResourceQuery<R>` | `run()` |
| `Create` | `POST` | no | no | `ResourceCreate<R>` | `create()` |
| `Save` | `PUT` | no | yes | `ResourceSave<R>` | `save()` |
| `Update` | `PATCH` | no | not in general | `ResourceUpdate<R>` | `update()` |
| `Delete` | `DELETE` | no | yes | `ResourceDelete<R>` | `delete()` |

The write verbs share `ResourceWrite<R>`: its `final` `run()` opens the transaction (or joins the caller's) and hands over to the verb's method. There is no global `TransactionService`, so a write call names its module's in the constructor, read when it runs. An action that fits none of the verbs gets a verb of its own and extends `ResourceCall<R>` directly.

```java
public class ProductQuery extends ResourceQuery<ProductInfo> {

    private Long productId;

    public ProductQuery withProductId(Long productId) {
        this.productId = productId;
        return this;
    }

    @Override
    public ProductInfo run() {
        var product = ProductRepository.BEAN.get().fetchById(productId, ProductInfo.projection());
        if (product == null) {
            throw new ProductNotFoundException();
        }
        return ProductInfo.create(product);
    }
}

public class PurchaseCreate extends ResourceCreate<Long> {

    public PurchaseCreate() {
        super(StoreTransactions.BEAN::get);   // the module's TransactionService
    }

    @Override
    protected Long create() {
        // runs inside the transaction opened by run()
    }
}
```

```java
var product = new ProductQuery()
        .withProductId(id)
        .run();
```

Why each verb promises what it does, and how the calls run the same in the Host and over HTTP: [docs/data-layer.md → Resource Calls](../../../docs/data-layer.md#resource-calls--actions-on-a-resource-as-objects).
