package br.com.wdc.shopping.view.android.impl

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter
import br.com.wdc.shopping.view.android.AbstractViewAndroid

class LoginViewAndroid(presenter: LoginPresenter) : AbstractViewAndroid<LoginPresenter>("login", presenter)
