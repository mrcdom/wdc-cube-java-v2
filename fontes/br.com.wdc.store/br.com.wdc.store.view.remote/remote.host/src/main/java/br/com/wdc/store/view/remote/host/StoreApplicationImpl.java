package br.com.wdc.store.view.remote.host;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.cube.CubeApplication;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.ScopeViews;
import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import br.com.wdc.framework.cube.remote.RemoteApplication;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.framework.cube.remote.RemoteApplicationSupport;
import br.com.wdc.framework.cube.remote.RemoteViewImpl;
import br.com.wdc.store.presentation.StoreApplication;

/**
 * Store-specific remote application implementation.
 * <p>
 * Extends {@link StoreApplication} (keeping presenter compatibility) and implements {@link RemoteApplication} by delegating to
 * {@link RemoteApplicationSupport}.
 */
public class StoreApplicationImpl extends StoreApplication implements RemoteApplication {

	private static final Log LOG = Log.getLogger(StoreApplicationImpl.class);

	private static RemoteAppSecurity appSecurity;
	private static RemoteApplicationRegistry<StoreApplicationImpl> registry;

	/**
	 * The views of every scope (a presenter's, a segment's, a row's): a remote view, known to the client by the
	 * CLASS_ID its scope class declares. The browser view draws the root presenter's scope; each view makes the views
	 * of the scopes it holds as it is written.
	 */
	private static final ScopeViews SCOPE_VIEWS = new ScopeViews().fallback(RemoteViewImpl::forScope);

	/**
	 * Initializes module-level references. Called once during bootstrap.
	 */
	public static void initialize(RemoteAppSecurity sec, RemoteApplicationRegistry<StoreApplicationImpl> reg) {
		appSecurity = sec;
		registry = reg;
	}

	public static RemoteApplicationRegistry<StoreApplicationImpl> getRegistry() {
		return registry;
	}

	// :: Instance fields

	private final RemoteApplicationSupport support;
	private final Defer cleanUp = new Defer();

	// :: Constructor

	public StoreApplicationImpl(String id) {
		this.setScopeViews(SCOPE_VIEWS);
		this.support = new RemoteApplicationSupport(id, new HostCallbacks(), appSecurity);
		this.support.postConstruct(this);
	}

	@Override
	protected Map<Integer, CubePresenter> createPresenterMap() {
		return new ConcurrentHashMap<>();
	}

	@Override
	protected Map<String, Object> createAttributeMap() {
		return new ConcurrentHashMap<>();
	}

	// :: StoreApplication overrides

	@Override
	public boolean isAuthenticated() {
		return this.getSubject() != null;
	}

	@Override
	public String b64Cipher(String text) {
		return this.support.getDataSecurity().b64Cipher(text);
	}

	@Override
	public String b64Decipher(String b64Text) {
		return this.support.getDataSecurity().b64Decipher(b64Text);
	}

	@Override
	public String getClientIp() {
		return this.support.getClientIp();
	}

	@Override
	public ClientStorage clientSessionStore() {
		return this.support.getClientStorage().session();
	}

	@Override
	public ClientStorage clientPersistentStore() {
		return this.support.getClientStorage().persistent();
	}

	@Override
	public void updateHistory() {
		this.support.updateHistory();
	}

	// :: Lifecycle

	@Override
	public void release() {
		try {
			this.support.release();
			this.cleanUp.run();
			super.release();
		} catch (Exception caught) {
			LOG.error("Running removeInstanceAction", caught);
		} finally {
			registry.remove(this.getId());
			LOG.info("Application removed: {}", this.getId());
		}
	}

	// :: RemoteApplication interface

	@Override
	public RemoteApplicationSupport getSupport() {
		return this.support;
	}

	// :: Factory (used by RemoteApplicationRegistry)

	@SuppressWarnings("unchecked")
	public static StoreApplicationImpl createApp(String appId, Map<String, Object> request) {
		var app = new StoreApplicationImpl(appId);
		try {
			app.cleanUp.push(() -> registry.remove(appId));
			app.support.setTimeSpan(registry.getSessionTimeSpan());

			// Initialize data security before auto-login (needed for decipher)
			var secret = (String) request.get("secret");
			if (StringUtils.isNotBlank(secret)) {
				app.support.getDataSecurity().updateSecret(secret);
			}

			// Populate client storage from bootstrap payload (both scopes)
			app.support.getClientStorage().loadFromBootstrap(request.get("storage"));

			var path = app.getFragment();
			var browserViewId = app.getSupport().getBrowserPresenter().getView().instanceId();

			Map<String, Object> browserFormData = (Map<String, Object>) request.get(browserViewId);
			if (browserFormData != null) {
				var requestPath = (String) browserFormData.get("p.path");
				if (StringUtils.isNotBlank(requestPath)) {
					path = requestPath;
				}
			}

			app.safeGo(path);
		} catch (Exception caught) {
			app.release();
			return ExceptionUtils.rethrow(caught);
		}
		return app;
	}

	// :: Host callbacks

	private class HostCallbacks implements RemoteApplicationSupport.Host {

		@Override
		public CubeApplication getCubeApp() {
			return StoreApplicationImpl.this;
		}

		@Override
		public RemoteApplicationRegistry<?> getRegistry() {
			return registry;
		}

		@Override
		public RemoteAppSecurity getAppSecurity() {
			return appSecurity;
		}

		@Override
		public void performGo(CubeIntent intent) {
			StoreApplicationImpl.this.go(intent);
		}

		@Override
		public boolean isAuthenticated() {
			return StoreApplicationImpl.this.isAuthenticated();
		}
	}
}
