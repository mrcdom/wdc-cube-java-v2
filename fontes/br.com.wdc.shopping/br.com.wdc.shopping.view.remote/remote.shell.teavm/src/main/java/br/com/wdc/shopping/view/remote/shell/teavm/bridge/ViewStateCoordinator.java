package br.com.wdc.shopping.view.remote.shell.teavm.bridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.view.teavm.commons.interop.Console;

/**
 * Core coordinator that manages:
 * <ul>
 *   <li>WebSocket connection to remote.host (via FlushRequestContext)</li>
 *   <li>Reconnection with exponential backoff (via ReconnectController)</li>
 *   <li>View garbage collection (via ViewGarbageCollector)</li>
 *   <li>View factory registry (viewId → view constructor)</li>
 *   <li>View state distribution (vsid → ViewScope)</li>
 *   <li>Form field collection and event submission</li>
 *   <li>Render scheduling via requestAnimationFrame (no recursion)</li>
 * </ul>
 * <p>
 * Faithful equivalent of ViewStateCoordinator.ts in remote.shell.react.
 */
public class ViewStateCoordinator {

    public static final ViewStateCoordinator INSTANCE = new ViewStateCoordinator();

    public static final String BROWSER_VID = "7b32e816a191";
    public static final String BROWSER_VSID = BROWSER_VID + ":0";

    // -- Public fields (accessed by FlushRequestContext, ReconnectController, ViewGarbageCollector) --

    public final String id;
    public final Map<String, ViewScope> viewMap = new LinkedHashMap<>();
    public boolean isConnected = false;
    public String path = "";

    public final FlushRequestContext contextExchanger;
    public final ReconnectController reconnectController;
    public final ViewGarbageCollector viewGarbageCollector;

    // -- Private fields --

    private final Map<String, Function<String, AbstractRemoteView>> viewFactoryMap = new LinkedHashMap<>();
    private final Map<String, AbstractRemoteView> viewInstanceMap = new LinkedHashMap<>();
    private Map<String, Object> formMap = new LinkedHashMap<>();
    private String baseWebSocketUrl = "";

    private ViewStateCoordinator() {
        viewMap.put(BROWSER_VSID, new ViewScope(BROWSER_VSID));

        // Read app_id from cookie (set by server's IndexHtmlController)
        String appIdFromCookie = getCookie("app_id");
        if (appIdFromCookie != null && !appIdFromCookie.isEmpty()) {
            removeCookie("app_id");
        }

        // Try sessionStorage first (survives F5), then fall back to cookie
        String appId = getSessionItem("app_id");
        if (appId == null || appId.isEmpty()) {
            appId = appIdFromCookie;
            if (appId != null && !appId.isEmpty()) {
                setSessionItem("app_id", appId);
            } else {
                appId = generateId() + ".fake";
            }
        }
        this.id = appId;

        // Build base WebSocket URL: ws[s]://host
        String protocol = getLocationProtocol();
        String wsProtocol = "https:".equals(protocol) ? "wss://" : "ws://";
        String host = getLocationHost();
        this.baseWebSocketUrl = wsProtocol + host;

        // Initialize sub-components
        this.contextExchanger = new FlushRequestContext(this);
        this.reconnectController = new ReconnectController(this);
        this.viewGarbageCollector = new ViewGarbageCollector(this);
    }

    // -- Public API --

    public String getBaseWebSocketUrl() {
        return baseWebSocketUrl;
    }

    public void registerView(String viewId, Function<String, AbstractRemoteView> factory) {
        viewFactoryMap.put(viewId, factory);
    }

    /**
     * Main entry point. Called from Main.main() after view registration.
     * Mirrors onStart() from the React shell.
     */
    public void start() {
        Console.log("App ID: " + id);

        // Connect via ReconnectController (delegates to FlushRequestContext.open)
        assureContextExchangerIsConnected();

        // Listen popstate (Back/Forward)
        Window.current().addEventListener("popstate", evt -> onPopState());

        // Send initial navigation event (equivalent to React's onStart event -1)
        String hash = getLocationHash();
        path = (hash != null && hash.length() > 1) ? hash.substring(1) : "/";
        setFormField(BROWSER_VSID, "p.path", path);
        submit(BROWSER_VSID, -1);
    }

    public void assureContextExchangerIsConnected() {
        contextExchanger.open(reconnectController.url);
    }

    // -- View state application --

