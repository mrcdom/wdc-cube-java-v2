import React from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Container from '@mui/material/Container'
import { type ViewProps } from '@root/App'
import { BaseViewClass } from '@root/utils/ViewUtils'
import HeaderPanel from './home/HeaderPanel'
import ContentPanel from './home/ContentPanel'

// :: View

export type HomeViewState = {
  nickName: string
  cartItemCount: number
  productsPanelViewId?: string
  purchasesPanelViewId?: string
  contentViewId?: string
  errorMessage: string
}

class HomeViewClass extends BaseViewClass<ViewProps, HomeViewState> {
  // :: Renderes

  override render({ vsid }: ViewProps) {
    const { state } = this

    return (
      <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column', bgcolor: 'grey.100', overflow: 'hidden' }}>
        <HeaderPanel vsid={vsid} nickName={state.nickName} cartItemCount={state.cartItemCount} />
        {/* Error */}
        {state.errorMessage && (
          <Alert severity="error" sx={{ mt: 1 }}>
            {state.errorMessage}
          </Alert>
        )}
        <Box sx={{ flex: 1, overflowY: 'auto' }}>
          <Container maxWidth="lg" sx={{ pt: 2, pb: 4 }}>
            <ContentPanel
              contentViewId={state.contentViewId}
              productsPanelViewId={state.productsPanelViewId}
              purchasesPanelViewId={state.purchasesPanelViewId}
            />
          </Container>
        </Box>
      </Box>
    )
  }
}

export default BaseViewClass.FC(HomeViewClass, '473dbdd7a36a')
