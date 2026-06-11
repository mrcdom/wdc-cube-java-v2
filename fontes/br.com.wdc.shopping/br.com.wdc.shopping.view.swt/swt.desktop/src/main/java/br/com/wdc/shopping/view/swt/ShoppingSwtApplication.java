package br.com.wdc.shopping.view.swt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.commons.storage.InMemoryClientStorage;
import br.com.wdc.framework.commons.storage.PreferencesClientStorage;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.ProxyRepositoryWrapper;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.view.swt.impl.CartViewSwt;
import br.com.wdc.shopping.view.swt.impl.HomeViewSwt;
import br.com.wdc.shopping.view.swt.impl.LoginViewSwt;
import br.com.wdc.shopping.view.swt.impl.ProductViewSwt;
import br.com.wdc.shopping.view.swt.impl.ProductsPanelViewSwt;
import br.com.wdc.shopping.view.swt.impl.PurchasesPanelViewSwt;
import br.com.wdc.shopping.view.swt.impl.ReceiptViewSwt;
import br.com.wdc.shopping.view.swt.impl.RootViewSwt;

public class ShoppingSwtApplication extends ShoppingApplication implements SwtApp {

	private static final Log LOG = Log.getLogger(ShoppingSwtApplication.class);

	private static final int FRAME_INTERVAL_MS = 16; // ~60fps

	private final Display display;
	private final Shell shell;
	private final ReentrantLock presenterLock = new ReentrantLock();
	private final ExecutorService presenterExecutor = Executors.newSingleThreadExecutor(r -> {
		var t = new Thread(r, "presenter-worker");
		t.setDaemon(true);
		return t;
	});
	private Composite rootPane;
	private Composite offscreen;
	private final Map<String, AbstractViewSwt> dirtyViewMap = new ConcurrentHashMap<>();
	private Runnable renderTimerRunnable;
	private boolean devMode;
	private volatile boolean historyDirty;
	private String lastIntent;

	public ShoppingSwtApplication(Display display, Shell shell) {
		this.display = display;
		this.shell = shell;
		this.renderTimerRunnable = ThrowingRunnable.noop();

		// Hidden container for views not yet connected to a visible slot
		this.offscreen = new Composite(shell, SWT.NONE);
		this.offscreen.setVisible(false);
		this.offscreen.setSize(0, 0);
		bindViews(this);
	}

	/**
	 * Registers view factories with state suppliers and event callbacks for each presenter.
	 */
	private static void bindViews(ShoppingSwtApplication app) {
		RootPresenter.createView = app.onUiThread(p -> {
			var v = new RootViewSwt(app); v.stateSupplier = () -> p.state; return v; });
		LoginPresenter.createView = app.onUiThread(p -> {
			var v = new LoginViewSwt(app); v.stateSupplier = () -> p.state;
			v.onEnter = p::onEnter; return v; });
		HomePresenter.createView = app.onUiThread(p -> {
			var v = new HomeViewSwt(app); v.stateSupplier = () -> p.state;
			v.onExit = p::onExit; v.onOpenCart = p::onOpenCart; return v; });
		CartPresenter.createView = app.onUiThread(p -> {
			var v = new CartViewSwt(app); v.stateSupplier = () -> p.state;
			v.onOpenProducts = p::onOpenProducts; v.onBuy = p::onBuy;
			v.onModifyQuantity = p::onModifyQuantity; v.onRemoveProduct = p::onRemoveProduct;
			return v; });
		ProductPresenter.createView = app.onUiThread(p -> {
			var v = new ProductViewSwt(app); v.stateSupplier = () -> p.state;
			v.onOpenProducts = p::onOpenProducts; v.onAddToCart = p::onAddToCart;
			return v; });
		ProductsPanelPresenter.createView = app.onUiThread(p -> {
			var v = new ProductsPanelViewSwt(app); v.stateSupplier = () -> p.state;
			v.onOpenProduct = p::onOpenProduct; return v; });
		PurchasesPanelPresenter.createView = app.onUiThread(p -> {
			var v = new PurchasesPanelViewSwt(app); v.stateSupplier = () -> p.state;
			v.onPageChange = p::onPageChange; v.onItemSizeCapacityChanged = p::onItemSizeCapacityChanged;
			v.onOpenReceipt = p::onOpenReceipt; return v; });
		ReceiptPresenter.createView = app.onUiThread(p -> {
			var v = new ReceiptViewSwt(app); v.stateSupplier = () -> p.state;
			v.onOpenProducts = p::onOpenProducts; return v; });
	}

	/**
	 * Wraps a view factory so that it always executes on the SWT UI thread.
	 */
	private <P> Function<P, CubeView> onUiThread(Function<P, AbstractViewSwt> factory) {
		return presenter -> {
			AbstractViewSwt swtView;
			if (this.display.getThread() == Thread.currentThread()) {
				swtView = factory.apply(presenter);
			} else {
				var result = new AbstractViewSwt[1];
				this.display.syncExec(() -> result[0] = factory.apply(presenter));
				swtView = result[0];
			}
			this.markDirty(swtView);
			return swtView;
		};
	}

	@Override
	public void runAction(Runnable action) {
		runPresenterAction(action);
	}

