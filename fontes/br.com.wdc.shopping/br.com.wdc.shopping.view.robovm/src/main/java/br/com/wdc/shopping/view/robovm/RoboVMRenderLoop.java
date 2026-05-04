package br.com.wdc.shopping.view.robovm;

import org.robovm.apple.foundation.NSRunLoop;
import org.robovm.apple.foundation.NSRunLoopMode;
import org.robovm.apple.foundation.NSTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Render loop for iOS using NSTimer on the main run loop.
 * Flushes dirty views at ~60fps, similar to SwingTimer/Choreographer patterns.
 */
public final class RoboVMRenderLoop {

    private static final Logger LOG = LoggerFactory.getLogger(RoboVMRenderLoop.class);

    static final int FRAME_INTERVAL_MS = 16; // ~60fps
    private static final double FRAME_INTERVAL_SEC = FRAME_INTERVAL_MS / 1000.0;

    private static NSTimer timer;
    private static ShoppingRoboVMApplication app;

    private RoboVMRenderLoop() {}

    @SuppressWarnings("unused")
	public static void start(ShoppingRoboVMApplication application) {
        app = application;
        timer = new NSTimer(FRAME_INTERVAL_SEC, t -> {
            try {
                app.flushDirtyViews();
            } catch (Exception e) {
                LOG.error("Error in render loop", e);
            }
        }, null, true);
        NSRunLoop.getMain().addTimer(NSRunLoopMode.Default, timer);
        LOG.info("Render loop started ({}ms interval)", FRAME_INTERVAL_MS);
    }

    public static void stop() {
        if (timer != null) {
            timer.invalidate();
            timer = null;
        }
        app = null;
        LOG.info("Render loop stopped");
    }
}
