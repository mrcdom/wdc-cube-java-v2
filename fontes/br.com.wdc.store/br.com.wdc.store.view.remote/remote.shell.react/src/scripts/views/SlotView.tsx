import React from "react"
import bridge, { type ViewProps } from "@root/bridge"

export type SlotViewState = {
  slot?: string
}

SlotView.VIEW_ID = "798574115fcd"

export default function SlotView({ vsid }: ViewProps) {
  const { state } = bridge.bindView<SlotViewState>(vsid)
  return state.slot ? bridge.createView(state.slot) : <></>
}
