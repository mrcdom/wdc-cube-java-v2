package br.com.wdc.framework.cube;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.util.Rethrow;

public class CubeNavigation<T extends CubeApplication> {

    private static final Log LOG = Log.getLogger(CubeNavigation.class.getSimpleName());

    protected final T app;

    protected final Map<Integer, CubePresenter> curPresenterMap;

    protected final Map<Integer, CubePresenter> newPresenterMap;

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
        this.newPresenterMap = new HashMap<>();
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
                    presenter = newPresenterMap.get(place.getId());
                }
                if (presenter == null) {
                    var newPresenter = place.presenterFactory().apply(this.app);
                    newPresenterMap.put(place.getId(), newPresenter);
                    holder.presenter = newPresenter;
                    holder.initialize = true;
                } else {
                    holder.presenter = presenter;
                    holder.initialize = false;
                }

                nextPresenters.add(holder);

                var goAhead = holder.presenter.applyParameters(targetIntent, holder.initialize, holder.deepst);
                if (!goAhead || !notInterruped) {
                    result = false;
                    break;
                }
            }

            if (notInterruped) {
                commit(nextPresenters);
            }
            return result;
        } catch (Exception caught) {
            if (notInterruped) {
                rollback(caught);
            }
            throw Rethrow.asRuntimeException(caught);
        }
    }

    public void interrupt() {
        this.notInterruped = false;
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
                        LOG.debug("Missing presenter for ID=" + presenter);
                    }
                } catch (Exception otherCaught) {
                    LOG.debug("Restoring source state: " + caught.getMessage());
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
            // Build the final presenter map from accepted presenters
            var finalMap = new HashMap<Integer, CubePresenter>();
            for (var holder : nextPresenters) {
                finalMap.put(holder.id, holder.presenter);
                newPresenterMap.remove(holder.id);
                curPresenterMap.remove(holder.id);
            }

            // Release non-accepted created and replaced presenters (leaf-first)
            newPresenterMap.putAll(curPresenterMap);
            if (curPresenterMap.size() > 0) {
                releasePresenters(newPresenterMap);
            }

            // Update the existing map in-place
            this.app.presenterMap.clear();
            this.app.presenterMap.putAll(finalMap);
        } finally {
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
                    LOG.error("Releasing presenter: " + caught.getMessage());
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