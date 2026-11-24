package br.com.wdc.framework.cube.remote.bridge.java.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Parsed representation of a single server message received over WebSocket.
 * <p>
 * A response may be:
 * <ul>
 *   <li>A <em>reply</em> to a client request — contains {@code requestId}</li>
 *   <li>An <em>async push</em> from the registry — no {@code requestId}</li>
 * </ul>
 * Both types update the {@link ScopeMap} via {@link #scopes()}.
 */
public record HostResponse(Long requestId, String uri,
                           List<ScopeSnapshot> scopes,
                           List<String> releasedViews,
                           String cipheredAccessToken,
                           Map<String, Object> storageDelta) {

    public HostResponse {
        scopes = scopes != null ? Collections.unmodifiableList(scopes) : Collections.emptyList();
        releasedViews = releasedViews != null ? Collections.unmodifiableList(releasedViews) : Collections.emptyList();
    }

    /** Whether this response contains at least one scope update. */
    public boolean hasScopes() {
        return !scopes.isEmpty();
    }

}
