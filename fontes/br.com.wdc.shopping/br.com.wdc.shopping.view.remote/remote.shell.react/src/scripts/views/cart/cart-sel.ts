/**
 * Seletores CSS do módulo carrinho.
 * Importado por CartView.tsx — não referenciar cart.scss diretamente nas views.
 */
import GlobalSel from '@root/global-sel'

import './cart.scss'

const CartSel = {
  ...GlobalSel,

  // :: Empty state

  emptyIconBox: 'wdc-cart__empty-icon-box',
  emptyIcon: 'wdc-cart__empty-icon',
  emptyTitle: 'wdc-cart__empty-title',
  emptySubtitle: 'wdc-cart__empty-subtitle',

  // :: Footer

  footer: 'wdc-cart__footer',
  footerLabel: 'wdc-cart__footer-label',
  footerTotal: 'wdc-cart__footer-total',
  actionsRow: 'wdc-cart__actions-row',

  // :: Item

  itemRow: 'wdc-cart__item-row',
  itemName: 'wdc-cart__item-name',
  stepperRow: 'wdc-cart__stepper-row',
  stepperIcon: 'wdc-cart__stepper-icon',
  stepperValue: 'wdc-cart__stepper-value',
  itemSubtotal: 'wdc-cart__item-subtotal',
  removeIcon: 'wdc-cart__remove-icon',
} as const

export default CartSel