    @SuppressWarnings("unchecked")
    public void applyViewStates(List<?> stateList) {
        for (int i = 0, len = stateList.size(); i < len; i++) {
            var item = stateList.get(i);
            if (item instanceof Map<?, ?> stateMap) {
                var map = (Map<String, Object>) stateMap;
                var vsidObj = map.get("#");
                if (vsidObj instanceof String vsid) {
                    ViewScope viewScope = viewMap.computeIfAbsent(vsid, ViewScope::new);

                    // Ensure view instance exists and is wired
                    ensureViewInstance(vsid);

                    // Set state (this calls forceUpdate → scheduleRender)
                    viewScope.setState(map);
                }
            }
        }
    }

    private void ensureViewInstance(String vsid) {
        if (viewInstanceMap.containsKey(vsid)) return;

        // Extract viewId from vsid (format: "viewId:instanceNum")
        int colonIdx = vsid.indexOf(':');
        String viewId = colonIdx > 0 ? vsid.substring(0, colonIdx) : vsid;

        var factory = viewFactoryMap.get(viewId);
        if (factory == null) {
            Console.warn("No view factory for viewId: " + viewId);
            return;
        }

        var view = factory.apply(vsid);
        viewInstanceMap.put(vsid, view);

        // Wire forceUpdate: view schedules its own render
        var scope = viewMap.get(vsid);
        if (scope != null) {
            scope.setForceUpdate(view::forceUpdate);
        }

        // Register with GC
        viewGarbageCollector.mount(vsid);
    }

    // -- Form fields and event submission (matches React's submit flow) --

    @SuppressWarnings("unchecked")
    public void setFormField(String vsid, String fieldName, Object fieldValue) {
        Map<String, Object> formData = (Map<String, Object>) formMap.computeIfAbsent(vsid, k -> new LinkedHashMap<>());
        formData.put(fieldName, fieldValue);
    }

    public void submit(String vsid, int eventId) {
        Map<String, Object> oldFormMap = this.formMap;
        this.formMap = new LinkedHashMap<>();
        boolean silent = BROWSER_VSID.equals(vsid);
        contextExchanger.submit(oldFormMap, vsid, eventId, silent);
    }

    public void submitSilent(String vsid, int eventId) {
        Map<String, Object> oldFormMap = this.formMap;
        this.formMap = new LinkedHashMap<>();
        contextExchanger.submit(oldFormMap, vsid, eventId, true);
    }

    // -- View tree access --

    public AbstractRemoteView getViewInstance(String vsid) {
        return vsid != null ? viewInstanceMap.get(vsid) : null;
    }

    public ViewScope getViewScope(String vsid) {
        return vsid != null ? viewMap.get(vsid) : null;
    }

    public HTMLElement getViewElement(String vsid) {
        var view = getViewInstance(vsid);
        return view != null ? view.getElement() : null;
    }

    public boolean isSubmitting() {
        var scope = viewMap.get(BROWSER_VSID);
        if (scope != null) {
            return scope.getBoolean("submitting");
        }
        return false;
    }

    // -- Browser navigation --

    private void onPopState() {
        String hash = getLocationHash();
        if (hash != null && hash.startsWith("#") && hash.length() > 1) {
            String newPath = hash.substring(1);
            if (!newPath.equals(path)) {
                path = newPath;
                setFormField(BROWSER_VSID, "p.path", newPath);
                submit(BROWSER_VSID, -2);
            }
        }
    }

    // -- JS interop --

    @JSBody(params = {}, script = "return location.protocol;")
    private static native String getLocationProtocol();

    @JSBody(params = {}, script = "return location.host;")
    private static native String getLocationHost();

    @JSBody(params = {}, script = "return location.hash || '';")
    private static native String getLocationHash();

    @JSBody(params = {"key"}, script = "try { return sessionStorage.getItem(key); } catch(e) { return null; }")
    private static native String getSessionItem(String key);

    @JSBody(params = {"key", "val"}, script = "try { sessionStorage.setItem(key, val); } catch(e) {}")
    private static native void setSessionItem(String key, String val);

    @JSBody(params = {"name"}, script = ""
            + "var m = document.cookie.match('(^|;)\\\\s*' + name + '\\\\s*=\\\\s*([^;]+)');"
            + "return m ? decodeURIComponent(m.pop()) : null;")
    private static native String getCookie(String name);

    @JSBody(params = {"name"}, script = "document.cookie = name + '=;path=/;expires=Thu,01 Jan 1970 00:00:00 GMT';")
    private static native void removeCookie(String name);

    @JSBody(params = {}, script = ""
            + "var a = new Uint8Array(16); crypto.getRandomValues(a);"
            + "return Array.from(a, function(b) { return b.toString(16).padStart(2,'0'); }).join('');")
    private static native String generateId();
}
