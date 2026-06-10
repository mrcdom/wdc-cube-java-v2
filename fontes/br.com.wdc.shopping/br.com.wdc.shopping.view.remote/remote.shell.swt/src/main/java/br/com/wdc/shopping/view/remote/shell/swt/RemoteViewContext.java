package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.Map;

import br.com.wdc.framework.cube.remote.bridge.java.model.ViewStateSnapshot;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;

/**
 * Context provided to remote view adapters, giving access to remote state and event submission. Implemented by {@link ShoppingSwtRemoteApp}.
 */
interface RemoteViewContext {

	/** Returns the current {@link ViewStateSnapshot} for the given vsid, or {@code null} if unavailable. */
	ViewStateSnapshot viewState(String vsid);

	/** Looks up a registered view by vsid, or {@code null} if not yet created. */
	AbstractViewSwt viewLookup(String vsid);

	/** Submits an event to the Host for the given vsid. */
	void submitEvent(String vsid, int eventCode, Map<String, Object> form);

	/** Submits a login event with the password ciphered via SecretContext. */
	void submitLogin(String vsid, String userName, String password);

}
