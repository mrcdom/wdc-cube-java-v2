import React from 'react'
import app, { type ViewProps, type ViewScope } from '@root/App'

const ZERO_DEPS: React.DependencyList = []

// :: ViewClass to Functional Component

export abstract class BaseViewClass<P extends ViewProps, S> {
  vsid!: string
  state!: S
  scope!: ViewScope
  initialized!: boolean

  abstract render(props: P, initial?: boolean): React.ReactNode

  protected forceUpdate() {
    this.scope.forceUpdate()
  }

  static FC<P extends ViewProps, S>(ctor: new (props: P) => BaseViewClass<P, S>, viewId: string) {
    const memoFactory = (props: P) => new ctor(props)
    View.VIEW_ID = viewId
    return View

    function View(props: P) {
      const memo = React.useMemo(memoFactory.bind(undefined, props), ZERO_DEPS)

      const { state, scope } = app.bindView<S>(props.vsid)
      memo.vsid = props.vsid
      memo.state = state
      memo.scope = scope

      if (memo.initialized) {
        return memo.render(props, false)
      }

      memo.initialized = true
      return memo.render(props, true)
    }
  }
}

// :: PanelClass to Functional Component

export abstract class BasePanelClass<P> {
  initialized!: boolean

  abstract render(props: P, initial?: boolean): React.ReactNode

  static FC<P>(ctor: new (props: P) => BasePanelClass<P>) {
    const memoFactory = (props: P) => new ctor(props)
    return Panel

    function Panel(props: P) {
      const memo = React.useMemo(memoFactory.bind(undefined, props), ZERO_DEPS)

      if (memo.initialized) {
        return memo.render(props, false)
      }

      memo.initialized = true
      return memo.render(props, true)
    }
  }
}
