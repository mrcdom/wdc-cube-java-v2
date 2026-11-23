package br.com.wdc.framework.cube;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;

public class PropTest {

    // ── Helper ─────────────────────────────────────────────────────────

    static class CounterState extends ViewState {
        public final Prop<Integer> count = prop(0);
        public final Prop<String> message = prop();
        public final Prop<List<String>> lines = prop(List.of());
    }

    static class CounterPresenter extends AbstractCubePresenter<CubePlaceTest.TestApp> {

        final CounterState state = bind(new CounterState());
        int updates;

        CounterPresenter() {
            super(new CubePlaceTest.TestApp());
        }

        @Override
        public void update() {
            updates++;
        }
    }

    // ── Tests ──────────────────────────────────────────────────────────

    @Test
    public void startsWithItsInitialValue() {
        var state = new CounterState();
        assertEquals(Integer.valueOf(0), state.count.get());
        assertNull(state.message.get());
        assertEquals(List.of(), state.lines.get());
    }

    @Test
    public void aChangeAsksTheBoundPresenterForARedraw() {
        var p = new CounterPresenter();

        p.state.count.set(1);
        p.state.message.set("hi");

        assertEquals(2, p.updates);
        assertEquals(Integer.valueOf(1), p.state.count.get());
        assertEquals("hi", p.state.message.get());
    }

    @Test
    public void theSameValueAsksForNothing() {
        var p = new CounterPresenter();

        p.state.count.set(0);
        p.state.message.set(null);
        p.state.lines.set(List.of()); // equal, though another instance

        assertEquals(0, p.updates);
    }

    @Test
    public void setSilentlyChangesTheValueAndAsksForNothing() {
        var p = new CounterPresenter();

        p.state.count.setSilently(3);

        assertEquals(Integer.valueOf(3), p.state.count.get());
        assertEquals(0, p.updates);
    }

    @Test
    public void aStateWithNoUpdaterJustKeepsTheValue() {
        var state = new CounterState();
        state.count.set(7);
        assertEquals(Integer.valueOf(7), state.count.get());
    }

    @Test
    public void bindReturnsTheSameState() {
        var state = new CounterState();
        var p = new CounterPresenter();
        assertSame(state, p.bind(state));
    }
}
