/**
 * Seletores CSS do módulo de produto (detalhe).
 * Importado por ProductView.tsx — não referenciar product.scss diretamente nas views.
 */
import GlobalSel from "@root/global-sel"

import "./product.scss"

const ProductSel = {
  ...GlobalSel,

  // :: Product detail

  title: "wdc-product__title",
  divider: "wdc-product__divider",
  descCard: "wdc-product__desc-card",
  descText: "wdc-product__desc-text",
  priceImageRow: "wdc-product__price-image-row",
  priceCol: "wdc-product__price-col",
  priceBadge: "wdc-product__price-badge",
  qtyRow: "wdc-product__qty-row",
  qtyLabel: "wdc-product__qty-label",
  qtyValue: "wdc-product__qty-value",
  imageBox: "wdc-product__image-box",
  image: "wdc-product__image",
  actionsRow: "wdc-product__actions-row",
} as const

export default ProductSel
