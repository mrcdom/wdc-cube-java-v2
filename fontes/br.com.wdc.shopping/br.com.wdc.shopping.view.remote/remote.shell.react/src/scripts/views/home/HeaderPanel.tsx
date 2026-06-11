import React from "react"
import clsx from "clsx"
import bridge from "@root/bridge"
import { BasePanelClass } from "@root/utils/ViewUtils"
import { Theme, ActionButton } from "@root/swc"
import Sel from "./home-sel"

// :: Actions

const ON_EXIT = 1
const ON_OPEN_CART = 2

// :: Panel

type HeaderPanelProps = {
  vsid: string
  nickName: string
  cartItemCount: number
}

class HeaderPanelClass extends BasePanelClass<HeaderPanelProps> {
  vsid!: string

  override render({ vsid, nickName, cartItemCount }: HeaderPanelProps) {
    this.vsid = vsid

    return (
      <Theme color="dark" scale="medium" className={Sel.navbar}>
        {/* Left: exit + greeting */}
        <div className={Sel.navGroup}>
          <ActionButton quiet ref={this.exitBtnRef}>
            <span className={clsx("bi bi-box-arrow-left", Sel.textLg, Sel.textWhite)}></span>
          </ActionButton>
          <div className={clsx(Sel.smShow, Sel.flexCol, Sel.leadingTight)}>
            <span className={clsx(Sel.textXs, Sel.textWhite70, Sel.fontNormal)}>Bem-vindo(a),</span>
            <span className={clsx(Sel.textSm, Sel.fontSemibold, Sel.textWhite)}>{nickName}</span>
          </div>
        </div>

        {/* Center: logo */}
        <div className={Sel.navGroup}>
          <div className={Sel.logoBox}>
            <span className={clsx("bi bi-bag-check", Sel.textXl, Sel.textWhite)}></span>
          </div>
          <div className={clsx(Sel.flexCol, Sel.leadingTight)}>
            <span className={clsx(Sel.textBase, Sel.fontBold, Sel.textWhite, Sel.trackingTight)}>Shopping</span>
            <span className={clsx(Sel.smShow, Sel.textXs, Sel.textWhite65, Sel.fontNormal, Sel.trackingWide)}>
              By WeDoCode
            </span>
          </div>
        </div>

        {/* Right: cart button */}
        <div className={Sel.flexItemsCenter}>
          <ActionButton quiet className={Sel.relative} ref={this.cartBtnRef}>
            <span className={clsx("bi bi-bag", Sel.textXl, Sel.textWhite)}></span>
            <span className={clsx(Sel.smShow, Sel.textSm, Sel.textWhite, Sel.fontMedium, Sel.ml6)}>Carrinho</span>
            <span className={Sel.cartBadge}>{cartItemCount}</span>
          </ActionButton>
        </div>
      </Theme>
    )
  }

  // :: Refs

  readonly exitBtnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener("click", this.emitExit)
  }

  readonly cartBtnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener("click", this.emitOpenCart)
  }

  // :: Emissors

  readonly emitOpenCart = () => {
    const { vsid } = this
    bridge.submit(vsid, ON_OPEN_CART)
  }

  readonly emitExit = () => {
    const { vsid } = this
    bridge.submit(vsid, ON_EXIT)
  }
}

export default BasePanelClass.FC(HeaderPanelClass)
