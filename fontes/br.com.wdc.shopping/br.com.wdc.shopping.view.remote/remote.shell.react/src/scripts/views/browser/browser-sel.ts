/**
 * Seletores CSS do módulo browser (shell raiz).
 * Importado por BrowserView.tsx — não referenciar browser.scss diretamente nas views.
 */
import GlobalSel from '@root/global-sel'

import './browser.scss'

const BrowserSel = {
  ...GlobalSel,

  // :: Browser

  host: 'wdc-browser',
  container: 'wdc-browser__container',
  loadingBar: 'wdc-browser__loading-bar',
  content: 'wdc-browser__content',
} as const

export default BrowserSel
