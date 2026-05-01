package br.com.wdc.shopping.view.vaadin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.shopping.presentation.presenter.Routes;

@Route("")
@Push(PushMode.AUTOMATIC)
public class MainLayout extends Div implements AppShellConfigurator {

    private static final long serialVersionUID = 4950391997168214131L;
    private static final Logger LOG = LoggerFactory.getLogger(MainLayout.class);
    
	private transient ShoppingVaadinApplication app;
    private transient ScheduledExecutorService executorService;

    public MainLayout() {
        setSizeFull();
        addClassName("main-layout");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        var ui = attachEvent.getUI();

        // Load application stylesheet
        ui.getPage().addStyleSheet("styles/app.css");

        this.executorService = Executors.newScheduledThreadPool(2);
        ScheduledExecutor.BEAN.set(new ScheduledExecutorVaadinAdapter(this.executorService, ui));

        // Try to restore application from cache (page refresh scenario)
        // Read hash synchronously via JS, then decide whether to restore or create new
        ui.getPage().executeJs("return window.location.hash")
                .then(String.class, hash -> {
                    if (hash != null && hash.startsWith("#") && hash.length() > 1) {
                        var signedIntent = hash.substring(1);
                        var cached = ShoppingVaadinApplication.restoreFromCache(signedIntent);
                        if (cached != null) {
                            // Restore existing app — presenters are preserved, only Vaadin components are recreated
                            this.app = cached;
                            this.app.reattach(ui, this);
                            var location = this.app.getIntentSigner().stripSignature(signedIntent);
                            LOG.info("Application restored from cache, re-navigating to: {}", location);
                            try {
                                this.app.go(location);
                            } catch (Exception e) {
                                LOG.error("Failed to restore navigation to: {}", location, e);
                            }
                            return;
                        }
                    }
                    // No cache hit — start fresh
                    initFreshApp(ui);
                });

        // Listen for browser back/forward via popstate on hash changes
        ui.getPage().executeJs(
                "window.addEventListener('popstate', () => {" +
                "  const hash = window.location.hash;" +
                "  if (hash && hash.length > 1) {" +
                "    $0.$server.onHashChanged(hash.substring(1));" +
                "  }" +
                "});", getElement());
    }

    private void initFreshApp(com.vaadin.flow.component.UI ui) {
        this.app = new ShoppingVaadinApplication(ui);
        this.app.setRootContainer(this);
        Routes.root(this.app);
    }

    @ClientCallable
    private void onHashChanged(String hash) {
        if (this.app != null && hash != null && !hash.isBlank()) {
            LOG.debug("Browser back/forward to: {}", hash);
            this.app.handleBrowserNavigation(hash);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (this.app != null) {
            // Only release if not cached (cached apps will be restored on refresh)
            if (!ShoppingVaadinApplication.isCached(this.app)) {
                this.app.release();
            }
            this.app = null;
        }

        if (this.executorService != null) {
            this.executorService.shutdownNow();
            this.executorService = null;
        }

        ScheduledExecutor.BEAN.set(null);
        super.onDetach(detachEvent);
    }
}
