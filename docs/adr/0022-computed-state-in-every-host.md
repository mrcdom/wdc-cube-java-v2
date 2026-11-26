# 0022. Computed state is committed once per paint cycle, in every host

- **Date:** 2026-11-26
- **Status:** Accepted; supersedes [0005](0005-computed-state-once-per-paint.md)

## Context

[0005](0005-computed-state-once-per-paint.md) introduced `commitComputedState()`, but only the remote host called it. Vaadin, SWT and teavm.web never did, so a derived value those hosts show had to be set again wherever its input changed (`HomePresenter` set `cartItemCount` both in `commitComputedState` and in its cart listener). The method meant "computed once, before painting" in one host and nothing in the others.

## Decision

`CubeApplication.commitComputedState()` runs on every live presenter **once per paint cycle, before the dirty views are drawn**, in every rendering technology:

- **remote host:** before it serializes the dirty scopes of a response (`RemoteApplicationSupport`), as before;
- **Vaadin:** at the start of `StoreVaadinApplication.flushDirtyViews`, inside `UI.access`;
- **SWT:** at the start of `StoreSwtApplication.flushDirtyViews`, under the presenter lock;
- **teavm.web:** at the start of the `requestAnimationFrame` flush in `StoreTeaVMApplication`.

What it changes marks views dirty before they are chosen, so it is painted in the same cycle.

## Consequences

- A derived value is right at paint time in every host, computed from the final state of what ran before the cycle.
- A paint cycle runs only when some view is dirty. A change of input that dirties nothing (the cart changed by another screen while the Home is shown) is not painted until something else is; `HomePresenter` therefore also sets `cartItemCount` in its cart listener, which marks the Home dirty.
- `commitComputedState()` must be cheap and idempotent: it runs once per cycle whether or not its inputs changed. Setting an unchanged `Prop` is free.
- A value that needs I/O is not derived state: an action loads it.
- A new rendering technology calls it in its own paint scheduler, before choosing the dirty views.
