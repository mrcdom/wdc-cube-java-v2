package br.com.wdc.shopping.view.swt;

import java.util.Map;
import java.util.Objects;
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
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.ScopeViews;
import br.com.wdc.shopping.presentation.ProxyRepositoryWrapper;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootScope;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginScope;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartScope;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeScope;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductScope;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptScope;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.impl.CartItemViewSwt;
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
	 * The view of each scope (the presenters', the segments', the rows'), with its state supplier and event callbacks.
	 */
	private static void bindViews(ShoppingSwtApplication app) {
		var rootView = app.onUiThread((RootScope s) -> {
			var v = new RootViewSwt(app); v.stateSupplier = () -> s; return v; });
		var loginView = app.onUiThread((LoginScope s) -> {
			var v = new LoginViewSwt(app); v.stateSupplier = () -> s;
			v.onEnter = s.onEnter::run; return v; });
		var homeView = app.onUiThread((HomeScope s) -> {
			var v = new HomeViewSwt(app); v.stateSupplier = () -> s;
			v.onExit = s.onExit::run; v.onOpenCart = s.onOpenCart::run; return v; });
		var cartView = app.onUiThread((CartScope s) -> {
			var v = new CartViewSwt(app); v.stateSupplier = () -> s;
			v.onOpenProducts = s.onOpenProducts::run; v.onBuy = s.onBuy::run;
			v.rowElements = () -> s.rows.get().stream()
					.map(row -> app.viewOf(row) instanceof AbstractViewSwt rv ? rv.getElement() : null)
					.filter(Objects::nonNull).toList();
			return v; });
		var productView = app.onUiThread((ProductScope s) -> {
			var v = new ProductViewSwt(app); v.stateSupplier = () -> s;
			v.onOpenProducts = s.onOpenProducts::run; v.onAddToCart = s.onAddToCart::run;
			return v; });
		var receiptView = app.onUiThread((ReceiptScope s) -> {
			var v = new ReceiptViewSwt(app); v.stateSupplier = () -> s;
			v.onOpenProducts = s.onOpenProducts::run; return v; });
		var cartItemView = app.onUiThread((CartScope.CartItemScope row) -> {
			var v = new CartItemViewSwt(app); v.stateSupplier = () -> row;
			v.onModifyQuantity = row.onModifyQuantity::run; v.onRemove = row.onRemove::run; return v; });
		var productsPanelView = app.onUiThread((HomeScope.ProductsPanelScope s) -> {
			var v = new ProductsPanelViewSwt(app); v.stateSupplier = () -> s;
			v.onOpenProduct = s.onOpenProduct::run; return v; });
		var purchasesPanelView = app.onUiThread((HomeScope.PurchasesPanelScope s) -> {
			var v = new PurchasesPanelViewSwt(app); v.stateSupplier = () -> s;
			v.onPageChange = s.onPageChange::run; v.onItemSizeCapacityChanged = s.onItemSizeCapacityChanged::run;
			v.onOpenReceipt = s.onOpenReceipt::run; return v; });
		app.setScopeViews(new ScopeViews()
				.register(RootScope.class, (ShoppingSwtApplication a, RootScope s) -> rootView.apply(s))
				.register(LoginScope.class, (ShoppingSwtApplication a, LoginScope s) -> loginView.apply(s))
				.register(HomeScope.class, (ShoppingSwtApplication a, HomeScope s) -> homeView.apply(s))
				.register(CartScope.class, (ShoppingSwtApplication a, CartScope s) -> cartView.apply(s))
				.register(ProductScope.class, (ShoppingSwtApplication a, ProductScope s) -> productView.apply(s))
				.register(ReceiptScope.class, (ShoppingSwtApplication a, ReceiptScope s) -> receiptView.apply(s))
				.register(CartScope.CartItemScope.class, (ShoppingSwtApplication a, CartScope.CartItemScope row) -> cartItemView.apply(row))
				.register(HomeScope.ProductsPanelScope.class, (ShoppingSwtApplication a, HomeScope.ProductsPanelScope s) -> productsPanelView.apply(s))
				.register(HomeScope.PurchasesPanelScope.class, (ShoppingSwtApplication a, HomeScope.PurchasesPanelScope s) -> purchasesPanelView.apply(s)));
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
		// The host draws the root presenter's scope; each view asks for the views of the scopes it holds
		var rootPresenter = this.getRootPresenter();
		if (rootPresenter != null) {
			this.viewOf(rootPresenter.state);
		}
		this.historyDirty = true;
	}

	public void markAllViewsDirty() {
		for (var view : this.liveViews()) {
			if (view instanceof AbstractViewSwt swtView) {
				this.markDirty(swtView);
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
		for (var view : this.liveViews()) {
			if (view instanceof AbstractViewSwt swtView) {
				swtView.rebuild();
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
