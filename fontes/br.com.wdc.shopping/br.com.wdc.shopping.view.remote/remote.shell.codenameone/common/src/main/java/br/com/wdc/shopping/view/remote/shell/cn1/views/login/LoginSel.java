package br.com.wdc.shopping.view.remote.shell.cn1.views.login;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) da tela de login — espelha {@code views/login/_login.scss}. Herda os globais. */
public final class LoginSel extends Sel {

    public static final LoginSel INSTANCE = new LoginSel();

    private LoginSel() {
        // singleton
    }

    public final String LOGIN_CARD = "LoginCardLgn";
    public final String LOGIN_HERO = "LoginHeroLgn";
    public final String LOGIN_BANNER = "LoginBannerLgn";
    public final String LOGO_BOX = "LogoBoxLgn";
    public final String BANNER_TITLE = "BannerTitleLgn";
    public final String BANNER_SUBTITLE = "BannerSubtitleLgn";
    public final String FEATURE_TEXT = "FeatureTextLgn";
    public final String WELCOME_TITLE = "WelcomeTitleLgn";
    public final String WELCOME_SUBTITLE = "WelcomeSubtitleLgn";
    public final String FIELD_LABEL = "FieldLabelLgn";
    public final String DEMO_HINT = "DemoHintLgn";
    public final String DEMO_TEXT = "DemoTextLgn";
}
