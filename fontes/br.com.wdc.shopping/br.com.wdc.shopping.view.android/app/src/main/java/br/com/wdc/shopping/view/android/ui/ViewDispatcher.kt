package br.com.wdc.shopping.view.android.ui

import androidx.compose.runtime.Composable
import br.com.wdc.shopping.view.android.AbstractViewAndroid
import br.com.wdc.shopping.view.android.impl.CartViewAndroid
import br.com.wdc.shopping.view.android.impl.HomeViewAndroid
import br.com.wdc.shopping.view.android.impl.LoginViewAndroid
import br.com.wdc.shopping.view.android.impl.ProductViewAndroid
import br.com.wdc.shopping.view.android.impl.ProductsPanelViewAndroid
import br.com.wdc.shopping.view.android.impl.PurchasesPanelViewAndroid
import br.com.wdc.shopping.view.android.impl.ReceiptViewAndroid
import br.com.wdc.shopping.view.android.impl.RootViewAndroid
import br.com.wdc.framework.cube.CubeView

/**
 * Dispatches a CubeView to its corresponding Composable renderer.
 * This is the single point where the Cube MVP view tree maps to Compose UI.
 */
@Composable
fun RenderView(view: CubeView?) {
    if (view == null) return

    // Read revision to trigger recomposition on state changes
    if (view is AbstractViewAndroid<*>) {
        view.revision.value // read for Compose observation
    }

    when (view) {
        is RootViewAndroid -> RootComposable(view)
        is LoginViewAndroid -> LoginComposable(view)
        is HomeViewAndroid -> HomeComposable(view)
        is CartViewAndroid -> CartComposable(view)
        is ProductViewAndroid -> ProductComposable(view)
        is ReceiptViewAndroid -> ReceiptComposable(view)
        is ProductsPanelViewAndroid -> ProductsPanelComposable(view)
        is PurchasesPanelViewAndroid -> PurchasesPanelComposable(view)
    }
}
