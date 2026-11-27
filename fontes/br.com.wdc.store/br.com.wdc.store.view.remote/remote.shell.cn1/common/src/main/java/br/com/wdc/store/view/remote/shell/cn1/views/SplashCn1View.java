package br.com.wdc.store.view.remote.shell.cn1.views;

import java.util.function.Consumer;

import com.codename1.components.InfiniteProgress;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.store.view.remote.shell.cn1.Sel;
import br.com.wdc.store.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.store.view.remote.shell.cn1.i18n.StoreMessages;
import br.com.wdc.store.view.remote.shell.cn1.i18n.Texts;
import br.com.wdc.store.view.remote.shell.cn1.theme.Colors;
import br.com.wdc.store.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.store.view.remote.shell.cn1.util.Decor;
import br.com.wdc.store.view.remote.shell.cn1.util.Guard;

/**
 * "Brand" screen (splash) — blue gradient + logo + title, with a <b>spinner</b> (connecting) or an
 * error icon + "Retry now". A <b>local</b> view (no vsid/bridge), in the non-reactive pattern: the
 * tree is built <b>once</b> (spinner and button always present) and {@link #setState(State)}
 * mutates logo/status and <b>visibility</b> from the {@link State}.
 */
public final class SplashCn1View extends AbstractItemCn1View<SplashCn1View.State> {

    /** Splash parameters: error mode, status message and "try again" action (or {@code null}). */
    public static final class State {
        public final boolean error;
        public final String status;
        public final Runnable onRetry;

        public State(boolean error, String status, Runnable onRetry) {
            this.error = error;
            this.status = status;
            this.onRetry = onRetry;
        }
    }

    private static final Sel sel = Sel.INSTANCE;
    /** Logo glyph size (FontImage param). */
    private static final float LOGO_ICON = 13f;

    private Label logo;
    private Consumer<String> status;
    private Component spinner;
    private Button retry;
    private Runnable currentRetry = () -> { };
    private char currentIcon;

    @Override
    protected Container build() {
        return Cn1Dom.render(new FlowLayout(Component.CENTER, Component.CENTER), (dom, r) -> {
            Decor.blueWithCircles(r);
            dom.boxY(content -> {
                // centered logo (white icon) — the glyph is set in doUpdate (connecting/error)
                dom.container(new FlowLayout(Component.CENTER), null, box -> logo = dom.label(l -> {
                    l.getAllStyles().setFgColor(Colors.SURFACE);
                    l.getAllStyles().setBgTransparency(0); // Android: native Label is opaque white
                }));

                dom.label(l -> {
                    l.setUIID(sel.HERO_TITLE);
                    l.setText(Texts.text(StoreMessages.APP_NAME));
                });
                dom.spanLabel(s -> {
                    s.setTextUIID(sel.HERO_SUBTITLE);
                    s.setText(Texts.text(StoreMessages.APP_TAGLINE));
                });

                // spinner: always present, visible only while connecting (doUpdate)
                dom.container(new FlowLayout(Component.CENTER), null, box -> {
                    InfiniteProgress sp = new InfiniteProgress();
                    sp.setMaterialDesignMode(true);
                    sp.getAllStyles().setFgColor(Colors.SURFACE);
                    sp.getAllStyles().setBgTransparency(0); // Android: the progress native UIID is white
                    spinner = dom.add(sp, null);
                });

                dom.container(new FlowLayout(Component.CENTER), null,
                        box -> status = Guard.text(dom.label(l -> l.setUIID(sel.SPLASH_STATUS))));

                // "try again" button: always present, visible only in error mode (doUpdate)
                dom.container(new FlowLayout(Component.CENTER), null, box -> retry = dom.button(b -> {
                    b.setUIID(sel.SPLASH_RETRY);
                    b.setText(Texts.text(StoreMessages.CONNECTION_RETRY_NOW));
                    b.addActionListener(e -> currentRetry.run());
                }));
            });
        });
    }

    @Override
    protected void doUpdate() {
        State s = data;
        setIcon(s.error ? FontImage.MATERIAL_CLOUD_OFF : FontImage.MATERIAL_SHOPPING_BAG);
        status.accept(s.status);
        Guard.visible(spinner, !s.error);
        currentRetry = s.onRetry != null ? s.onRetry : () -> { };
        Guard.visible(retry, s.onRetry != null);
    }

    /** Swaps the logo glyph only when it changes (re-baking a FontImage re-marks the component).
 */
    private void setIcon(char icon) {
        if (icon != currentIcon) {
            currentIcon = icon;
            FontImage.setMaterialIcon(logo, icon, LOGO_ICON);
        }
    }
}
