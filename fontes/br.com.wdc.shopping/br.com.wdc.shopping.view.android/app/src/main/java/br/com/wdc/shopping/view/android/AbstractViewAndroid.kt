package br.com.wdc.shopping.view.android

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import br.com.wdc.framework.cube.CubeView

/**
 * Base class for Compose-based Cube views.
 *
 * Each view holds a reference to its presenter and a revision counter.
 * When the presenter calls [update], the view is marked dirty in the application's
 * dirty-view map. The [AndroidRenderLoop] flushes dirty views on the main thread,
 * calling [doUpdate] which increments [revision] and triggers Compose recomposition.
 */
abstract class AbstractViewAndroid<P>(
    private val id: String,
    val presenter: P
) : CubeView {

    /** Revision counter observed by Compose — increments on each [doUpdate] call. */
    val revision: MutableState<Int> = mutableIntStateOf(0)

    override fun instanceId(): String = id

    override fun update() {
        AndroidRenderLoop.markDirty(this)
    }

    /**
     * Called on the main thread by [AndroidRenderLoop] when this view needs to refresh.
     * Subclasses override to sync slots/state before Compose recomposition.
     */
    open fun doUpdate() {
        revision.value = revision.value + 1
    }

    override fun release() {
        // Subclasses can override for cleanup
    }
}
