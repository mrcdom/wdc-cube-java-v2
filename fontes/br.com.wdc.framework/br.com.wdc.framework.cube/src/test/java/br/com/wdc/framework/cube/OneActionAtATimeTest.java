package br.com.wdc.framework.cube;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/** The actions of one application run one at a time; an action that runs another one runs it at once. */
public class OneActionAtATimeTest {

    static class BuyPresenter extends AbstractCubePresenter<GuardedActionTest.RecordingApp> {

        final List<String> calls = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch firstStarted = new CountDownLatch(1);
        final CountDownLatch releaseFirst = new CountDownLatch(1);

        final Action slowBuy = action(() -> {
            calls.add("first started");
            firstStarted.countDown();
            releaseFirst.await(5, TimeUnit.SECONDS);   // waits on the network
            calls.add("first done");
        });
        final Action buy = action(() -> calls.add("second"));
        final Action outer = action(() -> {
            calls.add("outer");
            buy.run();
        });

        BuyPresenter(GuardedActionTest.RecordingApp app) {
            super(app);
        }
    }

    @Test
    public void aSecondActionWaitsForTheFirst() throws Exception {
        var p = new BuyPresenter(new GuardedActionTest.RecordingApp());

        var first = new Thread(p.slowBuy::run);
        first.start();
        assertTrue(p.firstStarted.await(5, TimeUnit.SECONDS));

        var second = new Thread(p.buy::run);
        second.start();
        second.join(200);
        assertTrue("the second action waits while the first runs", second.isAlive());

        p.releaseFirst.countDown();
        first.join(5000);
        second.join(5000);
        assertFalse(second.isAlive());
        assertEquals(List.of("first started", "first done", "second"), p.calls);
    }

    @Test
    public void anActionThatRunsAnotherRunsItAtOnce() {
        var p = new BuyPresenter(new GuardedActionTest.RecordingApp());

        p.outer.run();

        assertEquals(List.of("outer", "second"), p.calls);
    }
}
