import type { ScopeCoordinator } from "./ScopeCoordinator"

/**
 * ViewScope GC driven by server authority.
 *
 * The server periodically reports (via the ping response) which views are active.
 * The client removes from the viewMap any view that:
 *   1. is NOT in the server's list
 *   2. is NOT mounted (no active bindView)
 */
export class ViewGarbageCollector {
  private readonly app: ScopeCoordinator
  private readonly mountedViews = new Set<string>()

  constructor(app: ScopeCoordinator) {
    this.app = app
  }

  mount(vsid: string) {
    this.mountedViews.add(vsid)
  }

  unmount(vsid: string) {
    this.mountedViews.delete(vsid)
  }

  /**
   * Removes specific views the server has released (eager GC, ~15s).
   * Only removes a view that is not mounted.
   */
  release(releasedViews: string[]) {
    for (const vsid of releasedViews) {
      if (!this.mountedViews.has(vsid)) {
        this.app.viewMap.delete(vsid)
      }
    }
  }

  /**
   * Full reconciliation: removes any view not listed by the server
   * and not mounted (full sweep, ~5min).
   */
  sweep(activeViews: string[]) {
    const serverActive = new Set(activeViews)

    for (const vsid of this.app.viewMap.keys()) {
      if (!serverActive.has(vsid) && !this.mountedViews.has(vsid)) {
        this.app.viewMap.delete(vsid)
      }
    }
  }
}
