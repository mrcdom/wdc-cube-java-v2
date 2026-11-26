package br.com.wdc.framework.cube;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;

public class PropTest {

    // ── Helper ─────────────────────────────────────────────────────────

    static class CounterScope extends Scope {
        public final Prop<Integer> count = prop(0);
        public final Prop<String> message = prop();
        public final Prop<List<String>> lines = prop(List.of());
    }

    static class CounterPresenter extends AbstractCubePresenter<CubePlaceTest.TestApp> {

        final CounterScope scope = bind(new CounterScope());
        int updates;

        CounterPresenter() {
            super(new CubePlaceTest.TestApp());
            // What a rendering technology does when it starts drawing the scope
            scope.setUpdater(() -> updates++);
        }
    }

    // ── Tests ──────────────────────────────────────────────────────────

    @Test
    public void startsWithItsInitialValue() {
        var scope = new CounterScope();
        assertEquals(Integer.valueOf(0), scope.count.get());
        assertNull(scope.message.get());
        assertEquals(List.of(), scope.lines.get());
    }

    @Test
    public void aChangeRedrawsWhatDrawsTheScope() {
        var p = new CounterPresenter();

        p.scope.count.set(1);
        p.scope.message.set("hi");

        assertEquals(2, p.updates);
        assertEquals(Integer.valueOf(1), p.scope.count.get());
        assertEquals("hi", p.scope.message.get());
    }

    @Test
    public void theSameValueAsksForNothing() {
        var p = new CounterPresenter();

        p.scope.count.set(0);
        p.scope.message.set(null);
        p.scope.lines.set(List.of()); // equal, though another instance

        assertEquals(0, p.updates);
    }

    @Test
    public void setSilentlyChangesTheValueAndAsksForNothing() {
        var p = new CounterPresenter();

        p.scope.count.setSilently(3);

        assertEquals(Integer.valueOf(3), p.scope.count.get());
        assertEquals(0, p.updates);
    }

    @Test
    public void aScopeWithNoUpdaterJustKeepsTheValue() {
        var scope = new CounterScope();
        scope.count.set(7);
        assertEquals(Integer.valueOf(7), scope.count.get());
    }

    @Test
    public void bindReturnsTheSameScope() {
        var scope = new CounterScope();
        var p = new CounterPresenter();
        assertSame(scope, p.bind(scope));
    }
}
