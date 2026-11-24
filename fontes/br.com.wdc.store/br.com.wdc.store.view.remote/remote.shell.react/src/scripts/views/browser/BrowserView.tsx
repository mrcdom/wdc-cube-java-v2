import React, { ReactNode } from "react"
import clsx from "clsx"
import bridge, { type ViewProps, BROWSER_VID, type BrowserViewState } from "@root/bridge"
import { BaseViewClass } from "@root/utils/ViewUtils"
import { ActionButton, ProgressCircle } from "@root/swc"
import Sel from "./browser-sel"

// :: Actions

const ON_ALERT_OK = 1

// :: View

class BrowserViewClass extends BaseViewClass<ViewProps, BrowserViewState> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { state } = this

    React.useEffect(() => {
      bridge.onStart()
      return () => {
        bridge.onStop()
      }
    }, [])

    let connectionAlert: ReactNode
    if (state.error) {
      connectionAlert = <ConnectionAlert delay={state.error.delay} onReconnectNow={this.onRecconectNow} />
    }

    let rootView: ReactNode
    if (state.contentViewId) {
      rootView = bridge.createView(state.contentViewId)
    } else {
      rootView = (
        <div className={Sel.flexCenter} style={{ minHeight: "100vh" }}>
          <ProgressCircle size="l" indeterminate></ProgressCircle>
        </div>
      )
    }

    let alertView: ReactNode
    if (state.alertId) {
      alertView = <AppAlert code={state.alertId} args={state.alertArgs ?? []} onDismiss={this.emitAlertOk} />
    }

    return (
      <div className={clsx(Sel.host, className)}>
        {state.submitting && <div className={Sel.loadingBar} />}
        {connectionAlert}
        {alertView}
        {rootView}
      </div>
    )
  }

  // :: Emissores

  readonly emitAlertOk = () => {
    const { vsid } = this
    bridge.submit(vsid, ON_ALERT_OK)
  }

  readonly onRecconectNow = () => {
    bridge.connect()
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
      msgNode = "The URI " + props.args[0] + " is not accessible"
      detailMessage = props.args[1]
      break
    default:
      msgNode = props.args.length > 0 ? props.args[0] : "An unexpected error occurred"
  }

  return (
    <div className={Sel.alertError} style={{ margin: "16px" }}>
      <span className={Sel.alertErrorIcon}>
        <i className="bi bi-exclamation-circle"></i>
      </span>
      <div style={{ flex: 1 }}>
        <div className={clsx(Sel.fontBold, Sel.textSm)} style={{ color: "var(--app-error-text)" }}>
          Warning!
        </div>
        <span className={Sel.alertErrorText}>{msgNode}</span>
        {detailMessage && (
          <div className={clsx(Sel.textXs, Sel.mt4)} style={{ color: "var(--app-error-text)" }}>
            {detailMessage}
          </div>
        )}
      </div>
      <ActionButton quiet size="s" onClick={props.onDismiss} style={{ marginLeft: "auto" }}>
        OK
      </ActionButton>
    </div>
  )
}

// :: Internal - ConnectionAlert

type ConnectionAlertProps = {
  delay: number
  onReconnectNow: () => void
}

function ConnectionAlert(props: ConnectionAlertProps) {
  let timeText: string
  let showRetry = false
  if (props.delay > 0) {
    let seconds = Math.floor(props.delay / 1000)
    let minutes = 0
    if (seconds > 60) {
      minutes = Math.floor(seconds / 60)
      seconds = seconds - minutes * 60
    }
    timeText = minutes > 0 ? `Reconnecting in ${minutes}m ${seconds}s...` : `Reconnecting in ${seconds}s...`
    showRetry = true
  } else {
    timeText = "Reconnecting now..."
  }

  return (
    <div style={{ textAlign: "center" }}>
      <div
        className={Sel.alertError}
        style={{ display: "inline-flex", borderRadius: "0 0 8px 8px", padding: "6px 12px", margin: 0 }}
      >
        <span className={clsx(Sel.fontBold, Sel.textSm)} style={{ color: "var(--app-error-text)" }}>
          Not connected.
        </span>{" "}
        <span className={Sel.textSm} style={{ color: "var(--app-error-text)" }}>
          {timeText}
        </span>
        {showRetry && (
          <span
            className={clsx(Sel.textSm, Sel.fontMedium, Sel.cursorPointer, Sel.ml4)}
            style={{ color: "var(--app-accent)", textDecoration: "underline" }}
            onClick={props.onReconnectNow}
          >
            Retry now
          </span>
        )}
      </div>
    </div>
  )
}
