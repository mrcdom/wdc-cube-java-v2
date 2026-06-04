package br.com.wdc.framework.cube.remote;

/**
 * Thrown by {@link RemoteApplicationRegistry#getOrCreate} when the server has reached
 * the configured maximum number of concurrent application sessions
 * ({@code server.maxSessions} in {@code application.toml}).
 */
public final class CapacityExceededException extends RuntimeException {

    private static final long serialVersionUID = -7614024256856350620L;

    public CapacityExceededException(int maxInstances) {
        super("Server at capacity: maximum " + maxInstances + " sessions reached");
    }
}
