package br.com.wdc.shopping.view.remote.host;

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
import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import br.com.wdc.framework.cube.remote.RemoteApplication;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.framework.cube.remote.RemoteApplicationSupport;
import br.com.wdc.framework.cube.remote.RemoteViewImpl;
import br.com.wdc.shopping.presentation.ProxyRepositoryWrapper;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;

/**
 * Shopping-specific remote application implementation.
 * <p>
 * Extends {@link ShoppingApplication} (keeping presenter compatibility) and implements {@link RemoteApplication} by delegating to
 * {@link RemoteApplicationSupport}.
 */
public class ShoppingApplicationImpl extends ShoppingApplication implements RemoteApplication {

	private static final Log LOG = Log.getLogger(ShoppingApplicationImpl.class);

	private static RemoteAppSecurity appSecurity;
	private static RemoteApplicationRegistry<ShoppingApplicationImpl> registry;

	static {
		RootPresenter.createView = p -> new RemoteViewImpl((RemoteApplication) p.app, p, p.state, p.skeleton());
		LoginPresenter.createView = p -> new RemoteViewImpl((RemoteApplication) p.app, p, p.state, p.skeleton());
		HomePresenter.createView = p -> new RemoteViewImpl((RemoteApplication) p.app, p, p.state, p.skeleton());
		ProductPresenter.createView = p -> new RemoteViewImpl((RemoteApplication) p.app, p, p.state, p.skeleton());
		CartPresenter.createView = p -> new RemoteViewImpl((RemoteApplication) p.app, p, p.state, p.skeleton());
		ReceiptPresenter.createView = p -> new RemoteViewImpl((RemoteApplication) p.app, p, p.state, p.skeleton());
		ProductsPanelPresenter.createView = p -> new RemoteViewImpl((RemoteApplication) p.app, p, p.state, p.skeleton());
		PurchasesPanelPresenter.createView = p -> new RemoteViewImpl((RemoteApplication) p.app, p, p.state, p.skeleton());
	}

	/**
	 * Initializes module-level references. Called once during bootstrap.
	 */
	public static void initialize(RemoteAppSecurity sec, RemoteApplicationRegistry<ShoppingApplicationImpl> reg) {
		appSecurity = sec;
		registry = reg;
	}

	public static RemoteApplicationRegistry<ShoppingApplicationImpl> getRegistry() {
		return registry;
	}

	// :: Instance fields

	private final RemoteApplicationSupport support;
	private final Defer cleanUp = new Defer();

	// :: Constructor

	public ShoppingApplicationImpl(String id) {
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

	@Override
	protected <T> T createDelegate(Class<T> repoInterface, T delegate) {
		return ProxyRepositoryWrapper.wrap(repoInterface, delegate, this::getSecurityContext);
	}

	// :: ShoppingApplication overrides

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
	public static ShoppingApplicationImpl createApp(String appId, Map<String, Object> request) {
		var app = new ShoppingApplicationImpl(appId);
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

			Map<String, Object> browserViewState = (Map<String, Object>) request.get(browserViewId);
			if (browserViewState != null) {
				var requestPath = (String) browserViewState.get("p.path");
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
			return ShoppingApplicationImpl.this;
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
			ShoppingApplicationImpl.this.go(intent);
		}

		@Override
		public boolean isAuthenticated() {
			return ShoppingApplicationImpl.this.isAuthenticated();
		}
	}
}
