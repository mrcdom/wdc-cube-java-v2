package br.com.wdc.shopping.view.remote.shell.cn1.views.login;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) da tela de login — espelha {@code views/login/_login.scss}. Herda os globais. */
public final class LoginSel extends Sel {

    public static final LoginSel INSTANCE = new LoginSel();

    private LoginSel() {
        // singleton
    }

    public final String LOGIN_CARD = "LoginCard";
    public final String LOGIN_HERO = "LoginHero";
    public final String LOGIN_BANNER = "LoginBanner";
    public final String LOGO_BOX = "LogoBox";
    public final String BANNER_TITLE = "BannerTitle";
    public final String BANNER_SUBTITLE = "BannerSubtitle";
    public final String FEATURE_TEXT = "FeatureText";
    public final String WELCOME_TITLE = "WelcomeTitle";
    public final String WELCOME_SUBTITLE = "WelcomeSubtitle";
    public final String FIELD_LABEL = "FieldLabel";
    public final String DEMO_HINT = "DemoHint";
    public final String DEMO_TEXT = "DemoText";
}
