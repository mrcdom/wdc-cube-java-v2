import React, { ReactNode } from 'react'
import Alert from '@mui/material/Alert'
import AlertTitle from '@mui/material/AlertTitle'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import LinearProgress from '@mui/material/LinearProgress'
import Link from '@mui/material/Link'
import Typography from '@mui/material/Typography'
import app, { type ViewProps, BROWSER_VID, type BrowserViewState } from '@root/App'
import { BaseViewClass } from '@root/utils/ViewUtils'

// :: Actions

const ON_ALERT_OK = 1

// :: View

class BrowserViewClass extends BaseViewClass<ViewProps, BrowserViewState> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { state } = this

    React.useEffect(() => {
      app.onStart()
      return () => {
        app.onStop()
      }
    }, [])

    let connectionAlert: ReactNode
    if (state.error) {
      connectionAlert = <ConnectionAlert delay={state.error.delay} onReconnectNow={this.onRecconectNow} />
    }

    let rootView: ReactNode
    if (state.contentViewId) {
      rootView = app.createView(state.contentViewId)
    } else {
      rootView = (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
          <CircularProgress />
        </Box>
      )
    }

    let alertView: ReactNode
    if (state.alertMessage) {
      alertView = (
        <AppAlert code={state.alertMessage.id} args={state.alertMessage.args ?? []} onDismiss={this.emitAlertOk} />
      )
    }

    return (
      <Box className={className}>
        {state.submitting && (
          <LinearProgress
            sx={{
              position: 'fixed',
              top: 0,
              left: 0,
              right: 0,
              zIndex: 9999,
            }}
          />
        )}
        {connectionAlert}
        {alertView}
        {rootView}
      </Box>
    )
  }

  // :: Emissores

  readonly emitAlertOk = () => {
    const { vsid } = this
    app.submit(vsid, ON_ALERT_OK)
  }

  readonly onRecconectNow = () => {
    app.connect()
  }
}

export default BaseViewClass.FC(BrowserViewClass, BROWSER_VID)

// :: Internal - AppAlert

type AppAlertProps = {
  code: number
  args: string[]
  onDismiss?: () => void
}

function AppAlert(props: AppAlertProps) {
  let msgNode: React.ReactNode | null = null
  let detailMessage: string | null = null
  switch (props.code) {
    case -1:
      msgNode = props.args[0]
      detailMessage = props.args[1]
      break
    case -2:
      msgNode = 'A URI ' + props.args[0] + ' não está acessível'
      detailMessage = props.args[1]
      break
    default:
      msgNode = props.args.length > 0 ? props.args[0] : 'Ocorreu um erro não esperado'
  }

  return (
    <Alert
      severity="warning"
      sx={{ m: 2 }}
      action={
        <Button color="inherit" size="small" onClick={props.onDismiss}>
          Ok
        </Button>
      }
    >
      <AlertTitle>Aviso!</AlertTitle>
      {msgNode}
      {detailMessage && (
        <Typography variant="body2" sx={{ mt: 1 }}>
          {detailMessage}
        </Typography>
      )}
    </Alert>
  )
}

// :: Internal - ConnectionAlert

type ConnectionAlertProps = {
  delay: number
  onReconnectNow: () => void
}

function ConnectionAlert(props: ConnectionAlertProps) {
  let timeElm: React.ReactNode, retryElm: React.ReactNode
  if (props.delay > 0) {
    let seconds = Math.floor(props.delay / 1000)
    let minutes = 0
    if (seconds > 60) {
      minutes = Math.floor(seconds / 60)
      seconds = seconds - minutes * 60
    }

    timeElm = (
      <Typography component="span" variant="body2">
        {minutes > 0 ? `Conectando em ${minutes}m e ${seconds}s...` : `Conectando em ${seconds}s...`}
      </Typography>
    )
    retryElm = (
      <Link
        component="button"
        variant="body2"
        underline="always"
        onClick={props.onReconnectNow}
        sx={{ cursor: 'pointer', ml: 1 }}
      >
        Tentar agora
      </Link>
    )
  } else {
    timeElm = (
      <Typography component="span" variant="body2">
        Conectando agora...
      </Typography>
    )
  }

  return (
    <Box sx={{ textAlign: 'center' }}>
      <Alert
        severity="warning"
        icon={false}
        sx={{
          display: 'inline-flex',
          borderRadius: '0 0 4px 4px',
          py: 0,
          px: 1.5,
        }}
      >
        <Typography component="span" variant="body2" sx={{ fontWeight: 'bold' }}>
          Não conectado.
        </Typography>{' '}
        {timeElm} {retryElm}.
      </Alert>
    </Box>
  )
}
