package br.com.wdc.shopping.view.android.impl

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter
import br.com.wdc.shopping.view.android.AbstractViewAndroid

class CartViewAndroid(presenter: CartPresenter) : AbstractViewAndroid<CartPresenter>("cart", presenter)
