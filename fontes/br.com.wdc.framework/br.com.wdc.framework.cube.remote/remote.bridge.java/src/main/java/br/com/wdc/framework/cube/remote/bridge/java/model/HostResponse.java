package br.com.wdc.framework.cube.remote.bridge.java.model;

import java.util.Collections;
import java.util.List;

/**
 * Parsed representation of a single server message received over WebSocket.
 * <p>
 * A response may be:
 * <ul>
 *   <li>A <em>reply</em> to a client request — contains {@code requestId}</li>
 *   <li>An <em>async push</em> from the registry — no {@code requestId}</li>
 * </ul>
 * Both types update the {@link ViewStateMap} via {@link #viewStates()}.
 */
public record HostResponse(Long requestId, String uri,
                           List<ViewStateSnapshot> viewStates,
                           List<String> releasedViews,
                           String cipheredAccessToken) {

    public HostResponse {
        viewStates = viewStates != null ? Collections.unmodifiableList(viewStates) : Collections.emptyList();
        releasedViews = releasedViews != null ? Collections.unmodifiableList(releasedViews) : Collections.emptyList();
    }

    /** Whether this response contains at least one ViewState update. */
    public boolean hasStates() {
        return !viewStates.isEmpty();
    }

}
