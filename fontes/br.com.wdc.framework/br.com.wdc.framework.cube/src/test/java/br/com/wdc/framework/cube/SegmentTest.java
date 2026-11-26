package br.com.wdc.framework.cube;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Map;

import org.junit.Test;

public class SegmentTest {

    // ── Helper ─────────────────────────────────────────────────────────

    /** What a rendering technology hands a scope it draws: counts the redraws asked of it. */
    static class Drawing implements Runnable {
        int updates;

        @Override
        public void run() {
            updates++;
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

        final PageScope scope = bind(new PageScope());
        final PageScope.PanelScope panel = new PageScope.PanelScope();
        final Drawing pageDrawing = new Drawing();
        final Drawing panelDrawing = new Drawing();

        PagePresenter() {
            super(new GuardedActionTest.RecordingApp());
            panel.onPageChange = actionOf(panel, 2, FormParam.asInteger("p.page"), panel.page::set);
            scope.panel.set(panel);
            // The rendering technology draws the presenter's scope; the panel only when the page's drawing asks
            scope.setUpdater(pageDrawing);
        }
    }

    // ── Tests ──────────────────────────────────────────────────────────

    @Test
    public void untilSomethingDrawsItTheSegmentRedrawsItsOwner() {
        var p = new PagePresenter();

        p.panel.page.set(3);

        assertEquals("The owner draws the segment it holds", 1, p.pageDrawing.updates);
    }

    @Test
    public void onceDrawnAChangeInTheSegmentRedrawsOnlyWhatDrawsIt() {
        var p = new PagePresenter();
        p.panel.setUpdater(p.panelDrawing);

        p.panel.page.set(3);

        assertEquals(1, p.panelDrawing.updates);
        assertEquals("The owner is not redrawn", 0, p.pageDrawing.updates);
    }

    @Test
    public void whenNothingDrawsItAnyMoreTheOwnerIsRedrawnAgain() {
        var p = new PagePresenter();
        p.panel.setUpdater(p.panelDrawing);

        p.panel.clearUpdater(p.panelDrawing);
        p.panel.page.set(3);

        assertEquals(0, p.panelDrawing.updates);
        assertEquals(1, p.pageDrawing.updates);
    }

    @Test
    public void clearingSomeoneElsesUpdaterKeepsTheCurrentOne() {
        var p = new PagePresenter();
        var stale = new Drawing();
        p.panel.setUpdater(p.panelDrawing);

        p.panel.clearUpdater(stale);
        p.panel.page.set(3);

        assertEquals("A drawing that already went does not take the new one's place", 1, p.panelDrawing.updates);
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
}
