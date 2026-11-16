package br.com.wdc.shopping.view.vaadin;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Configuração da página (app shell). O Vaadin exige que seja uma classe à parte, que não seja componente.
 * <p>
 * O Lumo é declarado aqui porque, desde o Vaadin 25, o tema padrão é o Aura, e o {@code styles/app.css} usa as
 * variáveis {@code --lumo-*}.
 */
@Push(PushMode.AUTOMATIC)
@StyleSheet(Lumo.STYLESHEET)
public class AppShell implements AppShellConfigurator {

    private static final long serialVersionUID = 1L;
}
