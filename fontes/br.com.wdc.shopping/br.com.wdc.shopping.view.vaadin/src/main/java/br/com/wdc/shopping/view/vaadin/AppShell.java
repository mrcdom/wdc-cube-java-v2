package br.com.wdc.shopping.view.vaadin;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Page configuration (app shell). Vaadin requires it to be a separate class that is not a component.
 * <p>
 * Lumo is declared here because, since Vaadin 25, the default theme is Aura, and {@code styles/app.css} uses the
 * {@code --lumo-*} variables.
 */
@Push(PushMode.AUTOMATIC)
@StyleSheet(Lumo.STYLESHEET)
public class AppShell implements AppShellConfigurator {

    private static final long serialVersionUID = 1L;
}
