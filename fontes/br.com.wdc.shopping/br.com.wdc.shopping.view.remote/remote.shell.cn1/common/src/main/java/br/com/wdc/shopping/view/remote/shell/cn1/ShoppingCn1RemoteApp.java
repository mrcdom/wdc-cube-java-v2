package br.com.wdc.shopping.view.remote.shell.cn1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Images;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.views.RootCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.SplashCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.cart.CartCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.cart.CartItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.home.HomeCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.home.ProductsPanelCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.home.PurchasesPanelCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.login.LoginCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.product.ProductCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.receipt.ReceiptCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.Slot;

/**
 * Thin Codename One shell (remote-shell) — coordinator.
 *
 * <p>
 * Connects to the bridge ({@link BridgeSession}), keeps a classId→factory registry and a
 * vsid→view cache, and on each push <b>re-syncs the active tree</b> in cascade from the root. Each
 * view is its own class that builds its widgets once and mutates them in {@code doUpdate()}
 * (non-reactive, Vaadin-style pattern) — nothing is rebuilt per push.
 * </p>
 */
public class ShoppingCn1RemoteApp extends Lifecycle {

    /** Minimum width (mm) the expanded layout needs; below it, compact (density-independent). */
    private static final float EXPANDED_MIN_MM = 110f;

    private BridgeSession session;
    private Form form;
    /** Backend base URL, resolved per platform/override in {@code runApp} (see {@link ServerConfig}). */
    private String baseUrl;
    private final Map<String, AbstractCn1View> views = new HashMap<>();
    /** Views to re-render on the next flush (single primitive, like SWT's {@code dirtyViews}). */
    private final Set<AbstractCn1View> dirty = new LinkedHashSet<>();
    /** The form's only child: holds the root view (and the splash) — "mount only on transition" guard. */
    private Slot rootSlot;
    /** Splash (connecting/error) — built once, reused via setState. */
    private SplashCn1View splash;
    private boolean flushScheduled;
    private boolean wasExpanded;
    /** The host alert being shown: a browser push while it is open does not show it again. */
    private int shownAlertId;
    private boolean wasPortrait;

    /**
     * Expanded layout (hero+card, products+history split) when the <b>available width</b> fits the
     * space the expanded layout needs ({@code >= EXPANDED_MIN_MM}); otherwise, compact. Purely a
     * <b>width</b> criterion (mm, density-independent) — independent of device (tablet/desktop/phone) and
     * orientation: what matters is fitting. E.g.: a phone in landscape wide enough is expanded; a narrow
     * one, compact. (The threshold is the size to tune if the split gets cramped.)
     */
    public boolean isExpanded() {
        Display d = Display.getInstance();
        return d.getDisplayWidth() >= d.convertToPixels(EXPANDED_MIN_MM);
    }

    /**
     * Resize. On <b>crossing the breakpoint</b> or <b>rotating the screen</b>, re-syncs everything via
     * {@link #refreshAll()}: each view adapts its own layout in {@code doUpdate()} (no rebuild),
     * preserving state (e.g.: typed text). Recomputes the safe area for the new orientation before the
     * {@code revalidate} (done by {@code refreshAll}). Within the same breakpoint+orientation it does
     * nothing: CN1 relays out by itself and the panels notify the backend through their own
     * {@code sizeChangedListener}.
     */
    private void onResize() {
        boolean nowExpanded = isExpanded();
        boolean nowPortrait = Display.getInstance().isPortrait();
        if (nowExpanded == wasExpanded && nowPortrait == wasPortrait) {
            return;
        }
        wasExpanded = nowExpanded;
        wasPortrait = nowPortrait;
        form.setSafeAreaChanged(); // recomputes the safe area for the new orientation (before revalidate)
        refreshAll();
    }

