package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.cube.remote.bridge.teavm.interop.Console;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.Cookies;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.WebCrypto;

/**
 * Core coordinator that manages:
 * <ul>
 * <li>WebSocket connection to remote.host (via FlushRequestContext)</li>
 * <li>Reconnection with exponential backoff (via ReconnectController)</li>
 * <li>View garbage collection (via ViewGarbageCollector)</li>
 * <li>View factory registry (viewId → view constructor)</li>
 * <li>View state distribution (vsid → ViewScope)</li>
 * <li>Form field collection and event submission</li>
 * <li>Render scheduling via requestAnimationFrame (no recursion)</li>
 * </ul>
 */
public class ViewStateCoordinator {

    public static final ViewStateCoordinator INSTANCE = new ViewStateCoordinator();

    public static final String BROWSER_VID = "7b32e816a191";
    public static final String BROWSER_VSID = BROWSER_VID + ":0";

    // -- Public fields --

    public final String id;
    public final Map<String, ViewScope> viewMap = new LinkedHashMap<>();
    public boolean isConnected = false;
    public String path = "";

    public final FlushRequestContext contextExchanger;
    public final ReconnectController reconnectController;
    public final ViewGarbageCollector viewGarbageCollector;

    // -- Private fields --

    private final Map<String, Function<String, RemoteView>> viewFactoryMap = new LinkedHashMap<>();
    private final Map<String, RemoteView> viewInstanceMap = new LinkedHashMap<>();
    private Map<String, Object> formMap = new LinkedHashMap<>();
    private String baseWebSocketUrl = "";

    private ViewStateCoordinator() {
        viewMap.put(BROWSER_VSID, new ViewScope(BROWSER_VSID));

        String appIdFromCookie = Cookies.get("app_id");
        if (appIdFromCookie != null && !appIdFromCookie.isEmpty()) {
            Cookies.remove("app_id");
        }

        String appId = Storage.getSessionStorage().getItem("app_id");
        if (appId == null || appId.isEmpty()) {
            appId = appIdFromCookie;
            if (appId != null && !appId.isEmpty()) {
                Storage.getSessionStorage().setItem("app_id", appId);
            } else {
                appId = generateId() + ".fake";
            }
        }
        this.id = appId;

        String protocol = getLocationProtocol();
        String wsProtocol = "https:".equals(protocol) ? "wss://" : "ws://";
        String host = getLocationHost();
        this.baseWebSocketUrl = wsProtocol + host;

        this.contextExchanger = new FlushRequestContext(this);
        this.reconnectController = new ReconnectController(this);
        this.viewGarbageCollector = new ViewGarbageCollector(this);
    }

    // -- Public API --

    public String getBaseWebSocketUrl() {
        return baseWebSocketUrl;
    }

    public void registerView(String viewId, Function<String, RemoteView> factory) {
        viewFactoryMap.put(viewId, factory);
    }

    public void start() {
        Console.log("App ID: " + id);

        assureContextExchangerIsConnected();

        Window.current().addEventListener("popstate", evt -> onPopState());

        String hash = getLocationHash();
        path = hash.length() > 1 ? hash.substring(1) : "/";
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

                    ensureViewInstance(vsid);

                    viewScope.setState(map);
                }
            }
        }
    }

    private void ensureViewInstance(String vsid) {
        if (viewInstanceMap.containsKey(vsid))
            return;

        int colonIdx = vsid.indexOf(':');
        String viewId = colonIdx > 0 ? vsid.substring(0, colonIdx) : vsid;

        var factory = viewFactoryMap.get(viewId);
        if (factory == null) {
            Console.warn("No view factory for viewId: " + viewId);
            return;
        }

        var view = factory.apply(vsid);
        viewInstanceMap.put(vsid, view);

        var scope = viewMap.get(vsid);
        if (scope != null) {
            scope.setForceUpdate(view::forceUpdate);
        }

        viewGarbageCollector.mount(vsid);
    }

    // -- Form fields and event submission --

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

    public RemoteView getViewInstance(String vsid) {
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
        if (hash.startsWith("#") && hash.length() > 1) {
            String newPath = hash.substring(1);
            if (!newPath.equals(path)) {
                path = newPath;
                setFormField(BROWSER_VSID, "p.path", newPath);
                submit(BROWSER_VSID, -2);
            }
        }
    }

    // -- JS interop --

    private static String getLocationProtocol() {
        return Location.current().getProtocol();
    }

    private static String getLocationHost() {
        return Location.current().getHost();
    }

    private static String getLocationHash() {
        var hash = Location.current().getHash();
        return hash != null ? hash : "";
    }

    private static String generateId() {
        var bytes = WebCrypto.getRandomBytes(16);
        var sb = new StringBuilder(32);
        for (var b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
