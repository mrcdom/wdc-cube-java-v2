import React from 'react'
import bridge from '@root/bridge'
import { BasePanelClass } from '@root/utils/ViewUtils'
import { Theme, ActionButton } from '@root/swc'

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
      <Theme color="dark" scale="medium" className="navbar">
        {/* Left: exit + greeting */}
        <div className="nav-group">
          <ActionButton quiet ref={this.exitBtnRef}>
            <span className="bi bi-box-arrow-left text-lg text-white"></span>
          </ActionButton>
          <div className="sm-show flex-col leading-tight">
            <span className="text-xs text-white-70 font-normal">Bem-vindo(a),</span>
            <span className="text-sm font-semibold text-white">{nickName}</span>
          </div>
        </div>

        {/* Center: logo */}
        <div className="nav-group">
          <div className="logo-box">
            <span className="bi bi-bag-check text-xl text-white"></span>
          </div>
          <div className="flex-col leading-tight">
            <span className="text-base font-bold text-white tracking-tight">Shopping</span>
            <span className="sm-show text-xs text-white-65 font-normal tracking-wide">By WeDoCode</span>
          </div>
        </div>

        {/* Right: cart button */}
        <div className="flex-items-center">
          <ActionButton quiet className="relative" ref={this.cartBtnRef}>
            <span className="bi bi-bag text-xl text-white"></span>
            <span className="sm-show text-sm text-white font-medium ml-6">Carrinho</span>
            <span className="cart-badge">{cartItemCount}</span>
          </ActionButton>
        </div>
      </Theme>
    )
  }

  // :: Refs

  readonly exitBtnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener('click', this.emitExit)
  }

  readonly cartBtnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener('click', this.emitOpenCart)
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