    @Override
    public void runApp() {
        baseUrl = ServerConfig.baseUrl();
        Images.setBaseUrl(baseUrl);
        wasExpanded = isExpanded();
        wasPortrait = Display.getInstance().isPortrait();

        form = new Form(new BorderLayout());
        // iOS (notch/Dynamic Island): without a toolbar, CN1 does not reserve the status bar and the header
        // would stick under it (touches would go to iOS). Keeps the content inside the safe area.
        form.getContentPane().setSafeArea(true);
        rootSlot = new Slot();
        form.add(BorderLayout.CENTER, rootSlot); // single, permanent child; survives resize
        form.addSizeChangedListener(e -> onResize());
        form.show();
        // hideToolbar() requires the form to be attached (getComponentForm()): on desktop/javase it is null
        // before show() (on device it is not). Since runApp runs on the EDT and paint comes later, no flicker.
        form.getToolbar().hideToolbar(); // no native title bar; each screen brings its own header

        startConnect();
    }

    /** Shows the "connecting" screen and opens the bridge on a thread; on failure, shows error + retry. */
    private void startConnect() {
        showSplash(false, "Connecting to server...", null);
        session = new BridgeSession(baseUrl, this::onPush);
        new Thread(() -> {
            try {
                session.connect();
            } catch (Exception e) {
                CN.callSerially(() -> showSplash(true, "Could not connect to the server.", this::startConnect));
            }
        }).start();
    }

    /** Returns (creating on demand) the view for a vsid; {@code null} if the classId is unknown. */
    public AbstractCn1View viewFor(String vsid) {
        if (vsid == null || vsid.isEmpty()) {
            return null;
        }
        AbstractCn1View v = views.get(vsid);
        if (v == null) {
            v = create(vsid);
            if (v != null) {
                views.put(vsid, v);
            }
        }
        return v;
    }

    /** Instantiates the classId's view (direct switch — no reflection or lambda factory). */
    private AbstractCn1View create(String vsid) {
        String classId = BridgeSession.classIdOf(vsid);
        switch (classId) {
            case RootCn1View.CLASS_ID:
                return new RootCn1View(vsid, session, this);
            case LoginCn1View.CLASS_ID:
                return new LoginCn1View(vsid, session, this);
            case HomeCn1View.CLASS_ID:
                return new HomeCn1View(vsid, session, this);
            case ProductsPanelCn1View.CLASS_ID:
                return new ProductsPanelCn1View(vsid, session, this);
            case PurchasesPanelCn1View.CLASS_ID:
                return new PurchasesPanelCn1View(vsid, session, this);
            case ProductCn1View.CLASS_ID:
                return new ProductCn1View(vsid, session, this);
            case CartCn1View.CLASS_ID:
                return new CartCn1View(vsid, session, this);
            case CartItemCn1View.CLASS_ID:
                return new CartItemCn1View(vsid, session, this);
            case ReceiptCn1View.CLASS_ID:
                return new ReceiptCn1View(vsid, session, this);
            default:
                com.codename1.io.Log.p("CN1 shell: no view for classId=" + classId + " (vsid=" + vsid + ")");
                return null;
        }
    }

    /**
     * Receives a server push (on the EDT): discards released views, mounts the root if it changed and
     * marks each received ViewState <b>dirty</b> — mirrors the SWT shell's {@code applyResponse}.
     * The actual render is done by the coalesced {@link #flush()}.
     */
    private void onPush(List<String> received, List<String> released) {
        for (String vsid : released) {
            evict(vsid);
        }
        mountRootIfChanged();
        showHostAlert();
        for (String vsid : received) {
            AbstractCn1View v = views.get(vsid); // already created/mounted (creation is lazy, via the parent)
            if (v != null) {
                dirty.add(v);
            }
        }
        requestFlush();
    }

    /** Discards a view released by the server: removes it from the cache and dirty set and unmounts it (avoids leaks). */
    private void evict(String vsid) {
        AbstractCn1View v = views.remove(vsid);
        if (v == null) {
            return;
        }
        dirty.remove(v);
        com.codename1.ui.Container el = v.peekElement();
        if (el != null && el.getParent() != null) {
            el.getParent().removeComponent(el);
        }
    }

    /** Mounts the root view in the slot when the browser starts pointing to another one (slot's internal guard). */
    private void mountRootIfChanged() {
        Map<String, Object> browser = session.state(BridgeSession.BROWSER_VSID);
        String rv = browser != null ? Json.str(browser, "contentViewId") : "";
        if (rv.isEmpty()) {
            return; // a push without contentViewId does not unmount the current root
        }
        rootSlot.mount(elementOf(rv)); // re-mounts only when the element reference changes (same vsid → same view)
    }

