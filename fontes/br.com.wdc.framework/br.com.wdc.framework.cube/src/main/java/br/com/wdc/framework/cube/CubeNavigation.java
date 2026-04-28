package br.com.wdc.framework.cube;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class CubeNavigation<T extends CubeApplication> {

    protected final T app;

    protected final ConcurrentHashMap<Integer, CubePresenter> curPresenterMap;

    protected final ConcurrentHashMap<Integer, CubePresenter> newPresenterMap;

    protected int reflowCount;

    private CubePlace targetPlace;

    private CubeIntent sourceIntent;

    private List<CubePlace> steps;
    private boolean notInterruped;

    @SuppressWarnings("unchecked")
    public CubeNavigation(CubeApplication app) {
        this.reflowCount = 1;
        this.notInterruped = true;
        this.app = (T) app;
        this.curPresenterMap = app.presenterMap;
        this.newPresenterMap = new ConcurrentHashMap<>();
        this.steps = new ArrayList<>();

        this.sourceIntent = app.newIntent();
    }

    public CubeNavigation<T> step(CubePlace place) {
        this.steps.add(place);
        return this;
    }

    public boolean execute(CubeIntent targetIntent) {
        try {
            this.targetPlace = steps.get(steps.size() - 1);

            var result = true;
            var nextPresenters = new ArrayList<PresenterHolder>();

            for (int i = 0, iLast = steps.size() - 1; i <= iLast; i++) {
                var place = steps.get(i);
                var deepest = i == iLast;

                targetIntent.setPlace(place);

                var holder = new PresenterHolder();
                holder.id = place.getId();
                holder.deepst = deepest;

                var presenter = curPresenterMap.get(place.getId());
                if (presenter == null) {
                    var newPresenter = place.presenterFactory().apply(this.app);
                    newPresenterMap.put(place.getId(), newPresenter);
                    holder.presenter = newPresenter;
                    holder.initialize = true;
                } else {
                    newPresenterMap.put(place.getId(), presenter);
                    holder.presenter = presenter;
                    holder.initialize = false;
                }

                nextPresenters.add(holder);

                var goAhread = holder.presenter.applyParameters(targetIntent, holder.initialize, holder.deepst);
                if (!goAhread || !notInterruped) {
                    result = false;
                    break;
                }
            }

            commit(nextPresenters);
            return result;
        } catch (Exception caught) {
            rollback(caught);
            throw ExceptionUtils.asRuntimeException(caught);
        }
    }

    public void interrupt() {
        this.notInterruped = false;
        this.newPresenterMap.forEach(this.curPresenterMap::put);
    }

    private void rollback(Exception caught) {
        try {
            var presenterIds = new ArrayList<Integer>();
            presenterIds.addAll(this.curPresenterMap.keySet());
            presenterIds.sort(Comparator.naturalOrder());

            for (int i = 0, iLast = presenterIds.size() - 1; i <= iLast; i++) {
                var presenterId = presenterIds.get(i);
                try {
                    this.newPresenterMap.remove(presenterId);

                    var presenter = this.curPresenterMap.get(presenterId);
                    if (presenter != null) {
                        presenter.applyParameters(this.sourceIntent, false, i == iLast);
                    } else {
                        CubeApplication.LOG.warn("Missing presenter for ID={}", presenter);
                    }
                } catch (Exception otherCaught) {
                    CubeApplication.LOG.error("Restoring source state", caught);
                    caught.addSuppressed(otherCaught);
                }
            }

            if (this.newPresenterMap.size() > 0) {
                releasePresenters(this.newPresenterMap);
                this.newPresenterMap.clear();
            }
        } finally {
            this.app.updateHistory();
            this.app.navigation = null;
        }
    }

    private void commit(List<PresenterHolder> nextPresenters) {
        try {
            for (var holder : nextPresenters) {
                this.curPresenterMap.remove(holder.id);
            }

            if (this.curPresenterMap.size() > 0) {
                releasePresenters(this.curPresenterMap);
            }
        } finally {
            this.app.presenterMap = this.newPresenterMap;
            this.app.lastPlace = this.targetPlace;
            this.app.navigation = null;
            this.app.updateHistory();
        }
    }

    private static void releasePresenters(Map<Integer, CubePresenter> presenterInstanceMap) {
        var presenterIds = new ArrayList<Integer>();
        for (var presenterId : presenterInstanceMap.keySet()) {
            presenterIds.add(presenterId);
        }

        presenterIds.sort(Comparator.reverseOrder());

        for (var presenterId : presenterIds) {
            var presenter = presenterInstanceMap.remove(presenterId);
            if (presenter != null) {
                try {
                    presenter.release();
                } catch (Exception caught) {
                    CubeApplication.LOG.error("Releasing presenter", caught);
                }
            }
        }
    }

    private static class PresenterHolder {
        Integer id;
        CubePresenter presenter;
        boolean initialize;
        boolean deepst;
    }
}