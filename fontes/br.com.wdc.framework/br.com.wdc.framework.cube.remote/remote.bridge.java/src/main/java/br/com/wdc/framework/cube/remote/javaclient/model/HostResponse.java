package br.com.wdc.framework.cube.remote.javaclient.model;

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
public final class HostResponse {

    private final Long requestId;
    private final String uri;
    private final List<ViewStateSnapshot> viewStates;
    private final List<String> releasedViews;
    private final String cipheredAccessToken;

    public HostResponse(Long requestId, String uri,
                 List<ViewStateSnapshot> viewStates,
                 List<String> releasedViews,
                 String cipheredAccessToken) {
        this.requestId = requestId;
        this.uri = uri;
        this.viewStates = viewStates != null ? Collections.unmodifiableList(viewStates) : Collections.emptyList();
        this.releasedViews = releasedViews != null ? Collections.unmodifiableList(releasedViews) : Collections.emptyList();
        this.cipheredAccessToken = cipheredAccessToken;
    }

    /**
     * The {@code requestId} echo from the server, or {@code null} for async pushes.
     */
    public Long requestId() {
        return requestId;
    }

    /**
     * The current navigation URI (signed fragment), or {@code null} if unchanged since last response.
     */
    public String uri() {
        return uri;
    }

    /**
     * ViewState snapshots included in this response (delta — only dirty views).
     */
    public List<ViewStateSnapshot> viewStates() {
        return viewStates;
    }

    /**
     * Instance IDs of views that were released (removed) on the server.
     */
    public List<String> releasedViews() {
        return releasedViews;
    }

    /**
     * AES-GCM ciphered access token, if the server emitted one (e.g., after login).
     * Decipher with {@link SecretContext#decipher(String)}.
     * {@code null} if absent.
     */
    public String cipheredAccessToken() {
        return cipheredAccessToken;
    }

    /** Whether this response contains at least one ViewState update. */
    public boolean hasStates() {
        return !viewStates.isEmpty();
    }

    @Override
    public String toString() {
        return "HostResponse{requestId=" + requestId
                + ", uri='" + uri + "'"
                + ", states=" + viewStates.size()
                + ", released=" + releasedViews.size()
                + "}";
    }
}
