# WDC Framework — JOOQ

Query builder declarativo sobre **JOOQ** com projeção JSON nativa e suporte a múltiplos bancos de dados.

---

## Propósito

Permitir consultas SELECT com mapeamento bean ↔ tabela de forma type-safe, onde o resultado é serializado em **JSON diretamente pelo banco** e desserializado via streaming (Gson `JsonReader`). Suporta projeção parcial (somente campos não-nulos do bean são incluídos no SELECT) e coleções filhas via subselect correlacionado.

---

## Classes Principais

| Classe/Interface | Responsabilidade |
|------------------|-----------------|
| `JsonQueryBuilder<B, T>` | Builder fluente — registra campos, projeções e coleções 1:N |
| `JsonQuery<B, T>` | Objeto imutável gerado pelo builder — executa fetch, projeção e parsing |
| `JsonChildQueryBuilder<B, T>` | Builder para coleções filhas (subselects correlacionados) |
| `JsonDialect` | Interface — gera `JSON_OBJECT` / `JSON_ARRAY` nativo por banco |
| `JooqUtils` | Utilitários auxiliares para JOOQ |
| `QueryContext` | Contexto de execução de uma query (alias únicos + `DSLContext`); o `DSLContext` é **injetado** via `JsonQueryBuilder.setDSLContextSupplier` — o módulo não tem holder global |
| `JsonFieldEntry` | Par (nome, Field) para projeção JSON |
| `JsonFieldType` | Enum de tipos de campo suportados |

---

## Dialetos Suportados

| Classe | Banco |
|--------|-------|
| `PostgresJsonDialect` | PostgreSQL |
| `MySqlJsonDialect` | MySQL / MariaDB |
| `H2JsonDialect` | H2 |
| `SQLiteJsonDialect` | SQLite |
| `DuckDbJsonDialect` | DuckDB |
| `GenericJsonDialect` | Fallback genérico |

---

## Exemplo de Uso

```java
private static final JsonQuery<Usuario, TUsuario> QUERY =
    new JsonQueryBuilder<Usuario, TUsuario>()
        .setAlias("u")
        .setBeanFactory(Usuario::new)
        .setTableFactory(alias -> USUARIO.as(alias))
        .addI64("id", u -> u.id, (u, v) -> u.id = v, t -> t.ID)
        .addStr("login", u -> u.login, (u, v) -> u.login = v, t -> t.LOGIN)
        .build();

// Projeção total
Usuario prj = QUERY.newProjectionBean();
List<Usuario> list = QUERY.fetchToList(prj, (t, q) -> q.where(t.STATUS.eq("ATIVO")));
```

---

## Coordenadas Maven

```xml
<dependency>
    <groupId>br.com.wdc.framework</groupId>
    <artifactId>br.com.wdc.framework.jooq</artifactId>
    <version>1.0.0</version>
</dependency>
```
