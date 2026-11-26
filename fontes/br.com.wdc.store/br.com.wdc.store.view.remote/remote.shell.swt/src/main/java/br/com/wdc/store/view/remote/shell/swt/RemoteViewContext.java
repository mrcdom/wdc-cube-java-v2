package br.com.wdc.store.view.remote.shell.swt;

import java.util.Map;

import br.com.wdc.framework.cube.Scope;

import br.com.wdc.framework.cube.remote.bridge.java.model.ScopeSnapshot;
import br.com.wdc.store.view.swt.AbstractViewSwt;

/**
 * Context provided to remote view adapters, giving access to the remote scopes and event submission. Implemented by {@link StoreSwtRemoteApp}.
 */
interface RemoteViewContext {

	/** Returns the current {@link ScopeSnapshot} for the given vsid, or {@code null} if unavailable. */
	ScopeSnapshot scope(String vsid);

	/** Looks up a registered view by vsid, or {@code null} if not yet created. */
	AbstractViewSwt viewLookup(String vsid);

	/**
	 * Tells which view draws a scope rebuilt from what the host sent: the one the host referred to by vsid. Returns the
	 * scope, for a prop.
	 */
	<S extends Scope> S drawnBy(S scope, String vsid);

	/** Submits an event to the Host for the given vsid. */
	void submitEvent(String vsid, int eventCode, Map<String, Object> form);

	/** Submits a login event with the password ciphered with the connection's key. */
	void submitLogin(String vsid, String userName, String password);

}
