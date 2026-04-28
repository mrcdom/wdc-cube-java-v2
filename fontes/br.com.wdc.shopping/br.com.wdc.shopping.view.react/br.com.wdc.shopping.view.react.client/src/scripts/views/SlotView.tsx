import React from 'react'
import app, { type ViewProps } from '@root/App'

export type SlotViewState = {
  slot?: string
}

SlotView.VIEW_ID = '798574115fcd'

export default function SlotView({ vsid }: ViewProps) {
  const { state } = app.bindView<SlotViewState>(vsid)
  return state.slot ? app.createView(state.slot) : <></>
}
