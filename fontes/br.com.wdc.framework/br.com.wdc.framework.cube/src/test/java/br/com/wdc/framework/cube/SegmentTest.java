package br.com.wdc.framework.cube;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

public class SegmentTest {

    // ── Helper ─────────────────────────────────────────────────────────

    static class CountingView implements CubeView {
        int updates;
        boolean released;

        @Override
        public String instanceId() {
            return "panel:1";
        }

        @Override
        public void update() {
            updates++;
        }

        @Override
        public void release() {
            released = true;
        }
    }

    static class PageScope extends Scope {

        static class PanelScope extends Scope {
            final Prop<Integer> page = prop(0);
            ActionOf<Integer> onPageChange = ActionOf.noop();
        }

        final Prop<PanelScope> panel = prop();
    }

    static class PagePresenter extends AbstractCubePresenter<GuardedActionTest.RecordingApp> {

        final PageScope state = bind(new PageScope());
        final PageScope.PanelScope panel = new PageScope.PanelScope();
        final CountingView pageView = new CountingView();
        final CountingView panelView = new CountingView();

        PagePresenter() {
            super(new GuardedActionTest.RecordingApp());
            app.setScopeViews(new ScopeViews()
                    .register(PageScope.class, (CubeApplication a, PageScope s) -> pageView)
                    .register(PageScope.PanelScope.class, (CubeApplication a, PageScope.PanelScope s) -> panelView));
            panel.onPageChange = actionOf(panel, 2, FormParam.asInteger("p.page"), panel.page::set);
            state.panel.set(panel);
            // The host draws the presenter's scope; the page's view then asks for the panel's when it needs it
            app.viewOf(state);
        }

        void dropPanel() {
            state.panel.set(null);
            detach(panel);
        }
    }

    // ── Tests ──────────────────────────────────────────────────────────

    @Test
    public void theViewIsMadeWhenTheViewLayerAsksAndDrawsOnce() {
        var p = new PagePresenter();
        assertNull("No view before someone asks for it", p.panel.view());

        var view = p.app.viewOf(p.panel);

        assertSame(p.panelView, view);
        assertSame(view, p.app.viewOf(p.panel));
        assertEquals(1, p.panelView.updates);
    }

    @Test
    public void beforeItHasAViewTheSegmentRedrawsItsOwner() {
        var p = new PagePresenter();
        var ownBefore = p.pageView.updates;

        p.panel.page.set(3);

        assertEquals("The owner draws the segment it holds", ownBefore + 1, p.pageView.updates);
    }

    @Test
    public void withAViewAChangeInTheSegmentRedrawsOnlyItsView() {
        var p = new PagePresenter();
        p.app.viewOf(p.panel);
        var ownBefore = p.pageView.updates;

        p.panel.page.set(3);

        assertEquals(2, p.panelView.updates);
        assertEquals("The presenter's own view is not redrawn", ownBefore, p.pageView.updates);
    }

    @Test
    public void theSegmentsSkeletonDispatchesToTheActionsBoundOnIt() throws Exception {
        var p = new PagePresenter();
        var skeleton = p.panel.skeleton("abcdef123456");

        skeleton.submit(2, 1, Map.of("p.page", "4"));

        assertEquals("abcdef123456", skeleton.classId());
        assertEquals(Integer.valueOf(4), p.panel.page.get());
        assertThrows(IllegalArgumentException.class, () -> skeleton.submit(9, 1, Map.of()));
    }

    @Test
    public void releasingThePresenterReleasesTheSegmentsView() {
        var p = new PagePresenter();
        p.app.viewOf(p.panel);

        p.release();
        p.panel.page.set(5);

        assertTrue(p.panelView.released);
        assertTrue("The presenter's scope view goes too", p.pageView.released);
        assertEquals("A released segment no longer redraws its view", 1, p.panelView.updates);
    }

    @Test
    public void detachReleasesOneSegment() {
        var p = new PagePresenter();
        p.app.viewOf(p.panel);

        p.dropPanel();

        assertTrue(p.panelView.released);
        assertNull(p.panel.view());
    }

    @Test
    public void aScopeWithNoViewRegisteredIsAnError() {
        var app = new GuardedActionTest.RecordingApp();
        app.setScopeViews(new ScopeViews());
        assertThrows(IllegalStateException.class, () -> app.viewOf(new PageScope.PanelScope()));
    }

    @Test
    public void theFallbackDrawsWhatNobodyRegistered() {
        var app = new GuardedActionTest.RecordingApp();
        var view = new CountingView();
        app.setScopeViews(new ScopeViews().fallback((CubeApplication a, Scope s) -> view));

        assertSame(view, app.viewOf(new PageScope.PanelScope()));
    }
}
