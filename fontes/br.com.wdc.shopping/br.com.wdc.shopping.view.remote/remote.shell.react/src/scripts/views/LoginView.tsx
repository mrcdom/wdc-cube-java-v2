import React from 'react'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass } from '@root/utils/ViewUtils'
import { Button, FieldLabel, Textfield } from '@root/swc'

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
      <div className={`login-root ${className || ''}`}>
        {/* Left decorative panel (hidden on mobile) */}
        <div className="login-left-panel md-show">
          <div className="deco-circle deco-circle--1" />
          <div className="deco-circle deco-circle--2" />
          <div className="deco-circle deco-circle--3" />
          <div className="login-content-center">
            <div className="logo-box-lg mb-16">
              <i className="bi bi-bag-check login-logo-icon-lg"></i>
            </div>
            <h1 className="login-title-lg">WDC Shopping</h1>
            <p className="login-subtitle-lg">Sua compra certa na internet.</p>
            <div className="login-features-list">
              <div className="login-feature-row">
                <i className="bi bi-shield-check login-feature-icon"></i>
                <span className="login-feature-text">Compra segura</span>
              </div>
              <div className="login-feature-row">
                <i className="bi bi-truck login-feature-icon"></i>
                <span className="login-feature-text">Entrega rápida</span>
              </div>
              <div className="login-feature-row">
                <i className="bi bi-arrow-repeat login-feature-icon"></i>
                <span className="login-feature-text">Troca garantida</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right form panel */}
        <div className="login-form-panel login-card">
          <div className="login-form-content">
            {/* Mobile header */}
            <div className="login-mobile-logo">
              <div className="login-mobile-circle-1" />
              <div className="login-mobile-circle-2" />
              <div className="login-mobile-content">
                <div className="login-logo-box-sm">
                  <i className="bi bi-bag-check login-icon-sm"></i>
                </div>
                <div className="login-mobile-title">WDC Shopping</div>
                <div className="login-mobile-subtitle">Sua compra certa na internet.</div>
              </div>
            </div>

            {/* Welcome text */}
            <div className="login-welcome-wrap">
              <h2 className="login-welcome-title">Bem-vindo</h2>
              <p className="login-welcome-subtitle">Entre com suas credenciais para continuar</p>
            </div>

            {/* Error message */}
            {state.errorMessage && (
              <div className="alert-error mb-16">
                <span className="alert-error-icon">
                  <i className="bi bi-exclamation-circle"></i>
                </span>
                <span className="alert-error-text">{state.errorMessage}</span>
              </div>
            )}

            {/* User field */}
            <FieldLabel for="login-user" className="login-field-label">
              Usuário
            </FieldLabel>
            <Textfield
              id="login-user"
              placeholder="Digite seu usuário"
              className="login-field"
              disabled={state.loading || undefined}
              ref={this.userFieldRef}
            ></Textfield>

            {/* Password field */}
            <FieldLabel for="login-password" className="login-field-label">
              Senha
            </FieldLabel>
            <Textfield
              id="login-password"
              type="password"
              placeholder="Digite sua senha"
              className="login-field-password"
              disabled={state.loading || undefined}
              ref={this.passFieldRef}
            ></Textfield>

            {/* Login button */}
            <Button
              variant="accent"
              size="l"
              className="login-enter-btn"
              disabled={state.loading || undefined}
              pending={state.loading || undefined}
              ref={this.btnRef}
            >
              {state.loading ? 'Entrando...' : 'Entrar'}
            </Button>

            {/* Demo hint */}
            <div className="login-demo-hint">
              <span className="login-demo-text">Acesso demo: </span>
              <span className="login-demo-highlight">admin</span>
              <span className="login-demo-text"> / </span>
              <span className="login-demo-highlight">admin</span>
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
    if (el) {
      el.addEventListener('click', this.emitLogin)
    }
  }

  readonly onKeyDown = (e: Event) => {
    if ((e as KeyboardEvent).key === 'Enter') {
      this.emitLogin()
    }
  }

  readonly emitLogin = async () => {
    const { vsid } = this
    const userName = (this.userEl as any)?.value || ''
    const password = (this.passEl as any)?.value || ''
    bridge.setFormField(vsid, 'userName', userName)
    const encryptedPassword = await bridge.cipher(password)
    bridge.setFormField(vsid, 'password', encryptedPassword)
    bridge.submit(vsid, ON_LOGIN)
  }
}

export default BaseViewClass.FC(LoginViewClass, 'c677cda52d14')
