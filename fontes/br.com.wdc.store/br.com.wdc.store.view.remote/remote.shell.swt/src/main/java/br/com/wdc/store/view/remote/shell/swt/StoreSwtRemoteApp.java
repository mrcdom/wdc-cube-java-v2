package br.com.wdc.store.view.remote.shell.swt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.commons.storage.PreferencesClientStorage;
import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;
import br.com.wdc.framework.cube.remote.bridge.java.model.ScopeSnapshot;
import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.view.swt.AbstractViewSwt;
import br.com.wdc.store.view.swt.SwtApp;
import br.com.wdc.store.view.swt.util.ProductImageCache;

/**
 * SWT shell application that renders WDC Store using the scopes it receives from the Host server.
 * <p>
 * The app:
 * <ol>
 * <li>Connects to the Host via {@link HostClient}</li>
 * <li>Creates SWT views from {@code swt.commons} keyed by classId</li>
 * <li>Supplies each view with a {@code scopeSupplier} that translates {@code ScopeSnapshot} values into typed Scope objects</li>
 * <li>Sends events back to the Host via non-blocking {@code submit()}</li>
 * <li>A background listener thread applies host responses on the UI thread</li>
 * <li>A render timer flushes dirty views at ~60 fps</li>
 * </ol>
 */
public class StoreSwtRemoteApp implements SwtApp, RemoteViewContext {

	private static final int FRAME_INTERVAL_MS = 16;
	private static final Logger LOG = LoggerFactory.getLogger(StoreSwtRemoteApp.class);

	private final Display display;
	private final Shell shell;
	private Composite offscreen;

	private HostClient hostClient;
	private final ClientStorage persistentStorage = new PreferencesClientStorage(StoreSwtRemoteApp.class);

	/** vsid → view */
	private final Map<String, AbstractViewSwt> viewRegistry = new ConcurrentHashMap<>();

	/** Views that need a doUpdate() call on the next render tick */
	private final Set<AbstractViewSwt> dirtyViews = Collections.newSetFromMap(new ConcurrentHashMap<>());

	/** classId → factory(vsid) */
	private final Map<String, Function<String, AbstractViewSwt>> viewFactories = new ConcurrentHashMap<>();

	/** The host's alert being shown, so that a redraw of the browser does not show it twice. */
	private int shownAlertId;

	private final AtomicReference<Runnable> renderTimerRunnable = new AtomicReference<>(() -> {
	});

	public StoreSwtRemoteApp(Display display, Shell shell) {
		this.display = display;
		this.shell = shell;
		this.offscreen = new Composite(shell, SWT.NONE);
		this.offscreen.setVisible(false);
		this.offscreen.setSize(0, 0);
		this.registerViewFactories();
	}

	// :: SwtApp

	@Override
	public Display getDisplay() {
		return this.display;
	}

	@Override
	public Shell getShell() {
		return this.shell;
	}

	@Override
	public Composite getOffscreen() {
		return this.offscreen;
	}

	@Override
	public void setRootPane(Composite pane) {
		// No-op: RootViewSwt sets topControl on the shell's StackLayout directly
	}

	@Override
	public void markDirty(AbstractViewSwt view) {
		this.dirtyViews.add(view);
	}

	@Override
	public void runAction(Runnable action) {
		// Events are non-blocking WS submits — run directly on the UI thread
		action.run();
	}

	@Override
	public void onActionError(String context, Throwable e) {
		LOG.error("Action error in {}: {}", context, e.getMessage(), e);
	}

	// :: Connect

	private static final String STORAGE_KEY_INTENT = "session.intent";

