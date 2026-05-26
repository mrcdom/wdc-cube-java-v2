package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.log.Log;

/**
 * Global lifecycle and lookup registry for ApplicationReactImpl sessions.
 *
 * <p>
 * Global periodic flush (50ms tick) combined with immediate wake-up for interactive events. Per-app coalescence
 * prevents duplicate queue entries and debounce prevents wake-up storms.
 * </p>
 */
public final class ApplicationReactRegistry {

    private static final Log LOG = Log.getLogger(ApplicationReactRegistry.class);

    private static final Duration FLUSH_INTERVAL = Duration.ofMillis(50);
    private static final Duration EXPIRY_CHECK_INTERVAL = Duration.ofSeconds(30);
    private static final long WAKEUP_MIN_GAP_NANOS = Duration.ofMillis(10).toNanos();

    private static final ConcurrentHashMap<String, ApplicationReactImpl> INSTANCE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<ApplicationReactImpl> DIRTY_APP_QUEUE = new ConcurrentLinkedQueue<>();

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private static volatile Registration flushRegistration = Registration.noop();
    private static volatile Registration expiryRegistration = Registration.noop();

    private ApplicationReactRegistry() {
        // NOOP
    }

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        var scheduler = ScheduledExecutor.BEAN.get();
        if (scheduler == null) {
            INITIALIZED.set(false);
            LOG.warn("ScheduledExecutor not available - registry background tasks were not started");
            return;
        }

        expiryRegistration = scheduler.scheduleAtFixedRate(
                ApplicationReactRegistry::removeExpireds,
                EXPIRY_CHECK_INTERVAL,
                EXPIRY_CHECK_INTERVAL);

        flushRegistration = scheduler.scheduleAtFixedRate(
                ApplicationReactRegistry::executeBackgroundFlush,
                FLUSH_INTERVAL,
                FLUSH_INTERVAL);

        LOG.info("ApplicationReactRegistry initialized");
    }

    public static void shutdown() {
        flushRegistration.remove();
        flushRegistration = Registration.noop();

        expiryRegistration.remove();
        expiryRegistration = Registration.noop();

        INITIALIZED.set(false);

        LOG.info("ApplicationReactRegistry stopped");
    }

    public static ApplicationReactImpl get(String appId) {
        if (StringUtils.isBlank(appId)) {
            return null;
        }
        return INSTANCE_MAP.get(appId);
    }

    public static ApplicationReactImpl getOrCreate(String appId, Map<String, Object> request) {
        return INSTANCE_MAP.computeIfAbsent(appId, id -> createApp(id, request));
    }

    public static ApplicationReactImpl remove(String appId) {
        return INSTANCE_MAP.remove(appId);
    }

    public static void enqueueDirty(ApplicationReactImpl app) {
        if (app == null) {
            return;
        }
        if (app.dirtyQueued.compareAndSet(false, true)) {
            DIRTY_APP_QUEUE.offer(app);
        }
    }

    /**
     * Triggers an immediate flush for the given app, bypassing the 50ms tick wait. Uses per-app debounce
     * ({@code WAKEUP_MIN_GAP_NANOS}) to prevent storm under burst. Falls back to tick if scheduler is unavailable or
     * debounce suppresses.
     */
    public static void triggerImmediateFlush(ApplicationReactImpl app) {
        if (app == null) {
            return;
        }

        // Per-app debounce: skip if last wake-up was too recent
        long now = System.nanoTime();
        long last = app.lastWakeupNanos;
        if (now - last < WAKEUP_MIN_GAP_NANOS) {
            return;
        }
        app.lastWakeupNanos = now;

        var scheduler = ScheduledExecutor.BEAN.get();
        if (scheduler == null) {
            return;
        }

        scheduler.execute(() -> {
            try {
                app.flushDirtyViewsFromRegistry();
            } catch (Exception e) {
                LOG.error("Immediate flush error for app {}", app.getId(), e);
            }
        });
    }

    static void removeExpireds() {
        var now = System.currentTimeMillis();

        var appIterator = INSTANCE_MAP.values().iterator();
        while (appIterator.hasNext()) {
            var app = appIterator.next();
            if (app.isExpired(now)) {
                if (app.getWsSession() == null) {
                    app.release();
                } else {
                    app.extendLife();
                }
            }
        }
    }

    private static void executeBackgroundFlush() {
        ApplicationReactImpl app;
        while ((app = DIRTY_APP_QUEUE.poll()) != null) {
            try {
                app.flushDirtyViewsFromRegistry();
            } catch (Exception e) {
                LOG.error("Background flush error for app {}", app.getId(), e);
            }
        }
    }

    private static ApplicationReactImpl createApp(String appId, Map<String, Object> request) {
        var app = new ApplicationReactImpl(appId);
        try {
            app.addReleaseAction(() -> INSTANCE_MAP.remove(appId));

            var path = app.getFragment();
            var browserViewId = app.getBrowserPresenter().getView().instanceId();

            @SuppressWarnings("unchecked")
            Map<String, Object> browserViewState = (Map<String, Object>) request.get(browserViewId);
            if (browserViewState != null) {
                path = (String) browserViewState.get("p.path");
                if (StringUtils.isBlank(path)) {
                    path = app.getFragment();
                }
            }

            app.safeGo(path);
        } catch (Exception caught) {
            app.release();
            return ExceptionUtils.rethrow(caught);
        }
        return app;
    }
}
