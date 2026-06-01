import type { ViewStateCoordinator } from './ViewStateCoordinator'

/**
 * GC de ViewScopes baseado em autoridade do servidor.
 *
 * O servidor informa periodicamente (via resposta ao ping) quais views estão ativas.
 * O cliente remove da viewMap qualquer view que:
 *   1. NÃO conste na lista do servidor
 *   2. NÃO esteja montada (bindView ativo)
 */
export class ViewGarbageCollector {
  private readonly app: ViewStateCoordinator
  private readonly mountedViews = new Set<string>()

  constructor(app: ViewStateCoordinator) {
    this.app = app
  }

  mount(vsid: string) {
    this.mountedViews.add(vsid)
  }

  unmount(vsid: string) {
    this.mountedViews.delete(vsid)
  }

  /**
   * Remove views específicas que o servidor liberou (eager GC, ~15s).
   * Só remove se não estiver montada.
   */
  release(releasedViews: string[]) {
    for (const vsid of releasedViews) {
      if (!this.mountedViews.has(vsid)) {
        this.app.viewMap.delete(vsid)
      }
    }
  }

  /**
   * Reconciliação completa: remove qualquer view não listada pelo servidor
   * e não montada (full sweep, ~5min).
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
