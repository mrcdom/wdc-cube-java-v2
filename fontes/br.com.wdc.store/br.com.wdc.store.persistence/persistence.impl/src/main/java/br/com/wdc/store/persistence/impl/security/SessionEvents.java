package br.com.wdc.store.persistence.impl.security;

import java.util.function.Consumer;

/**
 * How the backend instances tell each other that a session changed — signed out, expired, or renewed with a new
 * refresh token and key — so that none keeps answering from an old copy in its memory. Each instance keeps the sessions
 * it has read ({@link AccessContextCache}) and reads the database only for one it does not hold; without this, a
 * session ended on another instance stays valid here until its access token expires.
 * <p>
 * {@link #NONE} tells no one: right for one instance. The backend's mesh between instances (with {@code cluster.peers})
 * tells the instances it connects.
 */
public interface SessionEvents {

	/** Tells the other instances that {@code sessionId} changed; called once the change is stored. */
	void sessionChanged(String sessionId);

	/**
	 * What to do when another instance changes a session ({@code onChanged}, with its id), and when some notices may
	 * have been missed — so that nothing kept in memory can be trusted ({@code onMissed}).
	 */
	void listen(Consumer<String> onChanged, Runnable onMissed);

	/** One instance: there is no one to tell. */
	SessionEvents NONE = new SessionEvents() {

		@Override
		public void sessionChanged(String sessionId) {
			// no other instance
		}

		@Override
		public void listen(Consumer<String> onChanged, Runnable onMissed) {
			// no other instance
		}
	};
}
