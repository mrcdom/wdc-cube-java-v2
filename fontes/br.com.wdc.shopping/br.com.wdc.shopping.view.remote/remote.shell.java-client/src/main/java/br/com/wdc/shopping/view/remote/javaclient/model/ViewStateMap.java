package br.com.wdc.shopping.view.remote.javaclient.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe registry of all ViewStates currently active in the session.
 * <p>
 * Updated by each incoming server message. Views are added/replaced on
 * {@code states} payloads and removed on {@code releasedViews} payloads.
 */
public final class ViewStateMap {

    private final ConcurrentHashMap<String, ViewStateSnapshot> map = new ConcurrentHashMap<>();

    /**
     * Applies a list of state objects from the server {@code "states"} array.
     * Each object must contain a {@code "#"} field with the instanceId.
     */
    @SuppressWarnings("unchecked")
    public void applyStates(List<Object> stateList) {
        if (stateList == null) return;
        for (Object rawState : stateList) {
            if (rawState instanceof Map<?, ?> rawMap) {
                var stateMap = new HashMap<String, Object>((Map<String, Object>) rawMap);
                String instanceId = String.valueOf(stateMap.remove("#"));
                if (instanceId != null && !instanceId.equals("null")) {
                    map.put(instanceId, new ViewStateSnapshot(instanceId, stateMap));
                }
            }
        }
    }

    /**
     * Removes views listed in the server {@code "releasedViews"} array.
     */
    public void applyReleased(List<String> releasedIds) {
        if (releasedIds == null) return;
        releasedIds.forEach(map::remove);
    }

    /**
     * Returns the current snapshot for a given instanceId, or {@code null} if not present.
     */
    public ViewStateSnapshot get(String instanceId) {
        return map.get(instanceId);
    }

    /**
     * Returns all currently active ViewState snapshots.
     */
    public Collection<ViewStateSnapshot> all() {
        return Collections.unmodifiableCollection(map.values());
    }

    /**
     * Returns all active ViewState snapshots whose instanceId belongs to the given classId,
     * sorted by instance number descending so that the most recently created instance comes first.
     * <p>
     * The instanceId format is {@code "classId:instanceNumber"} (e.g., {@code "productList:1"}).
     * This method matches snapshots where {@code instanceId.startsWith(classId + ":")}.
     * <p>
     * The server may keep stale instances in the map if it does not send {@code releasedViews}
     * for them (e.g., when navigating away from a product to another product). Sorting by
     * instance number descending ensures {@code findFirst()} returns the most recent instance.
     */
    public Collection<ViewStateSnapshot> allByClassId(String classId) {
        String prefix = classId + ":";
        return map.values().stream()
                .filter(vs -> vs.instanceId().startsWith(prefix))
                .sorted(Comparator.comparingInt(ViewStateMap::instanceNumber).reversed())
                .collect(Collectors.toUnmodifiableList());
    }

    private static int instanceNumber(ViewStateSnapshot vs) {
        String id = vs.instanceId();
        int colon = id.lastIndexOf(':');
        if (colon < 0) return 0;
        try {
            return Integer.parseInt(id.substring(colon + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Returns the number of active views in the session.
     */
    public int size() {
        return map.size();
    }

    /**
     * Clears all state (used on reconnect).
     */
    public void clear() {
        map.clear();
    }
}
