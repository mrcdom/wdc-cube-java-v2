import React from "react"

export type BrowserViewState = {
  contentViewId?: string
  submitting?: boolean

  /** Server alert: 0 = none, -1 = unexpected error */
  alertId?: number
  alertArgs?: string[]

  error?: {
    delay: number
    cause: unknown
    numAttempt: number
  }
}

export type IViewFactory = (vid: string, props: Record<string, unknown>) => React.ReactNode

export type ViewProps = {
  vsid: string
  className?: string
  style?: React.CSSProperties
}

export type ViewComponent = React.ComponentType<ViewProps> & { VIEW_ID: string }

export type FormMapType = {
  requestId?: number
  event?: string[]
} & Record<string, unknown>
