package br.com.wdc.shopping.view.android.impl

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter
import br.com.wdc.shopping.view.android.AbstractViewAndroid

class PurchasesPanelViewAndroid(presenter: PurchasesPanelPresenter) :
    AbstractViewAndroid<PurchasesPanelPresenter>("purchasesPanel", presenter)