	/**
	 * Connects to the Host, awaits the initial scope push, starts the listener thread and render loop. Call from the UI thread before the event loop.
	 */
	public void connect(String serverUrl) throws Exception {
		LOG.info("Connecting to {}", serverUrl);
		this.hostClient = HostClient.connect(serverUrl, persistentStorage);
		ProductRepository.BEAN.set(new RemoteProductImageRepository(serverUrl));

		// Read saved intent BEFORE processing any response (responses may overwrite it)
		var savedIntent = persistentStorage.get(STORAGE_KEY_INTENT);

		// Initial async state push
		var initial = this.hostClient.awaitResponse();
		this.hostClient.applyStorageDelta(initial);
		persistUri(initial.uri());
		this.applyResponse(initial);

		// Restore last intent if logged in (not on login page)
		if (savedIntent != null && !savedIntent.startsWith("login")) {
			try {
				var navResp = this.hostClient.navigate(savedIntent);
				this.hostClient.applyStorageDelta(navResp);
				persistUri(navResp.uri());
				this.applyResponse(navResp);
			} catch (Exception e) {
				LOG.warn("Could not restore intent '{}': {}", savedIntent, e.getMessage());
			}
		}

		this.startListenerThread();
		this.startRenderLoop();
	}

	private void persistUri(String uri) {
		if (uri == null) return;
		var path = uri.contains("#") ? uri.substring(uri.indexOf('#') + 1) : uri;
		if (!path.isBlank()) {
			persistentStorage.set(STORAGE_KEY_INTENT, path);
		}
	}

	// :: Internal

	private void registerViewFactories() {
		this.viewFactories.put(RootViewSwtRemote.CID, vsid -> new RootViewSwtRemote(this, vsid, this));
		this.viewFactories.put(LoginViewSwtRemote.CID, vsid -> new LoginViewSwtRemote(this, vsid, this));
		this.viewFactories.put(HomeViewSwtRemote.CID, vsid -> new HomeViewSwtRemote(this, vsid, this));
		this.viewFactories.put(CartViewSwtRemote.CID, vsid -> new CartViewSwtRemote(this, vsid, this));
		this.viewFactories.put(CartItemViewSwtRemote.CID, vsid -> new CartItemViewSwtRemote(this, vsid, this));
		this.viewFactories.put(ProductViewSwtRemote.CID, vsid -> new ProductViewSwtRemote(this, vsid, this));
		this.viewFactories.put(ProductsPanelViewSwtRemote.CID, vsid -> new ProductsPanelViewSwtRemote(this, vsid, this));
		this.viewFactories.put(PurchasesPanelViewSwtRemote.CID, vsid -> new PurchasesPanelViewSwtRemote(this, vsid, this));
		this.viewFactories.put(ReceiptViewSwtRemote.CID, vsid -> new ReceiptViewSwtRemote(this, vsid, this));
	}

	private void applyResponse(HostResponse resp) {
		// Create views for newly published vsids
		for (var snapshot : resp.scopes()) {
			var vsid = snapshot.instanceId();
			if (!this.viewRegistry.containsKey(vsid)) {
				var classId = classIdOf(vsid);
				var factory = this.viewFactories.get(classId);
				if (factory != null) {
					var view = factory.apply(vsid);
					this.viewRegistry.put(vsid, view);
				} else {
					LOG.debug("No factory for classId={} (vsid={})", classId, vsid);
				}
			}
			var view = this.viewRegistry.get(vsid);
			if (view != null) {
				this.markDirty(view);
			}
		}

		this.showHostAlert();

		// Dispose released views
		for (var vsid : resp.releasedViews()) {
			var view = this.viewRegistry.remove(vsid);
			if (view != null) {
				this.display.asyncExec(() -> {
					if (!view.getElement().isDisposed()) {
						view.getElement().setVisible(false);
					}
				});
			}
		}
	}

