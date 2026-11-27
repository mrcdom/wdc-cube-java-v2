package br.com.wdc.framework.cube.remote;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.PresenterBase;
import br.com.wdc.framework.cube.Scope;
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

    default String getClientIp() {
        return getSupport().getClientIp();
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

    /** The view that draws the scope for the client, made the first time a scope that holds it is written. */
    default RemoteViewImpl viewOf(Scope scope) {
        return getSupport().viewOf(scope);
    }

    default void forgetView(RemoteViewImpl view) {
        getSupport().forgetView(view);
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

    /**
     * Shows the client an unexpected error, whatever raised it: the application's code or the host (a client message it
     * could not handle). By default the host's alert ({@link RemoteApplicationSupport#alertUnexpectedError}), which the
     * client words in its user's language; an application overrides it to show the error its own way.
     */
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
}
