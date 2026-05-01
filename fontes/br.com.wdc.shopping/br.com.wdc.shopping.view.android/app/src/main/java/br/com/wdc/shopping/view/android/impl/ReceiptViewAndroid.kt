package br.com.wdc.shopping.view.android.impl

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter
import br.com.wdc.shopping.view.android.AbstractViewAndroid

class ReceiptViewAndroid(presenter: ReceiptPresenter) : AbstractViewAndroid<ReceiptPresenter>("receipt", presenter)
