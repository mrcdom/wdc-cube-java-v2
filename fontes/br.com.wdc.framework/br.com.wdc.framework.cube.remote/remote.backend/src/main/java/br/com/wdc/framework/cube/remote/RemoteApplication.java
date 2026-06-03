package br.com.wdc.framework.cube.remote;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.PresenterBase;
import io.javalin.websocket.WsContext;

/**
 * Contract for a remote-capable application instance.
 * <p>
 * Implemented by concrete application classes that extend their
 * project-specific {@code CubeApplication} subclass and delegate remote
 * infrastructure to {@link RemoteApplicationSupport}.
 * <p>
 * Most methods have default implementations that delegate to
 * {@link #getSupport()}. Implementors only need to provide
 * {@code getSupport()}, {@code isAuthenticated()}, {@code getRootPresenter()},
 * and {@code release()}.
 */
public interface RemoteApplication {

    // :: Support accessor (single delegation point)

    RemoteApplicationSupport getSupport();

    // :: Identity

    default String getId() {
        return getSupport().getId();
    }

    // :: Authentication (app-specific, no default)

    boolean isAuthenticated();
    
    default RemoteDataSecurity getDataSecurity() {
        return this.getSupport().getDataSecurity();
    }

    // :: WebSocket session

    default WsContext getWsSession() {
        return getSupport().getWsSession();
    }

    default void setWsSession(WsContext ctx) {
        getSupport().setWsSession(ctx);
    }

    // :: Lifecycle

    void release();

    default void extendLife() {
        getSupport().extendLife();
    }

    default boolean isExpired(long now) {
        return getSupport().isExpired(now);
    }

    default void resetForReconnect() {
        getSupport().resetForReconnect();
    }

    // :: View management

    default int nextInstanceId() {
        return getSupport().nextInstanceId();
    }

    default void putView(RemoteViewImpl view) {
        getSupport().putView(view);
    }

    default void removeView(String instanceId) {
        getSupport().removeView(instanceId);
    }

    default void markDirty(RemoteViewImpl view) {
        getSupport().markDirty(view);
    }

    default void updateAllViews() {
        getSupport().updateAllViews();
    }

    // :: Request processing

    default void sendResponse(Map<String, Object> request) throws Exception {
        getSupport().sendResponse(request);
    }

    default void flushDirtyViewsFromRegistry() {
        getSupport().flushDirtyViewsFromRegistry();
    }

    // :: Error handling

    default void alertUnexpectedError(Log log, String msg, Throwable e) {
        getSupport().alertUnexpectedError(log, msg, e);
    }

    // :: Navigation (app-specific, no default)
    
    PresenterBase getRootPresenter();

    default void safeGo(String path) {
        getSupport().safeGo(path);
    }

    // :: Registry coordination

    default AtomicBoolean dirtyQueued() {
        return getSupport().dirtyQueued();
    }

    default long getLastWakeupNanos() {
        return getSupport().getLastWakeupNanos();
    }

    default void setLastWakeupNanos(long nanos) {
        getSupport().setLastWakeupNanos(nanos);
    }

    default boolean isProcessingSubmit() {
        return getSupport().isProcessingSubmit();
    }

    // :: Response envelope

    default void addResponseField(String key, Object value) {
        getSupport().addResponseField(key, value);
    }

    default void emitAccessToken(String token) {
        getSupport().emitAccessToken(token);
    }
}
