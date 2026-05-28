import React, { ReactNode } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Divider from '@mui/material/Divider'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import app, { type ViewProps } from '@root/App'
import { BaseViewClass } from '@root/utils/ViewUtils'
import ShoppingLogo from './home/ShoppingLogo'

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
        <Card
          elevation={0}
          sx={{
            width: 400,
            border: '1px solid',
            borderColor: 'grey.200',
            borderRadius: 3,
            overflow: 'hidden',
          }}
        >
          {/* Blue header with logo */}
          <Box
            sx={{
              bgcolor: 'primary.main',
              py: 3,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 1.5,
            }}
          >
            <ShoppingLogo height={38} />
          </Box>

          <CardContent sx={{ p: 4 }}>
            {/* Lock icon + title */}
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 3 }}>
              <Box
                sx={{
                  width: 44,
                  height: 44,
                  borderRadius: '50%',
                  bgcolor: 'primary.main',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  mb: 1.5,
                }}
              >
                <LockOutlinedIcon sx={{ color: '#fff', fontSize: 22 }} />
              </Box>
              <Typography variant="h6" sx={{ fontWeight: 'bold', color: 'text.primary' }}>
                Acesso ao sistema
              </Typography>
            </Box>

            {/* Form */}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }} onKeyDown={this.onKeyDown}>
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
                <Alert severity="error" sx={{ mt: 0.5 }}>
                  {state.errorMessage}
                </Alert>
              )}
              <Button
                type="button"
                variant="contained"
                color="primary"
                size="large"
                fullWidth
                sx={{ mt: 1, py: 1.2, borderRadius: 2, textTransform: 'none', fontWeight: 'bold', fontSize: '1rem' }}
                onClick={this.emitOnEnter}
              >
                Entrar
              </Button>
            </Box>

            {/* Demo credentials hint */}
            <Divider sx={{ mt: 3, mb: 2 }} />
            <Box
              sx={{
                bgcolor: 'grey.50',
                border: '1px dashed',
                borderColor: 'grey.300',
                borderRadius: 2,
                p: 1.5,
                textAlign: 'center',
              }}
            >
              <Typography variant="caption" sx={{ color: 'text.secondary', fontSize: '0.75rem' }}>
                Acesso demo: usuário <strong>admin</strong> / senha <strong>admin</strong>
              </Typography>
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
