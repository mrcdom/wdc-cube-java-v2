package br.com.wdc.shopping.view.android

import br.com.wdc.framework.cube.CubeView
import br.com.wdc.framework.cube.CubeViewSlot
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * A CubeViewSlot backed by a Compose MutableState.
 *
 * [setView] stores the pending value (thread-safe). The actual Compose MutableState
 * is updated by [flush], which is called on the main thread during the render loop.
 */
class ComposeViewSlot : CubeViewSlot {

    val current: MutableState<CubeView?> = mutableStateOf(null)

    @Volatile
    private var pending: CubeView? = null

    @Volatile
    private var dirty = false

    override fun setView(view: CubeView?) {
        pending = view
        dirty = true
    }

    /** Called on the main thread during doUpdate to sync pending value into Compose state. */
    fun flush() {
        if (dirty) {
            dirty = false
            current.value = pending
        }
    }
}
