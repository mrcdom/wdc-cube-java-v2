# WDC Framework — JOOQ

Declarative query builder over **JOOQ** with native JSON projection and support for multiple databases.

---

## Purpose

Enables SELECT queries with type-safe bean ↔ table mapping, where the result is serialized to **JSON directly by the database** and deserialized via streaming (Gson `JsonReader`). Supports partial projection (only the bean's non-null fields are included in the SELECT) and child collections via correlated subselects.

---

## Main Classes

| Class/Interface | Responsibility |
|-----------------|----------------|
| `JsonQueryBuilder<B, T>` | Fluent builder — registers fields, projections, ordering and 1:1 / 1:N relations |
| `JsonQuery<B, T>` | Immutable object produced by the builder — runs fetch, projection and parsing |
| `JsonChildQueryBuilder<B, T>` | Builder for child collections (correlated subselects) |
| `JsonDialect` | Interface — generates native `JSON_OBJECT` / `JSON_ARRAY` per database |
| `JooqUtils` | Helper utilities for JOOQ |
| `QueryContext` | Execution context of a query (unique aliases + `DSLContext`); the `DSLContext` is **injected** via `JsonQueryBuilder.setDSLContextSupplier` — the module has no global holder |
| `JsonFieldEntry` | (name, Field) pair for JSON projection |
| `JsonFieldType` | Enum of supported field types |
| `RelationKey` | Key of an associated entity mapped to this table's foreign-key column — an association projected by its key only needs no subselect |
| `CriterionTranslator` | Translates the domain's `Criterion` fields (`framework.domain.criteria`) into jOOQ `Condition`s |
| `TransactionAwareConnectionProvider` | jOOQ `ConnectionProvider` bound to `TransactionScope`: inside a transaction it hands out the scope's connection, outside it a standalone one |

---

## Supported Dialects

| Class | Database |
|-------|----------|
| `PostgresJsonDialect` | PostgreSQL |
| `MySqlJsonDialect` | MySQL / MariaDB |
| `H2JsonDialect` | H2 |
| `SQLiteJsonDialect` | SQLite |
| `DuckDbJsonDialect` | DuckDB |
| `SqlServerJsonDialect` | SQL Server |
| `GenericJsonDialect` | Generic fallback |

---

## Usage Example

```java
public static final JsonQuery<User, EnUser> QUERY = new JsonQueryBuilder<User, EnUser>()
        .setAlias("u")
        .setBeanFactory(User::new)
        .setTableFactory(EN_USER::as)
        .setDSLContextSupplier(UserRepositoryImpl::dsl)
        .addI64("id", u -> u.id(), (u, v) -> u.withId(v), t -> t.ID)
        .addStr("userName", u -> u.userName(), (u, v) -> u.withUserName(v), t -> t.USERNAME)
        .addStr("name", u -> u.name(), (u, v) -> u.withName(v), t -> t.NAME)
        .build();

// Full projection
User prj = QUERY.newProjectionBean();
List<User> list = QUERY.fetchToList(prj, (t, q) -> q.where(t.USERNAME.startsWith("j")));
```

---

## Dependencies

| Artifact | Role |
|----------|------|
| `framework-commons` | Utilities |
| `framework-persistence` | `TransactionScope`, used by `TransactionAwareConnectionProvider` (brings `framework-domain`, whose `Criterion` `CriterionTranslator` reads) |
| `org.jooq:jooq` | Query DSL and execution |
| `commons-text` | Text utilities |

---

## Maven Coordinates

```xml
<dependency>
    <groupId>io.github.mrcdom.wdc.java</groupId>
    <artifactId>framework-jooq</artifactId>
    <version>2.0.0</version>
</dependency>
```
