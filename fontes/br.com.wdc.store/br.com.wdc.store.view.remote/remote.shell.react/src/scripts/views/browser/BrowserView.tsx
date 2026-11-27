import React, { ReactNode } from "react"
import clsx from "clsx"
import bridge, { type ViewProps, BROWSER_VID, type BrowserScope } from "@root/bridge"
import { BaseViewClass } from "@root/utils/ViewUtils"
import { t } from "@root/utils/I18n"
import { ActionButton, ProgressCircle } from "@root/swc"
import Sel from "./browser-sel"

// :: Actions

const ON_ALERT_OK = 1

// :: View

class BrowserViewClass extends BaseViewClass<ViewProps, BrowserScope> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { scope } = this

    React.useEffect(() => {
      bridge.onStart()
      return () => {
        bridge.onStop()
      }
    }, [])

    let connectionAlert: ReactNode
    if (scope.error) {
      connectionAlert = <ConnectionAlert delay={scope.error.delay} onReconnectNow={this.onRecconectNow} />
    }

    let rootView: ReactNode
    if (scope.contentViewId) {
      rootView = bridge.createView(scope.contentViewId)
    } else {
      rootView = (
        <div className={Sel.flexCenter} style={{ minHeight: "100vh" }}>
          <ProgressCircle size="l" indeterminate></ProgressCircle>
        </div>
      )
    }

    let alertView: ReactNode
    if (scope.alertId) {
      alertView = <AppAlert code={scope.alertId} args={scope.alertArgs ?? []} onDismiss={this.emitAlertOk} />
    }

    return (
      <div className={clsx(Sel.host, className)}>
        {scope.submitting && <div className={Sel.loadingBar} />}
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
      msgNode = t("alert.uriNotAccessible", { uri: props.args[0] ?? "" })
      detailMessage = props.args[1]
      break
    default:
      msgNode = props.args.length > 0 ? props.args[0] : t("error.unexpected")
  }

  return (
    <div className={Sel.alertError} style={{ margin: "16px" }}>
      <span className={Sel.alertErrorIcon}>
        <i className="bi bi-exclamation-circle"></i>
      </span>
      <div style={{ flex: 1 }}>
        <div className={clsx(Sel.fontBold, Sel.textSm)} style={{ color: "var(--app-error-text)" }}>
          {t("alert.title")}
        </div>
        <span className={Sel.alertErrorText}>{msgNode}</span>
        {detailMessage && (
          <div className={clsx(Sel.textXs, Sel.mt4)} style={{ color: "var(--app-error-text)" }}>
            {detailMessage}
          </div>
        )}
      </div>
      <ActionButton quiet size="s" onClick={props.onDismiss} style={{ marginLeft: "auto" }}>
        {t("alert.ok")}
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
    timeText =
      minutes > 0
        ? t("connection.reconnectingInMinutes", { minutes: String(minutes), seconds: String(seconds) })
        : t("connection.reconnectingInSeconds", { seconds: String(seconds) })
    showRetry = true
  } else {
    timeText = t("connection.reconnectingNow")
  }

  return (
    <div style={{ textAlign: "center" }}>
      <div
        className={Sel.alertError}
        style={{ display: "inline-flex", borderRadius: "0 0 8px 8px", padding: "6px 12px", margin: 0 }}
      >
        <span className={clsx(Sel.fontBold, Sel.textSm)} style={{ color: "var(--app-error-text)" }}>
          {t("connection.notConnected")}
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
            {t("connection.retryNow")}
          </span>
        )}
      </div>
    </div>
  )
}