	@Override
	public void onActionError(String context, Throwable e) {
		alertUnexpectedError(LOG, context, e);
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

	@Override
	public void setSubject(Subject subject) {
		super.setSubject(subject);
		var storage = clientPersistentStore();
		if (storage != null) {
			if (subject != null && subject.getId() != null) {
				storage.set("session.userId", String.valueOf(subject.getId()));
			} else {
				storage.remove("session.userId");
			}
		}
	}

	public boolean isDevMode() {
		return this.devMode;
	}

	public void setDevMode(boolean devMode) {
		this.devMode = devMode;
	}

	public Display getDisplay() {
		return this.display;
	}

	public Shell getShell() {
		return this.shell;
	}

	/**
	 * Returns the hidden offscreen container. Views are created here and reparented to visible containers when needed.
	 */
	public Composite getOffscreen() {
		return this.offscreen;
	}

	public Composite getRootPane() {
		return this.rootPane;
	}

	public void setRootPane(Composite rootPane) {
		this.rootPane = rootPane;
	}

	/**
	 * Returns the reentrant lock used to synchronize view→presenter calls.
	 */
	public ReentrantLock getPresenterLock() {
		return this.presenterLock;
	}

	/**
	 * Submits a presenter action to execute on the dedicated worker thread, acquiring the presenter lock. The UI thread remains responsive.
	 */
	public void runPresenterAction(Runnable action) {
		this.presenterExecutor.execute(() -> {
			this.presenterLock.lock();
			try {
				action.run();
			} finally {
				this.presenterLock.unlock();
			}
		});
	}
	
	public void safeGo(String intent) {
		this.runPresenterAction(() -> this.go(intent));
	}

	public void start() {
		this.renderTimerRunnable = () -> {
			if (!this.display.isDisposed()) {
				this.flushHistory();
				this.flushDirtyViews();
				this.display.timerExec(FRAME_INTERVAL_MS, this.renderTimerRunnable);
			}
		};
		this.display.timerExec(FRAME_INTERVAL_MS, this.renderTimerRunnable);
	}

	@Override
	public void release() {
		this.renderTimerRunnable = ThrowingRunnable.noop();
		this.dirtyViewMap.clear();
		this.presenterExecutor.shutdownNow();
		br.com.wdc.shopping.view.swt.util.ProductImageCache.getInstance().dispose();
		var rootPresenter = this.getRootPresenter();
		if (rootPresenter != null) {
			rootPresenter.release();
		}
	}

	@Override
	public void updateHistory() {
		this.historyDirty = true;
	}

	public void markAllViewsDirty() {
		for (var presenter : this.presenterMap.values()) {
			if (presenter instanceof AbstractCubePresenter<?> acp) {
				var view = acp.view();
				if (view instanceof AbstractViewSwt swtView) {
					this.markDirty(swtView);
				}
			}
		}
	}

	private void flushHistory() {
		if (!this.historyDirty) {
			return;
		}
		if (!this.presenterLock.tryLock()) {
			return;
		}

var storage = clientPersistentStore();
		if (storage == null) {
			return;
		}

		var unlock = (ThrowingRunnable) this.presenterLock::unlock;
		try {
			var intent = this.newIntent();
			for (var presenter : this.presenterMap.values()) {
				presenter.publishParameters(intent);
			}
			unlock.run();
			unlock = ThrowingRunnable.noop();

			this.historyDirty = false;
			var intentStr = intent.toString();
			if (!intentStr.equals(this.lastIntent)) {
				this.lastIntent = intentStr;
				storage.set("session.intent", intentStr);
			}
		} catch (Exception e) {
			LOG.error("Failed to persist intent: " + e.getMessage());
		} finally {
			unlock.run();
		}
	}

	@Override
	public String b64Cipher(String text) {
		throw new AssertionError("not implemented");
	}

	@Override
	public String b64Decipher(String b64Text) {
		throw new AssertionError("not implemented");
	}

	@Override
	public String getClientIp() {
		try {
			return java.net.InetAddress.getLocalHost().getHostAddress();
		} catch (java.net.UnknownHostException e) {
			return "127.0.0.1";
		}
	}

	private final InMemoryClientStorage sessionStore = new InMemoryClientStorage();
	private final PreferencesClientStorage persistentStore = new PreferencesClientStorage(ShoppingSwtApplication.class);

	public ClientStorage clientSessionStore() {
		return sessionStore;
	}

	@Override
	public ClientStorage clientPersistentStore() {
		return persistentStore;
	}

	public void markDirty(AbstractViewSwt view) {
		if (this.dirtyViewMap.putIfAbsent(view.instanceId(), view) == null) {
			view.dirtyTimestamp = System.nanoTime();
		}
	}

	public void rebuildAllViews() {
		LOG.info("Rebuilding all SWT views...");
		this.dirtyViewMap.clear();
		for (var presenter : this.presenterMap.values()) {
			if (presenter instanceof AbstractCubePresenter<?> acp) {
				var view = acp.view();
				if (view instanceof AbstractViewSwt swtView) {
					swtView.rebuild();
				}
			}
		}
		if (this.rootPane != null && !this.rootPane.isDisposed()) {
			this.rootPane.layout(true, true);
		}
		LOG.info("All SWT views rebuilt.");
	}

	private void flushDirtyViews() {
		if (this.dirtyViewMap.isEmpty()) {
			return;
		}

		// Use tryLock to avoid blocking the UI thread while presenter is working
		if (!this.presenterLock.tryLock()) {
			return;
		}
		try {
			var threshold = System.nanoTime() - (FRAME_INTERVAL_MS * 1_000_000L);
			var iterator = this.dirtyViewMap.values().iterator();
			while (iterator.hasNext()) {
				var view = iterator.next();
				if (view.dirtyTimestamp <= threshold) {
					iterator.remove();
					view.performUpdate();
				}
			}
		} finally {
			this.presenterLock.unlock();
		}
	}
}
