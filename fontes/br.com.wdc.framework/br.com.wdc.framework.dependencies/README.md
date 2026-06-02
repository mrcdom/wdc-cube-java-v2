# WDC Framework — Dependencies

Módulo **POM-only** (BOM) que centraliza o gerenciamento de versões de todas as dependências externas e internas usadas pelo projeto.

---

## Propósito

- Garantir versões consistentes em todos os módulos (single source of truth).
- Evitar conflitos de versão entre módulos do framework e módulos da aplicação.
- Fornecer `<pluginManagement>` com versões fixas dos plugins Maven.

## Dependências Gerenciadas

### Externas

| Dependência | Versão |
|-------------|--------|
| SLF4J | 2.0.16 |
| Logback | 1.5.32 |
| Gson | 2.13.2 |
| JOOQ | 3.19.16 |
| Agroal Pool | 2.7.1 |
| H2 Database | 2.4.240 |
| Commons Lang3 | 3.20.0 |
| Commons Text | 1.15.0 |
| Commons Codec | 1.18.0 |
| Commons IO | 2.21.0 |
| JDBI3 | 3.52.1 |
| Jackson Databind | 2.17.2 |
| Jakarta Servlet API | 6.1.0 |
| Jakarta WebSocket API | 2.2.0 |
| JUnit 4 | 4.13.2 |

### Internas

| Artefato | Versão |
|----------|--------|
| `br.com.wdc.framework.commons` | 1.0.0 |
| `br.com.wdc.framework.jooq` | 1.0.0 |
| `br.com.wdc.framework.cube` | 2.0.0 |
| `remote.backend` | 1.0.0 |
| `remote.bridge.teavm` | 1.0.0 |

---

## Plugins Gerenciados

- `maven-compiler-plugin` 3.13.0 (release = 21)
- `maven-shade-plugin` 3.6.2
- `maven-jar-plugin` 3.5.0
- `maven-resources-plugin` 3.5.0

## Uso

Importar como parent ou via `<dependencyManagement>` com scope `import`:

```xml
<parent>
    <groupId>br.com.wdc.framework</groupId>
    <artifactId>br.com.wdc.framework.dependencies</artifactId>
    <version>1.0.0</version>
</parent>
```
