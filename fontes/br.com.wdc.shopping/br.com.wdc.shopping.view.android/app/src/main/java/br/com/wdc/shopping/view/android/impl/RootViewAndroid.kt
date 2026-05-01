package br.com.wdc.shopping.view.android.impl

import br.com.wdc.shopping.presentation.presenter.RootPresenter
import br.com.wdc.shopping.view.android.AbstractViewAndroid
import br.com.wdc.shopping.view.android.ComposeViewSlot

/**
 * Root view — holds the main content slot.
 * The presentation layer assigns child views (Login, Home, etc.) to the content slot.
 */
class RootViewAndroid(presenter: RootPresenter) : AbstractViewAndroid<RootPresenter>("root", presenter) {

    val contentSlot = ComposeViewSlot()

    override fun doUpdate() {
        contentSlot.setView(presenter.state.contentView)
        contentSlot.flush()
        super.doUpdate()
    }
}
