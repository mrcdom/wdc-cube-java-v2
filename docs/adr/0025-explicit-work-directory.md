# 0025. Every host runs from an explicit work directory

- **Date:** 2026-11-26
- **Status:** Accepted

## Context

The hosts (the backend, Vaadin and the local SWT desktop) found their configuration from wherever the process started: the system property `store.config.file`, or `config/application.toml` relative to the working directory, and then `app.basedir` inside the file, resolved relative to the file. Run from the IDE, from Maven or from a script, the same binary found different files, or none, and wrote data and logs in unexpected places. Maven's `exec:java` even declared a working directory that it ignores.

## Decision

A host does not start without a **work directory**, given by `--workdir=<folder>` or the `STORE_WORKDIR` variable (the argument wins); the folder must exist. There is no default relative to the process, and `store.config.file` and `app.basedir` are gone. Everything comes from fixed subfolders (`StoreConfig`):

| Folder | Holds |
|---|---|
| `config/` | `application.toml`, `application.local.toml` (local overrides and secrets) and `logback.xml` |
| `data/` | the local database and other data kept between runs |
| `log/` | the log files |
| `temp/` | temporary files |
| `frontend/` | one subfolder per context of static files served (the web clients) |

- `StoreConfig.resolveWorkDir(args, env)` resolves it and `StoreConfig.Internals.load(workDir)` loads the configuration and points the folders at it; code refers to the folders through `StoreConfig`, never to a fixed `work/...` path. The framework's `AppConfig.load(Path)` takes the file it is given and guesses nothing.
- In development the backend's work directory is the repository's `work/` (`work/bin/start-server.sh` and `mvn exec:java` pass it); Vaadin and SWT keep their own `work/` in their module. The folder names and the versioned `config/application.toml` stay as they were.

## Consequences

- The same binary behaves the same from anywhere; a second backend with another configuration is just another folder.
- Starting without the argument or the variable fails at once with a message saying how to give it, instead of guessing.
- Launch configurations that set `-Dstore.config.file` or relied on the working directory need `--workdir=` instead.
