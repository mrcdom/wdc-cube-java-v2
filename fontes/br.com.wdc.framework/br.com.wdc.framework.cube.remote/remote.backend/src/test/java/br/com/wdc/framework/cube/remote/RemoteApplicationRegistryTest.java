package br.com.wdc.framework.cube.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Test;

import br.com.wdc.framework.cube.PresenterBase;
import io.javalin.websocket.WsContext;

/**
 * What the registry tells about its sessions, for several instances of a host: each session made here, and the release
 * of one whose client went on elsewhere.
 */
public class RemoteApplicationRegistryTest {

    /** A session with only an id and whether it was released; no socket holds it. */
    static final class FakeApp implements RemoteApplication {

        final String id;
        RemoteApplicationRegistry<FakeApp> registry;
        boolean released;

        FakeApp(String id) {
            this.id = id;
        }

        @Override
        public RemoteApplicationSupport getSupport() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public WsContext getWsSession() {
            return null;
        }

        @Override
        public boolean isAuthenticated() {
            return false;
        }

        @Override
        public void release() {
            released = true;
            registry.remove(id);
        }

        @Override
        public PresenterBase getRootPresenter() {
            return null;
        }
    }

    private final RemoteApplicationRegistry<FakeApp> registry = new RemoteApplicationRegistry<>((id, request) -> {
        var app = new FakeApp(id);
        app.registry = this.registry;
        return app;
    });

    @Test
    public void eachSessionMadeHere_isTold_once() {
        var made = new CopyOnWriteArrayList<String>();
        var registration = registry.onCreated(made::add);

        var first = registry.getOrCreate("tab-1", Map.of());
        assertSame("the same session again: nothing new", first, registry.getOrCreate("tab-1", Map.of()));
        registry.getOrCreate("tab-2", Map.of());
        assertEquals(List.of("tab-1", "tab-2"), made);

        registration.remove();
        registry.getOrCreate("tab-3", Map.of());
        assertEquals(List.of("tab-1", "tab-2"), made);
    }

    @Test
    public void aSessionWithoutSocket_isReleased_whenItsClientWentElsewhere() {
        var app = registry.getOrCreate("tab-1", Map.of());

        assertTrue(registry.releaseIdle("tab-1"));
        assertTrue(app.released);
        assertNull(registry.get("tab-1"));
        assertFalse("not here: nothing to release", registry.releaseIdle("tab-1"));
        assertFalse(registry.releaseIdle("unknown"));
    }
}
