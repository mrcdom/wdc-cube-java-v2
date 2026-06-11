import React from "react"
import * as ReactDOM from "react-dom/client"
import clsx from "clsx"

import bridge from "./bridge"
import { Theme } from "./swc"
import GlobalSel from "./global-sel"

import BrowserView from "./views/browser/BrowserView"
import SlotView from "./views/SlotView"
import RootView from "./views/RootView"
import LoginView from "./views/login/LoginView"
import RestrictedView from "./views/home/HomeView"
import CartView from "./views/cart/CartView"
import ReceiptView from "./views/receipt/ReceiptView"
import ProductView from "./views/product/ProductView"
import ProductsPanel from "./views/home/ProductsPanel"
import PurchasesPanel from "./views/home/PurchasesPanel"

bridge.configure("~rr:")

bridge.registerComponents(
  BrowserView,
  SlotView,
  RootView,
  LoginView,
  RestrictedView,
  CartView,
  ReceiptView,
  ProductView,
  ProductsPanel,
  PurchasesPanel,
)

const domContainer = document.querySelector("#root") as HTMLElement | null
if (domContainer) {
  domContainer.style.height = "100%"
  const root = ReactDOM.createRoot(domContainer)
  root.render(
    <Theme color="light" scale="medium" className={clsx(GlobalSel.flexCol, GlobalSel.hFull, GlobalSel.overflowHidden)}>
      {bridge.createBrowserView()}
    </Theme>,
  )
}