	/**
	 * The host's alert (browser {@code alertId}/{@code alertArgs}): shown until dismissed, which tells the host (event 1
	 * of the browser), whose alert action may run then.
	 */
	private void showHostAlert() {
		var browser = this.scope(HostClient.BROWSER_VSID);
		var alertId = browser != null && browser.getLong("alertId") != null ? browser.getLong("alertId").intValue() : 0;
		if (alertId == 0 || alertId == this.shownAlertId) {
			this.shownAlertId = alertId;
			return;
		}
		this.shownAlertId = alertId;

		var args = new ArrayList<String>();
		if (browser.values().get("alertArgs") instanceof List<?> list) {
			for (var item : list) {
				args.add(String.valueOf(item));
			}
		}
		var message = switch (alertId) {
			case -2 -> "The URI " + (args.isEmpty() ? "" : args.get(0)) + " is not accessible";
			default -> args.isEmpty() ? "An unexpected error occurred" : args.get(0);
		};
		var detail = args.size() > 1 ? args.get(1) : null;

		this.display.asyncExec(() -> {
			if (this.shell.isDisposed()) {
				return;
			}
			var box = new MessageBox(this.shell, SWT.ICON_WARNING | SWT.OK);
			box.setText("Warning!");
			box.setMessage(detail != null ? message + "\n\n" + detail : message);
			box.open();
			this.shownAlertId = 0;
			this.submitEvent(HostClient.BROWSER_VSID, 1, Map.of());
		});
	}

	private void startListenerThread() {
		var thread = new Thread(() -> {
			var shouldStop = false;
			while (!shouldStop && !this.display.isDisposed() && this.hostClient != null) {
				try {
					var resp = this.hostClient.awaitResponse(java.time.Duration.ofSeconds(30));
					this.hostClient.applyStorageDelta(resp);
					persistUri(resp.uri());
					this.display.asyncExec(() -> this.applyResponse(resp));
				} catch (java.util.concurrent.TimeoutException e) {
					// heartbeat timeout — continue loop
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					shouldStop = true;
				} catch (Exception e) {
					LOG.error("Listener error: {}", e.getMessage(), e);
					shouldStop = true;
				}
			}
		}, "swt-remote-listener");
		thread.setDaemon(true);
		thread.start();
	}

	private void startRenderLoop() {
		this.renderTimerRunnable.set(() -> {
			if (!this.display.isDisposed()) {
				this.flushDirtyViews();
				this.display.timerExec(FRAME_INTERVAL_MS, this.renderTimerRunnable.get());
			}
		});
		this.display.timerExec(FRAME_INTERVAL_MS, this.renderTimerRunnable.get());
	}

	private void flushDirtyViews() {
		var batch = new ArrayList<>(this.dirtyViews);
		this.dirtyViews.removeAll(batch);
		for (var view : batch) {
			if (!view.getElement().isDisposed()) {
				view.performUpdate();
			}
		}
	}

	@Override
	public void submitEvent(String vsid, int eventCode, Map<String, Object> form) {
		if (this.hostClient != null) {
			this.hostClient.submit(vsid, eventCode, form);
		}
	}

	@Override
	public void submitLogin(String vsid, String userName, String password) {
		if (this.hostClient != null) {
			String ciphered = this.hostClient.secretContext().encipher(password);
			this.hostClient.submit(vsid, 1, Map.of("p.userName", userName, "p.password", ciphered));
		}
	}

	@Override
	public ScopeSnapshot scope(String vsid) {
		return this.hostClient != null ? this.hostClient.scope(vsid) : null;
	}

	@Override
	public AbstractViewSwt viewLookup(String vsid) {
		return this.viewRegistry.get(vsid);
	}

	// :: Utilities

	private static String classIdOf(String vsid) {
		int colon = vsid.lastIndexOf(':');
		return colon >= 0 ? vsid.substring(0, colon) : vsid;
	}

	public void stop() {
		this.renderTimerRunnable.set(() -> {
		});
		if (this.hostClient != null) {
			try {
				this.hostClient.close();
			} catch (Exception e) {
				LOG.debug("Ignored error closing HostClient: {}", e.getMessage());
			}
			this.hostClient = null;
		}
		ProductRepository.BEAN.set(null);
		ProductImageCache.getInstance().dispose();
	}
}
