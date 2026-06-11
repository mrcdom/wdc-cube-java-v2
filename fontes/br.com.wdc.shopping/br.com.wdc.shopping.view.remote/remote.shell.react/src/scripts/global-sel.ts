/**
 * Seletores CSS globais — wrappers tipados para classes utilitárias do WDC Shopping.
 *
 * Autocomplete, erros de compilação em vez de bugs silenciosos, refatoração segura.
 *
 * Uso:
 * ```tsx
 * import clsx from 'clsx'
 * import GlobalSel from '@root/global-sel'
 *
 * <div className={clsx(GlobalSel.flexCol, GlobalSel.flexGrow, GlobalSel.gap16)}>
 * ```
 */

const GlobalSel = {
  // =========================================================================
  // :: Display & Visibility
  // =========================================================================

  hidden: "hidden",
  block: "block",
  inlineFlex: "inline-flex",
  overflowHidden: "overflow-hidden",
  overflowYAuto: "overflow-y-auto",
  overflowAuto: "overflow-auto",

  // =========================================================================
  // :: Flex Layout
  // =========================================================================

  flex: "flex",
  flexCol: "flex-col",
  flexCenter: "flex-center",
  flexColCenter: "flex-col-center",
  flexItemsCenter: "flex-items-center",
  flexBetween: "flex-between",
  flexEnd: "flex-end",
  flexColStretch: "flex-col-stretch",

  flex1: "flex-1",
  flexGrow: "flex-grow",
  flexShrink0: "flex-shrink-0",
  flex1Scroll: "flex-1-scroll",
  flexWrap: "flex-wrap",

  // Gaps
  gap4: "gap-4",
  gap6: "gap-6",
  gap8: "gap-8",
  gap10: "gap-10",
  gap12: "gap-12",
  gap16: "gap-16",
  gap20: "gap-20",
  gap24: "gap-24",

  // Alignment modifiers
  itemsCenter: "items-center",
  itemsStart: "items-start",
  itemsEnd: "items-end",
  itemsStretch: "items-stretch",
  justifyCenter: "justify-center",
  justifyBetween: "justify-between",
  justifyEnd: "justify-end",
  justifyStart: "justify-start",

  // =========================================================================
  // :: Spacing — Padding
  // =========================================================================

  p0: "p-0",
  p4: "p-4",
  p8: "p-8",
  p10: "p-10",
  p12: "p-12",
  p16: "p-16",
  p20: "p-20",
  p24: "p-24",
  p32: "p-32",
  p48: "p-48",

  px8: "px-8",
  px12: "px-12",
  px16: "px-16",
  px20: "px-20",
  px24: "px-24",

  py8: "py-8",
  py10: "py-10",
  py12: "py-12",
  py16: "py-16",
  py20: "py-20",
  py24: "py-24",

  pt8: "pt-8",
  pt16: "pt-16",
  pt24: "pt-24",
  pb8: "pb-8",
  pb16: "pb-16",

  // =========================================================================
  // :: Spacing — Margin
  // =========================================================================

  m0: "m-0",
  mxAuto: "mx-auto",
  mt4: "mt-4",
  mt8: "mt-8",
  mt12: "mt-12",
  mt16: "mt-16",
  mt20: "mt-20",
  mt24: "mt-24",
  mt32: "mt-32",
  mb4: "mb-4",
  mb6: "mb-6",
  mb8: "mb-8",
  mb10: "mb-10",
  mb12: "mb-12",
  mb16: "mb-16",
  mb20: "mb-20",
  mb24: "mb-24",
  mb28: "mb-28",
  ml4: "ml-4",
  ml6: "ml-6",
  ml8: "ml-8",
  mr4: "mr-4",
  mr6: "mr-6",
  mr8: "mr-8",
  mtAuto: "mt-auto",

  // =========================================================================
  // :: Sizing
  // =========================================================================

  wFull: "w-full",
  hFull: "h-full",
  minH0: "min-h-0",
  minHFull: "min-h-full",
  minW0: "min-w-0",
  maxW280: "max-w-280",
  maxW320: "max-w-320",
  maxW460: "max-w-460",
  maxW900: "max-w-900",

  w28: "w-28",
  h28: "h-28",
  w40: "w-40",
  h40: "h-40",
  w48: "w-48",
  h48: "h-48",
  w80: "w-80",
  h80: "h-80",
  w100: "w-100",
  h100: "h-100",
  minW24: "min-w-24",
  minW28: "min-w-28",
  maxW160: "max-w-160",
  maxH160: "max-h-160",

  // =========================================================================
  // :: Typography — Font size
  // =========================================================================

  textXxs: "text-xxs",
  textXs: "text-xs",
  textSm: "text-sm",
  textBase: "text-base",
  textLg: "text-lg",
  textXl: "text-xl",
  text2xl: "text-2xl",
  text3xl: "text-3xl",
  text4xl: "text-4xl",

  // =========================================================================
  // :: Typography — Font weight
  // =========================================================================

  fontNormal: "font-normal",
  fontMedium: "font-medium",
  fontSemibold: "font-semibold",
  fontBold: "font-bold",
  fontExtrabold: "font-extrabold",

  // =========================================================================
  // :: Typography — Alignment, line-height, spacing
  // =========================================================================

  textCenter: "text-center",
  textLeft: "text-left",
  textRight: "text-right",

  leadingTight: "leading-tight",
  leadingNormal: "leading-normal",
  leadingRelaxed: "leading-relaxed",

  trackingTight: "tracking-tight",
  trackingTighter: "tracking-tighter",
  trackingWide: "tracking-wide",

  // =========================================================================
  // :: Typography — Colors
  // =========================================================================

  textPrimary: "text-primary",
  textSecondary: "text-secondary",
  textAccent: "text-accent",
  textWhite: "text-white",
  textWhite70: "text-white-70",
  textWhite65: "text-white-65",
  textWhite80: "text-white-80",

  textSmSecondary: "text-sm-secondary",
  textXsSecondary: "text-xs-secondary",

  // =========================================================================
  // :: Borders & Radius
  // =========================================================================

  border: "border",
  borderT: "border-t",
  borderB: "border-b",
  borderL: "border-l",
  borderNone: "border-none",
  borderWhite20: "border-white-20",
  borderDashed: "border-dashed",
  borderDotted: "border-dotted",
  borderTAccent: "border-t-accent",

  rounded: "rounded",
  roundedSm: "rounded-sm",
  rounded10: "rounded-10",
  rounded12: "rounded-12",
  rounded20: "rounded-20",
  roundedFull: "rounded-full",

  // =========================================================================
  // :: Background & Shadows
  // =========================================================================

  bgSurface: "bg-surface",
  bgDefault: "bg-default",
  bgAccentLight: "bg-accent-light",
  bgWhite: "bg-white",
  bgNone: "bg-none",
  bgTransparent: "bg-transparent",

  bgGradientPrimary: "bg-gradient-primary",
  bgGradientPrimaryExtended: "bg-gradient-primary-extended",

  bgWhite04: "bg-white-04",
  bgWhite05: "bg-white-05",
  bgWhite06: "bg-white-06",
  bgWhite12: "bg-white-12",
  bgWhite15: "bg-white-15",

  shadowSm: "shadow-sm",
  shadowMd: "shadow-md",
  shadowLg: "shadow-lg",
  shadowBlue: "shadow-blue",

  backdropBlurSm: "backdrop-blur-sm",
  backdropBlurMd: "backdrop-blur-md",

  // =========================================================================
  // :: Position
  // =========================================================================

  relative: "relative",
  absolute: "absolute",
  z1: "z-1",
  z10: "z-10",

  // =========================================================================
  // :: Cursor & Interaction
  // =========================================================================

  cursorPointer: "cursor-pointer",
  pointerEventsNone: "pointer-events-none",

  // =========================================================================
  // :: Transitions
  // =========================================================================

  transitionColors: "transition-colors",
  transitionTransform: "transition-transform",
  transitionAll: "transition-all",
  transitionTransformSpring: "transition-transform-spring",

  // =========================================================================
  // :: Text utilities
  // =========================================================================

  whitespaceNowrap: "whitespace-nowrap",
  textEllipsis: "text-ellipsis",
  textUppercase: "text-uppercase",

  objectContain: "object-contain",
  aspect1: "aspect-1",
  boxBorder: "box-border",

  // =========================================================================
  // :: Responsive
  // =========================================================================

  smShow: "sm-show",
  mdShow: "md-show",
  mdHide: "md-hide",
  mdRow: "md-row",
  mdGrow0: "md-grow-0",

  // =========================================================================
  // :: Shared component selectors (navbar, tabs, empty-state, etc.)
  // =========================================================================

  navbar: "wdc-navbar",
  navGroup: "wdc-navbar__group",

  tabNav: "wdc-tab-nav",
  tabItem: "wdc-tab-nav__item",
  tabItemActive: "wdc-tab-nav__item--active",
  tabItemInactive: "wdc-tab-nav__item--inactive",
  tabIndicator: "wdc-tab-nav__indicator",

  emptyState: "wdc-empty-state",

  logoBox: "wdc-logo-box",
  logoBoxLg: "wdc-logo-box--lg",

  cartBadge: "wdc-cart-badge",

  alertError: "wdc-alert--error",
  alertErrorIcon: "wdc-alert--error__icon",
  alertErrorText: "wdc-alert--error__text",

  alertSuccess: "wdc-alert--success",
  alertSuccessIcon: "wdc-alert--success__icon",
  alertSuccessText: "wdc-alert--success__text",

  pageScrollRoot: "wdc-page__scroll-root",
  pageWrapper: "wdc-page__wrapper",

  cardPanel: "wdc-card",
  cardPanelLg: "wdc-card--lg",
  cardHeaderRow: "wdc-card__header-row",
  cardHeaderIconBox: "wdc-card__header-icon-box",
  cardHeaderIcon: "wdc-card__header-icon",
  cardHeaderTitle: "wdc-card__header-title",
  cardHeaderSubtitle: "wdc-card__header-subtitle",

  totalRow: "wdc-total-row",

  decoCircle: "wdc-deco-circle",
  decoCircle1: "wdc-deco-circle--1",
  decoCircle2: "wdc-deco-circle--2",
  decoCircle3: "wdc-deco-circle--3",
} as const

export default GlobalSel
