package br.com.wdc.framework.cube.remote;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.util.Defer;

/**
 * Generic lifecycle and lookup registry for {@link RemoteApplication} sessions.
 * <p>
 * Each application type has its own registry instance — no shared global state. Manages periodic flush (50ms tick) and
 * session expiry (30s check).
 *
 * @param <T> the concrete RemoteApplication subclass
 */
public final class RemoteApplicationRegistry<T extends RemoteApplication> {

    private static final Log LOG = Log.getLogger(RemoteApplicationRegistry.class);

    private static final Duration FLUSH_INTERVAL = Duration.ofMillis(50);
    private static final Duration EXPIRY_CHECK_INTERVAL = Duration.ofSeconds(30);
    private static final long WAKEUP_MIN_GAP_NANOS = Duration.ofMillis(10).toNanos();
    private static final long WAKEUP_FAST_GAP_NANOS = Duration.ofMillis(16).toNanos();

    private final ConcurrentHashMap<String, T> instanceMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<T> dirtyAppQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicReference<Registration> flushRegistration = new AtomicReference<>(Registration.noop());
    private final AtomicReference<Registration> expiryRegistration = new AtomicReference<>(Registration.noop());

    private final BiFunction<String, Map<String, Object>, T> factory;

    /**
     * Maximum number of concurrent application sessions. {@code -1} (default) means unlimited.
     */
    private int maxInstances = -1;

    /**
     * TTL for authenticated sessions after WebSocket disconnect. {@code Duration.ZERO} means immediate release (no
     * cache). Default: {@link RemoteApplicationSupport#DEFAULT_TIME_SPAN}.
     */
    private Duration sessionTimeSpan = RemoteApplicationSupport.DEFAULT_TIME_SPAN;

    /**
     * @param factory creates a new application instance given (appId, initialRequest)
     */
    public RemoteApplicationRegistry(BiFunction<String, Map<String, Object>, T> factory) {
        this.factory = factory;
    }

    /**
     * Sets the maximum number of concurrent sessions. {@code -1} disables the limit (default).
     */
    public void setMaxInstances(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    public int size() {
        return instanceMap.size();
    }

    public boolean isFull() {
        return maxInstances > 0 && instanceMap.size() >= maxInstances;
    }

    /**
     * Sets the TTL for authenticated sessions after WebSocket disconnect. Use {@code Duration.ZERO} to disable caching
     * (immediate release).
     */
    public void setSessionTimeSpan(Duration sessionTimeSpan) {
        this.sessionTimeSpan = sessionTimeSpan != null ? sessionTimeSpan : RemoteApplicationSupport.DEFAULT_TIME_SPAN;
    }

    public Duration getSessionTimeSpan() {
        return sessionTimeSpan;
    }

    /** Returns true when sessions are released immediately on disconnect (TTL = 0). */
    public boolean isImmediateRelease() {
        return sessionTimeSpan.isZero();
    }

    public void init(Defer cleanUp) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        var scheduler = ScheduledExecutor.BEAN.get();
        if (scheduler == null) {
            initialized.set(false);
            LOG.warn("ScheduledExecutor not available - registry background tasks were not started");
            return;
        }

        expiryRegistration.set(scheduler.scheduleAtFixedRate(
                this::removeExpireds,
                EXPIRY_CHECK_INTERVAL,
                EXPIRY_CHECK_INTERVAL));
        cleanUp.push(() -> {
            expiryRegistration.get().remove();
            expiryRegistration.set(Registration.noop());
        });

        flushRegistration.set(scheduler.scheduleAtFixedRate(
                this::executeBackgroundFlush,
                FLUSH_INTERVAL,
                FLUSH_INTERVAL));
        cleanUp.push(() -> {
            flushRegistration.get().remove();
            flushRegistration.set(Registration.noop());
        });

        cleanUp.push(() -> {
            initialized.set(false);
            LOG.info("RemoteApplicationRegistry stopped");
        });
        LOG.info("RemoteApplicationRegistry initialized");
    }

    public T get(String appId) {
        if (appId == null || appId.isBlank()) {
            return null;
        }
        return instanceMap.get(appId);
    }

    public T getOrCreate(String appId, Map<String, Object> request) {
        T existing = instanceMap.get(appId);
        if (existing != null) {
            return existing;
        }
        if (isFull()) {
            throw new CapacityExceededException(maxInstances);
        }
        return instanceMap.computeIfAbsent(appId, id -> factory.apply(id, request));
    }

    public T remove(String appId) {
        return instanceMap.remove(appId);
    }

    public void enqueueDirty(RemoteApplication app) {
        if (app == null) {
            return;
        }
        if (app.dirtyQueued().compareAndSet(false, true)) {
            @SuppressWarnings("unchecked")
            T typed = (T) app;
            dirtyAppQueue.offer(typed);
        }
    }

    public void triggerImmediateFlush(RemoteApplication app) {
        if (app == null) {
            return;
        }

        long now = System.nanoTime();
        long last = app.getLastWakeupNanos();
        long gap = app.isProcessingSubmit() ? WAKEUP_FAST_GAP_NANOS : WAKEUP_MIN_GAP_NANOS;
        if (now - last < gap) {
            return;
        }
        app.setLastWakeupNanos(now);

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

    private void removeExpireds() {
        var now = System.currentTimeMillis();

        var appIterator = instanceMap.values().iterator();
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

    private void executeBackgroundFlush() {
        T app;
        while ((app = dirtyAppQueue.poll()) != null) {
            try {
                app.flushDirtyViewsFromRegistry();
            } catch (Exception e) {
                LOG.error("Background flush error for app {}", app.getId(), e);
            }
        }
    }
}
