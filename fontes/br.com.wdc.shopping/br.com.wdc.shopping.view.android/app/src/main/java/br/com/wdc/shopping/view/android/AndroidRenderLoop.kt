package br.com.wdc.shopping.view.android

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import java.util.concurrent.ConcurrentHashMap

/**
 * Render loop that mirrors the JFX AnimationTimer pattern.
 *
 * Background threads mark views as dirty via [markDirty]. On each vsync frame,
 * the Choreographer callback flushes dirty views on the main thread, calling
 * [AbstractViewAndroid.doUpdate] which increments the Compose revision counter.
 *
 * This ensures all MutableState writes happen on the main thread, avoiding
 * race conditions with the Compose Snapshot system.
 */
object AndroidRenderLoop {

    private val dirtyViewMap = ConcurrentHashMap<String, AbstractViewAndroid<*>>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var running = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            try {
                flushDirtyViews()
            } catch (e: Exception) {
                android.util.Log.e("AndroidRenderLoop", "Error flushing dirty views", e)
            }
            if (running) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    fun start() {
        if (running) return
        running = true
        mainHandler.post {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun stop() {
        running = false
        dirtyViewMap.clear()
    }

    fun markDirty(view: AbstractViewAndroid<*>) {
        dirtyViewMap[view.instanceId()] = view
    }

    private fun flushDirtyViews() {
        if (dirtyViewMap.isEmpty()) return

        val iterator = dirtyViewMap.values.iterator()
        while (iterator.hasNext()) {
            val view = iterator.next()
            iterator.remove()
            view.doUpdate()
        }
    }
}
