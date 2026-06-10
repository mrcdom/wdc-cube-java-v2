import React from 'react'
import clsx from 'clsx'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass } from '@root/utils/ViewUtils'
import { Button, FieldLabel, Textfield } from '@root/swc'
import Sel from './login-sel'

// :: Actions

const ON_LOGIN = 1

// :: View

export type LoginViewState = {
  errorMessage?: string
  loading?: boolean
}

class LoginViewClass extends BaseViewClass<ViewProps, LoginViewState> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { state } = this

    return (
      <div className={clsx(Sel.root, className)}>
        {/* Left decorative panel (hidden on mobile) */}
        <div className={clsx(Sel.leftPanel, Sel.mdShow)}>
          <div className={clsx(Sel.decoCircle, Sel.decoCircle1)} />
          <div className={clsx(Sel.decoCircle, Sel.decoCircle2)} />
          <div className={clsx(Sel.decoCircle, Sel.decoCircle3)} />
          <div className={Sel.contentCenter}>
            <div className={clsx(Sel.logoBoxLg, Sel.mb16)}>
              <i className={clsx('bi bi-bag-check', Sel.logoIconLg)}></i>
            </div>
            <h1 className={Sel.titleLg}>WDC Shopping</h1>
            <p className={Sel.subtitleLg}>Sua compra certa na internet.</p>
            <div className={Sel.featuresList}>
              <div className={Sel.featureRow}>
                <i className={clsx('bi bi-shield-check', Sel.featureIcon)}></i>
                <span className={Sel.featureText}>Compra segura</span>
              </div>
              <div className={Sel.featureRow}>
                <i className={clsx('bi bi-truck', Sel.featureIcon)}></i>
                <span className={Sel.featureText}>Entrega rápida</span>
              </div>
              <div className={Sel.featureRow}>
                <i className={clsx('bi bi-arrow-repeat', Sel.featureIcon)}></i>
                <span className={Sel.featureText}>Troca garantida</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right form panel */}
        <div className={Sel.formPanel}>
          <div className={Sel.formContent}>
            {/* Mobile header */}
            <div className={Sel.mobileLogo}>
              <div className={Sel.mobileCircle1} />
              <div className={Sel.mobileCircle2} />
              <div className={Sel.mobileContent}>
                <div className={Sel.logoBoxSm}>
                  <i className={clsx('bi bi-bag-check', Sel.iconSm)}></i>
                </div>
                <div className={Sel.mobileTitle}>WDC Shopping</div>
                <div className={Sel.mobileSubtitle}>Sua compra certa na internet.</div>
              </div>
            </div>

            {/* Welcome text */}
            <div className={Sel.welcomeWrap}>
              <h2 className={Sel.welcomeTitle}>Bem-vindo</h2>
              <p className={Sel.welcomeSubtitle}>Entre com suas credenciais para continuar</p>
            </div>

            {/* Error message */}
            {state.errorMessage && (
              <div className={clsx(Sel.alertError, Sel.mb16)}>
                <span className={Sel.alertErrorIcon}>
                  <i className="bi bi-exclamation-circle"></i>
                </span>
                <span className={Sel.alertErrorText}>{state.errorMessage}</span>
              </div>
            )}

            {/* User field */}
            <FieldLabel for="login-user" className={Sel.fieldLabel}>
              Usuário
            </FieldLabel>
            <Textfield
              id="login-user"
              placeholder="Digite seu usuário"
              className={Sel.field}
              disabled={state.loading || undefined}
              ref={this.userFieldRef}
            ></Textfield>

            {/* Password field */}
            <FieldLabel for="login-password" className={Sel.fieldLabel}>
              Senha
            </FieldLabel>
            <Textfield
              id="login-password"
              type="password"
              placeholder="Digite sua senha"
              className={Sel.fieldPassword}
              disabled={state.loading || undefined}
              ref={this.passFieldRef}
            ></Textfield>

            {/* Login button */}
            <Button
              variant="accent"
              size="l"
              className={Sel.enterBtn}
              disabled={state.loading || undefined}
              pending={state.loading || undefined}
              ref={this.btnRef}
            >
              {state.loading ? 'Entrando...' : 'Entrar'}
            </Button>

            {/* Demo hint */}
            <div className={Sel.demoHint}>
              <span className={Sel.demoText}>Acesso demo: </span>
              <span className={Sel.demoHighlight}>admin</span>
              <span className={Sel.demoText}> / </span>
              <span className={Sel.demoHighlight}>admin</span>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // :: Refs and Event Handlers

  private userEl: HTMLElement | null = null
  private passEl: HTMLElement | null = null

  readonly userFieldRef = (el: HTMLElement | null) => {
    if (this.userEl) {
      this.userEl.removeEventListener('keydown', this.onKeyDown)
    }
    this.userEl = el
    if (el) {
      el.addEventListener('keydown', this.onKeyDown)
    }
  }

  readonly passFieldRef = (el: HTMLElement | null) => {
    if (this.passEl) {
      this.passEl.removeEventListener('keydown', this.onKeyDown)
    }
    this.passEl = el
    if (el) {
      el.addEventListener('keydown', this.onKeyDown)
    }
  }

  readonly btnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener('click', this.emitLogin)
  }

  private readonly onKeyDown = (e: Event) => {
    if ((e as KeyboardEvent).key === 'Enter') {
      this.emitLogin()
    }
  }

  // :: Emissors

  readonly emitLogin = async () => {
    const { vsid } = this
    const userEl = this.userEl! as HTMLInputElement
    const passEl = this.passEl! as HTMLInputElement

    const userName = userEl.value || ''
    const password = passEl.value || ''
    bridge.setFormField(vsid, 'p.userName', userName)
    const encryptedPassword = await bridge.cipher(password)
    bridge.setFormField(vsid, 'p.password', encryptedPassword)
    bridge.submit(vsid, ON_LOGIN)
    passEl.value = ''
  }
}

export default BaseViewClass.FC(LoginViewClass, 'c677cda52d14')
