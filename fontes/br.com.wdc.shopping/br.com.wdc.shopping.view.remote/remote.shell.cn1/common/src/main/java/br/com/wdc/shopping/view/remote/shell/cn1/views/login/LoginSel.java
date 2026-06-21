package br.com.wdc.shopping.view.remote.shell.cn1.views.login;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) da tela de login — espelha {@code views/login/_login.scss}. Herda os globais. */
public final class LoginSel extends Sel {

    public static final LoginSel INSTANCE = new LoginSel();

    private LoginSel() {
        // singleton
    }

    public final String LOGIN_CARD = "LgnCard";
    public final String LOGIN_HERO = "LgnHero";
    public final String LOGIN_BANNER = "LgnBanner";
    public final String LOGO_BOX = "LgnLogoBox";
    public final String BANNER_TITLE = "LgnBannerTitle";
    public final String BANNER_SUBTITLE = "LgnBannerSubtitle";
    public final String FEATURE_TEXT = "LgnFeatureText";
    public final String WELCOME_TITLE = "LgnWelcomeTitle";
    public final String WELCOME_SUBTITLE = "LgnWelcomeSubtitle";
    public final String FIELD_LABEL = "LgnFieldLabel";
    public final String DEMO_HINT = "LgnDemoHint";
    public final String DEMO_TEXT = "LgnDemoText";
}
