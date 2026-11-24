# WDC Framework — Dependencies

**POM-only** module (BOM) that centralizes version management for all external and internal dependencies used by the project.

---

## Purpose

- Ensure consistent versions across all modules (single source of truth).
- Avoid version conflicts between framework modules and application modules.
- Provide `<pluginManagement>` with pinned Maven plugin versions.

## Managed Dependencies

### External

| Artifact | Version |
|----------|---------|
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

### Internal (`io.github.mrcdom.wdc.java`)

| Artifact | Version |
|----------|---------|
| `framework-commons` | 2.0.0 (the framework's) |
| `framework-domain` | 2.0.0 (the framework's) |
| `framework-persistence` | 2.0.0 (the framework's) |
| `framework-jooq` | 2.0.0 (the framework's) |
| `framework-cube` | 2.0.0 (the framework's) |
| `framework-cube-remote-backend` | 2.0.0 (the framework's) |
| `framework-cube-remote-bridge-java` | 2.0.0 (the framework's) |
| `framework-cube-remote-bridge-teavm` | 2.0.0 (the framework's) |

---

## Managed Plugins

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

## Usage

Import as parent or via `<dependencyManagement>` with scope `import`:

```xml
<parent>
    <groupId>io.github.mrcdom.wdc.java</groupId>
    <artifactId>framework-dependencies</artifactId>
    <version>2.0.0</version>
</parent>
```
