package br.com.wdc.shopping.view.android

import br.com.wdc.framework.cube.AbstractCubePresenter
import br.com.wdc.shopping.presentation.ShoppingApplication
import br.com.wdc.shopping.presentation.presenter.RootPresenter
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter
import br.com.wdc.shopping.view.android.impl.CartViewAndroid
import br.com.wdc.shopping.view.android.impl.HomeViewAndroid
import br.com.wdc.shopping.view.android.impl.LoginViewAndroid
import br.com.wdc.shopping.view.android.impl.ProductViewAndroid
import br.com.wdc.shopping.view.android.impl.ProductsPanelViewAndroid
import br.com.wdc.shopping.view.android.impl.PurchasesPanelViewAndroid
import br.com.wdc.shopping.view.android.impl.ReceiptViewAndroid
import br.com.wdc.shopping.view.android.impl.RootViewAndroid
import java.util.concurrent.ConcurrentHashMap

/**
 * Android-specific CubeApplication. Registers view factories and manages
 * dirty-view tracking for Compose recomposition.
 */
class ShoppingAndroidApplication : ShoppingApplication() {

    companion object {
        init {
            RootPresenter.createView = java.util.function.Function { p -> RootViewAndroid(p) }
            LoginPresenter.createView = java.util.function.Function { p -> LoginViewAndroid(p) }
            HomePresenter.createView = java.util.function.Function { p -> HomeViewAndroid(p) }
            CartPresenter.createView = java.util.function.Function { p -> CartViewAndroid(p) }
            ProductPresenter.createView = java.util.function.Function { p -> ProductViewAndroid(p) }
            ReceiptPresenter.createView = java.util.function.Function { p -> ReceiptViewAndroid(p) }
            ProductsPanelPresenter.createView = java.util.function.Function { p -> ProductsPanelViewAndroid(p) }
            PurchasesPanelPresenter.createView = java.util.function.Function { p -> PurchasesPanelViewAndroid(p) }
        }
    }

    private val attributeMap = ConcurrentHashMap<String, Any>()

    override fun updateHistory() {
        // Mark all active views dirty → AndroidRenderLoop will flush on main thread
        for (presenter in presenterMap.values) {
            if (presenter is AbstractCubePresenter<*>) {
                val view = presenter.view()
                if (view is AbstractViewAndroid<*>) {
                    AndroidRenderLoop.markDirty(view)
                }
            }
        }
    }

    override fun setAttribute(name: String, value: Any?): Any? {
        return if (value != null) attributeMap.put(name, value) else attributeMap.remove(name)
    }

    override fun getAttribute(name: String): Any? {
        return attributeMap[name]
    }

    override fun removeAttribute(name: String): Any? {
        return attributeMap.remove(name)
    }
}
