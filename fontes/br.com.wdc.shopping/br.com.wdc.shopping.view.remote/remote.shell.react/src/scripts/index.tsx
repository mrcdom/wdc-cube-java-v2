import React from 'react'
import * as ReactDOM from 'react-dom/client'

import bridge from './bridge'
import { Theme } from './swc'

import BrowserView from './views/BrowserView'
import SlotView from './views/SlotView'
import RootView from './views/RootView'
import LoginView from './views/LoginView'
import RestrictedView from './views/HomeView'
import CartView from './views/CartView'
import ReceiptView from './views/ReceiptView'
import ProductView from './views/ProductView'
import ProductsPanel from './views/home/ProductsPanel'
import PurchasesPanel from './views/home/PurchasesPanel'

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

const domContainer = document.querySelector('#root') as HTMLElement | null
if (domContainer) {
  domContainer.style.height = '100%'
  const root = ReactDOM.createRoot(domContainer)
  root.render(
    <Theme color="light" scale="medium" className="flex-col h-full overflow-hidden">
      {bridge.createBrowserView()}
    </Theme>,
  )
}
