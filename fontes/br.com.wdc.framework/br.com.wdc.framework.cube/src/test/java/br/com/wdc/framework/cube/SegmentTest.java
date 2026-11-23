package br.com.wdc.framework.cube;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
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
        final CountingView panelView = new CountingView();
        int ownUpdates;

        PagePresenter() {
            super(new GuardedActionTest.RecordingApp());
            panel.onPageChange = actionOf(panel, 2, FormParam.asInteger("p.page"), panel.page::set);
            state.panel.set(attach(panel, (app, s) -> panelView));
        }

        @Override
        public void update() {
            ownUpdates++;
        }
    }

    // ── Tests ──────────────────────────────────────────────────────────

    @Test
    public void attachGivesTheSegmentItsViewAndDrawsItOnce() {
        var p = new PagePresenter();

        assertSame(p.panelView, p.panel.view());
        assertEquals(1, p.panelView.updates);
    }

    @Test
    public void aChangeInTheSegmentRedrawsOnlyItsView() {
        var p = new PagePresenter();
        var ownBefore = p.ownUpdates;

        p.panel.page.set(3);

        assertEquals(2, p.panelView.updates);
        assertEquals("The presenter's own view is not redrawn", ownBefore, p.ownUpdates);
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

        p.release();
        p.panel.page.set(5);

        assertTrue(p.panelView.released);
        assertEquals("A released segment no longer redraws", 1, p.panelView.updates);
    }

    @Test
    public void theSameCodeOnTwoSegmentsIsFine() {
        var scopes = new ArrayList<Scope>();
        class TwoPanels extends AbstractCubePresenter<GuardedActionTest.RecordingApp> {
            final PageScope.PanelScope a = new PageScope.PanelScope();
            final PageScope.PanelScope b = new PageScope.PanelScope();

            TwoPanels() {
                super(new GuardedActionTest.RecordingApp());
                a.onPageChange = actionOf(a, 1, FormParam.asInteger("p"), v -> { });
                b.onPageChange = actionOf(b, 1, FormParam.asInteger("p"), v -> { });
                scopes.addAll(List.of(a, b));
            }
        }
        new TwoPanels();
        assertEquals(2, scopes.size());
    }
}
