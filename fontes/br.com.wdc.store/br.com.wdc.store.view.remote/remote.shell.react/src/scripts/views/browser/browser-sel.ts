/**
 * CSS selectors of the browser module (root shell).
 * Imported by BrowserView.tsx — do not reference browser.scss directly from the views.
 */
import GlobalSel from "@root/global-sel"

import "./browser.scss"

const BrowserSel = {
  ...GlobalSel,

  // :: Browser

  host: "wdc-browser",
  container: "wdc-browser__container",
  loadingBar: "wdc-browser__loading-bar",
  content: "wdc-browser__content",
} as const

export default BrowserSel
