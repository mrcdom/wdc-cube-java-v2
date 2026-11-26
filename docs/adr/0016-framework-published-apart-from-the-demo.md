# 0016. The framework is published apart from the demo

- **Date:** 2026-11-16
- **Status:** Accepted

## Context

The reusable part of the repository — `br.com.wdc.framework` (`commons`, `domain`, `cube`, `cube.remote`, `jooq`, `persistence`, `dependencies`) — is what adopters build on; the WDC Store and its backend are the demonstration. Both lived in one Maven tree whose root pom was the parent of every module, so consuming the framework would have pulled the root pom and, with it, the demo's coordinates and settings into the consumer's dependency resolution. The repository was also moving its demos and backend to Java 25, which would have forced Java 25 on anyone using the framework.

## Decision

- Only `br.com.wdc.framework` is published, to Maven Central, as **`io.github.mrcdom.wdc.java:framework-*`**, all modules at one version (`framework-parent`); internal references and the BOM use `${project.version}`.
- **`framework-parent` does not inherit from the root pom**, on purpose: a consumer resolves only the framework's tree, never `br.com.wdc.store` or `br.com.wdc.cube.backend`. The root pom still lists the framework as a module, so the repository builds everything in one reactor.
- **The framework compiles with release 21**; the root pom, the store, the backend and the TeaVM shells compile with release 25.
- Publishing is `cd fontes/br.com.wdc.framework && mvn -Prelease clean deploy`. The `release` profile attaches sources and javadoc, signs with GPG and uploads through the `central-publishing-maven-plugin`; the release itself is manual, in the Central portal. Central's metadata (license, developer, SCM, per-module description) is in the poms.
- `remote.bridge.java` depends only on `slf4j-api`; the logging backend is the consumer's choice.

## Consequences

- Adopters get the framework without the demo, and on Java 21.
- The framework cannot use language features newer than 21, even though the rest of the repository can.
- `framework-parent` declares its own plugin versions and compiler settings instead of inheriting them.
- Changing the framework's version means changing it in `framework-parent` and in each module's `<parent>`.

See the [framework README → Publishing to Maven Central](../../fontes/br.com.wdc.framework/README.md#publishing-to-maven-central).
