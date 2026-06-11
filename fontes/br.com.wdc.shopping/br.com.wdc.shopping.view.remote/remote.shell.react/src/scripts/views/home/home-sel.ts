/**
 * Seletores CSS do módulo home (header, tabs, layout de split products+purchases).
 * Importado por HomeView.tsx e painéis internos — não referenciar home.scss diretamente.
 */
import GlobalSel from "@root/global-sel"

import "./home.scss"

const HomeSel = {
  ...GlobalSel,

  // :: Header (navbar)

  headerExitIcon: "wdc-header__exit-icon",
  headerGreeting: "wdc-header__greeting",
  headerGreetingLine: "wdc-header__greeting-line",
  headerBrandCol: "wdc-header__brand-col",
  headerBrandTitle: "wdc-header__brand-title",
  headerBrandSub: "wdc-header__brand-sub",

  // :: Products panel

  productsPanel: "wdc-products",
  productGrid: "wdc-products__grid",
  productCard: "wdc-products__card",
  productCardImage: "wdc-products__card-image-wrap",
  productCardImg: "wdc-products__card-img",
  productCardBody: "wdc-products__card-body",
  productCardName: "wdc-products__card-name",
  productCardPrice: "wdc-products__card-price",

  // :: Purchases panel

  purchasesPanel: "wdc-purchases",
  purchasesSlot: "wdc-purchases__slot",
  purchasesHeaderRow: "wdc-purchases__header-row",
  purchasesHeaderIcon: "wdc-purchases__header-icon",
  purchasesHeaderTitle: "wdc-purchases__header-title",
  purchasesHint: "wdc-purchases__hint",
  purchasesListContainer: "wdc-purchases__list-container",
  purchasesPagination: "wdc-purchases__pagination",
  purchasesPagePill: "wdc-purchases__page-pill",
  purchasesPageBtn: "wdc-purchases__page-btn",
  purchasesPageBtnIcon: "wdc-purchases__page-btn-icon",
  purchasesPageInfo: "wdc-purchases__page-info",
  purchasesItemCard: "wdc-purchases__item-card",
  purchasesItemLine1: "wdc-purchases__item-line1",
  purchasesItemId: "wdc-purchases__item-id",
  purchasesItemDate: "wdc-purchases__item-date",
  purchasesItemLine2: "wdc-purchases__item-line2",
  purchasesItemItems: "wdc-purchases__item-items",
  purchasesItemTotal: "wdc-purchases__item-total",
} as const

export default HomeSel
