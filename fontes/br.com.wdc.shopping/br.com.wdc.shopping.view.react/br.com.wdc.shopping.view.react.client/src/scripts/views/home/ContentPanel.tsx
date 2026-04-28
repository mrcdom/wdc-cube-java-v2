import React from 'react'
import Box from '@mui/material/Box'
import app from '@root/App'
import { BasePanelClass } from '@root/utils/ViewUtils'

type ContentPanelProps = {
  contentViewId?: string
  purchasesPanelViewId?: string
  productsPanelViewId?: string
}

class ContentPanelClass extends BasePanelClass<ContentPanelProps> {
  override render({ contentViewId, purchasesPanelViewId, productsPanelViewId }: ContentPanelProps) {
    if (contentViewId) {
      return app.createView(contentViewId, { style: { marginTop: '25px', marginLeft: '25px' } })
    }

    return (
      <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', mt: '25px', ml: '25px' }}>
        {app.createView(purchasesPanelViewId)}
        {app.createView(productsPanelViewId)}
      </Box>
    )
  }
}

export default BasePanelClass.FC(ContentPanelClass)
