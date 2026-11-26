package br.com.wdc.framework.cube.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.PresenterBase;
import br.com.wdc.framework.cube.Scope;

/** A remote view lets go of the views it no longer refers to when it is written again; no presenter takes part. */
public class RemoteViewReleaseTest {

    // ── Helper ─────────────────────────────────────────────────────────

    public static class PageScope extends Scope {
        public static final String CLASS_ID = "page";
        public final Prop<Scope> content = prop();
        public final Prop<List<RowScope>> rows = prop(List.of());
    }

    public static class PanelScope extends Scope {
        public static final String CLASS_ID = "panel";
        public final Prop<Integer> page = prop(0);
        public final Prop<Scope> inner = prop();
    }

    public static class RowScope extends Scope {
        public static final String CLASS_ID = "row";
        public final Prop<String> name = prop();
    }

    /** The remote application, with only what a view uses: the views it made, the dirty ones and the released ids. */
    static class FakeApp implements RemoteApplication {
        final Map<Scope, RemoteViewImpl> views = new IdentityHashMap<>();
        final Set<RemoteViewImpl> dirty = new LinkedHashSet<>();
        final List<String> released = new ArrayList<>();
        int ids;

        @Override
        public RemoteApplicationSupport getSupport() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void release() {
            // NOOP
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
            released.add(instanceId);
        }

        @Override
        public void markDirty(RemoteViewImpl view) {
            dirty.add(view);
        }

        RemoteViewImpl write(Scope scope) {
            var view = viewOf(scope);
            view.writeScope(new JsonStreamWriter());
            return view;
        }
    }

    // ── Tests ──────────────────────────────────────────────────────────

    @Test
    public void writingAScopeMakesTheViewsOfTheScopesItHolds() {
        var app = new FakeApp();
        var page = new PageScope();
        var panel = new PanelScope();
        page.content.set(panel);

        app.write(page);

        assertTrue(app.views.containsKey(panel));
        assertTrue(app.released.isEmpty());
    }

    @Test
    public void aScopeThatLeftIsReleasedWhenItsHolderIsWrittenAgain() {
        var app = new FakeApp();
        var page = new PageScope();
        var panel = new PanelScope();
        page.content.set(panel);
        app.write(page);
        var panelView = app.views.get(panel);

        page.content.set(new PanelScope());
        app.write(page);

        assertTrue(panelView.isReleased());
        assertEquals(List.of(panelView.instanceId()), app.released);
        assertFalse(app.views.containsKey(panel));
    }

    @Test
    public void aReleasedViewNoLongerHearsItsScope() {
        var app = new FakeApp();
        var page = new PageScope();
        var panel = new PanelScope();
        page.content.set(panel);
        app.write(page);
        var panelView = app.views.get(panel);
        page.content.set(null);
        app.write(page);
        app.dirty.clear();

        panel.page.set(2);

        assertFalse(app.dirty.contains(panelView));
    }

    @Test
    public void theViewsAReleasedViewHeldGoWithIt() {
        var app = new FakeApp();
        var page = new PageScope();
        var panel = new PanelScope();
        var inner = new RowScope();
        panel.inner.set(inner);
        page.content.set(panel);
        app.write(page);
        app.write(panel);
        var innerView = app.views.get(inner);

        page.content.set(null);
        app.write(page);

        assertTrue(innerView.isReleased());
        assertEquals(2, app.released.size());
    }

    @Test
    public void aRowThatLeftTheListIsReleasedAndTheOthersStay() {
        var app = new FakeApp();
        var page = new PageScope();
        var first = new RowScope();
        var second = new RowScope();
        page.rows.set(List.of(first, second));
        app.write(page);
        var firstView = app.views.get(first);
        var secondView = app.views.get(second);

        page.rows.set(List.of(second));
        app.write(page);

        assertTrue(firstView.isReleased());
        assertFalse(secondView.isReleased());
        assertSame(secondView, app.views.get(second));
    }

    @Test
    public void aViewHeldByTwoViewsStaysUntilBothLetGo() {
        var app = new FakeApp();
        var a = new PanelScope();
        var b = new PanelScope();
        var shared = new RowScope();
        a.inner.set(shared);
        b.inner.set(shared);
        app.write(a);
        app.write(b);
        var sharedView = app.views.get(shared);

        a.inner.set(null);
        app.write(a);
        assertFalse(sharedView.isReleased());

        b.inner.set(null);
        app.write(b);
        assertTrue(sharedView.isReleased());
    }

    @Test
    public void aScopeThatComesBackGetsANewView() {
        var app = new FakeApp();
        var page = new PageScope();
        var panel = new PanelScope();
        page.content.set(panel);
        app.write(page);
        var before = app.views.get(panel);
        page.content.set(null);
        app.write(page);

        page.content.set(panel);
        app.write(page);

        assertNotSame(before, app.views.get(panel));
        assertFalse(app.views.get(panel).isReleased());
    }
}
