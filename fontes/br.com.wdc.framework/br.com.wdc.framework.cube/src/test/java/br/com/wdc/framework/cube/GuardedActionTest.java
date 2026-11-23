package br.com.wdc.framework.cube;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class GuardedActionTest {

    // ── Helper ─────────────────────────────────────────────────────────

    static class RecordingApp extends CubePlaceTest.TestApp {

        final List<Throwable> failures = new ArrayList<>();
        int historyUpdates;

        @Override
        public void updateHistory() {
            historyUpdates++;
        }

        @Override
        public void alertUnexpectedError(String message, Throwable error) {
            failures.add(error);
        }
    }

    static class ButtonsPresenter extends AbstractCubePresenter<RecordingApp> {

        final List<String> calls = new ArrayList<>();

        final Action ok = action(() -> calls.add("ok"));
        final Action fails = action(() -> {
            throw new IllegalStateException("boom");
        });
        final ActionOf<Long> open = actionOf(id -> calls.add("open " + id));
        final ActionOf2<String, Integer> set = actionOf2((name, qty) -> calls.add(name + "=" + qty));

        ButtonsPresenter(RecordingApp app) {
            super(app);
        }
    }

    // ── Tests ──────────────────────────────────────────────────────────

    @Test
    public void runsTheBodyAndRepublishesTheAddress() {
        var app = new RecordingApp();
        var p = new ButtonsPresenter(app);

        p.ok.run();
        p.open.run(7L);
        p.set.run("qty", 3);

        assertEquals(List.of("ok", "open 7", "qty=3"), p.calls);
        assertEquals(3, app.historyUpdates);
        assertEquals(List.of(), app.failures);
    }

    @Test
    public void aFailureGoesToTheApplicationNotToTheCaller() {
        var app = new RecordingApp();
        var p = new ButtonsPresenter(app);

        p.fails.run();

        assertEquals(1, app.failures.size());
        assertSame(IllegalStateException.class, app.failures.get(0).getClass());
        assertEquals("The address is republished even after a failure", 1, app.historyUpdates);
    }

    @Test
    public void unboundActionsDoNothing() {
        Action.NOOP.run();
        ActionOf.<Long>noop().run(1L);
        ActionOf2.<String, String>noop().run("a", "b");
    }
}
