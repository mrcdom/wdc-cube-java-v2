package br.com.wdc.shopping.view.android.ui

import android.util.Log
import br.com.wdc.framework.commons.concurrent.ScheduledExecutor
import br.com.wdc.shopping.presentation.ShoppingApplication

fun runAsync(app: ShoppingApplication? = null, action: () -> Unit) {
    val executor = ScheduledExecutor.BEAN.get() ?: run {
        Log.e("RunAsync", "ScheduledExecutor not initialized")
        return
    }
    executor.execute {
        try {
            action()
        } catch (e: Exception) {
            val msg = e.message ?: "Unexpected error"
            if (app != null) {
                app.alertUnexpectedError(null, msg, e)
            } else {
                Log.e("RunAsync", msg, e)
            }
        }
    }
}
