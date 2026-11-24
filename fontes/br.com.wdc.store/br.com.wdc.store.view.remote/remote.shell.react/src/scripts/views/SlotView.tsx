import React from "react"
import bridge, { type ViewProps } from "@root/bridge"

export type SlotScope = {
  slot?: string
}

SlotView.VIEW_ID = "798574115fcd"

export default function SlotView({ vsid }: ViewProps) {
  const { scope } = bridge.bindView<SlotScope>(vsid)
  return scope.slot ? bridge.createView(scope.slot) : <></>
}
