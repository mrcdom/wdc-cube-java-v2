package br.com.wdc.framework.cube;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // ── Event codes (remote clients) ───────────────────────────────────

    static class RemotePresenter extends AbstractCubePresenter<RecordingApp> {

        final List<String> calls = new ArrayList<>();
        String synced;

        final Action buy = action(1, () -> calls.add("buy"));
        final ActionOf<Long> remove = actionOf(2, FormParam.asLong("p.id"), id -> calls.add("remove " + id));
        final ActionOf2<Long, Integer> modify = actionOf2(3, FormParam.asLong("p.id"), FormParam.asInteger("p.qty"),
                (id, qty) -> calls.add("modify " + id + "=" + qty));
        final ActionOf<String> secret = actionOf(4, FormParam.of("p.secret", v -> new StringBuilder(String.valueOf(v)).reverse().toString()),
                v -> calls.add("secret " + v));

        RemotePresenter(RecordingApp app) {
            super(app);
        }

        CubeSkeleton skeleton() {
            return skeleton("abc123", formData -> synced = String.valueOf(formData.get("name")));
        }
    }

    @Test
    public void theSkeletonDispatchesEachEventToTheActionOfItsCode() throws Exception {
        var app = new RecordingApp();
        var p = new RemotePresenter(app);
        var skeleton = p.skeleton();

        skeleton.submit(1, 1, Map.of());
        skeleton.submit(2, 1, Map.of("p.id", "7"));
        skeleton.submit(3, 1, Map.of("p.id", 7L, "p.qty", "2"));
        skeleton.submit(4, 1, Map.of("p.secret", "cba"));
        skeleton.syncState(Map.of("name", "admin"));

        assertEquals("abc123", skeleton.classId());
        assertEquals(List.of("buy", "remove 7", "modify 7=2", "secret abc"), p.calls);
        assertEquals("admin", p.synced);
        assertEquals("Fired by code, the actions are still guarded", 4, app.historyUpdates);
    }

    @Test
    public void anUnknownCodeIsAnError() {
        var p = new RemotePresenter(new RecordingApp());
        assertThrows(IllegalArgumentException.class, () -> p.skeleton().submit(99, 1, Map.of()));
    }

    @Test
    public void aCodeBoundTwiceFailsWhenThePresenterIsBuilt() {
        class Twice extends AbstractCubePresenter<RecordingApp> {
            final Action a = action(1, () -> { });
            final Action b = action(1, () -> { });

            Twice() {
                super(new RecordingApp());
            }
        }
        assertThrows(IllegalStateException.class, Twice::new);
    }

    @Test
    public void unboundActionsDoNothing() {
        Action.NOOP.run();
        ActionOf.<Long>noop().run(1L);
        ActionOf2.<String, String>noop().run("a", "b");
    }
}
