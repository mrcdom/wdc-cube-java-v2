package br.com.wdc.framework.cube.remote;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.cube.CubeApplication;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.PresenterBase;
import br.com.wdc.framework.cube.Scope;

/**
 * The host's alert of an unexpected error carries no text of the server's: alert {@code -1}, with the cause's message
 * as its only argument when there is one, so that each client words it in its user's language.
 */
public class UnexpectedErrorAlertTest {

    private static final Log LOG = Log.getLogger(UnexpectedErrorAlertTest.class);

    /** A remote application with only what the browser presenter uses. */
    static final class FakeApp extends CubeApplication implements RemoteApplication {

        final Map<Scope, RemoteViewImpl> views = new IdentityHashMap<>();
        int ids;

        @Override
        public RemoteApplicationSupport getSupport() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAuthenticated() {
            return false;
        }

        @Override
        public PresenterBase getRootPresenter() {
            return null;
        }

        @Override
        public int nextInstanceId() {
            return ++ids;
        }

        @Override
        public RemoteViewImpl viewOf(Scope scope) {
            return views.computeIfAbsent(scope, s -> RemoteViewImpl.forScope(this, s));
        }

        @Override
        public void forgetView(RemoteViewImpl view) {
            views.remove(view.scope(), view);
        }

        @Override
        public void putView(RemoteViewImpl view) {
            // NOOP
        }

        @Override
        public void removeView(String instanceId) {
            // NOOP
        }

        @Override
        public void markDirty(RemoteViewImpl view) {
            // NOOP
        }

        @Override
        public CubePlace getRootPlace() {
            return null;
        }

        @Override
        public void updateHistory() {
            // NOOP
        }

        @Override
        public ClientStorage clientSessionStore() {
            return null;
        }

        @Override
        public ClientStorage clientPersistentStore() {
            return null;
        }

        @Override
        protected Map<Integer, CubePresenter> createPresenterMap() {
            return new HashMap<>();
        }
    }

    private final RemoteBrowserPresenter browser = new RemoteBrowserPresenter(new FakeApp());

    @Test
    public void theCause_isTheOnlyArgument() {
        browser.alertUnexpectedError(LOG, "submitting event", new IllegalStateException("eventCode(99)"));

        assertEquals(Integer.valueOf(RemoteBrowserPresenter.ALERT_UNEXPECTED_ERROR), browser.scope.alertId.get());
        assertEquals("what failed stays in the log", List.of("eventCode(99)"), browser.scope.alertArgs.get());
    }

    @Test
    public void withoutACause_thereIsNoArgument() {
        browser.alertUnexpectedError(LOG, "submitting event", null);

        assertEquals(Integer.valueOf(RemoteBrowserPresenter.ALERT_UNEXPECTED_ERROR), browser.scope.alertId.get());
        assertEquals(List.of(), browser.scope.alertArgs.get());
    }
}
