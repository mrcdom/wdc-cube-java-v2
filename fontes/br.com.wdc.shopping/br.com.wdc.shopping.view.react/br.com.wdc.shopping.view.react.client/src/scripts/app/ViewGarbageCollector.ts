import type { Application } from './Application'
import { ViewScope } from './ViewScope'
import { BROWSER_VSID } from './constants'

export class ViewGarbageCollector {
  private readonly app: Application
  private readonly garbageViewMap = new Map<string, ViewScope>()
  private readonly holdCount = new Map<string, number>()
  private readonly autoHeldByScope = new Map<string, Set<string>>()

  private taskHandler = 0

  constructor(app: Application) {
    this.app = app
  }

  hold(vsid: string) {
    this.holdCount.set(vsid, (this.holdCount.get(vsid) ?? 0) + 1)
    this.recover(vsid)
  }

  release(vsid: string) {
    const count = (this.holdCount.get(vsid) ?? 0) - 1
    if (count <= 0) {
      this.holdCount.delete(vsid)
    } else {
      this.holdCount.set(vsid, count)
    }
  }

  /**
   * Updates automatic holds based on ViewId references in a ViewScope's state.
   * Releases holds for references that were removed, holds new references.
   */
  updateAutoHolds(scope: ViewScope) {
    const scopeId = scope.getId()
    const prevRefs = this.autoHeldByScope.get(scopeId) ?? new Set()
    const newRefs = scope.getReferencedViewIds()

    for (const vsid of prevRefs) {
      if (!newRefs.has(vsid)) {
        this.release(vsid)
      }
    }

    for (const vsid of newRefs) {
      if (!prevRefs.has(vsid)) {
        this.hold(vsid)
      }
    }

    if (newRefs.size > 0) {
      this.autoHeldByScope.set(scopeId, newRefs)
    } else {
      this.autoHeldByScope.delete(scopeId)
    }
  }

  /**
   * Releases all automatic holds for a scope being collected.
   */
  private releaseAutoHolds(scopeId: string) {
    const refs = this.autoHeldByScope.get(scopeId)
    if (refs) {
      for (const vsid of refs) {
        this.release(vsid)
      }
      this.autoHeldByScope.delete(scopeId)
    }
  }

  mark(scope: ViewScope) {
    const vsid = scope.getId()
    if (vsid !== BROWSER_VSID) {
      this.garbageViewMap.set(vsid, scope)
      this.scheduleCollection()
    }
  }

  recover(vsid: string) {
    const scope = this.garbageViewMap.get(vsid)
    if (scope) {
      this.app.viewMap.set(vsid, scope)
      this.garbageViewMap.delete(vsid)
      this.cancelSchedulingIfEmpty()
      return scope
    } else {
      return null
    }
  }

  private scheduleCollection() {
    clearTimeout(this.taskHandler)
    this.taskHandler = setTimeout(this.doCollection, 4000)
  }

  private readonly doCollection = () => {
    clearTimeout(this.taskHandler)

    if (this.garbageViewMap.size > 0) {
      const toCollect: string[] = []

      this.garbageViewMap.forEach((_, vsid) => {
        if (!this.holdCount.has(vsid)) {
          toCollect.push(vsid)
        }
      })

      for (const vsid of toCollect) {
        this.releaseAutoHolds(vsid)
        this.app.viewMap.delete(vsid)
        this.garbageViewMap.delete(vsid)
      }
    }
  }

  private cancelSchedulingIfEmpty() {
    if (this.garbageViewMap.size === 0) {
      clearTimeout(this.taskHandler)
    }
  }
}
