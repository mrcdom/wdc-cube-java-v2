# 0002. The Cube knows only scopes; each rendering technology owns and releases its views

- **Date:** 2026-11-23 (views from a catalog per scope class); 2026-11-24 (the Cube knows only scopes)
- **Status:** Accepted

## Context

`framework.cube` used to carry a view concept: `CubeView`, a `ScopeViews` catalog, `viewOf`/`releaseView` on the application, `attachView`/`drawnBy` on the scope. Presenters took part in the views' life: they attached segment views and released them, and cleared their props on release. Each of the five rendering technologies then had to fit that one model, and a view that left the screen was released only if some presenter remembered to do it. The SWT remote shell hid released views instead of disposing them.

## Decision

`framework.cube` has no view concept. A presenter makes a `Scope` (with `Prop`s and actions), binds it with `bind(...)`, and hands it to its parent through a `ScopeSlot`. It never creates, attaches or releases a view.

Each rendering technology owns the binding of a scope to what draws it:

- a **catalog** of its own, scope class → view factory (for instance the `VIEWS` map in `StoreVaadinApplication`); the remote host needs none, since `RemoteViewImpl.forScope` makes a remote view for any scope, found by its `CLASS_ID`;
- a map of live views, and `viewOf(scope)`, which creates the view the first time and installs it as the scope's updater (`Scope.setUpdater`, taken back with `clearUpdater`);
- **holders counted**: a view asks for the views of the scopes it holds with its own `viewOf(...)` while it draws (`AbstractViewVaadin.redraw`); a held view that a drawing did not ask for drops that holder, and a view with no holder left is released, cascading to the views it held.

Releasing is what the technology does: Vaadin removes the component, SWT disposes the composite, TeaVM removes the element. The remote host releases a view when it writes the holder's scope again without it (`RemoteViewImpl`), queues its id, and sends it as `releasedViews` in its answer to the client's 15 s keep-alive ping (`RemoteApplicationSupport.sendPingResponseIfNeeded`).

## Consequences

- Presenters are plain logic over scopes; the same presenter runs under any technology, and the test mocks are one more technology with their own catalog.
- Release follows the drawing, so a scope that leaves a `Prop` releases its view without any presenter code.
- Each technology has to implement holders and cascade itself; a client that does not send the keep-alive never learns of released views (fixed for the Java bridge and the Codename One shell on 2026-11-24, commit `f963cfac`).

## Alternatives considered

- **A view catalog and view release in `framework.cube`** (`ScopeViews`, `CubeView`, presenters releasing views). Replaced: it tied every technology to one view model and left release to presenters.

See [cube-mvp-multiplatform-architecture.md](../cube-mvp-multiplatform-architecture.md#the-cube-mvp-pattern) and [bridge-skeleton-protocol.md §12](../bridge-skeleton-protocol.md#12-authority-over-scopes-and-view-garbage-collection).
