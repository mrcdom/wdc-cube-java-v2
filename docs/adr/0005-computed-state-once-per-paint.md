# 0005. Computed state is committed once per paint cycle

- **Date:** 2026-04-28
- **Status:** Accepted

## Context

Some values a scope shows are derived from state that other code changes: the number of items in the cart badge of the Home comes from the `CartManager`, and the root of the remote client (the browser view's `content`) is the scope of whichever root presenter exists after a navigation. A single client request can run an action, a navigation that creates and releases presenters, and several `Prop` changes; the derived values are only meaningful once all of that has settled.

## Decision

`PresenterBase` declares `commitComputedState()` (a no-op by default), and `CubeApplication.commitComputedState()` calls it on every live presenter, logging and skipping a presenter that fails.

It is not part of navigation. The **remote host** calls it, once per response that carries scopes, before writing any of them (`RemoteApplicationSupport.writeResponseJson`): under the session's lock, it calls `cubeApp.commitComputedState()` and then `browserPresenter.commitComputedState()`, and only then serializes the dirty views. A view made dirty by the commit is written in the same response.

The presenters that use it:

- `HomePresenter` sets `scope.cartItemCount` from the cart;
- `RemoteBrowserPresenter` sets `scope.content` to the root presenter's scope.

## Consequences

- What the remote client receives is consistent: derived values are computed from the final state of the request, not from an intermediate one.
- A presenter that overrides it must be cheap and idempotent, since it runs before every response with scopes, whether or not its inputs changed; setting an unchanged value is free, because `Prop.set` only requests a redraw on a different value.
- Only the remote host calls it. Vaadin, SWT and teavm.web never do, so a derived value those hosts show must also be set where its input changes: `HomePresenter` sets `cartItemCount` in its cart-change callback (`onCartChanged`) as well.

See [bridge-skeleton-protocol.md §9.1](../bridge-skeleton-protocol.md#91-response--scope-push-envelope) and [cube-mvp-multiplatform-architecture.md → Route Control](../cube-mvp-multiplatform-architecture.md#route-control--places-and-intents).
