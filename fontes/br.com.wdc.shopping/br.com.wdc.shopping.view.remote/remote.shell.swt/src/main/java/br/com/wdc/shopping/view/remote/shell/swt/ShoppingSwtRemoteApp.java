package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;
import br.com.wdc.framework.cube.remote.bridge.java.model.ViewStateSnapshot;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.util.ProductImageCache;

/**
 * SWT shell application that renders WDC Shopping using remote state from the Host server.
 * <p>
 * The app:
 * <ol>
 * <li>Connects to the Host via {@link HostClient}</li>
 * <li>Creates SWT views from {@code swt.commons} keyed by classId</li>
 * <li>Supplies each view with a {@code stateSupplier} that translates {@code ViewStateSnapshot} fields into typed ViewState objects</li>
 * <li>Sends events back to the Host via non-blocking {@code submit()}</li>
 * <li>A background listener thread applies host responses on the UI thread</li>
 * <li>A render timer flushes dirty views at ~60 fps</li>
 * </ol>
 */
public class ShoppingSwtRemoteApp implements SwtApp, RemoteViewContext {

	private static final int FRAME_INTERVAL_MS = 16;
	private static final Logger LOG = LoggerFactory.getLogger(ShoppingSwtRemoteApp.class);

	private final Display display;
	private final Shell shell;
	private Composite offscreen;

	private HostClient hostClient;

	/** vsid → view */
	private final Map<String, AbstractViewSwt> viewRegistry = new ConcurrentHashMap<>();

	/** Views that need a doUpdate() call on the next render tick */
	private final Set<AbstractViewSwt> dirtyViews = Collections.newSetFromMap(new ConcurrentHashMap<>());

	/** classId → factory(vsid) */
	private final Map<String, Function<String, AbstractViewSwt>> viewFactories = new ConcurrentHashMap<>();

	private final AtomicReference<Runnable> renderTimerRunnable = new AtomicReference<>(() -> {
	});

	public ShoppingSwtRemoteApp(Display display, Shell shell) {
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

	/**
	 * Connects to the Host, awaits the initial state push, starts the listener thread and render loop. Call from the UI thread before the event loop.
	 */
	public void connect(String serverUrl) throws Exception {
		LOG.info("Connecting to {}", serverUrl);
		this.hostClient = HostClient.connect(serverUrl);
		ProductRepository.BEAN.set(new RemoteProductImageRepository(serverUrl));

		// Initial async state push
		var initial = this.hostClient.awaitResponse();
		this.applyResponse(initial);

		this.startListenerThread();
		this.startRenderLoop();
	}

	// :: Internal

	private void registerViewFactories() {
		this.viewFactories.put(RootViewSwtRemote.CID, vsid -> new RootViewSwtRemote(this, vsid, this));
		this.viewFactories.put(LoginViewSwtRemote.CID, vsid -> new LoginViewSwtRemote(this, vsid, this));
		this.viewFactories.put(HomeViewSwtRemote.CID, vsid -> new HomeViewSwtRemote(this, vsid, this));
		this.viewFactories.put(CartViewSwtRemote.CID, vsid -> new CartViewSwtRemote(this, vsid, this));
		this.viewFactories.put(ProductViewSwtRemote.CID, vsid -> new ProductViewSwtRemote(this, vsid, this));
		this.viewFactories.put(ProductsPanelViewSwtRemote.CID, vsid -> new ProductsPanelViewSwtRemote(this, vsid, this));
		this.viewFactories.put(PurchasesPanelViewSwtRemote.CID, vsid -> new PurchasesPanelViewSwtRemote(this, vsid, this));
		this.viewFactories.put(ReceiptViewSwtRemote.CID, vsid -> new ReceiptViewSwtRemote(this, vsid, this));
	}

	private void applyResponse(HostResponse resp) {
		// Create views for newly published vsids
		for (var snapshot : resp.viewStates()) {
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

	private void startListenerThread() {
		var thread = new Thread(() -> {
			var shouldStop = false;
			while (!shouldStop && !this.display.isDisposed() && this.hostClient != null) {
				try {
					var resp = this.hostClient.awaitResponse(java.time.Duration.ofSeconds(30));
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
	public ViewStateSnapshot viewState(String vsid) {
		return this.hostClient != null ? this.hostClient.viewState(vsid) : null;
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
