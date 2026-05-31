/**
 * Seletores CSS do módulo de login.
 * Importado por LoginView.tsx — não referenciar login.scss diretamente nas views.
 */
import GlobalSel from '@root/global-sel'

import './login.scss'

const LoginSel = {
  ...GlobalSel,

  // :: Layout

  root: 'wdc-login',
  leftPanel: 'wdc-login__left-panel',
  contentCenter: 'wdc-login__content-center',
  logoIconLg: 'wdc-login__logo-icon-lg',
  titleLg: 'wdc-login__title-lg',
  subtitleLg: 'wdc-login__subtitle-lg',
  featuresList: 'wdc-login__features-list',
  featureRow: 'wdc-login__feature-row',
  featureIcon: 'wdc-login__feature-icon',
  featureText: 'wdc-login__feature-text',

  // :: Form panel

  formPanel: 'wdc-login__form-panel',
  formContent: 'wdc-login__form-content',

  // :: Mobile header

  mobileLogo: 'wdc-login__mobile-logo',
  mobileCircle1: 'wdc-login__mobile-circle-1',
  mobileCircle2: 'wdc-login__mobile-circle-2',
  mobileContent: 'wdc-login__mobile-content',
  logoBoxSm: 'wdc-login__logo-box-sm',
  iconSm: 'wdc-login__icon-sm',
  mobileTitle: 'wdc-login__mobile-title',
  mobileSubtitle: 'wdc-login__mobile-subtitle',

  // :: Welcome + fields

  welcomeWrap: 'wdc-login__welcome-wrap',
  welcomeTitle: 'wdc-login__welcome-title',
  welcomeSubtitle: 'wdc-login__welcome-subtitle',
  fieldLabel: 'wdc-login__field-label',
  field: 'wdc-login__field',
  fieldPassword: 'wdc-login__field-password',
  enterBtn: 'wdc-login__enter-btn',

  // :: Demo hint

  demoHint: 'wdc-login__demo-hint',
  demoText: 'wdc-login__demo-text',
  demoHighlight: 'wdc-login__demo-highlight',
} as const

export default LoginSel
