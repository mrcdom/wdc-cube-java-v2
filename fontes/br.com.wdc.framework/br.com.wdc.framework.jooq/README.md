# WDC Framework — JOOQ

Declarative query builder over **JOOQ** with native JSON projection and support for multiple databases.

---

## Purpose

Enables SELECT queries with type-safe bean ↔ table mapping, where the result is serialized to **JSON directly by the database** and deserialized via streaming (Gson `JsonReader`). Supports partial projection (only the bean's non-null fields are included in the SELECT) and child collections via correlated subselects.

---

## Main Classes

| Class/Interface | Responsibility |
|-----------------|----------------|
| `JsonQueryBuilder<B, T>` | Fluent builder — registers fields, projections and 1:N collections |
| `JsonQuery<B, T>` | Immutable object produced by the builder — runs fetch, projection and parsing |
| `JsonChildQueryBuilder<B, T>` | Builder for child collections (correlated subselects) |
| `JsonDialect` | Interface — generates native `JSON_OBJECT` / `JSON_ARRAY` per database |
| `JooqUtils` | Helper utilities for JOOQ |
| `QueryContext` | Execution context of a query (unique aliases + `DSLContext`); the `DSLContext` is **injected** via `JsonQueryBuilder.setDSLContextSupplier` — the module has no global holder |
| `JsonFieldEntry` | (name, Field) pair for JSON projection |
| `JsonFieldType` | Enum of supported field types |

---

## Supported Dialects

| Class | Database |
|-------|----------|
| `PostgresJsonDialect` | PostgreSQL |
| `MySqlJsonDialect` | MySQL / MariaDB |
| `H2JsonDialect` | H2 |
| `SQLiteJsonDialect` | SQLite |
| `DuckDbJsonDialect` | DuckDB |
| `GenericJsonDialect` | Generic fallback |

---

## Usage Example

```java
private static final JsonQuery<User, TUser> QUERY =
    new JsonQueryBuilder<User, TUser>()
        .setAlias("u")
        .setBeanFactory(User::new)
        .setTableFactory(alias -> USER.as(alias))
        .addI64("id", u -> u.id, (u, v) -> u.id = v, t -> t.ID)
        .addStr("login", u -> u.login, (u, v) -> u.login = v, t -> t.LOGIN)
        .build();

// Full projection
User prj = QUERY.newProjectionBean();
List<User> list = QUERY.fetchToList(prj, (t, q) -> q.where(t.STATUS.eq("ACTIVE")));
```

---

## Maven Coordinates

```xml
<dependency>
    <groupId>io.github.mrcdom.wdc.java</groupId>
    <artifactId>framework-jooq</artifactId>
    <version>2.0.0</version>
</dependency>
```
