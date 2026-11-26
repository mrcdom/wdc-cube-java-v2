# 0018. Five view technologies share the same presenters and scopes

- **Date:** 2026-04-28 (the premise of the project); 2026-11-16 (the current five)
- **Status:** Accepted

## Context

UI technologies come and go faster than the applications built on them, and presentation logic tangled with one toolkit dies with it. The Cube MVP pattern claims that presentation logic — presenters, scopes, navigation, business rules — can be written once, in plain Java with no UI dependency, and drawn by any technology. A claim like that is only credible if it is shown on several technologies that differ in kind: in-process and remote, server-side and client-side, web, desktop and mobile.

The set of technologies changed along the way: earlier JavaFX, Android, Swing and Gluon modules were replaced or removed, and the current five have stood since 2026-11-16.

## Decision

The WDC Store runs **one presentation module** (`br.com.wdc.store.presentation`) under five view technologies, and only the rendering layer changes between them:

| Technology | Module | Where the presenters run | How they reach the data |
|---|---|---|---|
| React 19 | `view.remote / remote.shell.react` | On the server (remote host) | Repositories directly |
| Flutter 3 | `view.remote / remote.shell.flutter` | On the server (remote host) | Repositories directly |
| Vaadin 25 | `view.vaadin` | On the server, in the Vaadin process | Repositories directly |
| Eclipse SWT | `view.swt` | In the desktop process | Repositories directly, local database |
| TeaVM (+ Tauri) | `view.teavm / teavm.web` | In the browser, compiled to JavaScript | HTTP repositories, through the REST API |

The remote host also serves other shells over the same protocol (a TeaVM shell, an SWT shell, a Codename One shell), all with the presenters on the server ([0014](0014-server-driven-remote-views.md)).

What makes it work: the presenters know only scopes ([0002](0002-the-cube-knows-only-scopes.md)); actions are scope fields ([0004](0004-actions-are-guarded-scope-fields.md)); repositories and transactions are contracts with in-process and HTTP implementations, chosen by each host's composition root ([0013](0013-dependency-injection-without-a-framework.md), [0010](0010-client-driven-remote-transactions.md)). The tests drive the same presenters through mock views, as one more technology.

## Consequences

- A change to presentation logic is made once and shows up in every frontend.
- A feature is done only when every frontend draws it; each technology keeps its own catalog of views and its own look.
- The trust boundary is not in the same place in every host: in teavm.web the presenters are client code, so the REST data policies protect the data; elsewhere the presenter does ([0006](0006-access-control-at-the-rest-boundary.md)).
- Building everything requires several toolchains (Maven, npm, Flutter, TeaVM; Tauri for native packaging).

See [cube-mvp-multiplatform-architecture.md → The View Layer](../cube-mvp-multiplatform-architecture.md#the-view-layer--five-independent-implementations) and [security-architecture.md → Trust Boundaries per Host](../security-architecture.md#trust-boundaries-per-host).
