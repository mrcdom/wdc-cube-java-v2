import React, { ReactNode } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import app, { type ViewProps } from '@root/App'
import { BaseViewClass } from '@root/utils/ViewUtils'

// :: Actions

const ON_ENTER = 1

// :: View

export type LoginViewState = {
  userName?: string
  password?: string
  errorMessage?: string
}

class LoginViewClass extends BaseViewClass<ViewProps, LoginViewState> {
  // :: Render

  override render({ className }: ViewProps): React.ReactNode {
    const { state } = this

    return (
      <Box
        className={className}
        sx={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: 'grey.100',
        }}
      >
        <Card elevation={4} sx={{ width: 420, p: 2 }}>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'center', mb: 3 }}>
              <Box component="img" src="images/big_logo.png" alt="Shopping Stela" sx={{ width: 240, height: 'auto' }} />
            </Box>
            <Typography variant="h5" align="center" gutterBottom sx={{ fontWeight: 'bold' }}>
              Acesso ao sistema
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }} onKeyDown={this.onKeyDown}>
              <TextField
                inputRef={this.usrInputRef}
                label="Usuário"
                type="text"
                name="login-usr"
                autoComplete="one-time-code"
                defaultValue={state.userName ?? ''}
                fullWidth
                size="small"
              />
              <TextField
                inputRef={this.pwdInputRef}
                label="Senha"
                type="password"
                name="login-pwd"
                autoComplete="one-time-code"
                defaultValue={state.password ?? ''}
                fullWidth
                size="small"
              />
              {!!state.errorMessage && (
                <Alert severity="error" sx={{ mt: 1 }}>
                  {state.errorMessage}
                </Alert>
              )}
              <Button
                type="button"
                variant="contained"
                color="primary"
                size="large"
                fullWidth
                sx={{ mt: 1 }}
                onClick={this.emitOnEnter}
              >
                LOGIN
              </Button>
            </Box>
          </CardContent>
        </Card>
      </Box>
    )
  }

  // :: Element Refs

  usrInputRef: React.RefObject<HTMLInputElement | null> = {
    current: null,
  }

  pwdInputRef: React.RefObject<HTMLInputElement | null> = {
    current: null,
  }

  // :: Emissors

  readonly emitOnEnter = async () => {
    const { vsid } = this
    const userName = this.usrInputRef.current?.value ?? ''
    const password = this.pwdInputRef.current?.value ?? ''
    app.setFormField(vsid, 'userName', userName)
    app.setFormField(vsid, 'password', await app.cipher(password))
    app.submit(vsid, ON_ENTER)
  }

  readonly onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      this.emitOnEnter()
    }
  }
}

export default BaseViewClass.FC(LoginViewClass, 'c677cda52d14')
