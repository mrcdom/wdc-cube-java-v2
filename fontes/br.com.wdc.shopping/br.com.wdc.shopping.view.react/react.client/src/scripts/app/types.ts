import React from 'react'

export type BrowserViewState = {
  contentViewId?: string
  submitting?: boolean

  alertMessage?: {
    id: number
    args?: string[]
  }

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
