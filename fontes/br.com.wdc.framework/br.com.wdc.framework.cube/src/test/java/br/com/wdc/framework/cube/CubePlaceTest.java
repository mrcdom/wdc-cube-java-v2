package br.com.wdc.framework.cube;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import br.com.wdc.framework.commons.storage.ClientStorage;

public class CubePlaceTest {

    // ── Helper ─────────────────────────────────────────────────────────

    static class TestApp extends CubeApplication {

        final List<String> log = new ArrayList<>();

        @Override
        public CubePlace getRootPlace() {
            return Tree.ROOT;
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

    static class LoggingPresenter extends AbstractCubePresenter<TestApp> {

        private final String name;

        LoggingPresenter(TestApp app, String name) {
            super(app);
            this.name = name;
        }

        @Override
        public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
            app.log.add(name + (initialization ? "+" : "") + (deepest ? "!" : ""));
            return true;
        }

        @Override
        public void release() {
            app.log.add(name + "-");
        }
    }

    static final class Tree {
        static final CubePlace ROOT = CubePlace.root("root", (TestApp a) -> new LoggingPresenter(a, "root"));
        static final CubePlace A = ROOT.child("a", (TestApp a) -> new LoggingPresenter(a, "a"));
        static final CubePlace A1 = A.child("a1", (TestApp a) -> new LoggingPresenter(a, "a1"));
        static final CubePlace B = ROOT.child("b", (TestApp a) -> new LoggingPresenter(a, "b"));
    }

    // ── Tree ───────────────────────────────────────────────────────────

    @Test
    public void pathGoesFromTheRootDownToThePlace() {
        assertEquals(List.of(Tree.ROOT, Tree.A, Tree.A1), Tree.A1.getPath());
        assertSame(Tree.A, Tree.A1.getParent());
        assertEquals(List.of(Tree.A, Tree.B), Tree.ROOT.getChildren());
    }

    @Test
    public void deeperPlacesHaveGreaterIds() {
        assertTrue(Tree.A.getId() > Tree.ROOT.getId());
        assertTrue(Tree.B.getId() > Tree.ROOT.getId());
        assertTrue(Tree.A1.getId() > Tree.B.getId());
    }

    @Test
    public void findsAnyPlaceOfTheTreeByName() {
        assertSame(Tree.A1, Tree.ROOT.find("a1"));
        assertSame(Tree.B, Tree.A1.find("b"));
        assertNull(Tree.ROOT.find("nope"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsARepeatedNameInTheSameTree() {
        var root = CubePlace.root("r", (TestApp a) -> new LoggingPresenter(a, "r"));
        var x = root.child("x", (TestApp a) -> new LoggingPresenter(a, "x"));
        x.child("x", (TestApp a) -> new LoggingPresenter(a, "x"));
    }

    @Test
    public void sameNameInAnotherTreeIsFine() {
        var other = CubePlace.root("other", (TestApp a) -> new LoggingPresenter(a, "other"));
        assertSame(other.child("a", (TestApp a) -> new LoggingPresenter(a, "a")), other.find("a"));
        assertSame(Tree.A, Tree.ROOT.find("a"));
    }

    // ── Navigation ─────────────────────────────────────────────────────

    @Test
    public void navigationWalksThePathAndReleasesWhatLeftIt() {
        var app = new TestApp();

        assertTrue(app.navigate(Tree.A1, new CubeIntent()));
        assertEquals(List.of("root+", "a+", "a1+!"), app.log);
        assertSame(Tree.A1, app.getLastPlace());

        app.log.clear();
        assertTrue(app.navigate(Tree.B, new CubeIntent()));
        assertEquals(List.of("root", "b+!", "a1-", "a-"), app.log);
    }

    @Test
    public void goResolvesThePlaceByName() {
        var app = new TestApp();

        assertTrue(app.go("a1?x=1"));
        assertSame(Tree.A1, app.getLastPlace());
        assertEquals("a1", app.newIntent().toString());
    }

    @Test
    public void goToAnUnknownNameGoesToTheRoot() {
        var app = new TestApp();

        assertTrue(app.go("nope"));
        assertSame(Tree.ROOT, app.getLastPlace());
        assertFalse(app.getRootPlace().isUnresolved());
    }
}