    /**
     * The host alert (the browser's {@code alertId}/{@code alertArgs}): stays on screen until dismissed, which
     * notifies the host (browser event 1), whose alert action can then run.
     */
    private void showHostAlert() {
        Map<String, Object> browser = session.state(BridgeSession.BROWSER_VSID);
        final int alertId = browser != null ? Json.intOf(browser, "alertId") : 0;
        if (alertId == 0 || alertId == shownAlertId) {
            shownAlertId = alertId;
            return;
        }
        shownAlertId = alertId;

        List<String> args = new ArrayList<>();
        for (Object item : Json.asList(browser.get("alertArgs"))) {
            args.add(String.valueOf(item));
        }
        final String message;
        if (alertId == -2) {
            message = "The URI " + (args.isEmpty() ? "" : args.get(0)) + " is not accessible";
        } else {
            message = args.isEmpty() ? "An unexpected error occurred" : args.get(0);
        }
        final String detail = args.size() > 1 ? args.get(1) : null;

        CN.callSerially(() -> {
            Dialog.show("Warning!", detail != null ? message + "\n\n" + detail : message, "OK", null);
            shownAlertId = 0;
            session.submit(BridgeSession.BROWSER_VSID, 1, new HashMap<String, Object>());
        });
    }

    /** Element of the {@code vsid} view (created on demand), or {@code null}. */
    private Container elementOf(String vsid) {
        AbstractCn1View v = viewFor(vsid);
        return v != null ? v.getElement() : null;
    }

    /** Schedules a view re-render (single primitive — server push and local interactions). */
    public void markDirty(AbstractCn1View view) {
        if (view != null) {
            dirty.add(view);
            requestFlush();
        }
    }

    /** Schedules a coalesced flush. */
    public void requestFlush() {
        if (!flushScheduled) {
            flushScheduled = true;
            CN.callSerially(this::flush);
        }
    }

    /**
     * Coalesced render (on the EDT): drains the {@code dirty} set and calls {@code doUpdate()} on each
     * view. Each view handles its own data and the <i>mounting</i> of its children; there is no manual
     * propagation of {@code doUpdate} by the parents (cf. React/TeaVM/SWT shells).
     */
    private void flush() {
        flushScheduled = false;
        try {
            List<AbstractCn1View> batch = new ArrayList<>(dirty);
            dirty.clear();
            for (AbstractCn1View v : batch) {
                v.doUpdate();
            }
            form.revalidate();
        } catch (Exception e) {
            com.codename1.io.Log.e(e);
            showSplash(true, "Error rendering the screen.", null);
        }
    }

    /**
     * Reconciles <b>all</b> mounted views with the current state: runs {@code doUpdate()} on each cached
     * (vsid-backed) view — which in turn re-syncs its item views via {@code syncList} — and on the
     * splash. Use it for a full re-render (e.g.: after reconnecting) and on resize, where each view
     * adapts its own layout (compact↔expanded) in {@code doUpdate()}.
     *
     * <p>Iterates a <b>snapshot</b> of {@code views}: a root's {@code doUpdate} can create/cache
     * new views ({@code viewFor}) and mutate the map during iteration.</p>
     */
    public void refreshAll() {
        try {
            for (AbstractCn1View v : new ArrayList<>(views.values())) {
                v.doUpdate();
            }
            if (splash != null) {
                splash.refresh();
            }
            form.revalidate();
        } catch (Exception e) {
            com.codename1.io.Log.e(e);
            showSplash(true, "Error rendering the screen.", null);
        }
    }

    /**
     * "Brand" screen shown before receiving the backend state (and on failure): blue gradient
     * + logo + title, with a <b>spinner</b> (connecting) or an error icon + "Try again".
     */
    private void showSplash(boolean error, String status, Runnable onRetry) {
        if (splash == null) {
            splash = new SplashCn1View(); // built once; reused (build-once + setState)
        }
        splash.setState(new SplashCn1View.State(error, status, onRetry));
        rootSlot.mount(splash.getElement()); // always the same reference → only re-mounts when coming from the root
    }
}
