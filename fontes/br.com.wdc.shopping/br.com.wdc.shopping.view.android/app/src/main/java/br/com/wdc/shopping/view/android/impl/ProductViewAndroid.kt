package br.com.wdc.shopping.view.android.impl

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter
import br.com.wdc.shopping.view.android.AbstractViewAndroid

class ProductViewAndroid(presenter: ProductPresenter) : AbstractViewAndroid<ProductPresenter>("product", presenter)
