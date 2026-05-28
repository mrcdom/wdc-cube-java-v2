import React from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import app, { type ViewProps } from '@root/App'
import { BaseViewClass } from '@root/utils/ViewUtils'

export type RootViewState = {
  contentViewId?: string
  errorMessage?: string
}

class RootViewClass extends BaseViewClass<ViewProps, RootViewState> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { state } = this

    if (state.errorMessage) {
      return (
        <Box sx={{ m: 2 }}>
          <Alert severity="error">{state.errorMessage}</Alert>
        </Box>
      )
    }

    if (state.contentViewId) {
      return app.createView(state.contentViewId)
    }

    return (
      <Box className={className} sx={{ m: 2 }}>
        <Alert severity="error">Falta conteúdo para a página inicial</Alert>
      </Box>
    )
  }
}

export default BaseViewClass.FC(RootViewClass, 'f2d345c4a610')
