package br.com.wdc.shopping.view.vaadin;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

public class AppServiceInitListener implements VaadinServiceInitListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        RouteConfiguration configuration = RouteConfiguration.forApplicationScope();
        configuration.setRoute("", MainLayout.class);
    }
}
