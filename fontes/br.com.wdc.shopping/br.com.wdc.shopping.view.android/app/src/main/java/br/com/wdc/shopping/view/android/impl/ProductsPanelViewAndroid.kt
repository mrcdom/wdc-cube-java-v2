package br.com.wdc.shopping.view.android.impl

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter
import br.com.wdc.shopping.view.android.AbstractViewAndroid

class ProductsPanelViewAndroid(presenter: ProductsPanelPresenter) :
    AbstractViewAndroid<ProductsPanelPresenter>("productsPanel", presenter)
