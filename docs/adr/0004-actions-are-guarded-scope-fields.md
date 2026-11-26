# 0004. Actions are guarded fields of the scope

- **Date:** 2026-11-23
- **Status:** Accepted

## Context

Views called presenter methods directly, and every presenter repeated a generic `try/catch` around its handlers. The remote skeleton of each presenter was a hand-written `switch` over numeric event codes that bound each action a second time, in another place; an unknown code fell into a `default -> new AssertionError(...)` that was never thrown, so it was silently ignored.

## Decision

An action is a field of the scope — `Action`, `ActionOf<T>`, `ActionOf2<T, U>` — bound once in the presenter's constructor through `AbstractPresenter`:

```java
this.scope.onBuy = action(1, this::buy);
actionOf(productsPanel, 1, FormParam.asLong("p.productId"), this::openProduct);   // on a segment
```

Every bound action runs through one **guard**: an exception the body did not handle goes to `onActionFailure`, which by default calls `CubeApplication.alertUnexpectedError`; at the end, failing or not, the application republishes the address (`app.updateHistory()`), since the action may have changed what presenters publish. The body handles only expected failures (an invalid cart item, an offline database) and turns them into messages in the scope.

The optional **event code** and `FormParam`s (`asLong`, `asInteger`, `asString`, `of` for an encoded value) let a remote client fire the same action. The scope's skeleton (`Scope.skeleton(CLASS_ID)`) dispatches each event to the action bound with its code; a code bound twice fails when the presenter is built, and an unknown code from the client is an `IllegalArgumentException`. `ScopeSerializer` skips action fields.

The framework does not serialize actions with a lock of its own. The remote host dispatches a client's events from the WebSocket message handler (`RemoteApplicationSupport.runDispatchPhase`); the `ReentrantLock` in `RemoteApplicationSupport` guards the form-data sync (`syncScopes`), navigation (`safeGo`), alerts and the commit of computed state, not the action bodies.

## Consequences

- Views only read the scope and call `scope.onX.run(...)`; they never see a presenter.
- Presenter code has no generic error handling, and an unhandled failure always reaches the user as an alert instead of a stack trace or silence.
- The event code is the contract with remote clients, declared next to the action instead of in a separate dispatcher.
- Local views call the action directly; the event code matters only for remote clients.

## Alternatives considered

- **Views calling presenter methods, with a hand-written skeleton per presenter.** Replaced, for the duplicated binding and error handling described above.

See [cube-mvp-multiplatform-architecture.md](../cube-mvp-multiplatform-architecture.md#the-cube-mvp-pattern).
