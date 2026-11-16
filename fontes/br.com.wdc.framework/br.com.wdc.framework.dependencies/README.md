# WDC Framework — Dependencies

Módulo **POM-only** (BOM) que centraliza o gerenciamento de versões de todas as dependências externas e internas usadas pelo projeto.

---

## Propósito

- Garantir versões consistentes em todos os módulos (single source of truth).
- Evitar conflitos de versão entre módulos do framework e módulos da aplicação.
- Fornecer `<pluginManagement>` com versões fixas dos plugins Maven.

## Dependências Gerenciadas

### Externas

| Artefato | Versão |
|----------|--------|
| `junit` | 4.13.2 |
| `slf4j-api` | 2.0.20 |
| `jcl-over-slf4j` | 2.0.20 |
| `jakarta.servlet-api` | 6.1.0 |
| `jakarta.websocket-api` | 2.2.0 |
| `jakarta.websocket-client-api` | 2.2.0 |
| `logback-classic` | 1.6.7 |
| `gson` | 2.15.0 |
| `agroal-api` | 3.3 |
| `agroal-pool` | 3.3 |
| `agroal-narayana` | 3.3 |
| `narayana-jta` | 7.3.4.Final |
| `jboss-logging` | 3.6.3.Final |
| `postgresql` | 42.7.14 |
| `h2` | 2.5.252 |
| `commons-lang3` | 3.22.0 |
| `commons-text` | 1.15.0 |
| `commons-codec` | 1.23.0 |
| `commons-io` | 2.22.0 |
| `jooq` | 3.21.9 |
| `jooq-codegen` | 3.21.9 |
| `jackson-databind` | 2.23.0 |
| `jakarta.transaction-api` | 2.0.1 |

### Internas (`io.github.mrcdom.wdc.java`)

| Artefato | Versão |
|----------|--------|
| `framework-commons` | 2.0.0 (a do framework) |
| `framework-domain` | 2.0.0 (a do framework) |
| `framework-persistence` | 2.0.0 (a do framework) |
| `framework-jooq` | 2.0.0 (a do framework) |
| `framework-cube` | 2.0.0 (a do framework) |
| `framework-cube-remote-backend` | 2.0.0 (a do framework) |
| `framework-cube-remote-bridge-java` | 2.0.0 (a do framework) |
| `framework-cube-remote-bridge-teavm` | 2.0.0 (a do framework) |

---

## Plugins Gerenciados

- `maven-shade-plugin` 3.6.2
- `maven-jar-plugin` 3.5.1
- `maven-resources-plugin` 3.5.0
- `maven-compiler-plugin` 3.16.0 (release = 21)
- `maven-source-plugin` 3.4.0
- `build-helper-maven-plugin` 3.8.0
- `exec-maven-plugin` 3.7.0
- `maven-surefire-plugin` 3.6.1
- `maven-assembly-plugin` 3.8.0
- `maven-antrun-plugin` 3.2.0
- `maven-clean-plugin` 3.5.0
- `maven-war-plugin` 3.5.1
- `maven-ear-plugin` 3.4.0
- `wildfly-maven-plugin` 6.2.0.Final
- `proguard-maven-plugin` 2.7.0

## Uso

Importar como parent ou via `<dependencyManagement>` com scope `import`:

```xml
<parent>
    <groupId>io.github.mrcdom.wdc.java</groupId>
    <artifactId>framework-dependencies</artifactId>
    <version>2.0.0</version>
</parent>
```
