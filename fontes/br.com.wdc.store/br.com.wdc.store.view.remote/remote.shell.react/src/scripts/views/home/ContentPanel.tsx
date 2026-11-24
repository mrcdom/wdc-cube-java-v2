import React from "react"
import bridge from "@root/bridge"
import { BasePanelClass } from "@root/utils/ViewUtils"

type ContentPanelProps = {
  contentViewId?: string
  productsPanelViewId?: string
}

class ContentPanelClass extends BasePanelClass<ContentPanelProps> {
  override render({ contentViewId, productsPanelViewId }: ContentPanelProps) {
    if (contentViewId) {
      return bridge.createView(contentViewId)
    }

    return <>{bridge.createView(productsPanelViewId)}</>
  }
}

export default BasePanelClass.FC(ContentPanelClass)
