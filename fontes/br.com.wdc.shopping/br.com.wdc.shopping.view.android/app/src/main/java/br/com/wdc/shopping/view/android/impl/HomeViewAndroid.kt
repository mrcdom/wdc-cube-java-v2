package br.com.wdc.shopping.view.android.impl

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter
import br.com.wdc.shopping.view.android.AbstractViewAndroid
import br.com.wdc.shopping.view.android.ComposeViewSlot

class HomeViewAndroid(presenter: HomePresenter) : AbstractViewAndroid<HomePresenter>("home", presenter) {

    val contentSlot = ComposeViewSlot()
    val productsPanelSlot = ComposeViewSlot()
    val purchasesPanelSlot = ComposeViewSlot()

    override fun doUpdate() {
        contentSlot.setView(presenter.state.contentView)
        productsPanelSlot.setView(presenter.state.productsPanelView)
        purchasesPanelSlot.setView(presenter.state.purchasesPanelView)
        contentSlot.flush()
        productsPanelSlot.flush()
        purchasesPanelSlot.flush()
        super.doUpdate()
    }
}
