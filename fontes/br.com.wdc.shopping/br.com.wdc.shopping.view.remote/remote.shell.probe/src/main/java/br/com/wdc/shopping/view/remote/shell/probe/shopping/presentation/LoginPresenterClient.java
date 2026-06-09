package br.com.wdc.shopping.view.remote.shell.probe.shopping.presentation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import br.com.wdc.framework.cube.remote.bridge.java.AbstractPresenterClient;
import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;

/**
 * Client mirror of {@code LoginPresenter} (classId {@value #CLASS_ID}).
 *
 * <pre>
 * Events:
 *   1 → onEnter(userName, password)   params: p.userName, p.password (AES-ciphered)
 * </pre>
 */
public class LoginPresenterClient extends AbstractPresenterClient {

    public static final String CLASS_ID = "c677cda52d14";
    
    public static Optional<LoginPresenterClient> getFirst(HostClient client) {
        return client.firstPresenterByClass(CLASS_ID, LoginPresenterClient.class);
    }

    public static List<LoginPresenterClient> getAll(HostClient client) {
        return client.presentersByClass(CLASS_ID, LoginPresenterClient.class);
    }

    public LoginPresenterClient(HostClient client, String vsid) {
        super(vsid, client);
    }

    // :: Events

    /**
     * Triggers {@code LoginPresenter.onEnter(userName, password)}.
     * The password is encrypted with the session AES key before transmission.
     */
    public HostResponse onEnter(String userName, String password)
            throws InterruptedException, TimeoutException {
        param("userName", userName);
        param("password", client.secretContext().encipher(password));
        return submit(1);
    }

    // :: State accessors

    public String userName() {
        var s = state();
        return s != null ? s.getString("userName") : null;
    }

    public boolean loading() {
        var s = state();
        return s != null && Boolean.TRUE.equals(s.getBoolean("loading"));
    }
}
