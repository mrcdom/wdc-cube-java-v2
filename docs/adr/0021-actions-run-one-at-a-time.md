# 0021. The actions of one application run one at a time

- **Date:** 2026-11-26
- **Status:** Accepted; supersedes the paragraph on serialization of [0004](0004-actions-are-guarded-scope-fields.md)

## Context

[0004](0004-actions-are-guarded-scope-fields.md) made actions guarded fields of the scope, but nothing kept two actions from running at the same time. Whether they could depended on the host: Vaadin runs them under its session lock and SWT on a single worker thread, but in teavm.web each action runs on its own green thread, so two clicks on "Buy" ran both purchases while the first one waited on the network, each seeing the other's state halfway. A presenter would need its own guards against double submission and torn state.

## Decision

Every guarded action runs inside `CubeApplication.runExclusively`, a monitor per application, from `AbstractPresenter`'s `action`/`actionOf`/`actionOf2`:

- an action that starts while another runs waits for it, without the presenter doing anything;
- the monitor is reentrant, so an action that runs another one on the same thread runs it at once;
- the guard's failure handling and the republishing of the address run inside it too.

It is a `synchronized` block, not a `java.util.concurrent` lock, because the same presenters run in the browser under TeaVM, whose threads support monitors and nothing else. On Java 25, a virtual thread blocked in `synchronized` releases its carrier (JEP 491).

## Consequences

- No action sees another one's state halfway, in any host. A double click makes two attempts one after the other: in teavm.web the second one finds the cart already bought and empty.
- An action that waits for a thread which itself runs an action of the same application would wait for itself; such code calls the logic directly instead of the action.
- A slow action delays the next one; long work belongs off the action, with its result applied by another action.

See `OneActionAtATimeTest` in `framework.cube`.
